package com.cbdog.ptvbox

import android.app.Instrumentation
import android.view.KeyEvent
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

object UiQa030 {
    private fun waitActivityFocus(i:Instrumentation,a:android.app.Activity){val until=System.currentTimeMillis()+3000;var focused=false;while(System.currentTimeMillis()<until){i.runOnMainSync{focused=a.hasWindowFocus()};if(focused)break;Thread.sleep(50)};check(focused);i.waitForIdleSync()}
    fun player(i:Instrumentation,m:PtvModel,a:android.app.Activity):JSONObject {
        val out=JSONObject();i.runOnMainSync{m.lastRestoredControl=""};InputQa.clickLabel(i,"字幕");InputQa.clickLabel(i,"关闭");Thread.sleep(400)
        i.runOnMainSync{check(m.lastRestoredControl=="字幕"){"Closing CC did not report actual focus on CC"}};out.put("panelFocusReturnsToOpener",true)
        i.runOnMainSync{m.player.pause()};InputQa.clickLabel(i,"隐藏控制");Thread.sleep(250)
        var before=0L;i.runOnMainSync{before=m.player.currentPosition}
        i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);Thread.sleep(200)
        check(InputQa.hasLabel(i,"左右调整 · 确认跳转 · 返回取消"));i.runOnMainSync{check(kotlin.math.abs(m.player.currentPosition-before)<500)}
        i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(200);check(!InputQa.hasLabel(i,"左右调整 · 确认跳转 · 返回取消"));out.put("seekPreviewAndCancel",true)
        i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(300);i.runOnMainSync{check(m.player.playWhenReady);m.player.pause()};out.put("hiddenCenterTogglesPlayback",true)
        val now=android.os.SystemClock.uptimeMillis();i.sendKeySync(KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));Thread.sleep(500);i.sendKeySync(KeyEvent(now,android.os.SystemClock.uptimeMillis(),KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,1));Thread.sleep(100);i.runOnMainSync{check(m.player.playbackParameters.speed==2.5f)};i.sendKeySync(KeyEvent(now,android.os.SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0));i.runOnMainSync{check(m.player.playbackParameters.speed==1f)};out.put("longCenterRestoresSpeed",true)
        i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_UP);Thread.sleep(300);check(InputQa.hasLabel(i,"播放线路"));i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);waitActivityFocus(i,a);i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);i.waitForIdleSync();Thread.sleep(250);i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);i.waitForIdleSync();Thread.sleep(400)
        var backState="";var retained=false;i.runOnMainSync{backState=m.screen;retained=m.current!=null&&m.player.mediaItemCount>0};check(backState=="detail"&&retained){"Back result screen=$backState retained=$retained"};out.put("backReturnsDetailsAndRetainsSession",true)
        val work=m.current!!;val originalCandidate=m.currentCandidate!!;i.runOnMainSync{m.player.play()};Thread.sleep(700);check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME));Thread.sleep(2500)
        var homeState="";var homeOk=false;i.runOnMainSync{homeOk=m.current==null&&!m.playing;homeState="current=${m.current!=null} playing=${m.playing} items=${m.player.mediaItemCount} focus=${a.hasWindowFocus()}"};check(homeOk){homeState};check((m.db.last(work)?.position?:0)>0);out.put("actualHomeStopsAndSaves",true).put("homeState",homeState)
        a.startActivity(android.content.Intent(a,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));waitActivityFocus(i,a);i.runOnMainSync{m.current=work;m.select(originalCandidate,0)};val readyUntil=System.currentTimeMillis()+15000;var recreated=false;while(System.currentTimeMillis()<readyUntil){Thread.sleep(300);i.runOnMainSync{recreated=m.hasRenderedFrame()&&m.player.currentPosition>1000};if(recreated)break};check(recreated);out.put("playerRecreatedAfterSystemHome",true)
        return out
    }
    fun run(i:Instrumentation,m:PtvModel):JSONObject {
        val result=JSONObject();fun shot(name:String){Thread.sleep(700);i.uiAutomation.takeScreenshot()?.let{b->File(i.targetContext.getExternalFilesDir(null),name).outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}}
        val start=System.currentTimeMillis();var ready=false
        while(System.currentTimeMillis()-start<25000){Thread.sleep(500);i.runOnMainSync{ready=m.feed.isNotEmpty()&&!m.busy};if(ready)break};check(ready)
        i.runOnMainSync{m.screen="home";m.lightTheme=false;if(m.includeUpcoming)m.toggleUpcoming()};Thread.sleep(2200);shot("qa030-home.png")
        InputQa.clickLabel(i,"查看资料与片源");i.runOnMainSync{check(m.screen=="detail");check(m.current==null);result.put("heroOpensDetailsWithoutPlayback",true)}
        Thread.sleep(4500);shot("qa030-detail.png")
        check(InputQa.hasLabel(i,"手动匹配"));check(InputQa.hasLabel(i,"清除匹配"));result.put("persistentProviderMatchActions",true)
        InputQa.clickLabel(i,"返回",true);Thread.sleep(800);i.runOnMainSync{check(m.screen=="home")};result.put("detailBackReturnsOrigin",true)
        InputQa.clickLabel(i,"显示未上映");Thread.sleep(1600);i.runOnMainSync{check(m.includeUpcoming);m.toggleUpcoming()};result.put("upcomingToggle",true)
        InputQa.clickLabel(i,"展开导航");shot("qa030-drawer.png");InputQa.clickLabel(i,"收起导航");result.put("drawerOverlayWorks",true)
        i.runOnMainSync{m.toggleTheme()};shot("qa030-light.png");i.runOnMainSync{m.toggleTheme()};result.put("themeSwitch",true)
        i.runOnMainSync{m.search("今天是太阳雨")};val searchUntil=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<searchUntil){Thread.sleep(250);var pending=false;i.runOnMainSync{pending=m.busy};if(!pending)break};shot("qa030-search.png")
        i.runOnMainSync{m.screen="library"};shot("qa030-category.png");i.runOnMainSync{m.screen="favorites"};shot("qa030-favorites.png");i.runOnMainSync{m.screen="history"};shot("qa030-history.png")
        val original=m.db.prefs.getString("rating_link_qa:tv:1_douban",null)
        val work=Work("1","QA","tv",provider="qa")
        try{m.metadata.saveRatingLink(work,"douban","1");m.metadata.saveRatingLink(work,"tmdb","2");m.metadata.clearRatingLink(work,"tmdb");check(m.db.prefs.getString("rating_link_${work.key}_douban",null)=="1");check(m.db.prefs.getString("rating_link_${work.key}_tmdb",null)=="none");result.put("clearOneProviderPreservesOther",true)}finally{m.db.prefs.edit().remove("rating_link_${work.key}_tmdb").apply();if(original==null)m.db.prefs.edit().remove("rating_link_${work.key}_douban").apply()else m.db.prefs.edit().putString("rating_link_${work.key}_douban",original).apply()}
        return result
    }
}






