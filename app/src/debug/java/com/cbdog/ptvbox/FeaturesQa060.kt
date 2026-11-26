package com.cbdog.ptvbox

import android.app.Instrumentation
import android.graphics.Rect
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject

object FeaturesQa060 {
 private fun render(i:Instrumentation,a:MainActivity,body:@Composable ()->Unit){i.runOnMainSync{a.setContent{CinemaTheme(false){Box(Modifier.fillMaxSize().background(LocalCinema.current.background)){body()}}}};Thread.sleep(500)}

 private fun windowTexts(i:Instrumentation):List<String>{
  val values=mutableListOf<String>()
  fun visit(node:AccessibilityNodeInfo?){if(node==null)return;node.text?.toString()?.let(values::add);node.contentDescription?.toString()?.let(values::add);for(index in 0 until node.childCount)visit(node.getChild(index))}
  i.uiAutomation.windows.forEach{visit(it.root)}
  return values
 }
 private fun focusedLabels(i:Instrumentation):List<String>{
  val values=mutableListOf<String>()
  fun visit(node:AccessibilityNodeInfo?){if(node==null)return;if(node.isFocused){node.contentDescription?.toString()?.let(values::add);node.text?.toString()?.let(values::add)};for(index in 0 until node.childCount)visit(node.getChild(index))}
  i.uiAutomation.windows.forEach{visit(it.root)}
  return values
 }
 private fun clickAccessible(i:Instrumentation,label:String){
  fun visit(node:AccessibilityNodeInfo?):AccessibilityNodeInfo?{if(node==null)return null;if(node.contentDescription?.toString()==label||node.text?.toString()==label)return node;for(index in 0 until node.childCount)visit(node.getChild(index))?.let{return it};return null}
  val found=i.uiAutomation.windows.firstNotNullOfOrNull{visit(it.root)}?:error("Missing accessibility target: $label")
  var target:AccessibilityNodeInfo?=found
  while(target!=null&&!target.isClickable)target=target.parent
  check(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true){"Accessibility click failed: $label"}
  Thread.sleep(300)
 }

 private fun intersects(rect:Rect,screen:Rect)=rect.bottom>screen.top&&rect.top<screen.bottom&&rect.right>screen.left&&rect.left<screen.right
 private fun scrollDown(i:Instrumentation){i.uiAutomation.executeShellCommand("input swipe 1500 920 1500 260 500").close();Thread.sleep(850)}
 private fun touch(i:Instrumentation,action:Int,x:Float,y:Float,start:Long){val p=arrayOf(MotionEvent.PointerProperties().apply{id=0;toolType=MotionEvent.TOOL_TYPE_FINGER});val c=arrayOf(MotionEvent.PointerCoords().apply{this.x=x;this.y=y;pressure=if(action==MotionEvent.ACTION_UP)0f else 1f;size=1f});val e=MotionEvent.obtain(start,android.os.SystemClock.uptimeMillis(),action,1,p,c,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);try{i.uiAutomation.injectInputEvent(e,true)}finally{e.recycle()}}
 private fun tap(i:Instrumentation,x:Float,y:Float){val t=android.os.SystemClock.uptimeMillis();touch(i,MotionEvent.ACTION_DOWN,x,y,t);Thread.sleep(35);touch(i,MotionEvent.ACTION_UP,x,y,t);Thread.sleep(70)}
 private fun hold(i:Instrumentation,x:Float,y:Float,ms:Long){val t=android.os.SystemClock.uptimeMillis();touch(i,MotionEvent.ACTION_DOWN,x,y,t);Thread.sleep(ms);touch(i,MotionEvent.ACTION_UP,x,y,t);Thread.sleep(150)}
 private fun drag(i:Instrumentation,fromX:Float,toX:Float,y:Float){val t=android.os.SystemClock.uptimeMillis();touch(i,MotionEvent.ACTION_DOWN,fromX,y,t);for(step in 1..10){Thread.sleep(18);touch(i,MotionEvent.ACTION_MOVE,fromX+(toX-fromX)*step/10f,y,t)};touch(i,MotionEvent.ACTION_UP,toX,y,t);Thread.sleep(250)}

