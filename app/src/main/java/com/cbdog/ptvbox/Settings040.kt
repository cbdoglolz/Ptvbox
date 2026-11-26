package com.cbdog.ptvbox

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicInteger

@Composable internal fun SettingsCard(title:String,description:String="",content:@Composable ColumnScope.()->Unit){
    val c=LocalCinema.current
    Column(
        Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(10.dp))
            .border(1.dp,c.border,RoundedCornerShape(10.dp)).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)
    ){
        Text(title,color=c.text,fontSize=19.sp)
        if(description.isNotBlank())Text(description,color=c.muted,fontSize=12.sp,maxLines=3)
        content()
    }
}

@Composable private fun SettingChoices(m:PtvModel,title:String,key:String,options:List<Pair<String,String>>,default:String){
    m.preferencesRevision
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
        Text(title,color=LocalCinema.current.text,fontSize=14.sp)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            options.forEach{(value,label)->PcChip(label,m.config.text(key,default)==value,{m.settingChanged(key,value)})}
        }
    }
}

@Composable private fun SettingNumbers(m:PtvModel,title:String,key:String,options:List<Int>,default:Int,unit:String=""){
    m.preferencesRevision
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
        Text(title,color=LocalCinema.current.text,fontSize=14.sp)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            options.forEach{value->PcChip("$value$unit",m.config.number(key,default)==value,{m.settingChanged(key,value)})}
        }
    }
}

@Composable private fun SettingToggle(m:PtvModel,title:String,key:String,default:Boolean=false,description:String=""){
    m.preferencesRevision
    val c=LocalCinema.current
    val current=m.config.flag(key,default)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement=Arrangement.spacedBy(14.dp)
    ){
        Column(Modifier.weight(1f)){
            Text(title,color=c.text,fontSize=14.sp)
            if(description.isNotBlank())Text(description,color=c.muted,fontSize=12.sp,maxLines=2)
        }
        InputButton(
            {m.settingChanged(key,!current)},
            Modifier.border(1.dp,if(current)PcGreen.copy(alpha=.7f)else c.border,RoundedCornerShape(20.dp)),
            shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(20.dp)),
            focusRadius=20.dp,
            colors=ptvButtonColors(if(current)androidx.compose.ui.graphics.Color(0x332ee08c)else c.raised,if(current)PcGreen else c.muted),
            contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)
        ){
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Box(Modifier.width(30.dp).height(16.dp).background(if(current)PcGreen.copy(alpha=.45f)else c.border,RoundedCornerShape(10.dp)).padding(2.dp)){
                    Box(Modifier.size(12.dp).align(if(current)androidx.compose.ui.Alignment.CenterEnd else androidx.compose.ui.Alignment.CenterStart).background(if(current)PcGreen else c.muted,androidx.compose.foundation.shape.CircleShape))
                }
                Text(if(current)"开启"else"关闭",color=if(current)PcGreen else c.text.copy(alpha=.78f),fontSize=12.sp)
            }
        }
    }
}

@Composable internal fun CredentialRow(m:PtvModel,name:String,title:String){
    val field=ConfigPortal052.fields.firstOrNull{it.id==name}
    ConfigValueCard(
        title=title,
        value=m.configurationValues052[name].orEmpty(),
        emptyText="配置",
        note=if(field?.kind=="link")"保存后在这里显示完整地址" else "保存后在这里明文显示；存储仍由Android Keystore加密",
        onSave={m.saveCredential(name,it)},
        onClear={m.clearCredential(name)}
    )
}

