package com.cbdog.ptvbox

import android.content.Context
import org.json.*
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI

/** PCBOX's bundled Kazumi rules, interpreted locally; no foreign application credentials. */
internal class AnimeRules056(private val context:Context){
 private val rules by lazy{context.assets.list("anime-rules").orEmpty().filter{it.endsWith(".json")&&it!="index.json"}.map{JSONObject(context.assets.open("anime-rules/$it").bufferedReader().use{r->r.readText()})}.associateBy{"kazumi_"+it.getString("name").lowercase()}}
 fun sites()=rules.map{(key,r)->Site(key,r.getString("name")+" · 新番",r.getString("baseURL"),9,"动画",headers(r))}
 private fun headers(r:JSONObject)=mapOf("User-Agent" to r.optString("userAgent").ifBlank{"Mozilla/5.0"},"Referer" to r.optString("referer").ifBlank{r.getString("baseURL")})
 private fun rule(site:Site)=rules[site.key]?:throw IOException("动画规则不存在")
 private fun expand(s:String,q:String="",id:String="",ep:String="")=s.replace("@keyword",q).replace("@source",id).replace("@episodeUrl",ep)
 private fun absolute(r:JSONObject,path:String)=URI(r.getString("baseURL")).resolve(path).toString().also{require(it.startsWith("https://")||it.startsWith("http://"))}
 private suspend fun read(r:JSONObject,url:String):String {val text=Net.text(url,headers(r));if(text.contains("cf-chl-")||text.contains("Just a moment...")||text.contains("gate-answer"))throw IOException("动画站点需要浏览器验证");return text}
 private suspend fun api(r:JSONObject,c:JSONObject,q:String="",id:String=""):String {
  val req=c.getJSONObject("request");var url=expand(req.getString("url"),java.net.URLEncoder.encode(q,"UTF-8"),java.net.URLEncoder.encode(id,"UTF-8"))
  req.optJSONObject("query")?.let{query->url=Net.query(url,query.keys().asSequence().associateWith{expand(query.get(it).toString(),q,id)})}
  val h=headers(r)+req.optJSONObject("headers").let{j->j?.keys()?.asSequence()?.associateWith{j.getString(it)}.orEmpty()}
  return if(req.optString("method")=="POST")Net.postJson(url,req.optJSONObject("body")?.let{b->JSONObject(b.toString()).apply{keys().asSequence().toList().forEach{k->if(opt(k) is String)put(k,expand(getString(k),q,id))}}.toString()}?:"{}",h)else Net.text(url,h)
 }
 suspend fun search(s:Site,q:String):List<Vod>{val r=rule(s);val raw=if(r.optString("searchMode")=="api")api(r,r.getJSONObject("searchApiConfig"),q)else read(r,expand(r.getString("searchURL"),java.net.URLEncoder.encode(q,"UTF-8")));return parseSearch(r,raw)}
 suspend fun detail(s:Site,v:Vod):Vod {if(v.roads.isNotEmpty())return v;val r=rule(s);val raw=if(r.optString("chapterMode")=="api")api(r,r.getJSONObject("chapterApiConfig"),id=v.id)else read(r,absolute(r,v.id));return v.copy(roads=parseDetail(r,v.id,raw))}
 suspend fun resolve(s:Site,e:Episode):Pair<Episode,Map<String,String>> {
  if(direct(e.url))return e to s.headers
  val raw=read(rule(s),e.url)
  val match=Regex("player_\\w+\\s*=\\s*(\\{.*?\\})\\s*;",RegexOption.DOT_MATCHES_ALL).find(raw)
  if(match!=null)runCatching{val j=JSONObject(match.groupValues[1]);var url=j.optString("url");if(j.optInt("encrypt")==2)url=String(java.util.Base64.getDecoder().decode(url));if(j.optInt("encrypt")==1||j.optInt("encrypt")==2)url=java.net.URLDecoder.decode(url.replace("+","%2B"),"UTF-8");if(direct(url))return e.copy(url=url) to (s.headers+mapOf("Referer" to e.url))}
  val found=WebMedia052.resolve(context,e.url,s.headers,raw);return e.copy(url=found.first) to found.second
 }
 companion object{
  private fun direct(url:String)=runCatching{Regex("(?i)\\.(m3u8|mp4|mkv|webm|flv|mpd|ts)$").containsMatchIn(URI(url).path.orEmpty())}.getOrDefault(false)
  internal fun path(root:Any,path:String):List<Any>{var list=listOf(root);for(part in path.replace("$[*]","[*]").removePrefix("$").trimStart('.').split('.').filter{it.isNotBlank()}){val key=part.removeSuffix("[*]");list=list.flatMap{n->val v=if(key.isBlank())n else(n as? JSONObject)?.opt(key);if(v==null||v==JSONObject.NULL)emptyList()else if(part.endsWith("[*]")&&v is JSONArray)(0 until v.length()).map{v.get(it)}else listOf(v)}};return list}
  private fun value(root:Any,path:String)=path(root,path).firstOrNull()?.toString().orEmpty()
  private fun nodes(e:org.jsoup.nodes.Element,x:String):List<org.jsoup.nodes.Element>{require(x.isNotBlank()){"动画规则缺少选择器"};return XPathLite056.select(e,x)}
  private fun textValue(e:org.jsoup.nodes.Element,x:String)=XPathLite056.text(e,x)
  private fun url(r:JSONObject,p:String)=URI(r.getString("baseURL")).resolve(p).toString()
  internal fun parseSearch(r:JSONObject,raw:String):List<Vod>{
   val pairs=if(r.optString("searchMode")=="api"){val c=r.getJSONObject("searchApiConfig");path(JSONTokener(raw).nextValue(),c.getString("listPath")).map{n->val id=value(n,c.getString("sourcePath"));val template=c.optString("sourceTemplate");(if(template.isBlank())id else template.replace("@source",id)) to value(n,c.getString("namePath"))}}
   else nodes(Jsoup.parse(raw),r.getString("searchList")).mapNotNull{e->val a=nodes(e,r.getString("searchResult")).firstOrNull()?:return@mapNotNull null;if(!a.hasAttr("href"))return@mapNotNull null;url(r,a.attr("href")) to textValue(e,r.getString("searchName"))}
   return pairs.filter{it.first.isNotBlank()&&it.second.isNotBlank()}.distinctBy{it.first}.take(100).map{Vod(it.first,it.second,"","","",emptyList())}
  }
  internal fun parseDetail(r:JSONObject,id:String,raw:String):List<Road>{
   if(r.optString("chapterMode")!="api")return nodes(Jsoup.parse(raw),r.getString("chapterRoads")).mapIndexedNotNull{i,road->val eps=nodes(road,r.getString("chapterResult")).filter{it.hasAttr("href")}.map{Episode(it.text(),url(r,it.attr("href")))};if(eps.isEmpty())null else Road("线路${i+1}",eps)}
   val root=JSONTokener(raw).nextValue();val c=r.getJSONObject("chapterApiConfig")
   fun page(ep:String):String{if(direct(ep))return ep;val cfg=c.optJSONObject("episodePage")?:return url(r,ep);val rawUrl=cfg.optString("url");if(rawUrl.isBlank())return url(r,ep);var result=rawUrl.replace("@source",id).replace("@episodeUrl",ep);cfg.optJSONObject("query")?.let{q->result=Net.query(result,q.keys().asSequence().associateWith{q.get(it).toString().replace("@source",id).replace("@episodeUrl",ep)})};return result}
   if(c.optString("format")=="delimited"){val names=value(root,c.getString("roadNamesPath")).split(c.getString("roadSeparator"));return value(root,c.getString("roadEpisodesPath")).split(c.getString("roadSeparator")).mapIndexedNotNull{i,row->val eps=row.split(c.getString("episodeSeparator")).mapNotNull{part->val fields=part.split(c.getString("fieldSeparator"),limit=2);if(fields.size<2||fields[1].isBlank())null else Episode(fields[0],page(fields[1]))};if(eps.isEmpty())null else Road(names.getOrElse(i){"线路${i+1}"},eps)}}
   return path(root,c.getString("roadsPath")).mapIndexedNotNull{i,road->val eps=path(road,c.getString("episodesPath")).map{Episode(value(it,c.getString("episodeNamePath")),page(value(it,c.getString("episodeUrlPath"))))};if(eps.isEmpty())null else Road(c.optString("roadNamePath").takeIf{it.isNotBlank()}?.let{value(road,it)}?:"线路${i+1}",eps)}
  }
 }
}