 fun episodes(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
  val out=JSONObject()
  val fixture=TmdbFixture055(m)
  val work=Work("1396","绝命毒师","tv","2008",provider="tmdb",season=1,identityKey="qa060-episode-ui")
  val cache=m.metadata.episodeCache055(work)
  val original=java.io.File(cache.parentFile,cache.name+".qa060-original")
  if(cache.exists())cache.renameTo(original)
  try{
   i.runOnMainSync{m.detail=work;m.screen="detail";m.details.load(work)}
   render(i,a){CinemaDetail040(m)}
   UiQa050.await(40000){m.details.series.value?.episodes?.firstOrNull()?.let{!it.pending&&it.overview.isNotBlank()}==true}
   UiQa050.await(10000){InputQa.hasLabel(i,"单集卡片 1")}

   val screen=i.uiAutomation.takeScreenshot()!!.let{Rect(0,0,it.width,it.height)}
   var visibleCard:Rect?=null
   for(step in 1..6){scrollDown(i);visibleCard=UiQa054.rect(i,"单集卡片 1");if(visibleCard?.let{intersects(it,screen)}==true)break}
   check(visibleCard?.let{intersects(it,screen)}==true){"episode card was not visible after scrolling: $visibleCard"}
   val card=visibleCard
   val marker=UiQa054.rect(i,"第1集观看标记")?:error("round watch marker missing")
   val progress=UiQa054.rect(i,"未播放")?:error("episode progress row missing")
   check(!InputQa.hasLabel(i,"收藏第1集"))
   check(card.width()<410&&card.height()<390){"episode card was not compressed: $card"}
   check(marker.width() in 35..70&&marker.left>card.centerX()&&marker.bottom<=card.bottom+2){"watch marker is not a compact bottom-right circle: marker=$marker card=$card"}
   check(kotlin.math.abs(marker.centerY()-progress.centerY())<25){"marker and playback progress are not aligned: marker=$marker progress=$progress"}
   out.put("cardWidthPx",card.width()).put("cardHeightPx",card.height()).put("favoriteButtonRemoved",true).put("roundBottomRightMarker",true).put("markerAlignedWithProgress",true)

   clickAccessible(i,"第1集观看标记")
   UiQa050.await(5000){InputQa.hasLabel(i,"未标记")}
   for(label in listOf("已看","在看","想看","未标记")){val r=UiQa054.rect(i,label)!!;check(intersects(r,screen)){"$label is outside the usable screen: $r"}}
   check(!InputQa.hasLabel(i,"关闭"))
   UiQa050.shot(i,"qa060-watch-picker-compact.png")
   i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(350)
   UiQa050.await(3000){!InputQa.hasLabel(i,"只改变标记 · 点外部或按返回关闭")}
   out.put("watchPickerAllOptionsVisibleWithoutCloseButton",true)

   var synopsis=UiQa054.rect(i,"第1集简介")
   if(synopsis?.let{intersects(it,screen)}!=true)scrollDown(i)
   synopsis=UiQa054.rect(i,"第1集简介")?:error("synopsis panel missing")
   check(intersects(synopsis,screen)){"synopsis is outside the visible page: $synopsis"}
   val episode=m.details.series.value!!.episodes.first()
   check(windowTexts(i).any{it.contains(episode.overview.take(24))}){"synopsis text is not rendered"}
   UiQa050.shot(i,"qa060-episode-synopsis-focused.png")
   out.put("selectedEpisodeSynopsisVisible",true).put("synopsisTopPx",synopsis.top).put("synopsisBottomPx",synopsis.bottom)

   render(i,a){SeasonEpisodes040(m,work)}
   UiQa050.await(5000){InputQa.hasLabel(i,"单集卡片 1")}
   InputQa.clickLabel(i,"第1集观看标记",idle=false)
   UiQa050.await(5000){InputQa.hasLabel(i,"未标记")}
   i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(350)
   UiQa050.await(3000){!InputQa.hasLabel(i,"只改变标记 · 点外部或按返回关闭")}
   out.put("roundMarkerTouchWorks",true)
   var synopsisFocused=false
   for(step in 1..8){UiQa054.key(i,KeyEvent.KEYCODE_DPAD_DOWN);synopsisFocused=InputQa.hasLabel(i,"第1集简介 · 当前焦点");if(synopsisFocused)break}
   check(synopsisFocused){"D-pad could not reach the synopsis panel; focused=${focusedLabels(i)}"}
   val componentScreen=i.uiAutomation.takeScreenshot()!!.let{Rect(0,0,it.width,it.height)}
   val focusedSynopsis=UiQa054.rect(i,"第1集简介 · 当前焦点")?:error("focused synopsis missing")
   check(intersects(focusedSynopsis,componentScreen)){"synopsis was not brought into view: $focusedSynopsis"}
   UiQa050.shot(i,"qa060-episode-synopsis-dpad.png")
   out.put("synopsisDpadReachable",true)
   return out
  }finally{
   fixture.close()
   i.runOnMainSync{m.details.cancelLoad()}
   if(original.exists()){cache.delete();original.renameTo(cache)}
  }
 }

