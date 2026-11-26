package com.cbdog.ptvbox
import android.app.Instrumentation
import org.json.JSONObject
import kotlinx.coroutines.*
import java.io.File
object LiveQa054 {
 fun run(i:Instrumentation,m:PtvModel,sampleCount:Int=16):JSONObject=runBlocking {
  val result=JSONObject();val site=m.sites.all().first{it.api=="csp_Live"};result.put("noticeBridge",m.sites.prepareAuth054(site).optBoolean("noticeHook"));val home=withTimeout(45000){m.sites.browse(site)};val cats=home.optJSONArray("class")?.objects().orEmpty();val huya=cats.first{it.optString("type_name").contains("虎牙")};val first=withTimeout(45000){m.sites.browse(site,huya.optString("type_id"))};val categories=first.optJSONArray("list")?.objects().orEmpty().map(VodParser::parse);val lol=categories.firstOrNull{it.title.contains("LOL",true)||it.title.contains("英雄联盟")};val rooms=if(lol!=null&&LiveParser050.folder(site,lol))withTimeout(45000){m.sites.browse(site,lol.id)}.optJSONArray("list")?.objects().orEmpty().map(VodParser::parse)else categories
  val room=rooms.first{!LiveParser050.folder(site,it)&&!LiveParser050.directoryLabel(it)};result.put("category",lol?.title?:huya.optString("type_name")).put("room",room.title)
  i.runOnMainSync{m.playLive050(LiveEntry050(site,room));m.player.volume=0f};UiQa050.await(65000){var ok=false;i.runOnMainSync{ok=m.hasRenderedFrame()};ok};i.runOnMainSync{val c=m.currentCandidate!!;File(i.targetContext.getExternalFilesDir(null),"candidate054-private.json").writeText(JSONObject().put("site",c.site.json()).put("roomId",room.id).put("url",c.episode.url).toString());result.put("headerNames",org.json.JSONArray(c.site.headers.keys.toList())).put("rangeHeader",c.site.headers.entries.firstOrNull{it.key.equals("Range",true)}?.value.orEmpty())};val samples=org.json.JSONArray()
  repeat(sampleCount){delay(5000);i.runOnMainSync{samples.put(JSONObject().put("elapsedSeconds",(it+1)*5).put("position",m.player.currentPosition).put("state",m.player.playbackState).put("playing",m.player.isPlaying).put("mime",m.currentCandidate?.mimeType).put("nativeAdapter",m.currentCandidate?.site?.api?.contains("Alllive")==true).put("notice",m.notice.take(160)))};if(it==3||it==15)UiQa050.shot(i,"qa054-huya-${(it+1)*5}s.png")}
  result.put("samples",samples);i.runOnMainSync{result.put("ended",m.player.playbackState==androidx.media3.common.Player.STATE_ENDED).put("continuous80Seconds",m.player.isPlaying&&m.player.currentPosition>60000).put("height",m.player.videoSize.height);m.stop()};File(i.targetContext.getExternalFilesDir(null),"qa054-live.json").writeText(result.toString(2));result
 }
}
