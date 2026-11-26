package com.cbdog.ptvbox

import android.content.Context
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SourceQa022 {
    fun run(context:Context,configUrl:String):JSONObject=runBlocking {
        val db=Library(context);val sites=Sites(context,db);try{
            val config=Net.json(configUrl);db.secrets.save("source_config",config.toString())
            val all=sites.all().filter{it.group=="私人订阅"};val rows=JSONArray()
            for(api in listOf("csp_HmysGuard","csp_AppgzGuard","csp_T4Guard","csp_LibvioGuard")){
                val site=all.firstOrNull{it.api==api}?:continue
                val start=System.currentTimeMillis();val row=JSONObject().put("apiClass",api)
                try {
                    val found=sites.search(site,"肖申克的救赎")
                    row.put("results",found.size)
                    if(found.isNotEmpty()) {
                        val detail=sites.detail(site,found.first())
                        row.put("roads",detail.roads.size)
                        val road=detail.roads.firstOrNull()
                        val ep=road?.episodes?.firstOrNull()
                        if(road!=null&&ep!=null) {
                            try{val candidate=sites.probe(site,road,ep);row.put("mediaProbe",true).put("height",candidate.height)}
                            catch(e:Throwable){row.put("probeFailure",e.javaClass.simpleName).put("probeReason",e.message?.takeIf{!it.contains("http")})}
                        }
                    }
                } catch(e:Throwable) {
                    row.put("failure",e.javaClass.simpleName).put("reason",e.message?.takeIf{!it.contains("http")})
                }
                row.put("elapsedMs",System.currentTimeMillis()-start);rows.put(row)
            }
            JSONObject().put("privateCount",all.size).put("supportedCount",all.count(sites::supports)).put("rows",rows)
        }finally{sites.close();db.close()}
    }
}