 fun settings(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
  val out=JSONObject()
  val oldSection=m.settingsSection
  val oldParallel=m.config.parallel
  try{
   i.runOnMainSync{m.settingsSection="remote"}
   render(i,a){Settings040(m)}
   UiQa050.await(5000){InputQa.hasLabel(i,"手机扫码局域网")}
   check(!InputQa.hasLabel(i,"外观与海报操作"))
   check(!InputQa.hasLabel(i,"隐私与数据"))
   check(!InputQa.hasLabel(i,"软件更新"))
   InputQa.clickLabel(i,"影视资料",idle=false)
   Thread.sleep(300)
   UiQa054.key(i,KeyEvent.KEYCODE_DPAD_DOWN)
   UiQa054.key(i,KeyEvent.KEYCODE_DPAD_CENTER)
   UiQa050.await(5000){InputQa.hasLabel(i,"输入新内容；留空不会覆盖现有配置")}
   UiQa050.shot(i,"qa060-settings-tmdb-dialog.png")
   i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(450)
   UiQa054.key(i,KeyEvent.KEYCODE_DPAD_CENTER)
   UiQa050.await(5000){InputQa.hasLabel(i,"输入新内容；留空不会覆盖现有配置")}
   i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(450)
   out.put("leftToRightFocusBridge",true)

   check(!InputQa.hasLabel(i,"海报主评分"))
   out.put("dialogCloseRestoresRightFocus",true).put("tmdbInlineInputRemoved",true)

   i.runOnMainSync{m.settingsSection="playback"};UiQa050.await(5000){InputQa.hasLabel(i,"测试当前并发")}
   InputQa.clickLabel(i,"4路",idle=false)
   InputQa.clickLabel(i,"测试当前并发",idle=false)
   UiQa050.await(6000){windowTexts(i).any{it.startsWith("当前 4 路生效")}}
   check(!InputQa.hasLabel(i,"默认倍速"))
   out.put("parallelSettingMeasured",windowTexts(i).first{it.startsWith("当前 4 路生效")}).put("defaultSpeedRemoved",true)

   i.runOnMainSync{m.settingsSection="subtitles"};UiQa050.await(5000){InputQa.hasLabel(i,"免费公共字幕 · 无需Key")}
   check(!InputQa.hasLabel(i,"复制ASSRT Token"))
   UiQa050.shot(i,"qa060-settings-subtitles.png")
   i.runOnMainSync{m.settingsSection="danmaku"};UiQa050.await(5000){InputQa.hasLabel(i,"免费公共弹幕 · 无需配置")}
   check(!InputQa.hasLabel(i,"复制弹弹play AppId"))
   check(InputQa.hasLabel(i,"✓ 已配置 · 公共兼容服务默认置顶"))
   for(step in 1..4){if(InputQa.hasLabel(i,"横向飞行距离"))break;scrollDown(i)}
   check(InputQa.hasLabel(i,"横向飞行距离"))
   check(InputQa.hasLabel(i,"实时预览"))
   UiQa050.shot(i,"qa060-settings-danmaku.png")
   out.put("freeSubtitlePinned",true).put("freeDanmakuPinned",true).put("inlineServiceInputsRemoved",true)

   i.runOnMainSync{m.settingsSection="configuration"};UiQa050.await(5000){InputQa.hasLabel(i,"私人TVBox点播订阅地址")}
   check(!InputQa.hasLabel(i,"当前站点 / 默认解析器"))
   check(!InputQa.hasLabel(i,"ATV系统首页"))
   out.put("legacyConfigurationCardsRemoved",true)

   i.runOnMainSync{m.settingsSection="network"};UiQa050.await(5000){InputQa.hasLabel(i,"网络与缓存")}
   for(step in 1..3){if(InputQa.hasLabel(i,"图片缓存上限"))break;scrollDown(i)}
   check(InputQa.hasLabel(i,"图片缓存上限"))
   i.runOnMainSync{m.settingsSection="about"};UiQa050.await(5000){InputQa.hasLabel(i,"关于 Ptvbox")}
   for(step in 1..3){if(InputQa.hasLabel(i,"检查更新"))break;scrollDown(i)}
   check(InputQa.hasLabel(i,"检查更新"))
   out.put("networkAndCacheMerged",true).put("updateMovedIntoAbout",true).put("privacySectionRemoved",true)

   i.runOnMainSync{m.settingsSection="remote"};UiQa050.await(5000){InputQa.hasLabel(i,"点击扫码")}
   InputQa.clickLabel(i,"点击扫码",idle=false)
   UiQa050.await(10000){InputQa.hasLabel(i,"关闭二维码")}
   val close=UiQa054.rect(i,"关闭二维码")?:error("close QR missing")
   val qr=UiQa054.rect(i,"手机配置配对二维码 · 点击打开浏览器")?:error("QR missing")
   check(close.left<qr.left)
   UiQa054.key(i,KeyEvent.KEYCODE_DPAD_RIGHT)
   UiQa050.await(4000){InputQa.hasLabel(i,"二维码 · 当前焦点")}
   out.put("qrActionsLeftQrRight",true)
   UiQa050.shot(i,"qa060-settings-remote.png")
   return out
  }finally{m.stopRemoteConfig();i.runOnMainSync{m.settingChanged("search_parallel",oldParallel);m.settingsSection=oldSection}}
 }

