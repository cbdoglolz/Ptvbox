package com.cbdog.ptvbox

import android.app.Instrumentation
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.*
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*

/** Production settings callbacks exercised only in the separate QA application's storage. */
object RemoteFlowQa {
    fun run(i:Instrumentation,m:PtvModel):JSONObject {
        val priorAssrt=m.db.secrets.read("assrt");val priorProfile=m.db.prefs.all["device_profile"];val priorParallel=m.db.prefs.all["search_parallel"];val priorConfig=m.db.secrets.read("source_config");val priorSub=m.db.secrets.read("subscription");val priorKey=m.db.secrets.read("tmdb");val priorPending=m.db.secrets.read("tmdb_pending");val priorStatus=m.db.prefs.getString("tmdb_status","")
        val fixture=java.net.ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))
        val data="""{"sites":[{"key":"qa-remote-flow","name":"QA temporary station","api":"https://example.invalid/api","type":1}]}""".toByteArray()
        val t=Thread{runCatching{fixture.accept().use{s->s.soTimeout=10000;val reader=s.getInputStream().bufferedReader();while(!reader.readLine().isNullOrBlank()){};s.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${data.size}\r\nConnection: close\r\n\r\n".toByteArray()+data)}}}.apply{isDaemon=true;start()}
        try {
            i.runOnMainSync{m.screen="settings";m.startRemoteConfig("127.0.0.1")}
            repeat(100){if(m.remoteUrl.isBlank())Thread.sleep(100)};check(m.remoteUrl.isNotBlank())
            val uri=URI(m.remoteUrl.substringBefore('#'));val token=m.remoteUrl.substringAfter('#');val fingerprint=m.remoteFingerprint
            val trust=object:X509TrustManager {
                override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
                override fun checkClientTrusted(c:Array<X509Certificate>,a:String){}
                override fun checkServerTrusted(c:Array<X509Certificate>,a:String){check(MessageDigest.getInstance("SHA-256").digest(c[0].encoded).joinToString(":"){"%02X".format(it)}==fingerprint)}
            }
            val tls=SSLContext.getInstance("TLS").apply{init(null,arrayOf(trust),null)}
            fun post(body:String):String=(tls.socketFactory.createSocket(uri.host,uri.port) as SSLSocket).use{s->s.soTimeout=30000;s.startHandshake();s.outputStream.write(("POST /api/config HTTP/1.1\r\nHost: ${uri.host}:${uri.port}\r\nOrigin: https://${uri.host}:${uri.port}\r\nAuthorization: Bearer $token\r\nContent-Type: application/json\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n"+body).toByteArray());s.outputStream.flush();s.inputStream.bufferedReader().readText()}
            val body=JSONObject().put("tmdb","invalid-short-key").put("subscription","http://127.0.0.1:${fixture.localPort}/").put("credentials",JSONObject().put("assrt","qa-local-placeholder-token")).put("settings",JSONObject().put("device_profile","economy").put("search_parallel",20)).toString()
            val response=post(body);check(response.startsWith("HTTP/1.1 200"));check(!response.contains(token));check(!response.contains("qa-local-placeholder-token"));check(m.db.secrets.read("assrt")=="qa-local-placeholder-token");i.runOnMainSync{check(m.config.parallel==20&&m.config.text("device_profile")=="economy")}
            check(m.db.secrets.read("tmdb")==priorKey)
            check(m.sites.all().any{it.key=="qa-remote-flow"})
            i.runOnMainSync{check(m.availableSites.any{it.key=="qa-remote-flow"})}
            return JSONObject().put("productionHttpsWrite",true).put("invalidKeyPreservesActive",true).put("subscriptionStoredEncrypted",m.db.secrets.read("source_config").contains("qa-remote-flow")).put("uiStationListRefreshed",true).put("noTokenReadback",true).put("serviceCredentialSavedEncrypted",true).put("profileAndParallelApplied",true)
        }finally {
            fixture.close();t.join(1000);m.db.secrets.save("assrt",priorAssrt);m.db.prefs.edit().apply{if(priorProfile==null)remove("device_profile")else putString("device_profile",priorProfile as String);if(priorParallel==null)remove("search_parallel")else putInt("search_parallel",priorParallel as Int)}.commit()
            m.db.secrets.save("source_config",priorConfig);m.db.secrets.save("subscription",priorSub);m.db.secrets.save("tmdb",priorKey);m.db.secrets.save("tmdb_pending",priorPending);m.db.prefs.edit().putString("tmdb_status",priorStatus).commit()
            i.runOnMainSync{m.stopRemoteConfig()}
        }
    }
}
