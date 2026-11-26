package com.cbdog.ptvbox

import com.github.promeg.pinyinhelper.Pinyin
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

data class PersonCredit(val id:String,val name:String,val role:String="",val avatar:String="",val provider:String="") {
    fun json()=JSONObject().put("id",id).put("name",name).put("role",role).put("avatar",avatar).put("provider",provider)
    companion object {fun from(j:JSONObject)=PersonCredit(j.optString("id"),j.optString("name"),j.optString("role"),j.optString("avatar"),j.optString("provider"))}
}
data class Work(val id:String,val title:String,val kind:String="movie",val year:String="",val poster:String="",val overview:String="",val rating:String="-",val provider:String="douban",val original:String="",val aliases:List<String> = emptyList(),val genres:List<String> = emptyList(),val season:Int=1,val backdrop:String="",val people:List<PersonCredit> = emptyList(),val identityKey:String="",val releaseDate:String="",val votes:Int=0,val facts:WorkFacts=WorkFacts(),val catalogSeason:Int=season,val episodeCount:Int=0) {
    val key get()=identityKey.ifBlank{"$provider:$kind:$id"}
    val legacyKey get()="$provider:$id"
    fun json()=JSONObject().put("id",id).put("title",title).put("kind",kind).put("year",year).put("poster",poster).put("overview",overview).put("rating",rating).put("provider",provider).put("original",original).put("aliases",JSONArray(aliases)).put("genres",JSONArray(genres)).put("season",season).put("backdrop",backdrop).put("people",JSONArray(people.map{it.json()})).put("identityKey",identityKey).put("releaseDate",releaseDate).put("votes",votes).put("facts",facts.json()).put("catalogSeason",catalogSeason).put("episodeCount",episodeCount)
    companion object { fun from(j:JSONObject)=Work(j.getString("id").also{require(it.isNotBlank())},j.getString("title").also{require(it.isNotBlank())},j.optString("kind","movie"),j.optString("year"),j.optString("poster"),j.optString("overview"),j.optString("rating","-"),j.optString("provider","douban"),j.optString("original"),j.arrayStrings("aliases"),j.arrayStrings("genres"),j.optInt("season",1),j.optString("backdrop"),j.optJSONArray("people")?.objects()?.map(PersonCredit::from)?:emptyList(),j.optString("identityKey"),j.optString("releaseDate"),j.optInt("votes"),WorkFacts.from(j.optJSONObject("facts")?:JSONObject()),j.optInt("catalogSeason",j.optInt("season",1)),j.optInt("episodeCount",0)) }
}
data class Site(val key:String,val name:String,val api:String,val type:Int=1,val group:String="默认点播",val headers:Map<String,String> = emptyMap(),val ext:String="",val jar:String="",val searchable:Boolean=true) {
    fun json()=JSONObject().put("key",key).put("name",name).put("api",api).put("type",type).put("group",group).put("ext",ext).put("jar",jar).put("searchable",if(searchable)1 else 0).put("header",JSONObject(headers))
    companion object { fun from(j:JSONObject)=Site(j.optString("key"),j.optString("name"),j.optString("api"),j.optInt("type",1),j.optString("group","默认点播"),j.optJSONObject("header")?.let{h->h.keys().asSequence().associateWith{h.optString(it)}}?:emptyMap(),j.optString("ext"),j.optString("jar"),j.opt("searchable")?.toString()?.lowercase() !in listOf("0","false")) }
}
data class Episode(val name:String,val url:String)
data class Road(val name:String,val episodes:List<Episode>)
data class Vod(val id:String,val title:String,val year:String,val poster:String,val overview:String,val roads:List<Road>,val folder:Boolean=false)
data class Candidate(val site:Site,val road:String,val episode:Episode,val height:Int=0,val bytes:Long=0,val delay:Long=0,val sourceId:String="",val mimeType:String="") {val label get()="${site.name} · $road";val identity get()="${site.key}|$road|${episode.name}"}
data class SiteStatus(val site:Site,val text:String="等待检测",val usable:Boolean=false,val candidate:Candidate?=null)
data class Watch(val work:Work,val episode:String,val siteKey:String,val road:String,val height:Int,val position:Long,val duration:Long,val updated:Long=System.currentTimeMillis(),val done:Boolean=false,val sourceId:String="") {
    val progress get()=if(duration>0)(position.toFloat()/duration).coerceIn(0f,1f) else 0f
    fun json()=JSONObject().put("work",work.json()).put("episode",episode).put("siteKey",siteKey).put("road",road).put("height",height).put("position",position).put("duration",duration).put("updated",updated).put("done",done).put("sourceId",sourceId)
    companion object {fun from(j:JSONObject)=Watch(Work.from(j.getJSONObject("work")),j.optString("episode"),j.optString("siteKey"),j.optString("road"),j.optInt("height"),j.optLong("position"),j.optLong("duration"),j.optLong("updated"),j.optBoolean("done"),j.optString("sourceId"))}
}
fun JSONObject.arrayStrings(name:String)=optJSONArray(name)?.let{a->(0 until a.length()).map{a.optString(it)}}?:emptyList()
fun JSONArray.objects()=(0 until length()).mapNotNull{optJSONObject(it)}
fun clock(ms:Long):String {val seconds=(ms.coerceAtLeast(0)/1000);return if(seconds>=3600)"%d:%02d:%02d".format(seconds/3600,seconds/60%60,seconds%60) else "%02d:%02d".format(seconds/60,seconds%60)}