 fun notice(i:Instrumentation):JSONObject{
  val client=SpiderClient(i.targetContext)
  return try{
   val site=Site("qa060-notice","QA通知拦截","csp_QaAuth054",3,group="QA",searchable=false)
   val result=runBlocking{client.noticeStatus(site)}
   Thread.sleep(800)
   val visible=windowTexts(i).any{it.contains("QA plugin toast")}
   check(result.optBoolean("noticeHook")){"Toast hook was not installed"}
   check(result.optInt("suppressed")>=1){"Toast service proxy did not suppress the probe"}
   check(!visible){"plugin Toast remained visible"}
   UiQa050.shot(i,"qa060-plugin-toast-suppressed.png")
   JSONObject().put("noticeHook",true).put("suppressed",result.optInt("suppressed")).put("toastVisible",false)
  }finally{client.close()}
 }

 fun exactPrivateNotice(i:Instrumentation):JSONObject{
  val client=SpiderClient(i.targetContext)
  return try{
   val site=Site("qa100-notice","QA私人提示拦截","csp_QaNotice100Guard",3,group="QA",searchable=false)
   val baseline=runBlocking{client.noticeStatus(site)}
   val before=baseline.optInt("suppressed")
   val result=runBlocking{client.noticeStatus(site,"此资源失效","饭：已过滤视频中广告(6条)","网盘源需使用百度网盘APP扫码登录")}
   Thread.sleep(1200)
   val visible=windowTexts(i).filter{it.contains(Regex("此资源失效|已过滤视频中广告|网盘源需使用",RegexOption.IGNORE_CASE))}
   val delta=result.optInt("suppressed")-before
   check(result.optBoolean("noticeHook")){"Plugin process Toast hook was not installed"}
   check(delta>=3){"Exact private notices bypassed the Toast proxy: delta=$delta visible=$visible"}
   check(visible.isEmpty()){"Exact private notices remained visible: $visible"}
   UiQa050.shot(i,"qa0100-exact-private-notice-suppressed.png")
   JSONObject().put("hook",true).put("suppressed",delta).put("visibleNoise",org.json.JSONArray(visible))
  }finally{client.close()}
 }

