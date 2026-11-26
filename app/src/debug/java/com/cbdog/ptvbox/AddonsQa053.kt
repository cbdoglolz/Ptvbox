package com.cbdog.ptvbox

import android.content.Context
import android.content.ContextWrapper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import java.io.File
import java.util.UUID

object AddonsQa053 {
 fun contracts(context:Context):JSONObject {
  val id="qa053_${UUID.randomUUID()}";val base=File(context.cacheDir,id).apply{mkdirs()}
  fun isolated(prefix:String)=object:ContextWrapper(context){override fun getSharedPreferences(name:String,mode:Int)=context.getSharedPreferences("${id}_${prefix}_$name",mode);override fun getFilesDir()=File(base,prefix).apply{mkdirs()}}
  val ca=isolated("a");val cb=isolated("b");val a=Library(ca,"$id-a.db");val b=Library(cb,"$id-b.db");val out=JSONObject()
  try{
   for(k in listOf("subdl","assrt","opensub_key","opensub_user","opensub_password","danmaku_secret","live_entry_qa","custom-future-secret"))a.secrets.save(k,"qa-fixture-$k")
   a.prefs.edit().putString("rating_manual_qa","38405325").putString("audio_qa","ja").commit()
   val w=Work("qa053","QA series","tv","2026",season=2);a.cache(w);a.toggleFavorite(w);a.save(Watch(w,"第03集","qa","1080p",1080,123456,500000,updated=1000));val name="a".repeat(64)+".json";File(a.userMediaDir,name).writeText("""{"text":"fixture","delay":500}""")
   val archive=UserBackup.export(a);val bytes=BackupCipher.encrypt(archive.toString().toByteArray(),"qa-fixture-password".toCharArray());check(!bytes.toString(Charsets.ISO_8859_1).contains("qa-fixture-subdl"));check(runCatching{BackupCipher.decrypt(bytes,"wrong-password".toCharArray())}.isFailure)
   b.secrets.save("opensub_user","qa-current-user");val decoded=JSONObject(BackupCipher.decrypt(bytes,"qa-fixture-password".toCharArray()).toString(Charsets.UTF_8));UserBackup.restore(b,decoded);check(b.secrets.read("opensub_user")=="qa-current-user");check(b.secrets.read("live_entry_qa")=="qa-fixture-live_entry_qa");check(b.secrets.read("custom-future-secret")=="qa-fixture-custom-future-secret");check(b.favorite(w)&&b.last(w)?.position==123456L);check(File(b.userMediaDir,name).readText().contains("fixture"));check(b.prefs.getString("audio_qa","")=="ja");UserBackup.restore(b,decoded,true);check(b.secrets.read("opensub_user")=="qa-fixture-opensub_user");out.put("portableCredentialsFavoritesProgressAndAudio",true).put("dynamicLiveCredentialsIncluded",true).put("subtitleCacheAndManualLinksIncluded",true).put("existingCredentialsPreservedUnlessExplicitReplace",true).put("wrongBackupPasswordRejected",true)
   val invalid=JSONObject(decoded.toString()).put("userMedia",JSONObject().put("../escape.json","e30="));check(runCatching{UserBackup.restore(b,invalid)}.isFailure);check(b.favorite(w));out.put("invalidAttachmentRejectedBeforeMutation",true);val badPrefs=JSONObject(decoded.toString()).put("preferences",JSONObject().put("invalid",JSONObject().put("type","Long").put("value","not-a-number")));check(runCatching{UserBackup.restore(b,badPrefs)}.isFailure);check(b.favorite(w));out.put("invalidPreferenceRejectedBeforeMutation",true)
   var downloadPosts=0;var fileRequests=0;var failPost=false;var failFile=false
   val srt="1\n00:00:01,000 --> 00:00:04,000\n中文测试\n"
   val http=OkHttpClient.Builder().addInterceptor{chain->val r=chain.request();val path=r.url.encodedPath;var code=200;var body="{}";var mime="application/json"
    when{
     r.url.host=="api.subdl.com"->{check(r.url.queryParameter("api_key")=="qa-fixture-subdl");body="""{"status":true,"subtitles":[{"release_name":"QA S02E03","url":"/fixture.srt","season":2,"episode":3},{"release_name":"QA S02E04","url":"/wrong.srt","season":2,"episode":4}]}"""}
     path=="/v1/sub/search"->{check(r.header("Authorization")=="Bearer qa-fixture-assrt");body="""{"status":0,"sub":{"subs":[{"id":123,"native_name":"QA S02E03","lang":{"desc":"简体中文"}}]}}"""}
     path=="/v1/sub/detail"->body="""{"status":0,"sub":{"subs":[{"filelist":[{"f":"QA S02E03.chs.srt","url":"https://cdn.assrt.net/fixture.srt"},{"f":"QA S02E04.chs.srt","url":"https://cdn.assrt.net/wrong.srt"}]}]}}"""
     path=="/api/v1/subtitles"->{check(r.header("Api-Key")=="qa-fixture-opensub_key");check(r.url.queryParameter("season_number")=="2"&&r.url.queryParameter("episode_number")=="3");body="""{"data":[{"id":"wrong-subtitle-id","attributes":{"release":"QA S02E03","feature_details":{"season_number":2,"episode_number":3},"files":[{"file_id":789,"file_name":"QA S02E03.chs.srt"}]}}]}"""}
     path=="/api/v1/login"->{check(r.method=="POST");body="""{"token":"qa-jwt","base_url":"vip-api.opensubtitles.com"}"""}
     path=="/api/v1/infos/user"->{check(r.url.host=="vip-api.opensubtitles.com");check(r.header("Authorization")=="Bearer qa-jwt");body="""{"data":{"username":"fixture"}}"""}
     path=="/api/v1/download"->{downloadPosts++;check(r.header("Api-Key")=="qa-fixture-opensub_key"&&r.header("Authorization")=="Bearer qa-jwt");check(r.header("User-Agent")=="Ptvbox v${BuildConfig.VERSION_NAME}");val buf=okio.Buffer();r.body!!.writeTo(buf);check(JSONObject(buf.readUtf8()).getLong("file_id")==789L);if(failPost){code=503;body="<html>gateway fixture</html>";mime="text/html"}else body="""{"link":"https://cdn.example.invalid/fixture.srt"}"""}
     else->{fileRequests++;check(r.header("Api-Key")==null&&r.header("Authorization")==null);if(failFile){code=503;body="<html>CDN fixture</html>";mime="text/html"}else{body=srt;mime="text/plain"}}
    };Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(code).message("fixture").header("Content-Type",mime).header("Retry-After","30").body(body.toResponseBody(mime.toMediaType())).build()
   }.build();val service=OnlineSubtitles053(a.secrets,http);val c=MediaContext053(w,3)
   check(service.search("SubDL",c).size==1);val pack=service.search("ASSRT",c).single();check(pack.pack);val file=service.files(pack,c).single();check(file.title.contains("S02E03"));check(service.download(file).single().second==srt);val os=service.search("OpenSubtitles",c).single();check(os.id=="789");check(service.download(os).single().second==srt);check(downloadPosts==1&&fileRequests==2);out.put("allThreeProvidersSearchAndFilesContract",true).put("wrongEpisodeFilteredFromSearchAndPack",true).put("opensubFileIdLoginBaseUrlAndBearer",true).put("cdnNeverReceivesServiceCredentials",true)
   failPost=true;val err=runCatching{service.download(os)}.exceptionOrNull()?.message.orEmpty();check(err.contains("获取下载链接 HTTP 503")&&err.contains("30"));check(downloadPosts==2);out.put("503StageAndRetryAfterNoQuotaRetry",true);failPost=false;failFile=true;check(runCatching{service.download(os)}.exceptionOrNull()?.message.orEmpty().contains("字幕文件下载 HTTP 503"));out.put("fileCdn503Distinguished",true)
   return out
  }finally{a.close();b.close();context.deleteDatabase("$id-a.db");context.deleteDatabase("$id-b.db");for(p in listOf("a","b"))for(n in listOf("preferences","encrypted_credentials"))context.deleteSharedPreferences("${id}_${p}_$n");base.deleteRecursively()}
 }
}
