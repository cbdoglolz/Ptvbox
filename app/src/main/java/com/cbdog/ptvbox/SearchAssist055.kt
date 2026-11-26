package com.cbdog.ptvbox
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
data class SearchHints055(val query:String,val names:List<String>,val online:Boolean)
object SearchText055 {
 val keys=('A'..'Z').map{it.toString()}+(1..9).map{it.toString()}+listOf("0","-",".","'","/","空格","删除")
 fun edit(text:String,key:String)=when(key){"删除"->if(text.isEmpty())text else text.dropLast(Character.charCount(text.codePointBefore(text.length)));"空格"->(text+" ").take(120);else->(text+key).take(120)}
 fun initialQuery(q:String)=q.matches(Regex("[A-Za-z0-9]{2,12}"))
 fun suggestions(j:JSONObject)=j.optJSONArray("data")?.objects().orEmpty().map{it.optString("name")}.filter{it.isNotBlank()&&it.length<=120}.distinct().take(50)
}
/** No API credentials. Suggestions are names only, never imported as IDs, years or ratings. */
class SearchAssist055(private val db:Library,private val metadata:Metadata){
 private val cache=java.util.Collections.synchronizedMap(object:LinkedHashMap<String,Pair<Long,SearchHints055>>(64,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,Pair<Long,SearchHints055>>?)=size>64})
 suspend fun suggest(q:String,hot:Map<String,List<Work>>):SearchHints055=withContext(Dispatchers.IO){val query=q.trim().take(120);if(query.isEmpty())return@withContext SearchHints055(query,emptyList(),false);val local=(db.cached()+hot.values.flatten()).filter{Identity.keyword(it,query)}.map{it.title}.distinct();cache[Identity.normalize(query)]?.takeIf{System.currentTimeMillis()-it.first<3600000}?.let{return@withContext it.second.copy(query=query,names=(local+it.second.names).distinct().take(60))}
  val remote=try{withTimeout(4500){SearchText055.suggestions(Net.json(Net.query("https://suggest.video.iqiyi.com/",mapOf("key" to query,"if" to "mobile","rltnum" to "30")),mapOf("Referer" to "https://www.iqiyi.com/")))}}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;emptyList()}catch(_:Exception){emptyList()};SearchHints055(query,(local+remote).distinct().take(60),remote.isNotEmpty()).also{if(remote.isNotEmpty())cache[Identity.normalize(query)]=System.currentTimeMillis() to it}
 }
 suspend fun hot():Map<String,List<Work>> =withContext(Dispatchers.IO){val cached=runCatching{JSONObject(db.prefs.getString("search_hot055","{}")!!)}.getOrDefault(JSONObject());val prior=listOf("电视榜","电影榜","综艺榜").associateWith{name->cached.optJSONArray(name)?.objects()?.map(Work::from).orEmpty()};if(prior.values.any{it.isNotEmpty()}&&System.currentTimeMillis()-cached.optLong("time")<6*3600000)return@withContext prior
  val result=supervisorScope{listOf(Triple("电视榜","tv","热门"),Triple("电影榜","movie","热门"),Triple("综艺榜","tv","综艺")).map{(label,kind,category)->async{label to try{withTimeout(6500){metadata.feed("douban",kind,category,1).take(18)}}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;prior[label].orEmpty()}catch(_:Exception){prior[label].orEmpty()}}}.awaitAll().toMap()};if(result.values.any{it.isNotEmpty()})db.prefs.edit().putString("search_hot055",JSONObject().put("time",System.currentTimeMillis()).apply{result.forEach{(k,v)->put(k,JSONArray(v.map{it.json()}))}}.toString()).apply();result
 }
}