@Composable internal fun Settings040(m:PtvModel){
    val context=LocalContext.current
    val c=LocalCinema.current
    val contentFocus=remember{FocusRequester()}
    val focusManager=LocalFocusManager.current
    val scope=rememberCoroutineScope()
    val section=m.settingsSection.takeIf{id->SettingsInventory.sections.any{it.id==id}}?:"remote"
    val sectionScrolls=SettingsInventory.sections.map{item->key(item.id){item.id to rememberSaveable(saver=LazyListState.Saver){LazyListState()}}}.toMap()
    val scroll=sectionScrolls.getValue(section)
    val addChannel=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){m.message("频道显示由系统自定义主屏幕管理")}
    var backupDialog by remember{mutableStateOf(false)}
    var cacheRefresh by remember{mutableIntStateOf(0)}
    var imageCacheBytes by remember{mutableLongStateOf(0L)}
    var metaCacheBytes by remember{mutableLongStateOf(0L)}
    val tmdbValue=m.configurationValues052["tmdb"].orEmpty()
    val tmdbHistory=ConfigHistory.items(m.db,"tmdb")
    LaunchedEffect(section,cacheRefresh){
        if(section=="network"){
            val stats=withContext(Dispatchers.IO){
                val img=CacheBudget101.dirBytes(java.io.File(context.cacheDir,"posters"))
                val meta=CacheBudget101.dirBytes(m.db.detailCacheDir)+CacheBudget101.dirBytes(m.db.episodeCacheDir055)+CacheBudget101.filesBytes(context.cacheDir.listFiles()?.filter{it.name.startsWith("anime-season056-")}.orEmpty())
                img to meta
            }
            imageCacheBytes=stats.first;metaCacheBytes=stats.second
        }
    }

    fun selectSection(id:String){
        if(m.settingsSection!=id){
            m.settingsSection=id
            m.settingChanged("settings_section",id)
        }
    }

    fun moveFocusToContent(){
        scope.launch{
            delay(40)
            runCatching{contentFocus.requestFocus()}.onFailure{runCatching{focusManager.moveFocus(FocusDirection.Right)}}
        }
    }

    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)){
        PcSection("设置","Ptvbox ${BuildConfig.VERSION_NAME} · ${m.config.budget.name}配置档位")
        Row(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(14.dp)){
            LazyColumn(Modifier.width(168.dp).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(6.dp)){
                items(SettingsInventory.sections,key={it.id}){item->
                    var leftFocused by remember(item.id){mutableStateOf(false)}
                    val configuredDot=when(item.id){
                        "subtitles","danmaku"->true
                        "metadata"->m.tmdbConfigured
                        "configuration"->m.configurationValues052["subscription"].orEmpty().isNotBlank()||m.configurationValues052["live_subscription"].orEmpty().isNotBlank()
                        "network"->m.configurationValues052["proxy"].orEmpty().isNotBlank()
                        else->false
                    }
                    InputButton(
                        {selectSection(item.id)},
                        Modifier.fillMaxWidth().heightIn(min=48.dp).border(1.dp,if(section==item.id)PcAccent.copy(alpha=.6f)else androidx.compose.ui.graphics.Color.Transparent,RoundedCornerShape(8.dp)).selection051(section==item.id)
                            .onFocusChanged{leftFocused=it.isFocused;if(it.isFocused)selectSection(item.id)}
                            .onKeyEvent{if(it.type==KeyEventType.KeyDown&&it.key==Key.DirectionRight){selectSection(item.id);moveFocusToContent();true}else false},
                        shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        focusRadius=8.dp,
                        colors=ptvButtonColors(if(section==item.id)c.raised else androidx.compose.ui.graphics.Color.Transparent,c.text),
                        contentPadding=PaddingValues(horizontal=12.dp,vertical=10.dp)
                    ){
                        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                            Text(item.title,color=if(section==item.id)PcAccent else c.muted,fontSize=13.sp,maxLines=2,modifier=Modifier.weight(1f).semantics{contentDescription=item.title+(if(leftFocused)" · 左侧焦点" else "")})
                            if(configuredDot)Text("●",color=PcGreen,fontSize=9.sp)
                        }
                    }
                }
            }
            LazyColumn(
                Modifier.weight(1f).focusRequester(contentFocus).focusRestorer(),
                state=scroll,
                verticalArrangement=Arrangement.spacedBy(12.dp),
                contentPadding=PaddingValues(3.dp,3.dp,8.dp,20.dp)
            ){
                item{
                    Row(Modifier.fillMaxWidth().padding(bottom=2.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        Text(SettingsInventory.sections.first{it.id==section}.title,color=c.text,fontSize=20.sp)
                        Box(Modifier.weight(1f).height(1.dp).background(c.border.copy(alpha=.6f)))
                    }
                }
                when(section){
                    "remote"->{
                        item{RemoteConfiguration(m)}
                        item{SettingsCard("数据导入与导出","PTVBOX-XFER 包含凭据、订阅、收藏、状态和进度；导入前必须预览确认。"){Action("备份 / 还原",{backupDialog=true})}}
                    }
                    "metadata"->{
                        item{
                            ConfigValueCard(
                                title="TMDB Key / Read Token",
                                value=tmdbValue,
                                history=tmdbHistory,
                                note="用于中文海报、标题艺术字和资料匹配；弹窗内完整明文展示与复制。",
                                onSave={m.saveTmdb(it)},
                                onClear={m.clearCredential("tmdb")},
                                onDeleteHistory={m.deleteTmdbHistory(it)},
                                onTest={m.testTmdb(it)}
                            )
                        }
                        item{SettingsCard("资料显示"){Text(m.keyStatus.ifBlank{"尚未配置TMDB"},color=if(m.tmdbConfigured)PcGreen else PcAccent,fontSize=13.sp);SettingToggle(m,"同时显示双平台评分","secondary_rating",true,"开启后在海报和详情页同时显示豆瓣与 TMDB 评分；关闭后仅显示主评分");SettingToggle(m,"不再提示配置TMDB","metadata_hint_hidden");Text("This product uses the TMDB API but is not endorsed or certified by TMDB.",color=c.muted,fontSize=11.sp)}}
                    }
                    "interface"->{
                        item{SettingsCard("主题与海报操作","点选海报先进入详情，播放按钮才开始播放。"){FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){PcChip("奈飞 · 蓝黑紫",!m.lightTheme,{m.settingChanged("light_theme",false)});PcChip("豆瓣 · 纯白",m.lightTheme,{m.settingChanged("light_theme",true)})};Text("海报与片名进入作品详情；播放操作只从详情或播放器开始。",color=c.muted,fontSize=12.sp)}}
                        item{SettingsCard("界面"){SettingToggle(m,"始终显示导航文字","drawer_pinned",false);SettingChoices(m,"搜索结果样式","search_display",listOf("sites" to "站点切换","groups" to "分组纵览"),"sites");SettingToggle(m,"进入详情预加载普通线路","preload_catalog",false,"关闭后点击“查找普通线路与选集”再查询")}}
                        item{
                            val notifOn=NotificationGuide102.notificationsEnabled(context)
                            SettingsCard("系统通知与插件弹窗说明","Android 13+ 默认关闭通知；Android 11 关闭系统通知仅拦截通知栏（前台插件文字气泡由系统直接渲染）。Ptvbox 已自动跳过未授权网盘站并在起播后立刻停扫以大幅减少插件弹窗。"){
                                Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                                    Text(if(notifOn)"当前状态：系统通知已开启"else"✓ 当前状态：系统通知已关闭",color=if(notifOn)androidx.compose.ui.graphics.Color(0xFFFBBF24)else PcGreen,fontSize=13.sp,modifier=Modifier.weight(1f))
                                    Action(if(notifOn)"前往关闭通知"else"系统通知设置",{if(!NotificationGuide102.openSystemNotificationSettings(context))m.message("请前往系统设置 → 应用 → Ptvbox → 通知")})
                                }
                            }
                        }
                    }
                    "configuration"->{
                        item{ConfigValueCard("私人TVBox点播订阅地址",m.configurationValues052["subscription"].orEmpty(),"配置",note="保存后立即导入，并在下面显示完整地址。",onSave={m.importSources(it)},onClear={m.clearStoredConfiguration("subscription")})}
                        item{ConfigValueCard("直播订阅地址",m.configurationValues052["live_subscription"].orEmpty(),"配置",note="支持TXT、M3U或含lives的TVBox JSON。",onSave={m.saveLiveConfiguration(it)},onClear={m.clearStoredConfiguration("live_subscription")})}
                    }
                    "playback"->{
                        item{SettingsCard("自动选源","先按站点分组优先级，再按当前播放偏好检测线路；Netflix 作品优先搜国际高清（HDHUB）。"){Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){PcChip("速度优先",m.preference=="speed",{m.changePreference("speed")});PcChip("高清优先",m.preference=="quality",{m.changePreference("quality")})};SettingNumbers(m,"搜索并发","search_parallel",listOf(4,6,10,12,20),m.config.budget.parallel,"路");Column(verticalArrangement=Arrangement.spacedBy(7.dp)){Text("找到可用源后自动停扫",color=c.text,fontSize=14.sp);FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){val curStop=m.config.sourceStopCount;listOf(1 to "1条",2 to "2条",3 to "3条",5 to "5条",8 to "8条",0 to "不限制").forEach{(n,label)->PcChip(label,curStop==n,{m.settingChanged("source_stop_count",n)})}}}}}
                        item{
                            m.preferencesRevision
                            val a1=m.config.audioPrimaryLang;val a2=m.config.audioSecondaryLang
                            SettingsCard("音频自动加载优先级","按首选 → 次选 → 片源默认自动匹配音轨；单部影片手动切换后会记住该片选择。"){
                                SettingChoices(m,"首选音轨语言","audio_lang_primary",TrackLanguagePolicy102.audioOptions,"zh")
                                SettingChoices(m,"次选音轨语言","audio_lang_secondary",TrackLanguagePolicy102.audioOptions,"en")
                                Text("当前策略：${TrackLanguagePolicy102.audioLabelOf(a1)} → ${TrackLanguagePolicy102.audioLabelOf(a2)} → 片源默认",color=PcAccent,fontSize=12.sp)
                            }
                        }
                        item{SettingsCard("播放与快进","遥控器左右键和进度条采用防抖预览合并，停顿后一次性跳转。"){FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){listOf("原始 / 自适应","拉伸","填充裁切").forEachIndexed{i,label->PcChip(label,m.resizeChoice==i,{m.resizeChoice=i;m.settingChanged("resize_choice",i)})}};SettingNumbers(m,"单步快进 / 快退","seek_step_seconds",listOf(5,10,15,30),5,"秒");SettingNumbers(m,"起播超时","startup_timeout",listOf(5,10,15,30,60),30,"秒");SettingToggle(m,"自动跳过片头片尾","auto_skip");SettingNumbers(m,"默认片头","skip_intro",listOf(0,30,60,90,120),0,"秒");SettingNumbers(m,"默认片尾","skip_outro",listOf(0,30,60,90,120),0,"秒")}}
                        item{SettingsCard("离开应用时","默认保存并停止。开启后台播放后，系统HOME保留媒体服务。"){SettingToggle(m,"允许系统后台播放","background_playback")}}
                    }
                    "subtitles"->{
                        item{
                            m.preferencesRevision
                            val s1=m.config.subtitlePrimaryLang;val s2=m.config.subtitleSecondaryLang
                            SettingsCard("字幕自动加载优先级","按首选 → 次选 → 片源默认自动加载内嵌字幕，并按同一优先级置顶在线字幕结果。"){
                                SettingChoices(m,"首选字幕语言","subtitle_lang_primary",TrackLanguagePolicy102.subtitleOptions,"zh")
                                SettingChoices(m,"次选字幕语言","subtitle_lang_secondary",TrackLanguagePolicy102.subtitleOptions,"en")
                                Text("当前策略：${TrackLanguagePolicy102.labelOf(s1)} → ${TrackLanguagePolicy102.labelOf(s2)} → 片源默认",color=PcAccent,fontSize=12.sp)
                            }
                        }
                        item{SettingsCard("内嵌字幕样式","图片字幕由解码器显示，不能更换字体。"){SubtitlePreview040(m);SettingToggle(m,"使用自定义内嵌字幕样式","subtitle_style_override");SettingNumbers(m,"字幕字号","subtitle_size",listOf(16,20,24,28,32,40),24,"sp");SettingChoices(m,"字体","subtitle_font",listOf("sans-serif" to "系统黑体","serif" to "衬线","monospace" to "等宽"),"sans-serif");SettingToggle(m,"粗体","subtitle_bold",true);SettingChoices(m,"文字颜色","subtitle_color",listOf("#FFFFFF" to "白","#FFF176" to "黄","#80DEEA" to "青","#A5D6A7" to "绿"),"#FFFFFF");SettingNumbers(m,"文字不透明度","subtitle_alpha",listOf(40,60,80,100),100,"%");SettingNumbers(m,"背景不透明度","subtitle_background_alpha",listOf(0,20,40,60,80,100),40,"%")}}
                        item{SettingsCard("免费公共字幕 · 无需Key","OpenSubtitles V3 公共聚合优先置顶。公共服务的可用性、中文覆盖和限流由提供方决定。"){Text("✓ 已配置 · 无需账号或API Key",color=PcGreen,fontSize=13.sp)}}
                        item{CredentialRow(m,"subdl","SubDL Key")}
                        item{CredentialRow(m,"assrt","ASSRT Token")}
                        item{CredentialRow(m,"opensub_key","OpenSubtitles API Key")}
                        item{CredentialRow(m,"opensub_user","OpenSubtitles账号")}
                        item{CredentialRow(m,"opensub_password","OpenSubtitles密码")}
                    }
                    "danmaku"->{
                        item{SettingsCard("弹幕显示样式","修改实时生效并保存；右侧预览与播放器使用同一组参数。"){BoxWithConstraints(Modifier.fillMaxWidth()){
                            val controls:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){SettingNumbers(m,"字号","danmaku_size",listOf(12,16,20,24,28,32,40),20,"sp");SettingNumbers(m,"不透明度","danmaku_alpha",listOf(40,60,80,100),80,"%");SettingNumbers(m,"密度","danmaku_density",listOf(4,8,12,16),12);SettingNumbers(m,"横穿秒数","danmaku_duration",listOf(6,8,10,12),8,"秒");SettingNumbers(m,"横向飞行距离","danmaku_travel_percent",listOf(33,50,67,75,100),100,"%");SettingChoices(m,"颜色","danmaku_color",listOf("#FFFFFF" to "白","#FFF176" to "黄","#80DEEA" to "青","#A5D6A7" to "绿"),"#FFFFFF")}}
                            if(maxWidth>=760.dp)Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=androidx.compose.ui.Alignment.Top){Box(Modifier.weight(1f)){controls()};DanmakuPreview060(m,Modifier.width(390.dp).height(260.dp))}
                            else Column(verticalArrangement=Arrangement.spacedBy(10.dp)){controls();DanmakuPreview060(m,Modifier.fillMaxWidth().height(210.dp))}
                        }}}
                        item{SettingsCard("免费公共弹幕 · 无需配置","以下服务已内置并提供自动回退；公共服务的可用性和版权状态由提供方决定。"){Text("✓ 已配置 · 公共兼容服务默认置顶",color=PcGreen,fontSize=13.sp);Text("✓ 已配置 · B站公开弹幕自动回退",color=PcGreen,fontSize=13.sp)}}
                        item{CredentialRow(m,"danmaku_url","自建弹幕服务地址")}
                        item{CredentialRow(m,"danmaku_appid","弹弹play AppId")}
                        item{CredentialRow(m,"danmaku_secret","弹弹play AppSecret")}
                    }
                    "network"->{
                        item{CredentialRow(m,"proxy","网络代理地址")}
                        item{
                            val limitBytes=m.config.imageMb*1024L*1024L
                            val usedMb=String.format(java.util.Locale.US,"%.1f",(imageCacheBytes+metaCacheBytes)/(1024.0*1024.0))
                            val imgMb=String.format(java.util.Locale.US,"%.1f",imageCacheBytes/(1024.0*1024.0))
                            val metaMb=String.format(java.util.Locale.US,"%.1f",metaCacheBytes/(1024.0*1024.0))
                            val ratio=if(limitBytes>0)(imageCacheBytes.toFloat()/limitBytes.toFloat()).coerceIn(0f,1f)else 0f
                            SettingsCard("网络与缓存","纯空间上限 LRU 淘汰策略：常用海报、直播台标与作品资料永久保留，超出上限自动清理最久未用内容，不删除收藏、进度、密钥或订阅。"){
                                Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
                                    Text("当前缓存占用：$usedMb MiB（图片与台标 $imgMb / ${m.config.imageMb} MiB · 资料与目录 $metaMb MiB）",color=PcAccent,fontSize=12.sp)
                                    PcProgress(ratio)
                                }
                                SettingNumbers(m,"站点超时","site_timeout",listOf(5,10,15,30,60),15,"秒")
                                SettingToggle(m,"安全DNS（DoH）","doh",false,"Cloudflare DNS over HTTPS，默认关闭")
                                SettingNumbers(m,"图片与台标缓存上限","image_cache_mb",listOf(64,128,256,512,1024),m.config.budget.imageMb," MiB")
                                Action("清空图片与临时缓存",{m.clearImageCache();scope.launch{delay(600);cacheRefresh++}})
                                Text("第三方Jar内部请求可能不服从宿主网络设置。",color=c.muted,fontSize=12.sp)
                            }
                        }
                    }
                    "about"->{item{SettingsCard("关于 Ptvbox","密码与密钥始终明文显示；磁盘保存仍使用Android Keystore加密。"){Text("Ptvbox ${BuildConfig.VERSION_NAME}\nAndroid TV · 支持触摸、鼠标和遥控器\nMedia3 / Android MediaCodec / SQLite / Android Keystore",color=c.text,fontSize=15.sp);Text("使用PCBOX页面设计与业务顺序，采用Android TV平台实现。",color=c.muted,fontSize=12.sp);AppUpdate040(m);Text("This product uses the TMDB API but is not endorsed or certified by TMDB.",color=c.muted,fontSize=11.sp)}}}
                }
            }
        }
    }
    if(backupDialog)Backup040(m){backupDialog=false}
}

