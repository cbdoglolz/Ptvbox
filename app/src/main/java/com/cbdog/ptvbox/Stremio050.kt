package com.cbdog.ptvbox
import kotlinx.coroutines.*
import org.json.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** HTTP stream resource only: no magnet/torrent is silently treated as a playable HTTP file. */
object Stremio050 {
 private val headers=ConcurrentHashMap<String,Map<String,String>>()
 fun headersFor(url:String)=headers[url].orEmpty()
 fun parse(site:Site,work:Work,episode:String,j:JSONObject):Vod {val rows=j.optJSONArray("streams")?.objects().orEmpty();val roads=rows.mapIndexedNotNull{n,s->
  if(s.optString("name").contains("Donation needed",true)||s.optString("name").contains("Join the Discord",true))return@mapIndexedNotNull null
  val url=s.optString("url");if(!url.startsWith("https://")&&!url.startsWith("http://"))return@mapIndexedNotNull null
  val h=s.optJSONObject("behaviorHints")?.optJSONObject("proxyHeaders")?.optJSONObject("request");if(h!=null){if(headers.size>512)headers.clear();headers[url]=h.keys().asSequence().filter{it.lowercase() !in listOf("host","content-length","connection")}.associateWith{h.optString(it)}}
  val label=listOf(s.optString("name"),s.optString("title").takeUnless{it=="null"}.orEmpty().ifBlank{s.optString("description").takeUnless{it=="null"}.orEmpty()}).filter{it.isNotBlank()}.joinToString(" · ").replace('\n',' ').take(400).ifBlank{"HTTP线路${n+1}"}
  val bytes=s.optJSONObject("behaviorHints")?.optLong("videoSize",0)?:0
  Road("$label${if(bytes>0)" · "+ResourceLabels040.size(bytes)else""} · #${n+1}",listOf(Episode(episode,url)))
 };return Vod(work.id,work.title,work.year,work.poster,work.overview,roads.sortedBy{ResourceLabels040.bytes(it.name).takeIf{n->n>0}?:Long.MAX_VALUE})}
 suspend fun lookup(site:Site,w:Work,episode:String,metadata:Metadata):Vod {
  var id=w.facts.imdbId
  if(!id.matches(Regex("tt[0-9]+"))){val full=metadata.details(w);id=full.facts.imdbId;if(!id.matches(Regex("tt[0-9]+")))id=metadata.ratingWork(full,"tmdb")?.facts?.imdbId.orEmpty()}
  if(!id.matches(Regex("tt[0-9]+")))throw IOException("无法唯一确认IMDb作品ID，请先校正TMDB关联")
  val type=if(w.kind=="tv")"series"else"movie";val streamId=if(w.kind=="tv"){val number=Identity.episode(episode)?:throw IOException("请明确选择单集");"$id:${w.season}:$number"}else id
  val root=org.json.JSONObject(Net.bytes("${site.api.trimEnd('/')}/stream/$type/$streamId.json",site.headers,direct=true).toString(Charsets.UTF_8))
  return parse(site,w,episode,root)
 }
}
