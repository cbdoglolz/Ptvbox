package com.cbdog.ptvbox

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Net {
    private val client=OkHttpClient.Builder().connectTimeout(java.time.Duration.ofSeconds(8)).readTimeout(java.time.Duration.ofSeconds(12)).callTimeout(java.time.Duration.ofSeconds(18)).followRedirects(false).followSslRedirects(false).build()
    fun query(base:String,params:Map<String,String>):String {val u=base.toHttpUrl().newBuilder();params.forEach{(k,v)->u.addQueryParameter(k,v)};return u.build().toString()}
    suspend fun bytes(url:String,headers:Map<String,String> = emptyMap(),limit:Int=4_000_000,truncate:Boolean=false,timeoutSeconds:Int?=null,direct:Boolean=false):ByteArray = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        var current=url.toHttpUrlOrNull()?:throw IOException("地址无效");var safeHeaders=headers
        repeat(5){
            val req=Request.Builder().url(current).header("User-Agent",NetworkPolicy.userAgent).apply{safeHeaders.forEach{(k,v)->if(!v.contains('\n')&&!v.contains('\r'))header(k,v)}}.build()
            val res=await(req,timeoutSeconds,if(direct)NetworkPolicy.directClient()else null)
            res.use {
                if(res.code in 300..399){val next=res.header("Location")?.let{current.resolve(it)}?:throw IOException("跳转地址缺失");if(next.scheme!="https"&&current.scheme=="https")throw IOException("拒绝不安全跳转");if(next.host!=current.host||next.port!=current.port)safeHeaders=safeHeaders.filterKeys{it.equals("User-Agent",true)||it.equals("Referer",true)||it.equals("Origin",true)};current=next;return@repeat}
                if(!res.isSuccessful)throw IOException("HTTP ${res.code}")
                if(!truncate&&res.body.contentLength()>limit)throw IOException("响应过大")
                val out=ByteArrayOutputStream();res.body.byteStream().use{input->val buffer=ByteArray(8192);while(true){val size=input.read(buffer,0,if(truncate)minOf(buffer.size,limit-out.size())else buffer.size);if(size<=0)break;if(out.size()+size>limit)throw IOException("响应超出限制");out.write(buffer,0,size);if(truncate&&out.size()>=limit)break}};return@withContext out.toByteArray()
            }
        };throw IOException("跳转过多")
    }
    suspend fun postJson(url:String,body:String,headers:Map<String,String> = emptyMap()):String=withContext(Dispatchers.IO){
        val req=Request.Builder().url(url).header("User-Agent",NetworkPolicy.userAgent).apply{headers.forEach{(k,v)->header(k,v)}}.post(body.toRequestBody("application/json".toMediaType())).build()
        await(req).use{res->if(!res.isSuccessful)throw IOException("HTTP ${res.code}");res.body.byteStream().use{MediaText053.bounded(it,4_000_000).toString(Charsets.UTF_8)}}
    }
    suspend fun text(url:String,headers:Map<String,String> = emptyMap(),limit:Int=4_000_000)=bytes(url,headers,limit).toString(Charsets.UTF_8)
    suspend fun json(url:String,headers:Map<String,String> = emptyMap())=JSONObject(text(url,headers))
    private suspend fun await(request:Request,timeoutSeconds:Int?=null,clientOverride:OkHttpClient?=null):Response=suspendCancellableCoroutine{c->val base=clientOverride?:NetworkPolicy.client();val client=if(timeoutSeconds==null)base else base.newBuilder().connectTimeout(timeoutSeconds.toLong(),java.util.concurrent.TimeUnit.SECONDS).readTimeout(timeoutSeconds.toLong(),java.util.concurrent.TimeUnit.SECONDS).callTimeout(timeoutSeconds.toLong(),java.util.concurrent.TimeUnit.SECONDS).build();val call=client.newCall(request);c.invokeOnCancellation{call.cancel()};call.enqueue(object:Callback{override fun onFailure(call:Call,e:IOException){if(c.isActive)c.resumeWithException(IOException("网络连接失败",e))};override fun onResponse(call:Call,response:Response){if(c.isActive)c.resume(response)else response.close()}})}
}