@Composable internal fun ExternalButton(label:String,address:String){
    val context=LocalContext.current
    Action(label,{runCatching{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(address)))}})
}

@Composable private fun Backup040(m:PtvModel,close:()->Unit){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var status by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var replaceConfiguration by remember{mutableStateOf(false)}
    var candidate by remember{mutableStateOf<JSONObject?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)scope.launch{busy=true;try{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"w")!!.use{it.write(UserBackup.encodeTransfer(m.db))}};status="无密码迁移文件已写入所选位置"}catch(_:Exception){status="导出失败，用户数据未改变"}finally{busy=false}}
    }
    val import=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch{busy=true;try{candidate=withContext(Dispatchers.IO){val data=context.contentResolver.openInputStream(uri)!!.use{it.readBytes()};UserBackup.decodeTransfer(data)};status=UserBackup.summary(candidate!!)}catch(_:Exception){candidate=null;status="文件损坏、版本不支持或不是PTVBOX-XFER文件；现有数据保留"}finally{busy=false}}
    }
    Dialog({if(!busy)close()},properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(Modifier.widthIn(max=650.dp).fillMaxWidth(.88f).background(LocalCinema.current.surface,RoundedCornerShape(12.dp)).padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            PcSection("无密码迁移")
            Text("文件包含凭据、订阅、收藏、状态和进度，请只保存在可信设备。",color=LocalCinema.current.muted,fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Action("导出PTVBOX-XFER",{if(!busy)runCatching{export.launch("Ptvbox-${BuildConfig.VERSION_NAME}-${java.time.LocalDate.now()}-portable.json")}.onFailure{status="此设备未提供文件选择器"}})
                Action("打开文件并预览",{if(!busy)runCatching{import.launch(arrayOf("application/json","text/plain","*/*"))}.onFailure{status="此设备未提供文件选择器"}})
            }
            Text(if(busy)"正在处理，请稍候…"else status,color=PcAccent,fontSize=13.sp)
            candidate?.let{data->
                PcChip(if(replaceConfiguration)"以备份替换现有配置和凭据 ✓"else"保留现有配置和凭据",replaceConfiguration,{replaceConfiguration=!replaceConfiguration})
                Action("确认合并还原",{if(!busy)scope.launch{busy=true;try{val count=withContext(Dispatchers.IO){UserBackup.restore(m.db,data,replaceConfiguration)};m.configurationChanged();status="已合并${count}条记录";candidate=null}catch(_:Exception){status="还原未完成，原有记录未清除"}finally{busy=false}}})
            }
            Action("关闭",{if(!busy)close()})
        }
    }
}
