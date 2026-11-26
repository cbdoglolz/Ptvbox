package com.cbdog.ptvbox

import org.json.JSONArray
import org.json.JSONObject

data class CompanyCredit(val id:String,val name:String,val role:String="出品方",val logo:String=""){
    fun json()=JSONObject().put("id",id).put("name",name).put("role",role).put("logo",logo)
    companion object{fun from(j:JSONObject)=CompanyCredit(j.optString("id"),j.optString("name"),j.optString("role"),j.optString("logo"))}
}
data class WorkFacts(val runtime:Int=0,val countries:List<String> = emptyList(),val languages:List<String> = emptyList(),val companies:List<CompanyCredit> = emptyList(),val collectionId:String="",val collectionName:String="",val imdbId:String="",val tagline:String="",val provider:String="",val doubanId:String="",val tmdbId:String="",val bangumiId:String=""){
    fun json()=JSONObject().put("runtime",runtime).put("countries",JSONArray(countries)).put("languages",JSONArray(languages)).put("companies",JSONArray(companies.map{it.json()})).put("collectionId",collectionId).put("collectionName",collectionName).put("imdbId",imdbId).put("tagline",tagline).put("provider",provider).put("doubanId",doubanId).put("tmdbId",tmdbId).put("bangumiId",bangumiId)
    companion object{
        fun from(j:JSONObject)=WorkFacts(j.optInt("runtime"),j.arrayStrings("countries"),j.arrayStrings("languages"),j.optJSONArray("companies")?.objects()?.map(CompanyCredit::from).orEmpty(),j.optString("collectionId"),j.optString("collectionName"),j.optString("imdbId"),j.optString("tagline"),j.optString("provider"),j.optString("doubanId"),j.optString("tmdbId"),j.optString("bangumiId"))
        fun tmdb(j:JSONObject)=WorkFacts(j.optInt("runtime").takeIf{it>0}?:j.optJSONArray("episode_run_time")?.optInt(0)?:0,j.optJSONArray("production_countries")?.objects()?.map{it.optString("name")}.orEmpty(),j.optJSONArray("spoken_languages")?.objects()?.map{it.optString("name")}.orEmpty(),listOf("production_companies" to "出品方","networks" to "播出平台").flatMap{(key,role)->j.optJSONArray(key)?.objects()?.map{CompanyCredit(it.optString("id"),it.optString("name"),role,it.optString("logo_path").takeIf{p->p.startsWith('/')}?.let{p->"https://image.tmdb.org/t/p/w185$p"}.orEmpty())}.orEmpty()},j.optJSONObject("belongs_to_collection")?.optString("id").orEmpty(),j.optJSONObject("belongs_to_collection")?.optString("name").orEmpty(),j.optJSONObject("external_ids")?.optString("imdb_id").orEmpty().ifBlank{j.optString("imdb_id")}.takeIf{it.matches(Regex("tt[0-9]+"))}.orEmpty(),j.optString("tagline"),"tmdb",tmdbId=j.optString("id"))
        fun douban(j:JSONObject)=WorkFacts(j.optJSONArray("durations")?.optString(0)?.let{Regex("[0-9]+").find(it)?.value?.toIntOrNull()}?:0,j.arrayStrings("countries"),j.arrayStrings("languages"),imdbId=j.optString("imdb_id").takeIf{it.matches(Regex("tt[0-9]+"))}.orEmpty(),provider="douban",doubanId=j.optString("id"))
    }
}
/** External metadata enriches one stable work; it never replaces its local identity. */
fun Work.enriched(other:Work)=copy(
    overview=other.overview.ifBlank{overview},poster=other.poster.ifBlank{poster},backdrop=other.backdrop.ifBlank{backdrop},
    original=other.original.ifBlank{original},aliases=(aliases+other.aliases+listOf(other.title)).filter{it.isNotBlank()}.distinct(),
    genres=other.genres.ifEmpty{genres},people=other.people.ifEmpty{people},releaseDate=other.releaseDate.ifBlank{releaseDate},
    facts=other.facts.copy(runtime=other.facts.runtime.takeIf{it>0}?:facts.runtime,countries=other.facts.countries.ifEmpty{facts.countries},languages=other.facts.languages.ifEmpty{facts.languages},companies=other.facts.companies.ifEmpty{facts.companies},collectionId=other.facts.collectionId.ifBlank{facts.collectionId},collectionName=other.facts.collectionName.ifBlank{facts.collectionName},imdbId=other.facts.imdbId.ifBlank{facts.imdbId},doubanId=other.facts.doubanId.ifBlank{facts.doubanId},tmdbId=other.facts.tmdbId.ifBlank{facts.tmdbId},bangumiId=other.facts.bangumiId.ifBlank{facts.bangumiId}))
