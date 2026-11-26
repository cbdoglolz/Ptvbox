package com.cbdog.ptvbox

import android.app.Activity
import android.app.ActivityManager
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File

/** Debug-only scene setup plus real input injection. No owner Release storage is accessed. */
object UiQa040 {
    private fun shot(i:Instrumentation,name:String){Thread.sleep(500);i.uiAutomation.takeScreenshot()?.let{b->File(i.targetContext.getExternalFilesDir(null),name).outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}}
    private fun swipe(i:Instrumentation,nav:Boolean=false,up:Boolean=false){
        val metrics=i.targetContext.resources.displayMetrics;val x=if(nav)163*metrics.density else metrics.widthPixels*.8f
        val start=metrics.heightPixels*(if(up).3f else .84f);val end=metrics.heightPixels*(if(up).85f else .3f);val time=SystemClock.uptimeMillis()
        fun event(action:Int,y:Float){val e=MotionEvent.obtain(time,SystemClock.uptimeMillis(),action,x,y,0);e.source=InputDevice.SOURCE_TOUCHSCREEN;try{check(i.uiAutomation.injectInputEvent(e,true))}finally{e.recycle()}}
        event(MotionEvent.ACTION_DOWN,start);for(n in 1..14){Thread.sleep(20);event(MotionEvent.ACTION_MOVE,start+(end-start)*n/14)};event(MotionEvent.ACTION_UP,end);Thread.sleep(300)
    }
    private fun reach(i:Instrumentation,label:String,nav:Boolean=false){repeat(12){if(InputQa.hasLabel(i,label))return;swipe(i,nav)};check(InputQa.hasLabel(i,label)){"Missing visible label $label"}}
    private fun await(until:Long=15000,condition:()->Boolean){val end=System.currentTimeMillis()+until;while(System.currentTimeMillis()<end){if(condition())return;Thread.sleep(150)};check(condition())}
    fun run(i:Instrumentation,m:PtvModel):JSONObject {
        val out=JSONObject();await(10000){var loaded=false;i.runOnMainSync{loaded=m.availableSites.isNotEmpty()};loaded};val originalSites=m.availableSites;val originalEnabled=m.enabledSiteKeys
        val keys=listOf("search_parallel","secondary_rating","drawer_pinned","settings_section","preload_catalog","search_display")
        val prior=keys.associateWith{m.db.prefs.all[it]};val synth=(1..180).map{Site("qa040-ui-$it","QA Station %03d".format(it),"https://example.invalid/api",group="QA UI")}
        try{
            await(30000){var ready=false;i.runOnMainSync{ready=m.feed.isNotEmpty()};ready}
            i.runOnMainSync{m.screen="home";m.lightTheme=false;m.settingChanged("preload_catalog",false)};Thread.sleep(800)
            check(InputQa.hasLabel(i,"Ptvbox"));check(!InputQa.hasLabel(i,"PCBOX"));out.put("ptvboxBrand",true);shot(i,"qa040-home.png")
            InputQa.clickLabel(i,"查看资料与片源");i.runOnMainSync{check(m.screen=="detail"&&m.current==null)};Thread.sleep(3500);shot(i,"qa040-detail-top.png")
            reach(i,"手动匹配");check(InputQa.hasLabel(i,"清除匹配"));check(InputQa.hasLabel(i,"刷新全部资料"));out.put("detailHasPcActionsWithoutAutoplay",true)
            InputQa.clickLabel(i,"手动匹配");check(InputQa.hasLabel(i,"搜索"));InputQa.clickLabel(i,"关闭");out.put("manualMatchDialogAndReturn",true)
            reach(i,"查找普通线路与选集");shot(i,"qa040-detail-source-section.png");out.put("sourceSectionBeforeCredits",true)
            i.sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(250);i.runOnMainSync{check(m.screen=="home")};out.put("detailSystemBackToOrigin",true)
            InputQa.clickLabel(i,"设置");Thread.sleep(300);i.runOnMainSync{check(m.screen=="settings")};shot(i,"qa040-settings-appearance.png")
            InputQa.clickLabel(i,"影视资料");Thread.sleep(300);reach(i,"TMDB Key / Read Token");check(!InputQa.hasLabel(i,"海报主评分"));out.put("metadataUsesDialogCard",true);shot(i,"qa040-settings-metadata.png")
            InputQa.clickLabel(i,"播放与自动选源");Thread.sleep(250);InputQa.clickLabel(i,"20路");i.runOnMainSync{check(m.config.parallel==20)};out.put("searchParallel20Saved",true);shot(i,"qa040-settings-playback.png")
            i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT);i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT);out.put("settingsDpadRoundTripNoCrash",true)
            reach(i,"允许系统后台播放");InputQa.clickLabel(i,"影视资料");InputQa.clickLabel(i,"播放与自动选源");check(InputQa.hasLabel(i,"允许系统后台播放"));out.put("settingsSectionScrollRestored",true)
            i.runOnMainSync{m.utility("sites")};Thread.sleep(300)
            i.runOnMainSync{check(m.screen=="sites");m.availableSites=originalSites+synth;m.enabledSiteKeys=originalEnabled+synth.map{it.key}};Thread.sleep(500);shot(i,"qa040-sites-top.png")
            InputQa.clickLabel(i,"QA UI 180");InputQa.clickLabel(i,"整组关闭");i.runOnMainSync{check(synth.none{m.siteEnabled(it)});check(synth.none{m.sites.enabled(it)})};out.put("groupSwitchSync",true)
            repeat(4){swipe(i)};shot(i,"qa040-sites-scrolled.png");out.put("virtualized180StationScroll",true)
            InputQa.clickLabel(i,"提高");i.runOnMainSync{check(m.config.groupPriority("QA UI")==1)};out.put("groupPrioritySaved",true)
            i.runOnMainSync{m.availableSites=originalSites;m.enabledSiteKeys=originalEnabled};i.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);Thread.sleep(300);i.runOnMainSync{check(m.screen=="settings")};out.put("sitesBackToSettings",true)
            i.runOnMainSync{m.settingsSection="subtitles"};Thread.sleep(500);shot(i,"qa040-settings-subtitle-services.png")
            i.runOnMainSync{m.settingsSection="network"};Thread.sleep(500);shot(i,"qa040-settings-network.png")
            i.runOnMainSync{m.settingsSection="about"};Thread.sleep(500);shot(i,"qa040-settings-about.png")
            return out
        }finally{
            val edit=m.db.prefs.edit();for((k,v) in prior){when(v){null->edit.remove(k);is String->edit.putString(k,v);is Int->edit.putInt(k,v);is Boolean->edit.putBoolean(k,v)}};synth.forEach{edit.remove("site_${it.key}")};edit.remove("priority_QA UI").commit()
            i.runOnMainSync{m.availableSites=originalSites;m.enabledSiteKeys=originalEnabled;m.preferencesRevision++;m.settingsSection="interface";m.screen="home"}
        }
    }
    fun player(i:Instrumentation,m:PtvModel,a:Activity):JSONObject {
        val out=JSONObject();val old=m.config.background
        try{
            i.runOnMainSync{m.settingChanged("background_playback",true);m.player.play()}
            await(5000){PlaybackOwner.serviceRunning};var before=0L;i.runOnMainSync{before=m.player.currentPosition}
            check(i.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME));Thread.sleep(2500)
            var retained=false;i.runOnMainSync{retained=m.current!=null&&m.player.isPlaying&&m.player.currentPosition>before&&m.player.mediaItemCount>0};check(retained){"Configured background playback did not continue"}
            @Suppress("DEPRECATION") val services=a.getSystemService(ActivityManager::class.java).getRunningServices(50)
            check(services.any{it.service.className==PlaybackSessionService::class.java.name&&it.foreground}){"Media service was not an actual foreground service"}
            out.put("optInBackgroundKeepsAdvancing",true).put("actualForegroundMediaService",true)
            a.startActivity(Intent(a,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));await(5000){var focused=false;i.runOnMainSync{focused=a.hasWindowFocus()};focused}
            i.runOnMainSync{m.settingChanged("background_playback",false)};await(5000){!PlaybackOwner.serviceRunning};out.put("disableBackgroundReleasesService",true)
            if(InputQa.hasLabel(i,"显示控制"))InputQa.clickLabel(i,"显示控制")
            out.put("defaultLifecycle",UiQa030.player(i,m,a))
            return out
        }finally{i.runOnMainSync{m.settingChanged("background_playback",old)}}
    }
}
