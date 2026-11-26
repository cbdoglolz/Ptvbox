package com.cbdog.ptvbox

import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import java.security.MessageDigest

data class DanmakuMatch053(val id:String,val title:String,val episode:String)
class DanmakuService053(private val secrets:SecretStore){
 private val client=NetworkPolicy.client().newBuilder().followRedirects(false).callTimeout(20,java.util.concurrent.TimeUnit.SECONDS).build()
 private fun get(path:String,params:Map<String,String> = emptyMap()):JSONObject {
  val custom=secrets.read("danmaku_url");val app=secrets.read("danmaku_appid");val secret=secrets.read("danmaku_secret");val official=custom.isBlank()&&app.isNotBlank()&&secret.isNotBlank()
  val root=custom.ifBlank{if(official)"https://api.dandanplay.net/api/v2/"else"https://danmaku-api.152468.xyz/api/v2/"}.trimEnd('/')+"/"
  val url=(root+path).toHttpUrl().newBuilder().apply{params.forEach{(k,v)->addQueryParameter(k,v)}}.build();require(url.isHttps){"弹幕服务需要HTTPS"}
  val request=Request.Builder().url(url).header("User-Agent","Ptvbox v${BuildConfig.VERSION_NAME}")
  if(official){val stamp=(System.currentTimeMillis()/1000).toString();val signature=java.util.Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest((app+stamp+url.encodedPath+secret).toByteArray()));request.header("X-AppId",app).header("X-Timestamp",stamp).header("X-Signature",signature)}
  client.newCall(request.build()).execute().use{r->require(r.isSuccessful){"弹幕服务 HTTP ${r.code}"};val out=java.io.ByteArrayOutputStream();r.body.byteStream().use{input->val buf=ByteArray(8192);while(true){val n=input.read(buf);if(n<0)break;require(out.size()+n<=16_000_000);out.write(buf,0,n)}};return try{JSONObject(out.toString("UTF-8"))}catch(_:Exception){throw java.io.IOException("弹幕服务返回非JSON内容")}}
 }
 fun matches(c:MediaContext053):List<DanmakuMatch053>{return try{catalog(c).ifEmpty{bilibili(c)}}catch(e:Exception){bilibili(c).ifEmpty{throw e}}}
 private fun stripSequel(title:String)=title.replace(Regex("(?i)\\s*(?:续篇|续集|完结篇|新篇|后篇|前篇|最终季|final\\s*season|sequel)$"),"").trim()
 private fun seriesMatch(c:MediaContext053,title:String)=c.exactName(title)||c.exactName(stripSequel(title))
 private fun catalog(c:MediaContext053):List<DanmakuMatch053>{val out=mutableListOf<DanmakuMatch053>();for(name in c.names.take(3)){
  val animes=get("search/anime",mapOf("keyword" to name)).optJSONArray("animes")?.objects().orEmpty()
  val sameSeries=animes.take(12).filter{a->val title=a.optString("animeTitle");val type=a.optString("type");seriesMatch(c,title)&&(type.isBlank()||(c.work.kind=="movie")==(type=="movie"))}
  val direct=sameSeries.filter{a->val title=a.optString("animeTitle");val year=a.optString("startDate").take(4);val yearOk=c.work.season>1||c.work.year.length!=4||year.length!=4||year==c.work.year;c.exactName(title)&&c.accepts(title)&&yearOk&&(c.work.kind!="tv"||c.work.season<=1||Identity.season(title)==c.work.season)}
  val picked=direct.ifEmpty{
   if(c.work.kind=="tv"&&c.work.season>1){
    val ordered=sameSeries.sortedBy{it.optString("startDate").ifBlank{"9999"}}
    val byIndex=ordered.getOrNull(c.work.season-1)
    listOfNotNull(byIndex)
   }else emptyList()
  }
  for(a in picked){val title=a.optString("animeTitle")
   val j=get("bangumi/${a.optString("animeId")}");val bangumi=j.optJSONObject("bangumi")?:j;val eps=bangumi.optJSONArray("episodes")?.objects().orEmpty();for(e in eps){val number=e.optString("episodeNumber").toIntOrNull()?:Identity.episode(e.optString("episodeTitle"));if(c.work.kind=="movie"&&eps.size==1||c.work.kind=="tv"&&c.episode!=null&&number==c.episode){val id=e.optString("episodeId");if(id.isNotBlank())out+=DanmakuMatch053(id,title,e.optString("episodeTitle"))}}
  };if(out.isNotEmpty())break
 };return out.distinctBy{it.id}}
 private var buvid=""
 private fun biliText(url:String):String {val req=Request.Builder().url(url).header("User-Agent","Mozilla/5.0 Ptvbox/${BuildConfig.VERSION_NAME}").header("Referer","https://www.bilibili.com/");if(buvid.isNotBlank())req.header("Cookie","buvid3=$buvid");client.newCall(req.build()).execute().use{r->require(r.isSuccessful){"B站弹幕 HTTP ${r.code}"};return r.body.byteStream().use{MediaText053.bounded(it,16_000_000)}.toString(Charsets.UTF_8)}}
 private fun bilibili(c:MediaContext053):List<DanmakuMatch053>{runCatching{buvid=JSONObject(biliText("https://api.bilibili.com/x/frontend/finger/spi")).optJSONObject("data")?.optString("b_3").orEmpty()};val out=mutableListOf<DanmakuMatch053>();for(name in c.names.take(2)){for(type in if(c.work.kind=="movie")listOf("media_ft")else listOf("media_bangumi","media_ft")){
  val url="https://api.bilibili.com/x/web-interface/search/type".toHttpUrl().newBuilder().addQueryParameter("search_type",type).addQueryParameter("keyword",name).build();val rows=JSONObject(biliText(url.toString())).optJSONObject("data")?.optJSONArray("result")?.objects().orEmpty();for(row in rows.take(12)){val title=org.jsoup.Jsoup.parse(row.optString("title")).text();if(!c.exactName(title)||!c.accepts(title)||c.work.season>1&&Identity.season(title)!=c.work.season)continue;val id=row.optLong("season_id");if(id<=0)continue;val season=JSONObject(biliText("https://api.bilibili.com/pgc/view/web/season?season_id=$id")).optJSONObject("result")?:continue;val year=season.optJSONObject("publish")?.optString("pub_time").orEmpty().take(4);if(c.work.year.length==4&&year.length==4&&year!=c.work.year)continue;val eps=season.optJSONArray("episodes")?.objects().orEmpty();for(ep in eps){val number=ep.optString("title").toIntOrNull();if(c.work.kind=="movie"&&eps.size==1||c.work.kind=="tv"&&number==c.episode&&number!=null){val cid=ep.optLong("cid");if(cid>0)out+=DanmakuMatch053("bili:$cid",title,"B站正片 · "+ep.optString("long_title"))}}}
 };if(out.isNotEmpty())break};return out.distinctBy{it.id}}
 fun comments(id:String):List<Danmaku053>{if(id.startsWith("bili:")){val cid=id.removePrefix("bili:");require(cid.matches(Regex("[0-9]{1,20}")));return MediaText053.danmaku(biliText("https://comment.bilibili.com/$cid.xml"))};require(id.matches(Regex("[a-zA-Z0-9_-]{1,100}")));return MediaText053.danmaku(get("comment/$id",mapOf("withRelated" to "true")).toString())}
}
