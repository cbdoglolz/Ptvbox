package com.cbdog.ptvbox

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.media3.common.*
import androidx.tv.material3.Text
import kotlinx.coroutines.*

@Composable internal fun SubtitlePanel053(m:PtvModel,close:()->Unit){val a=m.addons;val context=LocalContext.current;val scope=rememberCoroutineScope();var page by remember{mutableStateOf("results")};m.preferencesRevision
 val local=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{try{val bytes=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{MediaText053.bounded(it,5_000_000)}};a.local(bytes,uri.lastPathSegment.orEmpty())}catch(_:Exception){m.message("字幕导入失败：文件无法读取或超过5MB")}}}
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){val window=(LocalView.current.parent as? DialogWindowProvider)?.window;DisposableEffect(window){val old=window?.attributes?.dimAmount;window?.setDimAmount(0f);onDispose{old?.let{window.setDimAmount(it)}}};Column(Modifier.widthIn(max=680.dp).fillMaxWidth(.88f).heightIn(max=450.dp).background(Color(0x66191829),RoundedCornerShape(12.dp)).border(1.dp,Color(0xff76658f),RoundedCornerShape(12.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("字幕 · 中文字幕 ${a.results.size}条",color=PcAccent,fontSize=18.sp);PcChip("关闭",false,close)}
  Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){PcChip("搜索结果 ${a.results.size}",page=="results",{page="results"});PcChip("样式",page=="style",{page="style"});PcChip("校准时间轴",page=="timing",{page="timing"});PcChip(if(a.searching)"搜索中…"else"搜索中文字幕",false,a::search)}
  if(page=="style")SubtitlePreview040(m)
  Text(a.status.ifBlank{"内嵌字幕可直接选；在线字幕请核对年份、季集和压制版本"},color=Color(0xffeadcf8),fontSize=11.sp,maxLines=2)
  LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(6.dp)){
   if(page=="results"){
    if(a.selectedTitle.isNotBlank())item{Text("上次使用 · ${a.selectedTitle}",color=PcAccent,fontSize=12.sp,maxLines=2)}
    if(a.files.isNotEmpty()){item{Text("已展开字幕包 · 请选择字幕文件（${a.files.size}个）",color=PcAccent,fontSize=12.sp)};items(a.files,key={it.key}){s->Action((if(s.title==a.selectedTitle)"上次使用 · "else"")+"字幕文件 · "+s.title,{a.chooseFile(s)},Modifier.fillMaxWidth(),maxLines=3)}}
    items(a.results,key={it.key}){s->Action((if(s.key==a.selected||s.title==a.selectedTitle)"上次使用 · "else"")+"${if(s.archive)"字幕包 / 压缩包 · 点击展开文件"else"字幕文件 · 点击加载"} · ${s.provider} · ${s.title}\n${s.detail}",{a.choose(s)},Modifier.fillMaxWidth(),selected=s.key==a.selected,maxLines=3)}
    item{Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){PcChip("导入本地字幕",false,{runCatching{local.launch(arrayOf("*/*"))}.onFailure{m.message("当前设备没有文件选择器")}});PcChip("字幕服务配置",false,{close();m.settingsSection="subtitles";m.screen="settings"});PcChip("关闭所有字幕",false,a::off)}}
    items(m.player.currentTracks.groups.filter{it.type==C.TRACK_TYPE_TEXT}.flatMap{g->(0 until g.length).map{g to it}}){(g,i)->val f=g.getTrackFormat(i);Action("内嵌 · ${TrackLabels056.label(f.language,f.label,f.codecs,f.sampleMimeType,-1,i+1)}",{a.embedded();m.player.trackSelectionParameters=m.player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup,listOf(i))).build()},Modifier.fillMaxWidth(),selected=a.cues.isEmpty()&&g.isTrackSelected(i)&&C.TRACK_TYPE_TEXT !in m.player.trackSelectionParameters.disabledTrackTypes)}
   }else if(page=="style"){
    item{Text("修改实时生效 · 图片字幕不支持换字体 · ASS保留文本与时间轴",color=PcAccent,fontSize=11.sp)}
    item{FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
     Option053(m,"字号","subtitle_size",listOf(16,20,24,28,32,40),24);PcChip(if(m.config.flag("subtitle_bold",true))"粗体 ✓"else"粗体",m.config.flag("subtitle_bold",true),{m.settingChanged("subtitle_bold",!m.config.flag("subtitle_bold",true));m.settingChanged("subtitle_style_override",true)})
     val fonts=listOf("sans-serif","serif","monospace");val font=m.config.text("subtitle_font","sans-serif");PcChip("字体：$font",false,{m.settingChanged("subtitle_font",fonts[(fonts.indexOf(font)+1)%fonts.size]);m.settingChanged("subtitle_style_override",true)})
     val colors=listOf("#FFFFFF","#FFF176","#80DEEA","#A5D6A7","#FFAB91");val col=m.config.text("subtitle_color","#FFFFFF");PcChip("字色：$col",false,{m.settingChanged("subtitle_color",colors[(colors.indexOf(col)+1)%colors.size]);m.settingChanged("subtitle_style_override",true)})
     val backgrounds=listOf("#000000","#202040","#FFFFFF");val back=m.config.text("subtitle_background_color","#000000");PcChip("背景：$back",false,{m.settingChanged("subtitle_background_color",backgrounds[(backgrounds.indexOf(back)+1)%backgrounds.size])})
     Option053(m,"文字不透明度","subtitle_alpha",listOf(40,60,80,100),100);Option053(m,"背景不透明度","subtitle_background_alpha",listOf(0,20,40,60,80,100),40)
    }}
   }else{
    item{Text("延后 ${a.delayMs/1000.0} 秒（负数为提前）· 点一句将其对准当前画面",color=PcAccent,fontSize=12.sp)}
    item{FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){PcChip("字幕太早：延后0.5秒",false,{a.adjust(500)});PcChip("字幕太晚：提前0.5秒",false,{a.adjust(-500)});PcChip("重置偏移",false,{a.adjust(-a.delayMs)})}}
    items(a.cues){cue->Action("${clock(cue.start)} · ${cue.text}",{a.align(cue)},Modifier.fillMaxWidth(),maxLines=2)}
   }
  }
 }}
}
@Composable internal fun Option053(m:PtvModel,title:String,key:String,options:List<Int>,default:Int){var n by remember(key){mutableIntStateOf(m.config.number(key,default))};val focus=remember(key){FocusRequester()};var hadFocus by remember(key){mutableStateOf(false)};LaunchedEffect(n){if(hadFocus){withFrameNanos{};delay(60);runCatching{focus.requestFocus()}}};PcChip("$title $n",false,{val next=options[(options.indexOf(n).coerceAtLeast(0)+1)%options.size];n=next;m.settingChanged(key,next);if(key.startsWith("subtitle_"))m.settingChanged("subtitle_style_override",true)},Modifier.focusRequester(focus).onFocusChanged{if(it.isFocused)hadFocus=true})}
@Composable internal fun DanmakuPanel053(m:PtvModel,close:()->Unit){
 val a=m.addons;val context=LocalContext.current;val scope=rememberCoroutineScope()
 var pendingFocus by remember{mutableStateOf("")};val focusers=remember{mutableMapOf<String,FocusRequester>()}
 var areaValue by remember{mutableIntStateOf(m.config.number("danmaku_area",50))};var travelValue by remember{mutableIntStateOf(m.config.number("danmaku_travel_percent",100))};var enabledValue by remember{mutableStateOf(m.config.flag("danmaku_enabled",true))}
 LaunchedEffect(pendingFocus){if(pendingFocus.isNotBlank()){withFrameNanos{};delay(60);runCatching{focusers[pendingFocus]?.requestFocus()}}}
 val local=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch{try{val bytes=withContext(Dispatchers.IO){context.contentResolver.openInputStream(uri)!!.use{MediaText053.bounded(it,5_000_000)}};require(bytes.size<=5_000_000);a.localDanmaku(bytes)}catch(_:Exception){m.message("弹幕导入失败或文件过大")}}}
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Column(Modifier.widthIn(max=660.dp).fillMaxWidth(.85f).heightIn(max=(androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp-30).coerceIn(180,440).dp).background(Color(0xc0191829),RoundedCornerShape(12.dp)).padding(16.dp).focusRestorer(),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("弹幕 · 弹弹play开放弹幕网络",color=PcAccent,fontSize=18.sp);PcChip("关闭",false,close)}
   LazyColumn(Modifier.weight(1f,false),verticalArrangement=Arrangement.spacedBy(8.dp)){
    item{Text(a.danmakuStatus,color=Color.White,fontSize=12.sp,maxLines=3)}
    item{Text("点击参数立即生效并保存",color=PcAccent,fontSize=11.sp)}
    item{Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text("显示区域",color=PcAccent,fontSize=11.sp);FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){listOf(25 to "区域 1/4",50 to "区域 1/2",75 to "区域 3/4",100 to "区域全屏").forEach{(value,label)->val active=areaValue==value;val focusKey="area:$value";val focus=remember(focusKey){FocusRequester()};DisposableEffect(focus){focusers[focusKey]=focus;onDispose{if(focusers[focusKey]===focus)focusers.remove(focusKey)}};PcChip(label,active,{areaValue=value;m.settingChanged("danmaku_area",value);pendingFocus=focusKey},Modifier.focusRequester(focus).onFocusChanged{if(it.isFocused)pendingFocus=focusKey})}}}}
    item{Column(verticalArrangement=Arrangement.spacedBy(4.dp)){Text("横向飞行距离",color=PcAccent,fontSize=11.sp);FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){listOf(33 to "距离 1/3",50 to "距离 1/2",67 to "距离 2/3",75 to "距离 3/4",100 to "距离全屏").forEach{(value,label)->val active=travelValue==value;val focusKey="travel:$value";val focus=remember(focusKey){FocusRequester()};DisposableEffect(focus){focusers[focusKey]=focus;onDispose{if(focusers[focusKey]===focus)focusers.remove(focusKey)}};PcChip(label,active,{travelValue=value;m.settingChanged("danmaku_travel_percent",value);pendingFocus=focusKey},Modifier.focusRequester(focus).onFocusChanged{if(it.isFocused)pendingFocus=focusKey})}}}}
    item{FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
     PcChip(if(enabledValue)"弹幕已启用"else"弹幕已关闭",enabledValue,{val next=!enabledValue;enabledValue=next;m.settingChanged("danmaku_enabled",next);if(next&&a.danmaku.isEmpty())a.searchDanmaku()})
     PcChip("重新匹配弹幕",false,a::searchDanmaku)
     PcChip("导入XML/JSON",false,{runCatching{local.launch(arrayOf("*/*"))}})
     PcChip("服务配置",false,{close();m.settingsSection="danmaku";m.screen="settings"})
     Option053(m,"字号","danmaku_size",listOf(12,16,20,24,28,32,40),20)
     Option053(m,"不透明度","danmaku_alpha",listOf(40,60,80,100),80)
     Option053(m,"密度","danmaku_density",listOf(4,8,12,16),12)
     Option053(m,"横穿秒数","danmaku_duration",listOf(6,8,10,12),8)
     Option053(m,"延后秒数","danmaku_delay",listOf(-10,-5,0,5,10),0)
    }}
    items(a.matches,key={it.id}){match->Action("${match.title} · ${match.episode}",{a.loadDanmaku(match)},Modifier.fillMaxWidth(),maxLines=2)}
   }
  }
 }
}
