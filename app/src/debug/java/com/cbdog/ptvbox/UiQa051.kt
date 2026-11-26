package com.cbdog.ptvbox
import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.view.KeyEvent
import org.json.JSONObject

object UiQa051 {
 fun player(i:Instrumentation,m:PtvModel,a:Activity):JSONObject {val r=JSONObject();i.runOnMainSync{m.player.pause();m.playbackReturnScreen="favorites"}
  i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(250);check(!InputQa.hasLabel(i,"显示控制"));check(!InputQa.hasLabel(i,"隐藏控制"));r.put("backHidesAllUiWithoutShowControlButton",true)
  for(key in listOf(KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT)){i.sendKeyDownUpSync(key);Thread.sleep(150);check(InputQa.hasLabel(i,"隐藏控制"));i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(100);check(!InputQa.hasLabel(i,"隐藏控制"))};r.put("allDirectionsWakeUi",true)
  i.sendKeyDownUpSync(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);Thread.sleep(200);check(InputQa.hasLabel(i,"隐藏控制"));r.put("mediaPauseWakesUi",true)
  i.runOnMainSync{m.player.volume=.3f};InputQa.clickLabel(i,"静音");i.runOnMainSync{check(m.player.volume==0f)};InputQa.clickLabel(i,"取消静音");i.runOnMainSync{check(kotlin.math.abs(m.player.volume-.3f)<.01);m.player.volume=0f};check(!InputQa.hasLabel(i,"音量 +"));r.put("muteOnlyAndRestoresVolume",true)
  InputQa.clickLabel(i,"选集");check(InputQa.hasLabel(i,"已加载 1 集"));check(InputQa.hasLabel(i,"✓ 正片"));UiQa050.shot(i,"qa051-episodes-selected.png");InputQa.clickLabel(i,"关闭");r.put("movieHasOneEpisodeNotSourceVariants",true)
  i.runOnMainSync{val c=m.currentCandidate!!;m.candidates=listOf(c,c.copy(road="备用线路"))};InputQa.clickLabel(i,"播放线路");check(InputQa.hasLabel(i,"${m.currentCandidate!!.site.name} · 备用线路\n清晰度待确认 · ${m.currentCandidate!!.delay}ms"));UiQa050.shot(i,"qa051-source-selected.png");i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(200);check(!InputQa.hasLabel(i,"隐藏控制"));r.put("panelBackHidesUi",true)
  i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(300);i.runOnMainSync{check(m.screen=="favorites"&&m.current!=null&&m.player.mediaItemCount>0)};r.put("secondBackReturnsExactLaunchPageAndKeepsPlayback",true)
  i.runOnMainSync{m.screen="search"};Thread.sleep(300);i.runOnMainSync{m.showPlayer()};Thread.sleep(250);i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(250);i.runOnMainSync{check(m.screen=="search"){"return=${m.screen} anchor=${m.playbackReturnScreen}"}};r.put("searchReturnAnchor",true)
  i.runOnMainSync{m.player.play()};check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME));Thread.sleep(1800);i.runOnMainSync{check(m.current==null&&!m.playing)};r.put("systemHomeStillStopsByDefault",true)
  a.startActivity(Intent(a,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));return r
 }
 fun pagination(i:Instrumentation,m:PtvModel,url:String):JSONObject {val config=m.db.secrets.read("source_config");val live=m.db.secrets.read("live_subscription");val sites=m.availableSites;val enabled=m.enabledSiteKeys
  try{UiQa050.await{var ok=false;i.runOnMainSync{ok=m.availableSites.isNotEmpty()};ok};m.db.secrets.save("source_config","{}");m.db.secrets.save("live_subscription","");i.runOnMainSync{m.enabledSiteKeys=m.enabledSiteKeys+"qa-page051";m.availableSites=listOf(Site("qa-page051","QA体育分页",url,1,searchable=false));m.screen="live";m.preferencesRevision++};UiQa050.await{InputQa.hasLabel(i,"比赛06")};Thread.sleep(400)
   i.runOnMainSync{m.restoreFocus="livegrid|live:movie:6"};UiQa050.await{InputQa.labelFocused(i,"比赛06")||m.focusWork=="live:movie:6"};i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);UiQa050.await{InputQa.hasLabel(i,"比赛12")};check(InputQa.hasLabel(i,"比赛01"));UiQa050.shot(i,"qa051-sports-waterfall.png")
   i.runOnMainSync{m.restoreFocus="livegrid|live:movie:12"};UiQa050.await{m.focusWork=="live:movie:12"};i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_DOWN);UiQa050.await{InputQa.hasLabel(i,"已显示当前全部内容")};return JSONObject().put("lastRowDownLoadsSecondPage",true).put("firstPageRetained",true).put("noAutomaticChainedLoading",true).put("emptyPageStopsPagination",true)
  }finally{m.db.secrets.save("source_config",config);m.db.secrets.save("live_subscription",live);i.runOnMainSync{m.availableSites=sites;m.enabledSiteKeys=enabled;m.preferencesRevision++;m.screen="home"}}
 }
}
