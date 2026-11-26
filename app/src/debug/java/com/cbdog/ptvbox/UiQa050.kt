package com.cbdog.ptvbox
import android.app.Instrumentation
import android.graphics.Bitmap
import android.view.KeyEvent
import org.json.JSONObject
import java.io.File

object UiQa050 {
 fun await(ms:Long=15000,condition:()->Boolean){val end=System.currentTimeMillis()+ms;while(System.currentTimeMillis()<end){if(condition())return;Thread.sleep(150)};check(condition())}
 fun shot(i:Instrumentation,name:String){i.uiAutomation.takeScreenshot()?.let{b->File(i.targetContext.getExternalFilesDir(null),name).outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)}}}
 fun run(i:Instrumentation,m:PtvModel):JSONObject {val out=JSONObject();await(30000){var ok=false;i.runOnMainSync{ok=m.feed.isNotEmpty()};ok};i.runOnMainSync{m.screen="home"};Thread.sleep(800);check(!InputQa.hasLabel(i,"展开导航"));out.put("hamburgerRemoved",true)
  repeat(7){if(!InputQa.hasLabel(i,"导航提示：首页"))i.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_LEFT)};check(InputQa.hasLabel(i,"导航提示：首页"));out.put("dpadFocusShowsLabel",true);shot(i,"qa050-nav-focus.png")
  InputQa.clickLabel(i,"直播与体育");Thread.sleep(700);i.runOnMainSync{check(m.screen=="live")};check(!InputQa.hasLabel(i,"体育"));out.put("singleLiveAndSportsEntry",true);shot(i,"qa050-live-hub.png")
  i.runOnMainSync{m.screen="favorites"};Thread.sleep(500);for(label in CollectionTabs050.labels)check(InputQa.hasLabel(i,label));InputQa.clickLabel(i,"在看");check(InputQa.hasLabel(i,"暂无在看作品")||InputQa.hasLabel(i,"我的片库"));InputQa.clickLabel(i,"已看");InputQa.clickLabel(i,"想看");shot(i,"qa050-collection-tabs.png");out.put("fourCollectionFiltersTouchable",true)
  i.runOnMainSync{m.screen="home"};Thread.sleep(500);await(45000){var ok=false;i.runOnMainSync{ok=m.feed.firstOrNull()?.let{m.presentations[it.ratingViewKey]?.ratings?.get("douban")!=null}?:false};ok};out.put("homeRatingsLoadedWithoutOpeningDetails",true);shot(i,"qa050-home.png")
  InputQa.clickLabel(i,"查看资料与片源");Thread.sleep(2500);i.runOnMainSync{check(m.screen=="detail"&&m.current==null)};shot(i,"qa050-detail.png");out.put("detailStillDoesNotAutoplay",true)
  return out
 }
}
