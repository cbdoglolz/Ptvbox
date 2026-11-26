package com.cbdog.ptvbox

import org.json.JSONArray
import org.json.JSONObject

internal object IdentityBridge060 {
    private fun values(row:JSONObject):List<String>{
        return when(val value=row.opt("value")){
            is String->listOf(value)
            is JSONArray->(0 until value.length()).map{value.optJSONObject(it)?.optString("v")?:value.optString(it)}
            else->emptyList()
        }
    }
    private fun info(infobox:JSONArray?,keys:Set<String>)=infobox?.objects().orEmpty().filter{it.optString("key") in keys}.flatMap(::values)
    fun bangumiFacts(work:Work,bangumiId:String,infobox:JSONArray?):WorkFacts{
        val text=info(infobox,setOf("IMDb","TMDB","豆瓣","Bangumi","bangumi")).joinToString("|")
        val imdb=Regex("tt[0-9]{5,12}",RegexOption.IGNORE_CASE).find(text)?.value.orEmpty()
        val tmdb=Regex("(?i)(?:TMDB|themoviedb)[^0-9]{0,12}([0-9]{1,12})").find(text)?.groupValues?.get(1).orEmpty()
        val douban=Regex("豆瓣[^0-9]{0,12}([0-9]{1,12})").find(text)?.groupValues?.get(1).orEmpty()
        return work.facts.copy(bangumiId=bangumiId.ifBlank{work.facts.bangumiId},imdbId=work.facts.imdbId.ifBlank{imdb},tmdbId=work.facts.tmdbId.ifBlank{tmdb},doubanId=work.facts.doubanId.ifBlank{douban})
    }
}

internal suspend fun Metadata.resolveIdentity060(work:Work):Work{
    var facts=work.facts.copy(
        doubanId=work.facts.doubanId.ifBlank{if(work.provider=="douban")work.id else ""},
        tmdbId=work.facts.tmdbId.ifBlank{if(work.provider=="tmdb")work.id else ""}
    )
    var aliases=work.aliases.toMutableList()
    if(facts.imdbId.isBlank()&&facts.tmdbId.matches(Regex("[0-9]{1,12}"))&&tmdbConfiguredForIdentity())runCatching{
        val j=tmdb("${work.kind}/${facts.tmdbId}/external_ids")
        facts=facts.copy(imdbId=j.optString("imdb_id").takeIf{it.startsWith("tt")}?:facts.imdbId)
    }
    if(facts.imdbId.matches(Regex("tt[0-9]+"))&&!facts.tmdbId.matches(Regex("[0-9]{1,12}"))&&tmdbConfiguredForIdentity())runCatching{
        val find=tmdb("find/${facts.imdbId}",mapOf("external_source" to "imdb_id"))
        val row=(find.optJSONArray("movie_results")?.objects().orEmpty()+find.optJSONArray("tv_results")?.objects().orEmpty()).firstOrNull()
        if(row!=null){
            facts=facts.copy(tmdbId=row.optString("id"))
            val title=row.optString("title").ifBlank{row.optString("name")}
            aliases=(aliases+listOf(title,row.optString("original_title"),row.optString("original_name"))).filter{it.isNotBlank()}.distinct().toMutableList()
        }
    }
    if(facts.imdbId.isBlank()){
        val names=(listOf(work.title,work.original)+work.aliases).filter{it.isNotBlank()}.distinct().take(2)
        for(name in names){
            val type=if(work.kind=="tv")"series"else"movie"
            val raw=runCatching{Net.json("https://v3-cinemeta.strem.io/catalog/$type/top/search=${java.net.URLEncoder.encode(name,"UTF-8")}.json")}.getOrNull()?:continue
            val hits=raw.optJSONArray("metas")?.objects().orEmpty().filter{m->
                val title=m.optString("name")
                val year=m.optString("releaseInfo").take(4)
                (Identity.base(title)==Identity.base(name)||work.aliases.any{Identity.base(it)==Identity.base(title)})&&(work.year.length!=4||year.length!=4||year==work.year)
            }.distinctBy{it.optString("imdb_id")}
            if(hits.size==1){
                facts=facts.copy(imdbId=hits.single().optString("imdb_id"))
                aliases=(aliases+listOf(hits.single().optString("name"))).filter{it.isNotBlank()}.distinct().toMutableList()
                break
            }
        }
    }
    val isAnime=work.provider=="anilist"||work.genres.any{Regex("动画|动漫|anime|animation",RegexOption.IGNORE_CASE).containsMatchIn(it)}
    if(isAnime&&facts.bangumiId.isBlank())runCatching{
        val q=Identity.searchTitle(work.title).ifBlank{work.title}
        val body=JSONObject().put("keyword",q).put("filter",JSONObject().put("type",JSONArray(listOf(2))))
        val raw=Net.postJson("https://api.bgm.tv/v0/search/subjects?limit=8&offset=0",body.toString())
        val items=JSONObject(raw).optJSONArray("data")?.objects().orEmpty()
        val match=items.firstOrNull{b->
            val dateYear=b.optString("date").take(4)
            val yearOk=work.year.length!=4||dateYear.length!=4||dateYear==work.year||Identity.isNianfan(work)
            val bNames=listOf(b.optString("name"),b.optString("name_cn")).filter{it.isNotBlank()}
            yearOk&&bNames.any{Identity.base(it)==Identity.base(work.title)||Identity.normalize(Identity.seriesCore(it))==Identity.normalize(Identity.seriesCore(work.title))}
        }
        if(match!=null){
            facts=IdentityBridge060.bangumiFacts(work.copy(facts=facts),match.optString("id"),match.optJSONArray("infobox"))
            val extra=listOf(match.optString("name_cn"),match.optString("name"))+(match.optJSONArray("infobox")?.objects().orEmpty().filter{it.optString("key") in listOf("中文名","别名","简体中文名","繁体中文名")}.flatMap{row->when(val v=row.opt("value")){is String->listOf(v);is JSONArray->(0 until v.length()).map{v.optJSONObject(it)?.optString("v")?:v.optString(it)};else->emptyList()}})
            aliases=(aliases+extra).filter{it.isNotBlank()}.distinct().toMutableList()
        }
    }
    return work.copy(facts=facts,aliases=aliases)
}

private fun Metadata.tmdbConfiguredForIdentity()=tmdbReady

internal suspend fun Metadata.tmdbFromImdb060(imdb:String):Work?{
    if(!imdb.matches(Regex("tt[0-9]+"))||!tmdbConfiguredForIdentity())return null
    val find=tmdb("find/$imdb",mapOf("external_source" to "imdb_id"))
    val movie=find.optJSONArray("movie_results")?.objects().orEmpty()
    val tv=find.optJSONArray("tv_results")?.objects().orEmpty()
    val pairs=movie.map{it to "movie"}+tv.map{it to "tv"}
    if(pairs.size!=1)return null
    val (row,kind)=pairs.single()
    return tmdbWork(row,kind)
}
