package com.cbdog.ptvbox

import org.json.JSONObject
import kotlinx.coroutines.*
import java.io.IOException

suspend fun Metadata.collection(work:Work):List<Work>{
    val id=work.facts.collectionId;if(!id.matches(Regex("[0-9]+")))return emptyList()
    return tmdb("collection/$id").optJSONArray("parts")?.objects()?.map{tmdbWork(it,"movie")}?.filter{it.id.isNotBlank()}?.sortedBy{it.releaseDate}.orEmpty()
}
suspend fun Metadata.personWorks(person:PersonCredit,provider:String,page:Int=1):List<Work>{
    if(provider=="douban")return doubanPersonWorks(person,page)
    val id=if(provider=="tmdb")person.id else {
        val hits=tmdb("search/person",mapOf("query" to person.name)).optJSONArray("results")?.objects().orEmpty()
        val exact=hits.filter{Identity.normalize(it.optString("name"))==Identity.normalize(person.name)||Identity.normalize(it.optString("original_name"))==Identity.normalize(person.name)}
        val verified=if(exact.size==1)exact else {val names=mutableListOf<JSONObject>();for(hit in hits.take(4)){val detail=tmdb("person/${hit.optString("id")}");if((detail.arrayStrings("also_known_as")+detail.optString("name")).any{Identity.normalize(it)==Identity.normalize(person.name)})names+=detail};names}
        if(verified.size!=1)throw IOException("人物没有唯一TMDB匹配，未混入同名人物作品")
        verified.single().optString("id")
    }
    require(id.matches(Regex("[0-9]+")))
    val j=tmdb("person/$id/combined_credits")
    return (j.optJSONArray("cast")?.objects().orEmpty()+j.optJSONArray("crew")?.objects().orEmpty().filter{it.optString("job")=="Director"}).filter{it.optString("media_type") in listOf("movie","tv")}.map{tmdbWork(it,it.optString("media_type"))}.distinctBy{it.key}.sortedByDescending{it.releaseDate}
}
suspend fun Metadata.doubanPersonWorks(person:PersonCredit,page:Int=1):List<Work>{
    val headers=mapOf("Referer" to "https://movie.douban.com/","Origin" to "https://movie.douban.com")
    if(person.id.matches(Regex("[0-9]+")))runCatching{
        val j=Net.json(Net.query("https://m.douban.com/rexxar/api/v2/celebrity/${person.id}/works",mapOf("start" to ((page-1)*20).toString(),"count" to "20","sort" to "time")),headers)
        val direct=j.optJSONArray("works")?.objects().orEmpty().mapNotNull{it.optJSONObject("work")}.map{dbWork(it,it.optString("subtype").ifBlank{it.optString("type")})}.filter{it.id.isNotBlank()}
        if(direct.isNotEmpty())return direct
    }
    if(page>1)return emptyList()
    val search=Net.json(Net.query("https://m.douban.com/rexxar/api/v2/search",mapOf("q" to person.name)),headers)
    val personTarget=search.optJSONObject("subjects")?.optJSONArray("items")?.objects().orEmpty().mapNotNull{it.optJSONObject("target")}.firstOrNull{target->
        target.optString("target_type").ifBlank{target.optString("subtype")}=="person"&&Identity.normalize(target.optString("title"))==Identity.normalize(person.name)
    }?:return emptyList()
    val short=personTarget.optJSONObject("extra")?.optString("short_info").orEmpty()
    val tail=short.substringAfter('/',"").trim()
    val titles=(listOf(tail)+tail.split('/',' ','、','，','\t')).map{it.trim()}.filter{it.length>=2}.distinct().take(12)
    val rows=mutableListOf<Work>()
    for(title in titles){
        val hits=runCatching{DoubanSearch055.search(title)}.getOrDefault(emptyList())
        hits.firstOrNull{Identity.normalize(it.title)==Identity.normalize(title)}?.let{rows+=it}
    }
    return rows.distinctBy{it.id}
}
suspend fun Metadata.companyWorks(company:CompanyCredit,kind:String,page:Int=1):List<Work>{
    require(company.id.matches(Regex("[0-9]+")))
    val k=if(company.role=="播出平台")"tv"else kind
    return tmdb("discover/$k",mapOf((if(company.role=="播出平台")"with_networks"else"with_companies") to company.id,"page" to page.toString())).optJSONArray("results")?.objects()?.map{tmdbWork(it,k)}.orEmpty()
}
suspend fun Metadata.versions(work:Work):List<Work> = search(work.title.substringBefore('：').substringBefore(':')).filter{it.key!=work.key&&Identity.base(it.title)==Identity.base(work.title)}.take(16)
suspend fun Metadata.moreRelated(work:Work,page:Int):List<Work> = supervisorScope {
    val series=if(page==1)async{seriesLinks103(work)}else null
    val similar=async{
        try{
            withTimeout(6500){
                if(work.provider=="tmdb")tmdb("${work.kind}/${work.id}/recommendations",mapOf("page" to page.toString())).optJSONArray("results")?.objects()?.map{tmdbWork(it,work.kind)}.orEmpty()
                else if(page==1)related(work).ifEmpty{feed("douban",work.kind,if(work.genres.contains("动画"))"动画"else"热门",page)}
                else feed("douban",work.kind,if(work.genres.contains("动画"))"动画"else"热门",page)
            }
        }catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;emptyList()}catch(_:Exception){emptyList()}
    }
    SeriesLinks103.merge(work,series?.await().orEmpty(),similar.await())
}

fun Work.detailFacts():List<String> = (listOf(year,if(kind=="movie")"电影"else"电视剧 / 第${season}季")+facts.countries+genres).filter{it.isNotBlank()}.distinct()
internal object DetailFact056 {
    private val aliases=mapOf(
        "Japan" to "日本","United States of America" to "美国","United States" to "美国","China" to "中国大陆",
        "South Korea" to "韩国","United Kingdom" to "英国","France" to "法国","Germany" to "德国",
        "Comedy" to "喜剧","Action" to "动作","Drama" to "剧情","Animation" to "动画","Documentary" to "纪录片",
        "Fantasy" to "奇幻","History" to "历史","Adventure" to "冒险","Science Fiction" to "科幻",
        "Mystery" to "悬疑","Crime" to "犯罪","Romance" to "爱情","Horror" to "恐怖"
    )
    fun display(label:String):String=aliases[label.trim()]?:label.trim()
    fun normalize(label:String):String {
        val trimmed=display(label).substringBefore(" / ").trim()
        if(trimmed.matches(Regex("[12][0-9]{3}")))return trimmed
        if(trimmed in listOf("电影","电视剧"))return ""
        return trimmed
    }
}
fun Work.sourceFacts():String=buildList{
    if(original.isNotBlank())add(original)
    if(releaseDate.isNotBlank())add("上映/首播：$releaseDate")
    if(facts.runtime>0)add("片长：${facts.runtime} 分钟")
    if(facts.languages.isNotEmpty())add("语言：${facts.languages.joinToString(" / ")}")
    if(aliases.isNotEmpty())add("又名：${aliases.take(8).joinToString(" / ")}")
    if(facts.imdbId.isNotBlank())add("IMDb：${facts.imdbId}")
    if(facts.tmdbId.isNotBlank())add("TMDB：${facts.tmdbId}")
    if(facts.doubanId.isNotBlank())add("豆瓣：${facts.doubanId}")
    if(facts.bangumiId.isNotBlank())add("Bangumi：${facts.bangumiId}")
}.joinToString("\n")
