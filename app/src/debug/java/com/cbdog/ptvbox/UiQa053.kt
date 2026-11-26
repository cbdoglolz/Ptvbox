package com.cbdog.ptvbox
import android.app.Instrumentation
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import kotlinx.coroutines.runBlocking

object UiQa053 {
 fun anime(i:Instrumentation,m:PtvModel):JSONObject {val out=JSONObject();val all=m.sites.all();val prior=all.associateWith{m.sites.enabled(it)};val site=all.first{it.type==8};m.db.prefs.edit().apply{all.forEach{putBoolean("site_${it.key}",it.key==site.key)}}.commit();try{
  val w=Work("qa053-real-frieren","葬送的芙莉莲","tv","2023",original="Frieren: Beyond Journey's End",season=1,facts=WorkFacts(imdbId="tt22248376"));i.runOnMainSync{m.play(w,"第01集",true);m.player.volume=0f};UiQa050.await(130000){var ok=false;i.runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition>1500};ok};i.runOnMainSync{out.put("actualAnimeVideoFrame",m.hasRenderedFrame()).put("height",m.player.videoSize.height).put("episode",m.currentEpisode).put("source",m.currentCandidate?.site?.name)}
  UiQa050.await(75000){var ok=false;i.runOnMainSync{ok=m.addons.danmaku.isNotEmpty()||m.addons.matches.isNotEmpty()||m.addons.danmakuStatus.contains("失败")};ok};var choice:DanmakuMatch053?=null;i.runOnMainSync{if(m.addons.danmaku.isEmpty())choice=m.addons.matches.firstOrNull()};choice?.let{row->i.runOnMainSync{m.addons.loadDanmaku(row)};UiQa050.await(35000){var ok=false;i.runOnMainSync{ok=m.addons.danmaku.isNotEmpty()};ok}};i.runOnMainSync{out.put("danmakuCount",m.addons.danmaku.size).put("danmakuReady",m.addons.danmaku.isNotEmpty()).put("manualAmbiguityConfirmed",choice!=null);m.player.seekTo(5000)};Thread.sleep(800);UiQa050.shot(i,"qa053-real-anime.png");return out
 }finally{m.db.prefs.edit().apply{prior.forEach{(s,v)->putBoolean("site_${s.key}",v)}}.commit();i.runOnMainSync{m.stop()}}}
 private fun key(i:Instrumentation,code:Int){val now=android.os.SystemClock.uptimeMillis();i.uiAutomation.injectInputEvent(android.view.KeyEvent(now,now,android.view.KeyEvent.ACTION_DOWN,code,0),true);i.uiAutomation.injectInputEvent(android.view.KeyEvent(now,now+50,android.view.KeyEvent.ACTION_UP,code,0),true)}
 fun run(i:Instrumentation,m:PtvModel,activity:Activity,url:String):JSONObject {val out=JSONObject();val fixture=Work("hero053-one","首页普通片名 · 测试一","tv","2023",provider="site_fixture",poster=url+"/poster.png",overview="这是用于验证首页排版的故事简介。旅程结束后，伙伴们重新认识时间、友情和回忆。",genres=listOf("冒险","奇幻"),people=listOf(PersonCredit("1","主要演员甲","主演"),PersonCredit("2","主要演员乙","主演")))
  UiQa050.await(25000){var ready=false;i.runOnMainSync{ready=!m.busy};ready}
  val original=m.feed
  i.runOnMainSync{m.feed=listOf(fixture,fixture.copy(id="hero053-two",title="首页普通片名 · 测试二"));m.screen="home";m.presentations=m.presentations+mapOf(fixture.ratingViewKey to Presentation050(logo=url+"/logo.png",ratings=mapOf("douban" to fixture)))}
  Thread.sleep(1200);check(InputQa.hasLabel(i,"8秒自动切换"));check(InputQa.hasLabel(i,"主演：主要演员甲、主要演员乙"));out.put("heroSynopsisGenreCastAndCountdown",true);UiQa050.shot(i,"qa053-home-first.png")
  val frames=java.util.Collections.synchronizedList(mutableListOf<Long>());val listener=Window.OnFrameMetricsAvailableListener{_,metrics,_->frames.add(metrics.getMetric(FrameMetrics.TOTAL_DURATION))};i.runOnMainSync{activity.window.addOnFrameMetricsAvailableListener(listener,Handler(Looper.getMainLooper()))};Thread.sleep(8200);i.runOnMainSync{activity.window.removeOnFrameMetricsAvailableListener(listener)};check(InputQa.hasLabel(i,"横幅 2/2 · 首页普通片名 · 测试二"));out.put("heroAutoAdvances",true).put("frameMetricsScope","BlueStacks 8.2s countdown plus one automatic transition; includes loading and frame rendering; not physical TV acceptance");val sorted=synchronized(frames){frames.toList().sorted()};out.put("frames",sorted.size).put("frameP95Ms",sorted.getOrNull((sorted.size*.95).toInt())?.div(1_000_000.0)).put("framesOver16_67Ms",sorted.count{it>16666667}).put("frameDurationsNs",JSONArray(sorted));UiQa050.shot(i,"qa053-home-next.png")
  val w=Work("qa053-frieren","葬送的芙莉莲","tv","2023",original="Frieren",season=1);val site=Site("qa053-fixture","本地播放验证",url)
  i.runOnMainSync{m.current=w;m.select(Candidate(site,"本地测试",Episode("第01集",url),height=720),0,false);m.player.volume=0f};UiQa050.await(25000){var ok=false;i.runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition>1000};ok};out.put("actualVideoFrame",true)
  val srt="1\n00:00:00,000 --> 00:05:00,000\n中文字幕实时显示\nSubtitle playback test\n";i.runOnMainSync{m.addons.local(srt.toByteArray(),"QA-中文字幕.srt")};UiQa050.await{var ok=false;i.runOnMainSync{ok=m.addons.cues.isNotEmpty()};ok};i.runOnMainSync{check(m.addons.selectedTitle=="QA-中文字幕.srt");m.addons.adjust(500);check(m.addons.delayMs==500L)};out.put("subtitleImportedWithoutRestartingVideo",true);UiQa050.shot(i,"qa053-subtitle-video.png")
  InputQa.clickLabel(i,"字幕",idle=false);Thread.sleep(600);check(InputQa.hasLabel(i,"上次使用 · QA-中文字幕.srt"));InputQa.clickLabel(i,"样式",idle=false);Thread.sleep(400);check(InputQa.hasLabel(i,"星光照亮前行的路。"));UiQa050.shot(i,"qa053-subtitle-style.png");out.put("ccLastUsedAndLiveStylePreview",true);key(i,android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(250);key(i,android.view.KeyEvent.KEYCODE_DPAD_UP);Thread.sleep(250)
  i.runOnMainSync{m.addons.clear();m.addons.bind(w,"第01集")};UiQa050.await{var ok=false;i.runOnMainSync{ok=m.addons.cues.isNotEmpty()&&m.addons.delayMs==500L};ok};out.put("lastSubtitleAndDelayAutoRestored",true)
  val until=System.currentTimeMillis()+75000;while(System.currentTimeMillis()<until){var done=false;i.runOnMainSync{done=m.addons.danmaku.isNotEmpty()||m.addons.matches.isNotEmpty()||m.addons.danmakuStatus.contains("失败")||m.addons.danmakuStatus.contains("未找到")};if(done)break;Thread.sleep(400)}
  var choice:DanmakuMatch053?=null;i.runOnMainSync{if(m.addons.danmaku.isEmpty())choice=m.addons.matches.firstOrNull()};choice?.let{candidate->i.runOnMainSync{m.addons.loadDanmaku(candidate)};UiQa050.await(45000){var ok=false;i.runOnMainSync{ok=m.addons.danmaku.isNotEmpty()};ok}};out.put("ambiguousCandidateManuallyChosen",choice!=null)
  i.runOnMainSync{out.put("danmakuCount",m.addons.danmaku.size).put("danmakuStatus",m.addons.danmakuStatus).put("danmakuLoaded",m.addons.danmaku.isNotEmpty()).put("autoDanmakuLoaded",choice==null&&m.addons.danmaku.isNotEmpty());m.player.seekTo(2500)};Thread.sleep(600);UiQa050.shot(i,"qa053-danmaku-subtitle.png")
  i.runOnMainSync{m.stop();m.feed=original};File(i.targetContext.getExternalFilesDir(null),"qa053-ui-result.json").writeText(out.toString(2));return out
 }
}