object Identity {
    private val cnDigits=mapOf('零' to 0,'〇' to 0,'一' to 1,'二' to 2,'两' to 2,'三' to 3,'四' to 4,'五' to 5,'六' to 6,'七' to 7,'八' to 8,'九' to 9)
    private val enNumbers=mapOf("one" to 1,"two" to 2,"three" to 3,"four" to 4,"five" to 5,"six" to 6,"seven" to 7,"eight" to 8,"nine" to 9,"ten" to 10,"eleven" to 11,"twelve" to 12,"thirteen" to 13,"fourteen" to 14,"fifteen" to 15,"sixteen" to 16,"seventeen" to 17,"eighteen" to 18,"nineteen" to 19,"twenty" to 20)
    private val romanNumbers=mapOf("ii" to 2,"iii" to 3,"iv" to 4,"v" to 5,"vi" to 6,"vii" to 7,"viii" to 8,"ix" to 9,"x" to 10,"xi" to 11,"xii" to 12)
    private fun chineseNumber(value:String):Int?{
        if(value.all{it.isDigit()})return value.toIntOrNull()
        if(value=="十")return 10
        val ten=value.indexOf('十')
        return if(ten>=0){val head=if(ten==0)1 else cnDigits[value[ten-1]]?:return null;val tail=if(ten==value.lastIndex)0 else cnDigits[value[ten+1]]?:return null;head*10+tail}else value.mapNotNull(cnDigits::get).joinToString("").toIntOrNull()
    }
    private fun normalizeNumberWords(value:String):String{
        var out=value
        Regex("(?i)\\b(season|part|volume|vol)\\s+(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty)\\b").findAll(out).toList().asReversed().forEach{m->out=out.replaceRange(m.range,"${m.groupValues[1]} ${enNumbers[m.groupValues[2].lowercase()]?:m.groupValues[2]}")}
        Regex("(?i)\\b(ii|iii|iv|v|vi|vii|viii|ix|x|xi|xii)$").find(out)?.let{m->romanNumbers[m.value.lowercase()]?.let{out=out.replaceRange(m.range,it.toString())}}
        return out
    }
    fun searchTitle(value:String)=value.replace(Regex("第[零〇一二三四五六七八九十百两0-9]+[季期部集话話]|(?i)season\\s*[0-9一二三四五六七八九十]+|(?i)\\bS\\d{1,2}\\b"),"").trim()
    fun sameEpisode(a:String,b:String):Boolean = normalize(a)==normalize(b)||(episode(a)?.let{it==episode(b)}?:false)
    fun normalize(value:String)=normalizeNumberWords(Normalizer.normalize(value,Normalizer.Form.NFKC).lowercase()).replace(Regex("[\\p{P}\\p{Z}\\s]"),"").let{s->
        Regex("第([零〇一二三四五六七八九十百两]+)([季期部集话話])").replace(s){m->"第${chineseNumber(m.groupValues[1])?:m.groupValues[1]}${m.groupValues[2]}"}
    }
    fun base(value:String)=normalize(searchTitle(value))
    fun episode(value:String):Int? {
        Regex("(?i)S\\d{1,2}\\s*E(?:P)?\\s*(\\d{1,4})").find(value)?.let{return it.groupValues[1].toInt()}
        Regex("第\\s*(\\d{1,4})\\s*[集话話]").find(value)?.let{return it.groupValues[1].toInt()}
        Regex("(?i)(?:episode|ep|e)\\s*0*(\\d{1,4})(?!\\d)").find(value)?.let{return it.groupValues[1].toInt()}
        return value.trim().takeIf{it.matches(Regex("\\d{1,4}"))}?.toInt()
    }
    fun season(value:String):Int? {Regex("(?i)(?:season|\\bS)\\s*(\\d{1,2})").find(value)?.let{return it.groupValues[1].toInt()};Regex("第\\s*(\\d{1,2})\\s*[季期部]").find(value)?.let{return it.groupValues[1].toInt()};val cn=Regex("第([一二三四五六七八九十])[季期部]").find(value)?.groupValues?.get(1);return cn?.let{"一二三四五六七八九十".indexOf(it)+1}}
    fun matches(work:Work,vod:Vod):Boolean {
        if(vod.title.contains(Regex("解说|預告|预告|花絮|幕后|抢先看")))return false
        if(work.year.length==4&&vod.year.length==4&&work.year!=vod.year)return false
        if(work.kind=="tv"){val actual=season(vod.title);if(actual!=null&&actual!=work.season)return false;if(actual==null&&work.season>1)return false}
        return (listOf(work.title,work.original)+work.aliases).filter{it.isNotBlank()}.any{base(it)==base(vod.title)}
    }
    fun pick(road:Road,episodeName:String,kind:String):Episode? {
        val number=episode(episodeName)
        if(kind=="movie")return road.episodes.firstOrNull{!it.name.contains(Regex("预告|花絮"))}
        return road.episodes.firstOrNull{normalize(it.name)==normalize(episodeName)}?:number?.let{n->road.episodes.firstOrNull{episode(it.name)==n}}
    }
    fun initials(value:String)=value.map{if(Pinyin.isChinese(it))Pinyin.toPinyin(it).first().lowercaseChar() else it.lowercaseChar()}.filter{it.isLetterOrDigit()}.joinToString("")
    fun keyword(work:Work,q:String):Boolean {val query=normalize(q);return (listOf(work.title,work.original)+work.aliases).any{normalize(it).contains(query)||initials(it).contains(query)||(it.contains("攻壳")&&initials(it).replaceFirst("gq","gk").contains(query))}}
}

object VodParser {
    fun parse(j:JSONObject):Vod {
        val flags=j.optString("vod_play_from").split("$$$");val rows=j.optString("vod_play_url").split("$$$")
        val roads=rows.mapIndexedNotNull{i,row->val episodes=row.split('#').mapNotNull{part->val at=part.indexOf('$');if(at<0)null else Episode(part.substring(0,at).ifBlank{"正片"},part.substring(at+1))};if(episodes.isEmpty())null else Road(flags.getOrElse(i){"线路${i+1}"},episodes)}
        return Vod(j.optString("vod_id"),j.optString("vod_name"),j.optString("vod_year"),j.optString("vod_pic"),j.optString("vod_content").replace(Regex("<[^>]+>"),"").ifBlank{j.optString("vod_remarks")},roads,j.optString("vod_tag")=="folder")
    }
}

fun Work.catalogKey()="$key|season:$season"

val Work.ratingViewKey get()=if(kind=="tv")"$key|season:$season"else key