class Metadata(private val db:Library,private val tmdbTransport103:(suspend (String,Map<String,String>)->JSONObject)?=null) {
    private data class RatingCache057(val at:Long,val value:Work?)
    private val ratingCache057=java.util.concurrent.ConcurrentHashMap<String,RatingCache057>()
    private val animeMaps103 by lazy{AnimeMapping103.parse(db.assetText103("anime-mappings103.json"))}
    internal fun animeMapping103(w:Work):AnimeCoordinate103? {
        val mapped=AnimeMapping103.lookup(animeMaps103,w)?:return null
        val manual=db.prefs.getString(ratingStorageKey(w,"tmdb"),null)
        return mapped.takeIf{manual==null||manual==mapped.tmdb}
    }
    internal fun identifyAnime103(w:Work):Work {
        if(w.provider!="anilist"||w.kind!="tv")return w
        val mapped=animeMapping103(w)
        val explicit=AnimeMapping103.titleSeason(listOf(w.original,w.title)+w.aliases)
        val logical=if(explicit>1)explicit else mapped?.logicalSeason?.takeIf{it>0}?:w.season
        return if(w.season==w.catalogSeason)w.copy(season=logical,catalogSeason=logical)else w
    }
    internal fun episodeCache055(w:Work)=java.io.File(db.episodeCacheDir055,sha256(("v103|"+w.catalogKey()).toByteArray())+".json")
    val preloadCatalog get()=db.prefs.getBoolean("preload_catalog",false)
    internal val tmdbReady get()=tmdbTransport103!=null||db.secrets.read("tmdb").isNotBlank()
    private val doubanHeaders=mapOf("Referer" to "https://movie.douban.com/","Origin" to "https://movie.douban.com")
    internal fun doubanHdImage(url:String):String {
        if(url.isBlank())return ""
        if(!url.contains("doubanio.com"))return url
        return url
            .replace("/view/photo/s_ratio_poster/","/view/photo/l/")
            .replace("/view/photo/m_ratio_poster/","/view/photo/l/")
            .replace("/view/photo/s/","/view/photo/l/")
            .replace("/view/photo/m/","/view/photo/l/")
    }
    internal fun dbWork(j:JSONObject,kind:String):Work {
        val cover=j.optJSONObject("pic")?:j.optJSONObject("images")?:j.optJSONObject("cover")
        val rawPoster=cover?.optString("large")?.ifBlank{cover.optString("url").ifBlank{cover.optString("normal")}}?:j.optString("cover").takeIf{it.startsWith("http")}.orEmpty().ifBlank{j.optString("cover_url")}
        val poster=doubanHdImage(rawPoster)
        val subtype=j.optString("type").ifBlank{j.optString("subtype")};return Work(j.optString("id"),j.optString("title").ifBlank{j.optString("name")},if(subtype=="tv"||kind=="tv")"tv" else "movie",j.optString("year").ifBlank{Regex("[12][0-9]{3}").find(j.optString("card_subtitle"))?.value.orEmpty()},poster,j.optString("intro").ifBlank{j.optString("summary")},j.optJSONObject("rating")?.optString("value")?.takeIf{it!="null"&&it!="0"}?:"-",original=j.optString("original_title"),aliases=j.arrayStrings("aka"),genres=j.arrayStrings("genres"),season=Identity.season(j.optString("title"))?:1,releaseDate=MetadataQueries040.date(j.opt("pubdate")),votes=j.optJSONObject("rating")?.optInt("count")?:0,episodeCount=j.optInt("episodes_count",0).takeIf{it>0}?:Regex("""(?:集数|总集数)[^\d]*(\d{1,5})""").find(j.optString("episodes_info")+j.optString("intro")+j.optString("summary"))?.groupValues?.get(1)?.toIntOrNull()?:0) 
    }
    internal fun tmdbWork(j:JSONObject,kind:String):Work {val name=j.optString("title").ifBlank{j.optString("name")};val votes=j.optInt("vote_count");val avg=j.optDouble("vote_average",0.0);val ratingText=if(votes>0&&avg>0.0)String.format(java.util.Locale.US,"%.1f",avg)else"-";return Work(j.optString("id"),name,kind,j.optString("release_date").ifBlank{j.optString("first_air_date")}.take(4),j.optString("poster_path").takeIf{it.isNotBlank()&&it!="null"}?.let{"https://image.tmdb.org/t/p/w342$it"}.orEmpty(),j.optString("overview"),ratingText,"tmdb",j.optString("original_title").ifBlank{j.optString("original_name")},season=1,releaseDate=j.optString("release_date").ifBlank{j.optString("first_air_date")},votes=votes,episodeCount=j.optInt("number_of_episodes",0))} 
    private fun detailCache(w:Work)=java.io.File(db.detailCacheDir,sha256(w.ratingViewKey.toByteArray())+".json")
    fun invalidateDetail(w:Work){runCatching{detailCache(w).delete()}}
    internal suspend fun tmdb(path:String,params:Map<String,String> = emptyMap()):JSONObject {tmdbTransport103?.let{return it(path,params)};val key=db.secrets.read("tmdb");if(key.isBlank())throw IOException("请到设置添加个人TMDB Key，豆瓣和点播不受影响");val q=params+mapOf("language" to "zh-CN","include_adult" to "false");return Net.json(Net.query("https://api.themoviedb.org/3/$path",if(key.length==32)q+mapOf("api_key" to key)else q),if(key.length==32)emptyMap()else mapOf("Authorization" to "Bearer $key"))}
    suspend fun feed(provider:String,kind:String,category:String,page:Int,includeUpcoming:Boolean=false):List<Work> {
        val list=if(provider=="tmdb") {
            val route=if(category=="高分")"$kind/top_rated" else if(category=="最新")"discover/$kind" else "$kind/popular"
            val q=mutableMapOf("page" to page.toString());if(category=="最新"){q["sort_by"]=if(kind=="movie")"primary_release_date.desc" else "first_air_date.desc";if(!includeUpcoming)q[if(kind=="movie")"primary_release_date.lte" else "first_air_date.lte"]=java.time.LocalDate.now().toString()}
            tmdb(route,q).optJSONArray("results")?.objects()?.map{tmdbWork(it,kind)}?:emptyList()
        }else {
            val root="https://m.douban.com/rexxar/api/v2/";val q=mutableMapOf("start" to ((page-1)*20).toString(),"count" to "20");val factTag=category.removePrefix("筛选:").takeIf{category.startsWith("筛选:")};val route=if(category=="TOP250")"subject_collection/movie_top250/items" else if(provider=="netflix"||category=="最新"||category=="高分"||category.startsWith("动画")||category in listOf("纪录片","综艺")||factTag!=null)"$kind/recommend" else "subject/recent_hot/$kind"
            if(route.endsWith("recommend")){q["tags"]=factTag?:listOfNotNull(if(provider=="netflix")"Netflix" else null,if(category.startsWith("动画"))"动画" else if(category in listOf("纪录片","综艺"))category else if(kind=="tv")"电视剧" else "电影").joinToString(",");q["sort"]=if(category=="最新"||category=="动画最新")"R" else if(category=="高分")"S"else "U"}else{q["limit"]="20";q["category"]=if(kind=="tv")"tv" else "热门";q["type"]=if(kind=="tv")"tv" else "全部"}
            val json=Net.json(Net.query(root+route,q),doubanHeaders);val rows=json.optJSONArray("items")?:json.optJSONArray("subject_collection_items")?:json.optJSONArray("subjects")?:JSONArray();rows.objects().map{dbWork(it,if(category=="TOP250")"movie"else kind)}
        }
        return list.filter{it.id.isNotBlank()&&it.title.isNotBlank()&&(includeUpcoming||!it.isUpcoming())}.onEach(db::cache)
    }
    suspend fun search(q:String):List<Work>{
        val local=db.cached().filter{Identity.keyword(it,q)}
        val remote=DoubanSearch055.search(q)
        val tm=if(db.secrets.read("tmdb").isNotBlank())runCatching{tmdb("search/multi",mapOf("query" to q)).optJSONArray("results")?.objects()?.filter{it.optString("media_type") in listOf("movie","tv")}?.map{tmdbWork(it,it.optString("media_type"))}?:emptyList()}.getOrDefault(emptyList())else emptyList()
        return (local+remote+tm).filter{it.title.isNotBlank()}.distinctBy{it.key}.onEach(db::cache)
    }
    suspend fun details(work:Work):Work {
        if(work.provider.startsWith("site_"))return work
        if(work.provider=="anilist"){
            if(!work.isUpcoming()||!work.id.matches(Regex("[0-9]+")))return work
            return runCatching{
                val query="query { Media(id:${work.id},type:ANIME){status episodes startDate{year month day}} }"
                val j=JSONObject(Net.postJson("https://graphql.anilist.co",JSONObject().put("query",query).toString())).getJSONObject("data").getJSONObject("Media")
                val d=j.getJSONObject("startDate")
                val date=runCatching{java.time.LocalDate.of(d.getInt("year"),d.getInt("month"),d.getInt("day")).toString()}.getOrDefault(work.releaseDate)
                work.copy(releaseStatus=j.optString("status",work.releaseStatus),episodeCount=j.optInt("episodes",work.episodeCount),year=d.optString("year").takeUnless{it=="null"||it.isBlank()}?:work.year,releaseDate=date)
            }.getOrDefault(work)
        }
        val cacheFile=detailCache(work)
        if(cacheFile.isFile){
            runCatching{Work.from(JSONObject(cacheFile.readText()))}.getOrNull()?.let{CacheBudget101.touch(cacheFile);return it.copy(identityKey=work.identityKey.ifBlank{it.identityKey},season=work.season,catalogSeason=work.catalogSeason)}
        }
        val fresh=if(work.provider=="tmdb") {
            val json=tmdb("${work.kind}/${work.id}",mapOf("append_to_response" to "credits,external_ids,alternative_titles"))
            val credits=json.optJSONObject("credits")
            val people=(credits?.optJSONArray("crew")?.objects().orEmpty().filter{it.optString("job")=="Director"}+credits?.optJSONArray("cast")?.objects().orEmpty().take(16)).map{j->PersonCredit(j.optString("id"),j.optString("name"),j.optString("character").ifBlank{j.optString("job")},j.optString("profile_path").takeIf{it.startsWith("/")}?.let{"https://image.tmdb.org/t/p/w185$it"}.orEmpty(),"tmdb")}.filter{it.name.isNotBlank()}.distinctBy{it.id}
            tmdbWork(json,work.kind).copy(season=work.season,backdrop=json.optString("backdrop_path").takeIf{it.startsWith("/")}?.let{"https://image.tmdb.org/t/p/w1280$it"}.orEmpty(),people=people,facts=WorkFacts.tmdb(json),aliases=json.optJSONObject("alternative_titles")?.let{a->(a.optJSONArray("titles")?:a.optJSONArray("results"))?.objects()?.map{it.optString("title")}}.orEmpty())
        }else{
            val preferred=if(Identity.season(work.title)!=null)"tv"else work.kind
            var actual=preferred
            val j=try{Net.json("https://m.douban.com/rexxar/api/v2/$preferred/${work.id}",doubanHeaders)}catch(e:IOException){if(e.message?.contains("HTTP 404")!=true)throw e;actual=if(preferred=="tv")"movie"else"tv";Net.json("https://m.douban.com/rexxar/api/v2/$actual/${work.id}",doubanHeaders)}
            val credits=runCatching{Net.json("https://m.douban.com/rexxar/api/v2/$actual/${work.id}/credits?type=$actual",doubanHeaders)}.getOrNull()
            val people=credits?.optJSONArray("items")?.objects().orEmpty().take(24).map{p->
                val avatar=p.optJSONObject("avatar")?.let{it.optString("large").ifBlank{it.optString("normal")}}?:p.optString("avatar")
                PersonCredit(p.optString("id"),p.optString("name"),p.optString("character").ifBlank{p.arrayStrings("roles").joinToString(" / ")},avatar.takeIf{it.startsWith("http")}.orEmpty(),"douban")
            }.filter{it.name.isNotBlank()}.distinctBy{it.id.ifBlank{it.name}}.ifEmpty{
                listOf("directors" to "导演","actors" to "演员").flatMap{(field,role)->j.optJSONArray(field)?.objects().orEmpty().take(16).map{p->PersonCredit(p.optString("id"),p.optString("name"),role,(p.optJSONObject("avatar")?.let{it.optString("large").ifBlank{it.optString("normal")}}?:p.optString("avatar")).takeIf{it.startsWith("http")}.orEmpty(),"douban")}}.filter{it.name.isNotBlank()}.distinctBy{it.id.ifBlank{it.name}}
            }
            val photos=runCatching{Net.json("https://m.douban.com/rexxar/api/v2/$actual/${work.id}/photos?start=0&count=6",doubanHeaders)}.getOrNull()?.optJSONArray("photos")?.objects().orEmpty()
            val landscapeBackdrop=photos.firstOrNull{p->val img=p.optJSONObject("image")?.optJSONObject("large");(img?.optInt("width",0)?:0)>(img?.optInt("height",0)?:0)}?.optJSONObject("image")?.optJSONObject("large")?.optString("url").orEmpty().ifBlank{photos.firstOrNull()?.optJSONObject("image")?.optJSONObject("large")?.optString("url").orEmpty()}
            dbWork(j,actual).copy(season=work.season,backdrop=doubanHdImage(landscapeBackdrop),people=people,facts=WorkFacts.douban(j))
        }
        val identified=runCatching{resolveIdentity060(fresh)}.getOrDefault(fresh)
        val selectedSeason=work.kind=="tv"&&work.season!=fresh.catalogSeason
        val otherSeason=selectedSeason&&work.provider=="douban"
        return identified.copy(title=if(selectedSeason)work.title else identified.title,year=if(selectedSeason)work.year else identified.year,rating=if(otherSeason)"-"else identified.rating,votes=if(otherSeason)0 else identified.votes,releaseDate=if(selectedSeason)work.releaseDate else identified.releaseDate.ifBlank{work.releaseDate},identityKey=work.identityKey.ifBlank{if(identified.kind!=work.kind&&identified.provider==work.provider&&identified.id==work.id)work.key else ""},poster=identified.poster.ifBlank{work.poster},original=identified.original.ifBlank{work.original},aliases=(identified.aliases+work.aliases).distinct()).also{result->
            db.cache(result);runCatching{cacheFile.writeText(result.json().toString());CacheBudget101.trimDirectory(db.detailCacheDir)}
        }
    }
    suspend fun related(work:Work):List<Work> = if(work.provider=="tmdb")tmdb("${work.kind}/${work.id}/recommendations").optJSONArray("results")?.objects()?.map{tmdbWork(it,work.kind)}?.filter{it.id.isNotBlank()&&it.title.isNotBlank()}?.take(16).orEmpty()else{
        runCatching{val raw=Net.text("https://m.douban.com/rexxar/api/v2/${work.kind}/${work.id}/recommendations",doubanHeaders);JSONArray(raw).objects().map{dbWork(it,if(it.optString("type").isBlank())work.kind else it.optString("type"))}.filter{it.id.isNotBlank()&&it.title.isNotBlank()}.take(16)}.getOrDefault(emptyList())
    }
    fun ratingStorageKey(w:Work,p:String)="rating_link_${w.key}${if(p=="douban"&&w.kind=="tv"&&w.season!=w.catalogSeason)"@season:${w.season}"else ""}_$p"
    fun saveRatingLink(w:Work,p:String,id:String){require(p in listOf("douban","tmdb")&&id.matches(Regex("[0-9]+")));db.prefs.edit().putString(ratingStorageKey(w,p),id).apply();ratingCache057.keys.removeIf{it.startsWith("${w.ratingViewKey}|$p|")}}
    fun clearRatingLink(w:Work,p:String){db.prefs.edit().putString(ratingStorageKey(w,p),"none").apply();ratingCache057.keys.removeIf{it.startsWith("${w.ratingViewKey}|$p|")}}
    suspend fun providerCandidates(w:Work,p:String,q:String):List<Work>{
        if(q.matches(Regex("[0-9]+")))return listOf(details(Work(q,w.title,w.kind,provider=p,season=w.season)))
        val rows=if(p=="douban")DoubanSearch055.search(q) else tmdb("search/${w.kind}",mapOf("query" to q)).optJSONArray("results")?.objects()?.map{tmdbWork(it,w.kind)}.orEmpty()
        return rows.filter{it.kind==w.kind}.distinctBy{it.id}.take(20)
    }
    suspend fun ratingWork(w:Work,p:String):Work?{
        val saved=db.prefs.getString(ratingStorageKey(w,p),null)
        val key="${w.ratingViewKey}|$p|${saved.orEmpty()}|${w.title.hashCode()}|${w.aliases.joinToString("|").hashCode()}|${db.secrets.read("tmdb").isNotBlank()}"
        ratingCache057[key]?.takeIf{System.currentTimeMillis()-it.at<30*60*1000L}?.let{return it.value}
        return ratingWorkUncached(w,p).also{ratingCache057[key]=RatingCache057(System.currentTimeMillis(),it)}
    }
    private suspend fun ratingWorkUncached(w:Work,p:String):Work?{
        if(p=="tmdb"&&db.secrets.read("tmdb").isBlank())return null
        val mapped=animeMapping103(w);val base=if(mapped!=null)w.copy(facts=w.facts.copy(tmdbId=mapped.tmdb))else w
        val identified=runCatching{resolveIdentity060(base)}.getOrDefault(base)
        if(p=="tmdb"){
            identified.facts.tmdbId.takeIf{it.matches(Regex("[0-9]{1,12}"))}?.let{id->
                return runCatching{details(Work(id,identified.title,identified.kind,provider="tmdb",season=identified.season))}.getOrNull()
            }
            tmdbFromImdb060(identified.facts.imdbId)?.let{return details(it.copy(season=identified.season))}
        }
        if(p=="douban"&&!(w.kind=="tv"&&w.season!=w.catalogSeason)){
            identified.facts.doubanId.takeIf{it.matches(Regex("[0-9]{1,12}"))}?.let{id->
                return runCatching{details(Work(id,identified.title,identified.kind,provider="douban",season=identified.season))}.getOrNull()
            }
        }
        val saved=db.prefs.getString(ratingStorageKey(w,p),null);if(saved=="none")return null
        val scoped=p=="douban"&&w.kind=="tv"&&w.season!=w.catalogSeason
        val id=saved?:if(w.provider==p&&!scoped)w.id else if(p=="tmdb"&&w.kind=="tv")db.prefs.getString("tmdb_link_${w.key}",null)else null
        if(id!=null)return runCatching{details(Work(id,w.title,w.kind,provider=p,season=w.season))}.getOrElse{if(w.provider==p&&w.id==id)w else throw it}
        val names=MetadataQueries040.names(w)
        val candidates=mutableListOf<Work>();for(name in names.take(3)){candidates+=providerCandidates(w,p,name);if(candidates.any{hit->names.any{Identity.base(it)==Identity.base(hit.title)||Identity.base(it)==Identity.base(hit.original)}})break}
        var hits=candidates.distinctBy{it.id}.filter{RatingMatch055.matches(w,it,names,scoped)}
        if(hits.isEmpty()){val enriched=coroutineScope{candidates.distinctBy{it.id}.filter{it.kind==w.kind&&(it.year.isBlank()||w.year.isBlank()||it.year==w.year)}.take(4).map{c->async{withTimeoutOrNull(6000){try{details(c)}catch(e:CancellationException){throw e}catch(_:Exception){null}}}}.awaitAll().filterNotNull()};hits=enriched.filter{RatingMatch055.matches(w,it,names,scoped)}}
        if(hits.size!=1)return null
        val result=details(hits.single());if(!RatingMatch055.matches(w,result,names,scoped))return null;val now=db.prefs.getString(ratingStorageKey(w,p),null);if(now==saved){saveRatingLink(w,p,result.id);return result};return if(now=="none")null else now?.let{details(Work(it,w.title,w.kind,provider=p,season=w.season))}
    }
    suspend fun tmdbCheck()=tmdb("configuration").has("images")
    suspend fun validateCredential(value:String):Boolean {require(value.isNotBlank()&&value.length<=4096&&!value.any{it.isWhitespace()});val key=value.trim();return Net.json(Net.query("https://api.themoviedb.org/3/configuration",if(key.length==32)mapOf("api_key" to key)else emptyMap()),if(key.length==32)emptyMap()else mapOf("Authorization" to "Bearer $key")).has("images")}
    suspend fun episodeSeries(work:Work,selectedSeason:Int=work.season,manualId:String?=null,onOutline:(SeriesMeta)->Unit={}):SeriesMeta? {
        if(work.isUpcoming()&&work.episodeCount==0)return null
        if(work.kind!="tv"||!tmdbReady||(manualId==null&&db.prefs.getString("rating_link_${work.key}_tmdb",null)=="none"))return null
        val savedRatingId=db.prefs.getString(ratingStorageKey(work,"tmdb"),null)?.takeIf{it.matches(Regex("[0-9]{1,12}"))}
        val coordinate=if(manualId==null)animeMapping103(work)else null
        var id=manualId?:savedRatingId?:coordinate?.tmdb?:work.facts.tmdbId.takeIf{it.matches(Regex("[0-9]{1,12}"))}?:savedRatingId?:db.prefs.getString("tmdb_link_${work.key}",null)?:work.id.takeIf{work.provider=="tmdb"}
        if(id==null){
            val names=MetadataQueries040.names(work)
            val nianfan=Identity.isNianfan(work)
            val sequelSeason=Identity.season(work.title)?:Identity.trailingSeason(work.title)?:work.season
            val hits=names.take(4).flatMap{q->val clean=Identity.stripTrailingSeason(Identity.searchTitle(q)).ifBlank{q};tmdb("search/tv",mapOf("query" to clean)).optJSONArray("results")?.objects().orEmpty()}.distinctBy{it.optString("id")}
            val matches=hits.filter{j->listOf(j.optString("name"),j.optString("original_name")).any{name->names.any{Identity.base(Identity.stripTrailingSeason(it))==Identity.base(Identity.stripTrailingSeason(name))}}&&(nianfan||sequelSeason>1||work.year.isBlank()||j.optString("first_air_date").take(4)==work.year)}
            if(matches.size!=1)return null
            id=matches.single().optString("id")
        }
        require(id.matches(Regex("[0-9]{1,12}"))){"请输入数字TMDB剧集ID"}
        val show=tmdb("tv/$id");val seasons=show.optJSONArray("seasons")?.objects()?.map{SeasonMeta(it.optInt("season_number"),it.optString("name"),it.optString("air_date").take(4),it.optInt("episode_count"),it.optString("poster_path").takeIf{path->path.startsWith('/')}?.let{path->"https://image.tmdb.org/t/p/w185$path"}.orEmpty(),it.optString("overview"),it.optString("air_date"))}.orEmpty()
        val isInstallment=AnimeMapping103.installment(work)&&selectedSeason==work.catalogSeason
        val desiredSeason=coordinate?.season?:if(manualId!=null||selectedSeason!=work.catalogSeason)selectedSeason else TmdbSeasonResolver101.pickSeason(work,selectedSeason,seasons)
        val regularSeasons=seasons.filter{it.number>0}
        val exact=regularSeasons.firstOrNull{it.number==desiredSeason}
        val resolvedSeason=exact?.number?:if(isInstallment&&regularSeasons.size==1)regularSeasons.single().number else return null
        val outline=EpisodeRail055.outline(id,show.optString("name"),show.optString("original_name"),seasons,resolvedSeason)
        if(!isInstallment)onOutline(outline)else if(coordinate!=null)AnimeMapping103.project(work,outline,coordinate)?.let(onOutline)
        // A failed requested season must not silently substitute another season's episodes.
        val season=tmdb("tv/$id/season/$resolvedSeason")
        val raw=SeriesMeta(id,show.optString("name"),show.optString("original_name"),seasons,TmdbEpisodeParser.episodes(season))
        val result=if(isInstallment)AnimeMapping103.project(work,raw,coordinate?.takeIf{it.season==resolvedSeason})?:return null else raw
        if(manualId!=null)saveRatingLink(work,"tmdb",id)
        db.prefs.edit().putString("tmdb_link_${work.key}",id).apply()
        return result
    }
}

