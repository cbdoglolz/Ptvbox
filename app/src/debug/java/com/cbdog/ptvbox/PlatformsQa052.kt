package com.cbdog.ptvbox
import android.app.Instrumentation
import kotlinx.coroutines.*
import org.json.*
object PlatformsQa052 {
 fun run(i:Instrumentation,m:PtvModel):JSONObject {val site=m.sites.all().first{it.api=="csp_Live"};val records=JSONArray()
  for((id,name)in listOf("bili\$7788489" to "B站","cc\$359820972" to "网易")){val r=JSONObject().put("platform",name);records.put(r)
   try{val v=runBlocking{withTimeout(45000){m.sites.detail(site,Vod(id,name,"","","",emptyList()))}};r.put("title",v.title).put("roads",v.roads.size);val errors=JSONArray();r.put("attempts",errors)
    routes@ for(road in v.roads.take(3))for(ep in road.episodes.take(1)){val a=JSONObject().put("road",road.name);errors.put(a);try{val c=runBlocking{withTimeout(25000){m.sites.probe(site,road,ep)}};a.put("probe",true);i.runOnMainSync{m.current=Work("qa-platform052-$name",v.title,provider="live");m.candidates=listOf(c);m.select(c,0,false);m.player.volume=0f};try{UiQa050.await(25000){var ok=false;i.runOnMainSync{ok=m.hasRenderedFrame()};ok};a.put("firstFrame",true);UiQa050.shot(i,"qa052-platform-${if(name=="B站")"bili"else"cc"}.png")}catch(_:Exception){a.put("firstFrame",false)};i.runOnMainSync{m.player.stop()};if(a.optBoolean("firstFrame"))break@routes}catch(e:Exception){a.put("error",e.message?.replace(Regex("https?://[^\\s]+"),"<url>")?.take(130))}}
   }catch(e:Exception){r.put("error",e.message?.replace(Regex("https?://[^\\s]+"),"<url>")?.take(130))}
  };return JSONObject().put("platforms",records)
 }
}