 fun gestures(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
  var toggles=0;var shown=0;val seeks=mutableListOf<Long>();val boosts=mutableListOf<Boolean>();val commits=mutableListOf<Long>()
  render(i,a){
   Box(Modifier.fillMaxSize()){
    PlayerTouchLayer060(
     m,
     onShowControls={shown++},
     onPreview={},
     onCommit={it?.let(commits::add)},
     onToggle={toggles++},
     onSeek={seeks+=it},
     onBoost={boosts+=it}
    )
   }
  }
  tap(i,960f,540f);tap(i,960f,540f)
  UiQa050.await(3000){toggles==1}
  tap(i,960f,800f)
  UiQa050.await(3000){shown>=1}
  tap(i,250f,540f);tap(i,250f,540f)
  UiQa050.await(3000){seeks.size>=1}
  tap(i,1700f,540f);tap(i,1700f,540f)
  UiQa050.await(3000){seeks.size>=2}
  hold(i,960f,540f,900)
  check(boosts.contains(true)&&boosts.lastOrNull()==false)
  drag(i,600f,1300f,700f)
  UiQa050.await(3000){commits.isNotEmpty()}
  return JSONObject().put("centerDoubleTapToggles",true).put("leftRightDoubleTapSeeks",seeks.size>=2).put("longPressBoostEvents",boosts.size).put("horizontalDragCommits",commits.size).put("singleTapShowsControls",shown>=1)
 }

 fun mainNotice(i:Instrumentation):JSONObject{
  val before=PluginUi054.suppressed.get()
  val done=java.util.concurrent.CountDownLatch(1)
  i.runOnMainSync{
   android.os.Handler(android.os.Looper.getMainLooper()).post{
    runCatching{android.widget.Toast.makeText(i.targetContext,"QA main toast",android.widget.Toast.LENGTH_SHORT).show()}
    done.countDown()
   }
  }
  check(done.await(5,java.util.concurrent.TimeUnit.SECONDS))
  Thread.sleep(500)
  check(PluginUi054.noticeHook)
  check(PluginUi054.suppressed.get()>before)
  check(!windowTexts(i).any{it.contains("QA main toast")})
  return JSONObject().put("noticeHook",true).put("suppressedDelta",PluginUi054.suppressed.get()-before).put("toastVisible",false)
 }

 fun publicSubtitle(i:Instrumentation,m:PtvModel):JSONObject{
  val work=Work("qa-public-subtitle","肖申克的救赎","movie","1994",provider="douban",facts=WorkFacts(imdbId="tt0111161"))
  val rows=OnlineSubtitles053(m.db.secrets).search("OpenSubtitles V3",MediaContext053(work,null))
  return JSONObject().put("provider","OpenSubtitles V3").put("reachable",true).put("chineseResults",rows.size).put("requiresApiKey",false)
 }

 fun privateClasses(i:Instrumentation,m:PtvModel,api:String?):JSONObject{
  val config=java.io.File(i.targetContext.getExternalFilesDir(null),"qa-private-subscription.json")
  check(config.isFile){"Private subscription fixture missing"}
  val prior=m.db.secrets.read("source_config")
  return try{
   m.db.secrets.save("source_config",config.readText())
   runBlocking{m.configurationChanged()}
   val site=m.sites.all().firstOrNull{it.api==api}?:error("Private site missing")
   val client=SpiderClient(i.targetContext)
   try{JSONObject().put("api",site.api).put("classes",runBlocking{client.loadedClasses(site)})}finally{client.close()}
  }finally{m.db.secrets.save("source_config",prior);runBlocking{m.configurationChanged()}}
 }

 fun privateSearch(i:Instrumentation,m:PtvModel,titles:String):JSONObject{
  val config=java.io.File(i.targetContext.getExternalFilesDir(null),"qa-private-subscription.json")
  check(config.isFile){"Private subscription fixture missing"}
  val prior=m.db.secrets.read("source_config")
  return try{
   m.db.secrets.save("source_config",config.readText())
   runBlocking{m.configurationChanged()}
   val sites=m.sites.all().filter{it.group=="私人订阅"&&it.api.startsWith("csp_")}
   val rows=org.json.JSONArray()
   runBlocking{
    kotlinx.coroutines.coroutineScope{
     val gate=kotlinx.coroutines.sync.Semaphore(6)
     sites.map{site->
      async(kotlinx.coroutines.Dispatchers.IO){
       gate.withPermit{
        val row=JSONObject().put("name",site.name).put("api",site.api)
        val matches=mutableListOf<Pair<String,String>>()
        val errors=mutableListOf<String>()
        for(title in titles.split(',').map{it.trim()}.filter{it.isNotBlank()}){
         try{
          val hits=kotlinx.coroutines.withTimeout(30000){m.sites.search(site,title)}
          hits.filter{Identity.matches(Work("",title,"movie",""),it)}.take(3).forEach{matches+=title to it.title}
         }catch(e:Exception){errors+=e.javaClass.simpleName}
        }
        row.put("matches",org.json.JSONArray(matches.map{org.json.JSONObject().put("query",it.first).put("title",it.second)}))
        row.put("errors",org.json.JSONArray(errors.distinct()))
        synchronized(rows){rows.put(row)}
       }
      }
     }.forEach{it.await()}
    }
   }
   JSONObject().put("sites",sites.size).put("results",rows)
  }finally{m.db.secrets.save("source_config",prior);runBlocking{m.configurationChanged()}}
 }

