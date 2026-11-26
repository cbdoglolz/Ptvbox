package com.cbdog.ptvbox
import android.app.Instrumentation
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*
object PortalQa052 {
 fun run(i:Instrumentation,m:PtvModel):JSONObject {val creds=listOf("assrt","live_subscription").associateWith{m.db.secrets.read(it)};val parallel=m.db.prefs.all["search_parallel"];val r=JSONObject()
  try{i.runOnMainSync{m.startRemoteConfig("127.0.0.1")};UiQa050.await{m.remoteUrl.isNotBlank()};val uri=URI(m.remoteUrl.substringBefore('#'));val token=m.remoteUrl.substringAfter('#');val fingerprint=m.remoteFingerprint
   val trust=object:X509TrustManager{override fun getAcceptedIssuers()=emptyArray<X509Certificate>();override fun checkClientTrusted(c:Array<X509Certificate>,a:String){};override fun checkServerTrusted(c:Array<X509Certificate>,a:String){check(MessageDigest.getInstance("SHA-256").digest(c[0].encoded).joinToString(":"){"%02X".format(it)}==fingerprint)}}
   val tls=SSLContext.getInstance("TLS").apply{init(null,arrayOf(trust),null)}
   fun call(body:JSONObject?=null):JSONObject=(tls.socketFactory.createSocket(uri.host,uri.port)as SSLSocket).use{s->s.soTimeout=35000;s.startHandshake();val b=body?.toString().orEmpty();s.outputStream.write(("${if(body==null)"GET /api/status"else"POST /api/config"} HTTP/1.1\r\nHost: ${uri.host}:${uri.port}\r\nOrigin: https://${uri.host}:${uri.port}\r\nAuthorization: Bearer $token\r\nContent-Type: application/json\r\nContent-Length: ${b.toByteArray().size}\r\nConnection: close\r\n\r\n"+b).toByteArray());s.outputStream.flush();val text=s.inputStream.bufferedReader().readText();check(text.startsWith("HTTP/1.1 200"));JSONObject(text.substringAfter("\r\n\r\n"))}
   call(JSONObject().put("fields",JSONObject().put("assrt","qa-placeholder-052").put("live_subscription","https://example.org/qa-channels.m3u").put("search_parallel","20")));val state=call();check(!state.has("hideSensitive"));val fields=state.getJSONArray("fields").objects();check(fields.first{it.getString("id")=="assrt"}.getString("value")=="qa-placeholder-052");check(fields.first{it.getString("id")=="live_subscription"}.getString("value")=="https://example.org/qa-channels.m3u");i.runOnMainSync{check(m.config.parallel==20&&m.configurationValues052["live_subscription"]=="https://example.org/qa-channels.m3u");m.screen="settings";m.settingsSection="subtitles"};Thread.sleep(650);check(!InputQa.hasLabel(i,"复制ASSRT Token"));check(InputQa.hasLabel(i,"ASSRT Token"));val ids=state.getJSONArray("pages").objects().associate{it.getString("id")to it.getString("title")};check("sites" in ids);check(ids["subtitles"]==SettingsInventory.portalSections.first{it.id=="subtitles"}.title);r.put("pairedHttpsSavesAndSyncsNative",true).put("allCredentialsPlaintextOnPairedPhone",true).put("nativeUsesDialogButtons",true).put("sitesOnlyInPhonePortal",true);return r
  }finally{creds.forEach{(k,v)->m.db.secrets.save(k,v)};m.db.prefs.edit().apply{remove("hide_sensitive");if(parallel==null)remove("search_parallel")else putInt("search_parallel",parallel as Int)}.commit();i.runOnMainSync{m.stopRemoteConfig()}}
 }
}
