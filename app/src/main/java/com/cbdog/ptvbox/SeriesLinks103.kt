package com.cbdog.ptvbox

import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/** Recommendation relationships never change the identity or playback coordinates of a Work. */
internal object SeriesLinks103 {
    private val edition=Regex("(?i)(?:剧场版|劇場版|电影版|電影版|\\bthe movie\\b|\\bmovie\\b|\\bTV series\\b|电视版|電視版)")
    private val suffix=Regex("(?i)(?:第[一二三四五六七八九十百0-9]+[季期部篇]|\\bseason\\s*\\d+|\\b[0-9]+(?:st|nd|rd|th) season|[：:]|\\s+[—–-]\\s+)")
    fun root(name:String):String {
        var text=edition.replace(name," ").trim(' ','·','：',':','-','—','（','）','(',')')
        suffix.find(text)?.let{text=text.substring(0,it.range.first)}
        // Chinese franchise names conventionally separate the subtitle with a space.
        if(Regex("^[一-鿿]{3,}\\s").containsMatchIn(text))text=text.substringBefore(' ')
        return text.trim().trimEnd('Ⅱ','Ⅲ','Ⅳ').replace(Regex("\\s+[2-9]$"),"").trim()
    }
    fun names(w:Work)=(listOf(w.title,w.original)+w.aliases).filter{it.isNotBlank()&&it!="null"}.distinct()
    fun queries(w:Work):List<String> = names(w).map(::root).filter{Identity.normalize(it).length>=3}.distinctBy{Identity.normalize(it)}.take(3)
    fun matches(w:Work,c:Work):Boolean {
        if(w.key==c.key||c.kind !in listOf("movie","tv"))return false
        val roots=names(w).map{Identity.normalize(root(it))}.filter{it.length>=3}.toSet()
        return names(c).any{Identity.normalize(root(it)) in roots}
    }
    private fun sameEntry(a:Work,b:Work):Boolean {
        if(a.key==b.key)return true
        if(a.kind!=b.kind)return false
        if(a.facts.imdbId.isNotBlank()&&a.facts.imdbId==b.facts.imdbId)return true
        val names=names(a).map{Identity.normalize(it)}.toSet()
        return a.year.isNotBlank()&&a.year==b.year&&names(b).any{Identity.normalize(it) in names}
    }
    fun merge(w:Work,series:List<Work>,similar:List<Work>):List<Work> = (series+similar)
        .filter{!sameEntry(w,it)&&it.id.isNotBlank()&&it.title.isNotBlank()}
        .distinctBy{it.key}.distinctBy{listOf(it.kind,Identity.normalize(it.original.ifBlank{it.title}),it.year,it.season.toString())}

    private val relationTypes=setOf("PREQUEL","SEQUEL","SIDE_STORY","SUMMARY","ALTERNATIVE","PARENT","SPIN_OFF","COMPILATION","CONTAINS")
    private const val fields="id type format title{native romaji english} synonyms episodes status startDate{year month day} coverImage{large} bannerImage"
    fun relationQuery()="query(\$id:Int!){Media(id:\$id,type:ANIME){relations{edges{relationType node{$fields relations{edges{relationType node{$fields}}}}}}}}"
    fun parseRelations(raw:JSONObject):List<Work> {
        val nodes=mutableListOf<JSONObject>()
        fun walk(media:JSONObject,depth:Int){
            for(edge in media.optJSONObject("relations")?.optJSONArray("edges")?.objects().orEmpty()){
                val node=edge.optJSONObject("node")?:continue
                if(edge.optString("relationType") !in relationTypes||node.optString("type")!="ANIME")continue
                if(node.optString("format") in listOf("TV","TV_SHORT","MOVIE","OVA","ONA","SPECIAL"))nodes+=node
                if(depth<1)walk(node,depth+1)
            }
        }
        raw.optJSONObject("data")?.optJSONObject("Media")?.let{walk(it,0)}
        val page=JSONObject().put("data",JSONObject().put("Page",JSONObject().put("pageInfo",JSONObject()).put("media",JSONArray(nodes))))
        return AnimeSeason056.parse(page.toString()).first.map{it.work}.distinctBy{it.key}
    }
}

internal suspend fun Metadata.seriesLinks103(w:Work):List<Work> = supervisorScope {
    suspend fun safe(block:suspend ()->List<Work>):List<Work> = try{withTimeout(5500){block()}}
        catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;emptyList()}catch(_:Exception){emptyList()}
    val names=SeriesLinks103.queries(w).take(2)
    val searches=names.map{q->async{safe{DoubanSearch055.search(q)}}}+
        if(tmdbReady)names.map{q->async{safe{tmdb("search/multi",mapOf("query" to q)).optJSONArray("results")?.objects().orEmpty().filter{it.optString("media_type") in listOf("movie","tv")}.map{tmdbWork(it,it.optString("media_type"))}}}}else emptyList()
    val anime=async{safe{
        val isAnime=w.provider=="anilist"||w.genres.any{Regex("动画|動漫|动漫|animation|anime",RegexOption.IGNORE_CASE).containsMatchIn(it)}
        if(!isAnime)return@safe emptyList()
        val id=if(w.provider=="anilist")w.id else {
            val query=w.original.ifBlank{w.title}
            val hits=AnimeSeason056.search(query).map{it.work}.filter{candidate->
                candidate.kind==w.kind&&(w.year.isBlank()||candidate.year==w.year)&&
                SeriesLinks103.names(candidate).any{n->SeriesLinks103.names(w).any{Identity.normalize(it)==Identity.normalize(n)}}
            }
            hits.singleOrNull()?.id.orEmpty()
        }
        if(!id.matches(Regex("[0-9]+")))return@safe emptyList()
        SeriesLinks103.parseRelations(JSONObject(Net.postJson("https://graphql.anilist.co",JSONObject().put("query",SeriesLinks103.relationQuery()).put("variables",JSONObject().put("id",id.toInt())).toString())))
    }}
    SeriesLinks103.merge(w,searches.awaitAll().flatten().filter{SeriesLinks103.matches(w,it)},anime.await())
}
