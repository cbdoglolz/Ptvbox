package com.cbdog.ptvbox

import android.app.Instrumentation
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

object FeaturesQa057 {
 private fun render(i:Instrumentation,a:MainActivity,body:@Composable ()->Unit){i.runOnMainSync{a.setContent{CinemaTheme(false){Box(Modifier.fillMaxSize().background(LocalCinema.current.background)){body()}}}};Thread.sleep(500)}
 private fun focusedLabel(i:Instrumentation):String{val root=i.uiAutomation.rootInActiveWindow?:return "";fun walk(n:android.view.accessibility.AccessibilityNodeInfo?):String{ if(n==null)return "";if(n.isFocused)return n.contentDescription?.toString()?.ifBlank{n.text?.toString().orEmpty()}.orEmpty()?:n.text?.toString().orEmpty();for(index in 0 until n.childCount)walk(n.getChild(index)).takeIf{it.isNotBlank()}?.let{return it};return "" };return walk(root)}
 private fun clickNode(i:Instrumentation,label:String){val root=i.uiAutomation.rootInActiveWindow?:error("No window");var match:android.view.accessibility.AccessibilityNodeInfo?=null;fun walk(n:android.view.accessibility.AccessibilityNodeInfo?){if(n==null||match!=null)return;if(n.contentDescription?.toString()==label||n.text?.toString()==label){match=n;return};for(index in 0 until n.childCount){walk(n.getChild(index));if(match!=null)return}};walk(root);var target=match;while(target!=null&&!target.isClickable)target=target.parent;check(target?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)==true)}
 private fun hold(i:Instrumentation,x:Float,y:Float,millis:Long=900){val started=android.os.SystemClock.uptimeMillis();val props=arrayOf(MotionEvent.PointerProperties().apply{id=0;toolType=MotionEvent.TOOL_TYPE_FINGER});val coords=arrayOf(MotionEvent.PointerCoords().apply{this.x=x;this.y=y;pressure=1f;size=1f});fun event(action:Int){val e=MotionEvent.obtain(started,android.os.SystemClock.uptimeMillis(),action,1,props,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);i.uiAutomation.injectInputEvent(e,true);e.recycle()};event(MotionEvent.ACTION_DOWN);Thread.sleep(millis);event(MotionEvent.ACTION_UP);Thread.sleep(250)}
 private fun key(i:Instrumentation,keyCode:Int){i.uiAutomation.injectInputEvent(KeyEvent(android.os.SystemClock.uptimeMillis(),android.os.SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,keyCode,0),true);Thread.sleep(30);i.uiAutomation.injectInputEvent(KeyEvent(android.os.SystemClock.uptimeMillis(),android.os.SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,keyCode,0),true)}

 fun layout(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
  val out=JSONObject();val previous=m.current;m.current=Work("qa-nav057","导航测试","movie")
  render(i,a){Row(Modifier.fillMaxSize()){NavigationRail(m)}};UiQa050.await{InputQa.hasLabel(i,"首页")&&InputQa.hasLabel(i,"返回播放")}
  val brand=UiQa054.rect(i,"Ptvbox")!!;val version=UiQa054.rect(i,"v${BuildConfig.VERSION_NAME}")!!;val home=UiQa054.rect(i,"首页")!!;val back=UiQa054.rect(i,"返回播放")!!
  check(brand.top<home.top&&version.top<home.top&&home.top<back.top);check(home.height()>=50&&back.height()>=50);check(!InputQa.hasLabel(i,"资源（高级）"));UiQa050.shot(i,"qa057-navigation.png");out.put("brandReadOnlyAboveHome",true).put("returnBelowHome",true).put("resourceEntryRemoved",true)
  val artUri="android.resource://${i.targetContext.packageName}/${R.drawable.brand}";val work=Work("qa-detail057","详情背景测试","tv","2022",poster=artUri,overview="这是一段用于验证首屏完整显示的剧情简介。艺术字片名、简介、评分和人物资料都不需要光标上移才能看到。",provider="douban",genres=listOf("喜剧","动作"),backdrop=artUri,facts=WorkFacts(countries=listOf("日本")),people=listOf(PersonCredit("1","测试导演","导演",artUri,"douban")))
  i.runOnMainSync{m.detail=work;m.screen="detail"};render(i,a){CinemaDetail040(m)};UiQa050.await{InputQa.hasLabel(i,"刷新全部资料")&&InputQa.hasLabel(i,"小窗播放").not()}
  check(!InputQa.hasLabel(i,"← 返回"));for(label in listOf("手动匹配豆瓣","清除豆瓣匹配","手动匹配TMDB","清除TMDB匹配"))check(InputQa.hasLabel(i,label));val search=UiQa054.rect(i,"搜索高清源")!!;val refresh=UiQa054.rect(i,"刷新全部资料")!!;check(refresh.left>search.left&&abs(refresh.centerY()-search.centerY())<8);UiQa050.shot(i,"qa057-detail-background.png");out.put("persistentBackgroundLayout",true).put("ratingAndMatchOneRow",true)
  i.runOnMainSync{m.current=previous}
  return out
 }

 fun history(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
  val out=JSONObject();val work=Work("qa-history057","历史测试片","movie","2026",poster="https://img1.doubanio.com/view/photo/s_ratio_poster/public/p480747492.jpg",provider="douban");val now=1_765_000_123_000L;val watch=Watch(work,"正片","qa-site","主线路",1080,123_000,7_200_000,now)
  m.users.write{save(watch)};UiQa050.await(10000){m.user.watches.any{it.work.key==work.key}}
  i.runOnMainSync{m.restoreFocus="history|${work.key}"};render(i,a){HistoryPage057(m)};UiQa050.await{InputQa.hasLabel(i,"历史测试片")};UiQa050.await{m.focusWork==work.key};UiQa050.shot(i,"qa057-history-initial.png");check(InputQa.hasLabel(i,HistoryTime057.format(now)));UiQa054.key(i,KeyEvent.KEYCODE_DPAD_CENTER,900);UiQa050.await{InputQa.hasLabel(i,"已选择 1 部")};check(InputQa.hasLabel(i,"删除选中的 1 部历史"));UiQa050.shot(i,"qa057-history-selected.png");InputQa.clickLabel(i,"删除选中的 1 部历史",idle=false);UiQa050.await{InputQa.hasLabel(i,"删除选中的 1 部历史？")};InputQa.clickLabel(i,"确认删除",idle=false);UiQa050.await(10000){m.user.watches.none{it.work.key==work.key}};out.put("timestampShown",true).put("multiSelectAndConfirmedDelete",true)
  return out
 }

