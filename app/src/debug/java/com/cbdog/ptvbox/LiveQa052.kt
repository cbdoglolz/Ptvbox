package com.cbdog.ptvbox
import android.app.Instrumentation
import kotlinx.coroutines.*
import org.json.*
import java.io.File
object LiveQa052 {
 fun run(i:Instrumentation,m:PtvModel,only:String=""):JSONObject=runBlocking {
  val records=JSONArray();val privateRows=JSONArray()
  val configured=m.sites.all();val sources=LiveParser050.sources("{}","",configured).filter{it.site!=null};val sites=sources.map{it.site!!}.filter{only.isBlank()||it.api==only}
  require(sites.isNotEmpty()){"QA profile has no live sites"}
  for(s in sites){val record=JSONObject().put("site",s.name).put("class",s.api);records.put(record)
   try{val home=withTimeout(60000){m.sites.browse(s)};val platform=sources.firstOrNull{it.key==s.key}?.platform.orEmpty();val cats=home.optJSONArray("class")?.objects().orEmpty().filter{platform.isBlank()||it.optString("type_name").contains(platform)};record.put("categories",cats.map{it.optString("type_name")});val samples=JSONArray();record.put("samples",samples)
    for(cat in cats.take(if(only.isNotBlank()&&!only.contains("Kanqiu"))1 else 3)){val page=withTimeout(45000){m.sites.browse(s,cat.optString("type_id"),1)};val rows=page.optJSONArray("list")?.objects().orEmpty();val sample=JSONObject().put("category",cat.optString("type_name")).put("rows",rows.size);samples.put(sample)
     val raw=rows.firstOrNull{val v=VodParser.parse(it);!LiveParser050.directoryLabel(v)&&!LiveParser050.awaitingSignal(v)}?:rows.firstOrNull{!LiveParser050.directoryLabel(VodParser.parse(it))}?:continue;privateRows.put(JSONObject().put("site",s.json()).put("vod",raw));var item=VodParser.parse(raw);repeat(3){if(LiveParser050.folder(s,item)){val nested=m.sites.browse(s,item.id,1).optJSONArray("list")?.objects().orEmpty();sample.put("nestedRooms",nested.size);if(nested.isNotEmpty())item=VodParser.parse(nested.first())}};if(LiveParser050.awaitingSignal(item)){sample.put("notYetBroadcast",true);continue};val v=m.sites.detail(s,item);sample.put("title",v.title).put("roads",v.roads.size);privateRows.put(JSONObject().put("site",s.json()).put("vod",raw).put("roads",JSONArray(v.roads.map{r->JSONObject().put("name",r.name).put("entries",JSONArray(r.episodes.map{e->JSONObject().put("name",e.name).put("url",e.url)}))})))
     val attempts=JSONArray();sample.put("attempts",attempts)
     routes@ for(r in v.roads.take(2))for(ep in r.episodes.take(1)){val a=JSONObject().put("road",r.name);attempts.put(a);try{val c=withTimeout(30000){m.sites.probe(s,r,ep)};a.put("probe",true).put("mime",c.mimeType);i.runOnMainSync{m.current=Work(v.id,v.title,provider="live");m.select(c,0,false);m.player.volume=0f};try{UiQa050.await(25000){var frame=false;i.runOnMainSync{frame=m.hasRenderedFrame()};frame};a.put("firstFrame",true)}catch(_:Exception){a.put("firstFrame",false)};i.runOnMainSync{m.player.stop()};if(a.optBoolean("firstFrame"))break@routes}catch(e:Exception){a.put("probe",false).put("error",e.message?.replace(Regex("https?://[^\\s]+"),"<url>")?.take(140))}}
     if(attempts.objects().any{it.optBoolean("firstFrame")})break
    }
   }catch(e:Exception){record.put("error",e.message?.replace(Regex("https?://[^\\s]+"),"<url>")?.take(140))}
  };File(i.targetContext.getExternalFilesDir(null),"live052-private.json").writeText(privateRows.toString());JSONObject().put("configuredCount",configured.size).put("pluginCount",configured.count{it.type==3}).put("sources",records)
 }
}