 fun privateDirect(i:Instrumentation,m:PtvModel,a:MainActivity,targetApi:String,title:String,year:String):JSONObject{
  val config=java.io.File(i.targetContext.getExternalFilesDir(null),"qa-private-subscription.json")
  check(config.isFile){"Private subscription fixture missing"}
  val prior=m.db.secrets.read("source_config")
  return try{
   m.db.secrets.save("source_config",config.readText())
   runBlocking{m.configurationChanged()}
   val site=m.sites.all().firstOrNull{it.api==targetApi}?:error("Private site missing")
   val work=Work("qa-direct-$title-$year",title,"movie",year,provider="douban")
   val hits=try{runBlocking{kotlinx.coroutines.withTimeout(45000){m.sites.search(site,title)}}}
     catch(e:Exception){throw IllegalStateException("搜索阶段超时/失败: ${e.javaClass.simpleName}")}
   val match=hits.firstOrNull{Identity.matches(work,it)}?:error("No matching private title")
   val vod=try{runBlocking{kotlinx.coroutines.withTimeout(45000){m.sites.detail(site,match)}}}
     catch(e:Exception){throw IllegalStateException("详情阶段超时/失败: ${e.javaClass.simpleName}")}
   var candidate:Candidate?=null
   val errors=mutableListOf<String>()
   for(road in vod.roads){
    val episode=Identity.pick(road,"正片",work.kind)?:road.episodes.firstOrNull()?:continue
    try{
     candidate=runBlocking{kotlinx.coroutines.withTimeout(45000){m.sites.probe(site,road,episode).copy(sourceId="${site.key}|${vod.id}|${road.name}")}}
     break
    }catch(e:Exception){
     errors+=e.javaClass.simpleName
    }
   }
   val picked=candidate?:return JSONObject().put("site",site.api).put("hits",hits.size).put("roads",vod.roads.size)
     .put("candidate",false).put("probeErrors",org.json.JSONArray(errors.distinct()))
   val before=PluginUi054.suppressed.get()
   i.runOnMainSync{
    m.player.volume=0f
    m.current=work
    m.detail=work
    m.select(picked,0,false)
   }
   val noise=linkedSetOf<String>()
   val end=System.currentTimeMillis()+60000
   var rendered=false
   var waited=0L
   while(System.currentTimeMillis()<end){
    windowTexts(i).filter{it.contains(Regex("资源失效|已过滤.{0,40}广告|网盘源需|扫码登录|二维码",RegexOption.IGNORE_CASE))}.forEach(noise::add)
    i.runOnMainSync{rendered=m.hasRenderedFrame()&&m.player.currentPosition>1000}
    if(rendered)break
    Thread.sleep(250)
   }
   if(rendered){
    for((delay,label) in listOf(5000L to "5s",20000L to "20s",20000L to "40s")){
     val until=System.currentTimeMillis()+delay
     while(System.currentTimeMillis()<until){
      windowTexts(i).filter{it.contains(Regex("资源失效|已过滤.{0,40}广告|网盘源需|扫码登录|二维码",RegexOption.IGNORE_CASE))}.forEach(noise::add)
      Thread.sleep(250)
     }
     UiQa050.shot(i,"qa0100-private-direct-$label.png")
    }
    waited=40000
   }
   UiQa050.shot(i,"qa0100-private-direct-final.png")
   var position=0L
   i.runOnMainSync{
    position=m.player.currentPosition
    m.stop()
   }
   JSONObject().put("site",site.api).put("hits",hits.size).put("roads",vod.roads.size).put("candidate",true)
     .put("rendered",rendered).put("height",picked.height).put("positionMs",position)
     .put("playbackSampleMs",waited).put("visibleNoise",org.json.JSONArray(noise))
     .put("suppressedDelta",PluginUi054.suppressed.get()-before)
  }finally{m.db.secrets.save("source_config",prior);runBlocking{m.configurationChanged()}}
 }