fun danmaku(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
  val beforeArea=m.config.number("danmaku_area",50);val beforeSize=m.config.number("danmaku_size",20);val out=JSONObject()
  try{i.runOnMainSync{m.settingChanged("danmaku_area",50);m.settingChanged("danmaku_size",20)};render(i,a){DanmakuPanel053(m,{})};UiQa050.shot(i,"qa057-danmaku-before.png");InputQa.clickLabel(i,"区域 1/4",idle=false);InputQa.clickLabel(i,"字号 20",idle=false);UiQa050.await(5000){m.config.number("danmaku_area",50)==25&&m.config.number("danmaku_size",20)==24};UiQa050.shot(i,"qa057-danmaku-focus.png");val optionFocus=focusedLabel(i);out.put("focused",optionFocus).put("area",m.config.number("danmaku_area",50)).put("size",m.config.number("danmaku_size",20)).put("localStateUpdates",true);check(m.config.number("danmaku_area",50)==25&&m.config.number("danmaku_size",20)==24)}finally{i.runOnMainSync{m.settingChanged("danmaku_area",beforeArea);m.settingChanged("danmaku_size",beforeSize)}}
  return out
}

 suspend fun douban(m:PtvModel):JSONObject{
  val previous=m.db.secrets.read("proxy");try{m.db.secrets.save("proxy","");NetworkPolicy.apply(m.getApplication<android.app.Application>());val work=Work("1292052","肖申克的救赎","movie","1994",provider="douban");val detail=m.metadata.details(work);check(detail.people.size>=5);check(detail.people.count{it.avatar.startsWith("https://")}>=5);val related=m.metadata.related(work);check(related.isNotEmpty());val personWorks=detail.people.firstOrNull{it.role.contains("演员")}?.let{m.metadata.doubanPersonWorks(it)}.orEmpty();val first=System.nanoTime();m.metadata.ratingWork(work,"douban");val firstMs=(System.nanoTime()-first)/1_000_000;val second=System.nanoTime();m.metadata.ratingWork(work,"douban");val cachedMs=(System.nanoTime()-second)/1_000_000;return JSONObject().put("people",detail.people.size).put("avatars",detail.people.count{it.avatar.startsWith("https://")}).put("related",related.size).put("personWorks",personWorks.size).put("withoutTmdb",true).put("firstMatchMs",firstMs).put("cachedMatchMs",cachedMs).put("cachedFaster",cachedMs<=firstMs)}finally{m.db.secrets.save("proxy",previous);NetworkPolicy.apply(m.getApplication<android.app.Application>())}
 }

 suspend fun anime2017():JSONObject{
  val rows=AnimeSeason056.load(2017,0);check(rows.isNotEmpty());val search=AnimeSeason056.search("进击的巨人");check(search.isNotEmpty());return JSONObject().put("2017Winter",rows.size).put("search",search.size).put("samples",JSONArray(rows.take(5).map{it.work.title}))
 }

 suspend fun hdhubDirect(i:Instrumentation,m:PtvModel):JSONObject{
  val previous=m.db.secrets.read("proxy");val enabled=m.enabledSiteKeys;val sites=m.sites.all();val site=sites.first{it.type==8};var passed=false
  try{m.db.secrets.save("proxy","http://127.0.0.1:9");NetworkPolicy.apply(i.targetContext);i.runOnMainSync{m.availableSites=sites;m.enabledSiteKeys=setOf(site.key)};val work=Work("qa-hdhub057","肖申克的救赎","movie","1994",facts=WorkFacts(imdbId="tt0111161"));val vod=Stremio050.lookup(site,work,"正片",m.metadata);check(vod.roads.isNotEmpty());val road=vod.roads.first();val candidate=m.sites.probe(site,road,road.episodes.first());check(candidate.site.key==site.key);i.runOnMainSync{m.current=work;m.select(candidate,0);m.player.volume=0f};UiQa050.await(45000){var ok=false;i.runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition>1000};ok};var height=0;i.runOnMainSync{height=m.player.videoSize.height;m.stop()};passed=true;return JSONObject().put("bogusProxyConfigured",true).put("directLookupPassed",true).put("directFirstFrame",true).put("height",height).put("variants",vod.roads.size)
  }finally{i.runOnMainSync{m.enabledSiteKeys=enabled;if(!passed)m.stop()};m.db.secrets.save("proxy",if(previous=="http://127.0.0.1:9")"" else previous);NetworkPolicy.apply(i.targetContext)}
 }

 suspend fun player(i:Instrumentation,m:PtvModel,activity:MainActivity):JSONObject{
  val priorProxy=m.db.secrets.read("proxy");m.db.secrets.save("proxy","");NetworkPolicy.apply(i.targetContext)
  try{
  val rules=AnimeRules056(i.targetContext);val site=rules.sites().first{it.key=="kazumi_girigirilove"};val row=withTimeout(22000){rules.search(site,"间谍过家家")}.first();val detail=withTimeout(22000){rules.detail(site,row)};val road=detail.roads.first{it.episodes.isNotEmpty()};val episode=road.episodes.first();val candidate=withTimeout(40000){m.sites.probe(site,road,episode)};i.runOnMainSync{m.current=Work("qa057-player","间谍过家家","tv",provider="site_${site.key}",season=3);m.currentEpisode="第01集";m.select(candidate,0);m.player.volume=0f};UiQa050.await(45000){var ok=false;i.runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition>1500};ok}
  check(InputQa.hasLabel(i,"小窗播放"));check(!InputQa.hasLabel(i,"全屏"));check(!InputQa.hasLabel(i,"返回原页面"));check(!InputQa.hasLabel(i,"更多设置"));UiQa050.shot(i,"qa057-player-ui.png")
  key(i,KeyEvent.KEYCODE_BACK);Thread.sleep(350);key(i,KeyEvent.KEYCODE_DPAD_CENTER);UiQa050.await(5000){var paused=false;i.runOnMainSync{paused=!m.player.playWhenReady};paused};key(i,KeyEvent.KEYCODE_DPAD_CENTER);UiQa050.await(5000){var playing=false;i.runOnMainSync{playing=m.player.playWhenReady};playing}
  var before=0L;i.runOnMainSync{before=m.player.currentPosition}
  i.uiAutomation.injectInputEvent(KeyEvent(android.os.SystemClock.uptimeMillis(),android.os.SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT,0,0),true);Thread.sleep(5_400);i.uiAutomation.injectInputEvent(KeyEvent(android.os.SystemClock.uptimeMillis(),android.os.SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT,0,0),true);UiQa050.await(15000){var ok=false;i.runOnMainSync{ok=m.player.currentPosition>60_000};ok};var after=0L;i.runOnMainSync{after=m.player.currentPosition};UiQa050.shot(i,"qa057-player-seek-release.png")
  key(i,KeyEvent.KEYCODE_DPAD_UP);UiQa050.await{InputQa.hasLabel(i,"小窗播放")};InputQa.clickLabel(i,"小窗播放",idle=false);Thread.sleep(1200);var pip=false;i.runOnMainSync{pip=activity.isInPictureInPictureMode};i.runOnMainSync{m.stop()}
  return JSONObject().put("confirmTogglesPlayPause",true).put("seekBefore",before).put("seekAfter",after).put("holdReleasedToSeek",after>60_000).put("pipButton",true).put("pictureInPictureEntered",pip)
  }finally{m.db.secrets.save("proxy",priorProxy);NetworkPolicy.apply(i.targetContext)}
 }
}
