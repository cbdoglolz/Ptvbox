package com.cbdog.ptvbox

import org.json.JSONObject
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*
import kotlinx.coroutines.runBlocking

object ConfigQa022 {
    fun run(context:android.content.Context):JSONObject=runBlocking {
        var writes=0
        val server=LocalConfigServer(context,status={JSONObject().put("tmdbSaved",true)},apply={writes++;JSONObject().put("message","saved")})
        val link=server.start("127.0.0.1");val token=link.substringAfter('#')
        val trust=object:X509TrustManager {
            override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
            override fun checkClientTrusted(c:Array<X509Certificate>,a:String){}
            override fun checkServerTrusted(c:Array<X509Certificate>,a:String){check(MessageDigest.getInstance("SHA-256").digest(c[0].encoded).joinToString(":"){"%02X".format(it)}==server.fingerprint)}
        }
        val tls=SSLContext.getInstance("TLS").apply{init(null,arrayOf(trust),null)}
        fun request(method:String,path:String,auth:Boolean=false,origin:String?=null,body:String="",host:String="127.0.0.1:${server.port}"):String {
            val s=tls.socketFactory.createSocket("127.0.0.1",server.port) as SSLSocket
            return s.use{it.soTimeout=15000;it.startHandshake();val headers="$method $path HTTP/1.1\r\nHost: $host\r\n"+(if(auth)"Authorization: Bearer $token\r\n"else"")+(origin?.let{"Origin: $it\r\n"}?:"")+"Content-Type: application/json\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n";it.outputStream.write((headers+body).toByteArray());it.outputStream.flush();it.inputStream.bufferedReader().readText()}
        }
        try {
            check(request("GET","/").contains("Ptvbox"));check(request("GET","/api/status").startsWith("HTTP/1.1 403"));check(writes==0)
            val status=request("GET","/api/status",true);check(status.startsWith("HTTP/1.1 200")&&status.contains("tmdbSaved")&&!status.contains(token))
            check(request("POST","/api/config",true,"https://untrusted.invalid","{}").startsWith("HTTP/1.1 403"));check(writes==0)
            check(request("GET","/api/status",true,host="untrusted.invalid").startsWith("HTTP/1.1 403"))
            check(request("POST","/api/config",true,body="{}").startsWith("HTTP/1.1 200"));check(writes==1)
            check(server.running);server.close();check(!server.running)
            JSONObject().put("tlsPinnedCertificate",true).put("anonymousPageOnly",true).put("unauthorizedRejected",true).put("noCredentialReadBack",true).put("crossOriginRejected",true).put("wrongHostRejected",true).put("authorizedWriteOnce",true).put("stopRevokesSession",true)
        }catch(e:Exception){Thread.sleep(500);throw IllegalStateException("HTTPS test: ${e.javaClass.simpleName}: ${e.message}; server: ${server.lastError}") }finally{server.close()}
    }
}