class Sites(private val context:android.content.Context,private val db:Library) {
    private val bridge=SpiderClient(context)
    private val anime=AnimeRules056(context)
    val settings=TvPreferences(context)
    val parallel get()=settings.parallel
    fun ordered(work:Work?=null)=SourceRoutePolicy.order(work,all(),settings)
    fun close(){bridge.close()}
    fun releaseIdleSpider(keepAlive:Boolean){if(!keepAlive)bridge.close()}
    fun all():List<Site>{val built=JSONObject(context.assets.open("default-sites.json").bufferedReader().use{it.readText()}).getJSONArray("sites").objects().map{Site.from(it)};val custom=runCatching{val config=JSONObject(db.secrets.read("source_config"));val jar=config.opt("spider").let{if(it is JSONObject)it.optString("url")else it?.toString().orEmpty()};config.optJSONArray("sites")?.objects()?.map{Site.from(it).let{s->s.copy(group="私人订阅",jar=s.jar.ifBlank{jar})}}?:emptyList()}.getOrDefault(emptyList());val international=JSONObject(context.assets.open("international-sites.json").bufferedReader().use{it.readText()}).getJSONArray("sites").objects().map{Site.from(it)};return (custom+built+international+anime.sites()).distinctBy{it.key}}
    fun supports(site:Site)=site.type in listOf(0,1,4,8,9,10)||(site.type==3&&site.api.startsWith("csp_"))
    fun enabled(site:Site)=db.prefs.getBoolean("site_${site.key}",true)
    fun toggle(site:Site){db.prefs.edit().putBoolean("site_${site.key}",!enabled(site)).apply()}
    suspend fun import(url:String):Int {val body=Net.text(url);val count=SubscriptionConfig.validate(body);db.secrets.save("source_config",body);db.secrets.save("subscription",url);return count}
    suspend fun browse(site:Site,category:String="",page:Int=1):JSONObject {return if(site.type==3){val raw=bridge.browse(site,category,page);if(raw.trimStart().startsWith("<"))throw IOException("直播接口返回登录/网页内容，频道JSON不可用；请检查接口访问权限");JSONObject(raw)}else Net.json(Net.query(site.api,mapOf("ac" to "detail","t" to category,"pg" to page.toString())),site.headers)}
    suspend fun search(site:Site,keyword:String):List<Vod> {if(site.type==10)return OneTouch103.search(keyword);if(site.type==9)return anime.search(site,keyword);if(site.type==8)throw IOException("需要作品ID，请从影片详情播放或搜索高清源");if(site.type==3){if(!site.api.startsWith("csp_"))throw IOException("JS规则适配器尚未接入");return bridge.search(site,keyword)};if(site.type !in listOf(0,1,4))throw IOException("此站需要专用插件，尚未接入");val text=Net.text(Net.query(site.api,mapOf("ac" to if(site.type==4)"search"else"detail","wd" to keyword)),site.headers);return parseSiteRows(text)}
    suspend fun detail(site:Site,vod:Vod):Vod {if(site.type==10)return OneTouch103.detail(vod);if(site.type==9)return anime.detail(site,vod);if(vod.roads.any{it.episodes.isNotEmpty()})return vod;if(site.type==3)return try{bridge.detail(site,vod.id)}catch(e:Exception){if(LiveParser050.isPortal(site)&&vod.id.startsWith("http"))vod.copy(roads=listOf(Road("网页直播",listOf(Episode(vod.title,vod.id)))))else throw e};val text=Net.text(Net.query(site.api,mapOf("ac" to "detail","ids" to vod.id)),site.headers);return parseSiteRows(text).firstOrNull()?:vod}
    private fun parseSiteRows(text:String):List<Vod>{if(!text.trimStart().startsWith("<"))return JSONObject(text).optJSONArray("list")?.objects()?.map(VodParser::parse).orEmpty();val factory=javax.xml.parsers.DocumentBuilderFactory.newInstance().apply{setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);setFeature("http://xml.org/sax/features/external-general-entities",false);setFeature("http://xml.org/sax/features/external-parameter-entities",false)};val videos=factory.newDocumentBuilder().parse(text.byteInputStream()).getElementsByTagName("video");return (0 until videos.length).map{index->val v=videos.item(index) as org.w3c.dom.Element;fun field(k:String)=v.getElementsByTagName(k).item(0)?.textContent.orEmpty()
val dd=v.getElementsByTagName("dd");Vod(field("id"),field("name"),field("year"),field("pic"),field("des"),(0 until dd.length).map{n->val d=dd.item(n) as org.w3c.dom.Element;Road(d.getAttribute("flag"),d.textContent.split('#').mapNotNull{val at=it.indexOf('$');if(at<0)null else Episode(it.substring(0,at),it.substring(at+1))})})}}
    suspend fun prepareAuth054(site:Site)=bridge.prepare(site)
    suspend fun probe(site:Site,road:Road,episode:Episode,interactive:Boolean=false):Candidate {
        if(!interactive&&(CloudPolicy054.needsAuthorization(site)||CloudPolicy054.isCloudRoad(road.name))&&System.currentTimeMillis()-db.prefs.getLong("cloud_authenticated_${site.key}",0)>30L*86400000)throw IOException("需要扫码授权，请手动打开网盘")
        if(SourceDelivery103.downloadOnly(road.name))throw IOException("此线路仅供下载，请使用下载按钮")
        val resolved=if(site.type==3)bridge.player(site,road,episode)else if(site.type==10)OneTouch103.resolve(episode)else if(site.type==9)anime.resolve(site,episode)else episode to emptyMap();var media=resolved.first;var requestSite=site.copy(headers=site.headers+resolved.second+if(site.type==8)Stremio050.headersFor(media.url)else emptyMap())
        val u=media.url.toHttpUrlOrNull()?:throw IOException("播放地址无效");if(u.scheme !in listOf("http","https"))throw IOException("线路协议未支持")
        val start=System.nanoTime();var data=try{if(requestSite.type in listOf(8,10))InternationalHttp103.bytes(media.url,requestSite.headers+mapOf("Range" to "bytes=0-4095"),4096,true,media=requestSite.type==8)else Net.bytes(media.url,requestSite.headers+mapOf("Range" to "bytes=0-4095"),4096,true)}catch(e:CancellationException){throw e}catch(e:Exception){if(LiveParser050.isPortal(site)||site.group=="直播与体育"||e.message.orEmpty().contains("HTTP 405")||e.message.orEmpty().contains("HTTP 416")||e.message.orEmpty().contains("HTTP 501"))if(requestSite.type in listOf(8,10))InternationalHttp103.bytes(media.url,requestSite.headers,4096,true,media=requestSite.type==8)else Net.bytes(media.url,requestSite.headers,4096,true)else throw e}
        if(!MediaBytes052.accept(data)){val html=data.toString(Charsets.UTF_8);if(html.trimStart().startsWith("<")||media.url.contains("/play")||LiveParser050.isPortal(site)||site.group=="直播与体育"){val result=WebMedia052.resolve(context,media.url,requestSite.headers,html);media=media.copy(url=result.first);requestSite=requestSite.copy(headers=result.second);data=try{if(requestSite.type in listOf(8,10))InternationalHttp103.bytes(media.url,requestSite.headers+mapOf("Range" to "bytes=0-4095"),4096,true,media=requestSite.type==8)else Net.bytes(media.url,requestSite.headers+mapOf("Range" to "bytes=0-4095"),4096,true)}catch(e:CancellationException){throw e}catch(_:Exception){if(requestSite.type in listOf(8,10))InternationalHttp103.bytes(media.url,requestSite.headers,4096,true,media=requestSite.type==8)else Net.bytes(media.url,requestSite.headers,4096,true)}}}
        val text=data.toString(Charsets.UTF_8);val hls=MediaBytes052.hls(data);if(!MediaBytes052.accept(data))throw IOException("线路返回网页或无效媒体，请切换其他线路")
        val height=Regex("RESOLUTION=\\d+x(\\d+)").findAll(text).mapNotNull{it.groupValues[1].toIntOrNull()}.maxOrNull()?:Regex("(720|1080|2160)[pP]").find(road.name)?.groupValues?.get(1)?.toInt()?:0
        return Candidate(requestSite,road.name,media,height,bytes=ResourceLabels040.bytes(road.name+" "+episode.name),delay=(System.nanoTime()-start)/1_000_000,mimeType=if(hls)"application/x-mpegURL"else if(data.size>=4&&data.take(4).toByteArray().contentEquals(byteArrayOf(0x1a,0x45,0xdf.toByte(),0xa3.toByte())))"video/x-matroska"else "")
    }
}
