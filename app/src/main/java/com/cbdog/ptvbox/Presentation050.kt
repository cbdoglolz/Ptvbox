package com.cbdog.ptvbox

import androidx.compose.runtime.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.json.JSONObject

/** Derived artwork/ratings cache; stable work, favorites and progress identities are never rewritten. */
data class Presentation050(val poster:String="",val backdrop:String="",val logo:String="",val ratings:Map<String,Work?> = emptyMap()) {
 fun json()=JSONObject().put("poster",poster).put("backdrop",backdrop).put("logo",logo).put("ratings",JSONObject().apply{ratings.forEach{(p,w)->put(p,w?.json()?:JSONObject.NULL)}})
 companion object {
  fun from(j:JSONObject):Presentation050 {val r=j.optJSONObject("ratings")?:JSONObject();return Presentation050(j.optString("poster"),j.optString("backdrop"),j.optString("logo"),r.keys().asSequence().associateWith{p->r.optJSONObject(p)?.let(Work::from)})}
  fun image(j:JSONObject,type:String,chineseOnly:Boolean):String {val rows=j.optJSONArray(type)?.objects().orEmpty().filter{it.optString("file_path").startsWith('/')};val chinese=rows.filter{it.optString("iso_639_1")=="zh"};val pool=chinese.ifEmpty{if(chineseOnly)emptyList()else rows.filter{it.optString("iso_639_1") in listOf("en","null","")}};return pool.sortedWith(compareByDescending<JSONObject>{it.optDouble("vote_average")}.thenByDescending{it.optInt("width")}).firstOrNull()?.optString("file_path")?.let{"https://image.tmdb.org/t/p/${if(type=="logos")"w500"else if(type=="posters")"w342"else"w1280"}${if(type=="logos"&&it.endsWith(".svg"))it.removeSuffix(".svg")+".png"else it}"}.orEmpty() }
 }
}
class PresentationQueue050(private val model:PtvModel){
 private val pending=mutableSetOf<String>();private val queue=Channel<Work>(256)
 init {repeat(if(model.config.budget.name=="轻量")2 else 4){model.viewModelScope.launch{for(w in queue){try{resolve(w)}catch(e:CancellationException){throw e}catch(_:Exception){}finally{synchronized(pending){pending.remove(w.ratingViewKey)}}}}}}
 fun request(w:Work){if(w.provider.startsWith("site_")||w.provider=="live")return;if(synchronized(pending){pending.add(w.ratingViewKey)}){if(queue.trySend(w).isFailure)synchronized(pending){pending.remove(w.ratingViewKey)}}}
 private fun signature(w:Work)="match102|${w.title}|${w.original}|${w.aliases.joinToString("|").hashCode()}|${w.year}|${w.season}|"+listOf(model.tmdbConfigured.toString(),"douban","tmdb").joinToString("|"){p->if(p in listOf("douban","tmdb"))model.db.prefs.getString(model.metadata.ratingStorageKey(w,p),"").orEmpty()else p}
 private fun table(){model.db.writableDatabase.execSQL("CREATE TABLE IF NOT EXISTS presentation_v5(k TEXT PRIMARY KEY,j TEXT NOT NULL,stamp INTEGER NOT NULL,signature TEXT NOT NULL)")}
 suspend fun invalidate(w:Work){withContext(Dispatchers.IO){table();model.db.writableDatabase.delete("presentation_v5","k=?",arrayOf(w.ratingViewKey))};model.presentations=model.presentations-w.ratingViewKey;request(w)}
 private suspend fun apply(w:Work,value:Presentation050){withContext(Dispatchers.Main){model.presentations=model.presentations+(w.ratingViewKey to value);model.linkedRatings=model.linkedRatings+(w.ratingViewKey to value.ratings)}}
 private suspend fun resolve(w:Work){
  val sig=signature(w);val cached=withContext(Dispatchers.IO){table();model.db.readableDatabase.rawQuery("SELECT j,stamp,signature FROM presentation_v5 WHERE k=?",arrayOf(w.ratingViewKey)).use{c->if(c.moveToFirst())Triple(Presentation050.from(JSONObject(c.getString(0))),c.getLong(1),c.getString(2)==sig)else null}}
  if(cached!=null){val retained=cached.first.copy(ratings=cached.first.ratings.mapValues{(p,v)->val manual=model.db.prefs.getString(model.metadata.ratingStorageKey(w,p),null);if(p=="tmdb"&&!model.tmdbConfigured||manual=="none"||manual!=null&&manual!=v?.id)null else v});apply(w,retained);withContext(Dispatchers.IO){CacheBudget101.touchPresentation(model.db.writableDatabase,w.ratingViewKey)};val hasDouban=retained.ratings["douban"]!=null||model.db.prefs.getString(model.metadata.ratingStorageKey(w,"douban"),null)=="none";if(cached.third&&hasDouban&&cached.first.ratings.values.filterNotNull().any{it.people.isNotEmpty()&&it.genres.isNotEmpty()})return}
  val ratings=(cached?.first?.ratings.orEmpty().filterValues{it!=null}).toMutableMap();var source=w;var poster=cached?.first?.poster.orEmpty();var backdrop=cached?.first?.backdrop.orEmpty();var logo=cached?.first?.logo.orEmpty()
  withTimeout(40000){
   suspend fun artwork(tm:Work?){
    if(tm==null||!model.tmdbConfigured)return
    try{val j=withContext(Dispatchers.IO){model.metadata.tmdb("${tm.kind}/${tm.id}/images",mapOf("include_image_language" to "zh,en,null"))};poster=Presentation050.image(j,"posters",true).ifBlank{poster};logo=Presentation050.image(j,"logos",false).ifBlank{logo};backdrop=Presentation050.image(j,"backdrops",false).ifBlank{backdrop};apply(w,Presentation050(poster,backdrop,logo,ratings))}catch(e:CancellationException){throw e}catch(_:Exception){}
   }
   if(model.tmdbConfigured){val tm=try{withContext(Dispatchers.IO){model.metadata.ratingWork(source,"tmdb")}}catch(e:CancellationException){throw e}catch(_:Exception){ratings["tmdb"]};if(tm!=null){ratings["tmdb"]=tm;source=source.enriched(tm);apply(w,Presentation050(poster,backdrop,logo,ratings));artwork(tm)}}
   for(p in listOf("douban")){
    val own=p==w.provider&&!model.db.prefs.contains(model.metadata.ratingStorageKey(w,p))
    val value=try{withContext(Dispatchers.IO){if(own){if(w.people.isEmpty()||w.overview.isBlank()||w.genres.isEmpty()||w.aliases.isEmpty()&&w.original.isBlank())runCatching{model.metadata.details(w)}.getOrDefault(w)else w}else model.metadata.ratingWork(source,p)}}catch(e:CancellationException){throw e}catch(_:Exception){ratings[p]}
    ratings[p]=value;if(value!=null)source=source.enriched(value)
   }
   val cacheSig=signature(w)
   if(ratings.any{(p,value)->val id=model.db.prefs.getString(model.metadata.ratingStorageKey(w,p),null);id!=null&&id!=(value?.id?:"none")}){queue.trySend(w);return@withTimeout}
   val tm=ratings["tmdb"];var finalPoster="";var finalBackdrop=""
   if(tm!=null&&model.tmdbConfigured){try{val j=withContext(Dispatchers.IO){model.metadata.tmdb("${tm.kind}/${tm.id}/images",mapOf("include_image_language" to "zh,en,null"))};finalPoster=Presentation050.image(j,"posters",true);logo=Presentation050.image(j,"logos",false).ifBlank{logo};finalBackdrop=Presentation050.image(j,"backdrops",false)}catch(e:CancellationException){throw e}catch(_:Exception){}}
   val value=Presentation050(finalPoster.ifBlank{poster},finalBackdrop.ifBlank{backdrop},logo,ratings)
   if(signature(w)!=cacheSig){queue.trySend(w);return@withTimeout}
   withContext(Dispatchers.IO){table();val values=android.content.ContentValues().apply{put("k",w.ratingViewKey);put("j",value.json().toString());put("stamp",System.currentTimeMillis());put("signature",cacheSig)};model.db.writableDatabase.insertWithOnConflict("presentation_v5",null,values,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);CacheBudget101.trimPresentation(model.db.writableDatabase)};apply(w,value)
  }
 }
}
@Composable fun presented050(m:PtvModel,w:Work):Pair<Work,Presentation050?> {
 LaunchedEffect(w.ratingViewKey,m.tmdbConfigured){m.artworkQueue.request(w)}
 val art=m.presentations[w.ratingViewKey]
 val chineseTitle=if(InfoChinese056.isLikelyChinese(w.title))w.title else listOfNotNull(art?.ratings?.get("douban")?.title,art?.ratings?.get("tmdb")?.title).firstOrNull{InfoChinese056.hasHan(it)&&(InfoChinese056.isLikelyChinese(it)||w.title.any{ch->ch in '\u3040'..'\u30ff'})}?:w.aliases.firstOrNull{InfoChinese056.isLikelyChinese(it)}?:w.title
 return w.copy(title=chineseTitle,poster=art?.poster?.ifBlank{w.poster}?:w.poster,backdrop=art?.backdrop?.ifBlank{w.backdrop}?:w.backdrop) to art
}
