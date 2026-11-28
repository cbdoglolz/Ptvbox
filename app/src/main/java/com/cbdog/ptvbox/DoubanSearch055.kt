package com.cbdog.ptvbox
import kotlinx.coroutines.*
import org.json.JSONObject
/** Parse only movie/TV subject entries. Feed comments, people and search words are not works. */
object DoubanSearch055 {
 fun subjects(root:JSONObject):List<Work> =(root.optJSONObject("subjects")?.optJSONArray("items")?:root.optJSONArray("items"))?.objects().orEmpty().mapNotNull{row->val kind=row.optString("target_type");val j=row.optJSONObject("target")?:return@mapNotNull null;if(kind !in listOf("movie","tv"))return@mapNotNull null;parse(j,kind)}
 fun cards(root:JSONObject):List<Work> = root.optJSONArray("cards")?.objects().orEmpty().mapNotNull{j->if(j.optString("type") !in listOf("movie","tv"))return@mapNotNull null;val id=Regex("https://movie\\.douban\\.com/subject/([0-9]+)/?").matchEntire(j.optString("url"))?.groupValues?.get(1)?:return@mapNotNull null;parse(j.put("id",id),if(j.optString("type")=="tv"||Identity.season(j.optString("title"))!=null)"tv"else"movie")}
 private fun parse(j:JSONObject,kind:String):Work? {
  val id=j.optString("id");val title=j.optString("title")
  if(!id.matches(Regex("[0-9]+"))||title.isBlank())return null
  val rating=j.optJSONObject("rating");val sub=j.optString("card_subtitle")
  val images=listOf(j.optString("cover_url"),j.optString("cover"))+listOf("pic","images","cover").flatMap{key->
   val pic=j.optJSONObject(key);listOf("large","normal","url","small").map{pic?.optString(it).orEmpty()}
  }
  return Work(id,title,kind,j.optString("year").ifBlank{Regex("(?:19|20)[0-9]{2}").find(sub)?.value.orEmpty()},
   poster=images.firstOrNull{it.startsWith("https://")||it.startsWith("http://")}.orEmpty(),
   original=j.optString("original_title"),aliases=j.arrayStrings("aka"),genres=j.arrayStrings("genres"),
   rating=rating?.optString("value")?.takeUnless{it in listOf("null","0","")}?:Regex("([0-9]\\.[0-9])分").find(sub)?.groupValues?.get(1)?:"-",
   votes=rating?.optInt("count")?:0,season=Identity.season(title)?:1)
 }

 suspend fun search(q:String):List<Work> = supervisorScope {
  val headers=mapOf("Referer" to Net.query("https://m.douban.com/search/",mapOf("query" to q)),"Origin" to "https://m.douban.com")
  val full=async{try{withTimeout(7000){subjects(Net.json(Net.query("https://m.douban.com/rexxar/api/v2/search",mapOf("q" to q,"type" to "movie")),headers))}}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;emptyList()}catch(_:Exception){emptyList()}}
  val short=async{try{withTimeout(6000){cards(Net.json(Net.query("https://www.douban.com/j/search_suggest",mapOf("q" to q)),headers))}}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;emptyList()}catch(_:Exception){emptyList()}}
  (full.await()+short.await()).distinctBy{it.id}
 }
}