 fun privatePlayback(i:Instrumentation,m:PtvModel,a:MainActivity,targetApi:String?=null,title:String="肖申克的救赎",year:String="1994"):JSONObject{
  val config=java.io.File(i.targetContext.getExternalFilesDir(null),"qa-private-subscription.json")
  check(config.isFile){"Private subscription fixture missing"}
  val priorSource=m.db.secrets.read("source_config")
  val priorSites=runBlocking{m.sites.all()}
  val priorEnabled=m.enabledSiteKeys
  val priorSiteFlags=priorSites.associate{it.key to m.sites.enabled(it)}
  val noise=linkedSetOf<String>()
  var target:Site?=null
  try{
   m.db.secrets.save("source_config",config.readText())
   runBlocking{m.configurationChanged()}
   val all=m.sites.all()
   target=targetApi?.let{api->all.firstOrNull{it.api==api}}?:all.firstOrNull{it.api=="csp_AppgzGuard"}?:all.firstOrNull{it.group=="私人订阅"}
   check(target!=null){"No private site"}
   m.db.prefs.edit().apply{all.forEach{putBoolean("site_${it.key}",it.key==target!!.key)}}.commit()
   i.runOnMainSync{
    m.availableSites=all
    m.enabledSiteKeys=setOf(target.key)
    m.player.volume=0f
    m.play(Work("qa-private-$title-$year",title,"movie",year,provider="douban",facts=if(title=="肖申克的救赎")WorkFacts(imdbId="tt0111161")else WorkFacts()),"正片",true)
   }
   val end=System.currentTimeMillis()+180_000
   var rendered=false
   var candidate=false
   while(System.currentTimeMillis()<end){
    val texts=windowTexts(i)
    texts.filter{it.contains(Regex("资源失效|已过滤.{0,30}广告|网盘源需|扫码登录|二维码",RegexOption.IGNORE_CASE))}.forEach(noise::add)
    i.runOnMainSync{rendered=m.hasRenderedFrame()&&m.player.currentPosition>1000;candidate=m.currentCandidate!=null}
    if(rendered&&candidate)break
    Thread.sleep(250)
   }
   UiQa050.shot(i,"qa090-private-playback.png")
   return JSONObject().put("site",target!!.api).put("rendered",rendered).put("candidate",candidate)
    .put("privateSites",all.count{it.group=="私人订阅"}).put("visibleNoise",org.json.JSONArray(noise))
    .put("mainNoticeHook",PluginUi054.noticeHook).put("mainSuppressed",PluginUi054.suppressed.get())
    .put("siteStates",org.json.JSONArray(m.siteStates.map{org.json.JSONObject().put("api",it.site.api).put("state",it.text)}))
  }finally{
   i.runOnMainSync{m.stop();m.enabledSiteKeys=priorEnabled}
   m.db.prefs.edit().apply{priorSiteFlags.forEach{(key,enabled)->putBoolean("site_$key",enabled)}}.commit()
   m.db.secrets.save("source_config",priorSource)
   runBlocking{m.configurationChanged()}
  }
 }

 fun routing(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
  val old=m.screen
  try{
   i.runOnMainSync{m.screen="sites"}
   render(i,a){CinemaTheme(false){Sites040(m)}}
   UiQa050.await(5000){InputQa.hasLabel(i,"分类选源")}
   InputQa.clickLabel(i,"分类选源",idle=false)
   UiQa050.await(5000){InputQa.hasLabel(i,"中文剧集")}
   InputQa.clickLabel(i,"动画",idle=false)
   UiQa050.await(5000){InputQa.hasLabel(i,"1. 动画")}
   InputQa.clickLabel(i,"下移",idle=false)
   UiQa050.await(5000){InputQa.hasLabel(i,"1. 私人订阅")}
   InputQa.clickLabel(i,"恢复本类默认",idle=false)
   UiQa050.await(5000){InputQa.hasLabel(i,"1. 动画")}
   UiQa050.shot(i,"qa080-site-routing.png")
   return JSONObject().put("categoryRoutingVisible",true).put("categoryOrderEditable",true).put("defaultRestored",true)
  }finally{i.runOnMainSync{m.screen=old}}
 }
}
