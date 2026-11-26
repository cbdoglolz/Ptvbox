package com.cbdog.ptvbox
import android.content.Context
import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.*
object BackupQa054 {
 fun run(context:Context):JSONObject=runBlocking {
  val id="qa054_${java.util.UUID.randomUUID()}";fun isolated(p:String)=object:ContextWrapper(context){override fun getSharedPreferences(n:String,m:Int)=context.getSharedPreferences("${id}_${p}_$n",m);override fun getFilesDir()=java.io.File(context.cacheDir,"$id/$p").apply{mkdirs()}}
  val a=Library(isolated("a"),"$id-a.db");val b=Library(isolated("b"),"$id-b.db");var active=a;val exporter=BackupPortal054(a);val importer=BackupPortal054(b)
  val server=LocalConfigServer(context,status={JSONObject()},apply={JSONObject()},backup={j->if(active===a)exporter.apply(j)else importer.apply(j)})
  try{
   val w=Work("qa054","QA serial","tv",season=2);a.cache(w);a.toggleFavorite(w);a.save(Watch(w,"第03集","qa-site","qa-road",1080,123456,300000,updated=1234));a.secrets.save("tmdb","qa-credential-only");a.markEpisode(w,3,state=WatchState.PLANNED);check(a.snapshot().episodeMarks[episodeRef(w,3)]?.watchState==WatchState.PLANNED);check(!a.snapshot().episodeMarks.getValue(episodeRef(w,3)).watched)
   a.markEpisode(w,3,state=WatchState.WATCHING);check(a.snapshot().episodeMarks[episodeRef(w,3)]?.watchState==WatchState.WATCHING);a.markEpisode(w,3,state=WatchState.COMPLETED);check(a.snapshot().episodeMarks.getValue(episodeRef(w,3)).watched);a.markEpisode(w,3,clearState=true);check(a.snapshot().episodeMarks.getValue(episodeRef(w,3)).watchState==null);a.markEpisode(w,3,state=WatchState.WATCHING);check(a.last(w)?.position==123456L&&a.favorite(w))
   val link=server.start("127.0.0.1");val token=link.substringAfter('#');val trust=object:X509TrustManager{override fun getAcceptedIssuers()=emptyArray<X509Certificate>();override fun checkClientTrusted(c:Array<X509Certificate>,s:String){};override fun checkServerTrusted(c:Array<X509Certificate>,s:String){check(MessageDigest.getInstance("SHA-256").digest(c[0].encoded).joinToString(":"){"%02X".format(it)}==server.fingerprint)}};val tls=SSLContext.getInstance("TLS").apply{init(null,arrayOf(trust),null)}
   fun call(j:JSONObject,auth:Boolean=true,origin:String?=null):Pair<Int,JSONObject>{val text=j.toString();val raw=(tls.socketFactory.createSocket("127.0.0.1",server.port) as SSLSocket).use{s->s.soTimeout=30000;s.startHandshake();val h="POST /api/backup HTTP/1.1\r\nHost: 127.0.0.1:${server.port}\r\n"+(if(auth)"Authorization: Bearer $token\r\n"else"")+(origin?.let{"Origin: $it\r\n"}?:"")+"Content-Type: application/json\r\nContent-Length: ${text.toByteArray().size}\r\nConnection: close\r\n\r\n";s.outputStream.write((h+text).toByteArray());s.outputStream.flush();s.inputStream.bufferedReader().readText()};return raw.substringAfter(' ').substringBefore(' ').toInt() to JSONObject(raw.substringAfter("\r\n\r\n"))}
   check(call(JSONObject().put("action","export"),auth=false).first==403);check(call(JSONObject().put("action","export"),origin="https://untrusted.invalid").first==403)
   val exported=call(JSONObject().put("action","export")).second
   check(exported.has("data")){"export response: $exported"}
   val transfer=String(java.util.Base64.getDecoder().decode(exported.getString("data")),Charsets.UTF_8)
   check(!exported.toString().contains("qa-credential-only")&&transfer.contains("PTVBOX-XFER")&&transfer.contains("qa-credential-only"))
   active=b
   val badData=java.util.Base64.getEncoder().encodeToString("not-a-transfer".toByteArray())
   val bad=call(JSONObject().put("action","preview").put("data",badData)).second
   check(bad.optBoolean("error")&&b.cached().isEmpty())
   val preview=call(JSONObject().put("action","preview").put("data",exported.getString("data"))).second
   check(preview.has("ticket")&&!preview.toString().contains("qa-credential-only"));check(b.cached().isEmpty())
   val confirm=JSONObject().put("action","import").put("ticket",preview.getString("ticket")).put("confirmed",true);val restored=call(confirm).second;check(!restored.optBoolean("error"));check(b.secrets.read("tmdb")=="qa-credential-only"&&b.favorite(w)&&b.last(w)?.position==123456L&&b.snapshot().episodeMarks[episodeRef(w,3)]?.watchState==WatchState.WATCHING);check(call(confirm).second.optBoolean("error"));check(b.favorite(w))
   JSONObject().put("backupEndpointRejectsAnonymous",true).put("backupEndpointRejectsCrossOrigin",true).put("exportUsesPasswordlessTransfer",true).put("invalidTransferChangesNothing",true).put("previewDoesNotWriteOrRevealCredentials",true).put("confirmedImportRestoresCredentialsMarksAndProgress",true).put("ticketIsSingleUse",true).put("fourEpisodeStatesPreserveEpisodeAndProgress",true)
  }finally{server.close();exporter.clear();importer.clear();a.close();b.close();context.deleteDatabase("$id-a.db");context.deleteDatabase("$id-b.db");for(p in listOf("a","b"))for(n in listOf("preferences","encrypted_credentials"))context.deleteSharedPreferences("${id}_${p}_$n");java.io.File(context.cacheDir,id).deleteRecursively()}
 }
}
