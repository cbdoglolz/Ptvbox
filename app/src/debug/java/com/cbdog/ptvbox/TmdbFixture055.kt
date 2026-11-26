package com.cbdog.ptvbox
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONObject
import org.json.JSONArray
/** Deterministic latency fixture, debug only. No borrowed key and no owner Release storage. */
class TmdbFixture055(private val m:PtvModel):java.io.Closeable {
 private val field=NetworkPolicy::class.java.getDeclaredField("current").apply{isAccessible=true};private val original=NetworkPolicy.client();private val credential=m.db.secrets.read("tmdb")
 init{m.db.secrets.save("tmdb","00000000000000000000000000000000");field.set(null,original.newBuilder().addInterceptor{chain->if(chain.request().url.host!="api.themoviedb.org")return@addInterceptor chain.proceed(chain.request());val path=chain.request().url.encodedPath;val body=when{
  path=="/3/tv/1396"->{Thread.sleep(3200);JSONObject().put("id",1396).put("name","绝命毒师").put("first_air_date","2008-01-20").put("seasons",JSONArray().put(JSONObject().put("season_number",1).put("name","第一季").put("air_date","2008-01-20").put("episode_count",3)))}
  path=="/3/tv/1396/season/1"->{Thread.sleep(3200);JSONObject().put("episodes",JSONArray((1..3).map{n->JSONObject().put("id",100+n).put("season_number",1).put("episode_number",n).put("name","第${n}集测试标题").put("overview","第${n}集故事简介 · QA模拟数据").put("vote_average",7.5).put("vote_count",12)}))}
  else->JSONObject().put("results",JSONArray())
 };Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("QA fixture").body(body.toString().toResponseBody("application/json".toMediaType())).build()}.build())}
 override fun close(){field.set(null,original);m.db.secrets.save("tmdb",credential)}
}
