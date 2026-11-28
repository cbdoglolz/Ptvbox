package com.cbdog.ptvbox

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class Library(private val appContext:Context,name:String="ptvbox-library.db"):SQLiteOpenHelper(appContext,name,null,3) {
    internal fun assetText103(name:String)=appContext.assets.open(name).bufferedReader().use{it.readText()}
    val episodeCacheDir055 get()=java.io.File(appContext.cacheDir,"episode-series055").apply{mkdirs()}
    val detailCacheDir get()=java.io.File(appContext.cacheDir,"detail-metadata060").apply{mkdirs()}
    val userMediaDir get()=java.io.File(appContext.filesDir,"user-media").apply{mkdirs()}
    val prefs=appContext.getSharedPreferences("preferences",Context.MODE_PRIVATE)
    val secrets=SecretStore(appContext)
    val changes=kotlinx.coroutines.flow.MutableStateFlow(0L)
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE works(k TEXT PRIMARY KEY,j TEXT NOT NULL)")
        db.execSQL("CREATE TABLE favorites(k TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE progress(k TEXT PRIMARY KEY,work TEXT NOT NULL,updated INTEGER NOT NULL,j TEXT NOT NULL)")
        schema(db)
        episodeSchema(db)
    }
    private fun schema(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE media_v2(k TEXT PRIMARY KEY,media_id TEXT UNIQUE NOT NULL,j TEXT NOT NULL)")
        db.execSQL("CREATE TABLE aliases_v2(legacy TEXT NOT NULL,k TEXT NOT NULL,PRIMARY KEY(legacy,k))")
        db.execSQL("CREATE TABLE favorites_v2(k TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE state_v2(k TEXT PRIMARY KEY,state TEXT NOT NULL)")
        db.execSQL("CREATE TABLE episodes_v2(k TEXT PRIMARY KEY,episode_id TEXT UNIQUE NOT NULL,media_key TEXT NOT NULL,season INTEGER NOT NULL,name TEXT NOT NULL)")
        db.execSQL("CREATE INDEX episodes_media ON episodes_v2(media_key)")
        db.execSQL("CREATE TABLE progress_v2(k TEXT PRIMARY KEY,work TEXT NOT NULL,updated INTEGER NOT NULL,position_ms INTEGER NOT NULL,duration_ms INTEGER NOT NULL,j TEXT NOT NULL)")
        db.execSQL("CREATE INDEX progress_work ON progress_v2(work,updated DESC)")
        db.execSQL("CREATE TABLE hidden_v2(k TEXT PRIMARY KEY)")
        db.execSQL("CREATE TABLE catalog_v2(work TEXT PRIMARY KEY,j TEXT NOT NULL)")
        db.execSQL("CREATE TABLE episode_sources_v2(work TEXT NOT NULL,group_key TEXT NOT NULL,name TEXT NOT NULL,episode_key TEXT NOT NULL,PRIMARY KEY(work,group_key,name))")
        db.execSQL("CREATE TABLE changes_v2(revision INTEGER PRIMARY KEY AUTOINCREMENT,device_id TEXT NOT NULL,session_id TEXT NOT NULL,kind TEXT NOT NULL,entity TEXT NOT NULL,updated INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE migration_issues(table_name TEXT NOT NULL,legacy_key TEXT NOT NULL,reason TEXT NOT NULL,PRIMARY KEY(table_name,legacy_key))")
    }
    override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int){if(old<2){
        // SQLiteOpenHelper wraps this in a transaction. Original tables are retained unchanged.
        val backup=JSONObject()
        for(table in listOf("works","favorites","progress"))db.rawQuery("SELECT * FROM $table",null).use{c->val a=JSONArray();while(c.moveToNext()){val row=JSONObject();for(i in 0 until c.columnCount)row.put(c.getColumnName(i),c.getString(i));a.put(row)};backup.put(table,a)}
        val dir=java.io.File(appContext.filesDir,"migration-backups").apply{mkdirs()}
        val target=java.io.File(dir,"${databaseName}.v1.json")
        if(!target.exists()){val tmp=java.io.File.createTempFile("v1-",".tmp",dir);try{tmp.writeText(backup.toString());check(tmp.renameTo(target)){"Private migration backup failed"}}finally{tmp.delete()}}
        schema(db)
        db.rawQuery("SELECT k,j FROM works",null).use{c->while(c.moveToNext())try{putWork(db,Work.from(JSONObject(c.getString(1))))}catch(e:Exception){issue(db,"works",c.getString(0))}}
        // Progress embeds the work as well: recover its aliases before migrating favorites.
        db.rawQuery("SELECT k,j FROM progress ORDER BY updated",null).use{c->while(c.moveToNext())try{putWatch(db,Watch.from(JSONObject(c.getString(1))),false)}catch(e:Exception){issue(db,"progress",c.getString(0))}}
        db.rawQuery("SELECT k FROM favorites",null).use{c->while(c.moveToNext()){val oldKey=c.getString(0);val keys=aliasKeys(db,oldKey);if(keys.size==1)db.execSQL("INSERT OR IGNORE INTO favorites_v2(k) VALUES(?)",arrayOf(keys.single()))else issue(db,"favorites",oldKey)}}
    };if(old<3)episodeSchema(db)}
    private fun episodeSchema(db:SQLiteDatabase){db.execSQL("CREATE TABLE episode_flags_v3(k TEXT PRIMARY KEY,favorite INTEGER NOT NULL,watched INTEGER NOT NULL)")}
    @Synchronized fun markEpisode(work:Work,number:Int,favorite:Boolean?=null,watched:Boolean?=null,state:WatchState?=null,clearState:Boolean=false)=transaction{db->val key=episodeRef(work,number);val prior=episodeFlags(db)[key]?:EpisodeMark();val next=if(clearState)null else state?:watched?.let{if(it)WatchState.COMPLETED else null}?:prior.watchState;val chosen=if(watched==false&&state==null)null else next;db.execSQL("INSERT OR REPLACE INTO episode_flags_v3 VALUES(?,?,?)",arrayOf<Any>(key,if(favorite?:prior.favorite)1 else 0,if(chosen==WatchState.COMPLETED)1 else 0));val edit=prefs.edit().putString("episode_mark_work_$key",work.json().toString());if(chosen==null)edit.remove("episode_state_$key")else edit.putString("episode_state_$key",chosen.name);edit.commit();record(db,"episode_mark",key)}
    private fun episodeFlags(db:SQLiteDatabase)=db.rawQuery("SELECT k,favorite,watched FROM episode_flags_v3",null).use{c->buildMap{while(c.moveToNext()){val key=c.getString(0);val watched=c.getInt(2)!=0;val state=runCatching{WatchState.valueOf(prefs.getString("episode_state_$key","").orEmpty())}.getOrNull()?:if(watched)WatchState.COMPLETED else null;put(key,EpisodeMark(c.getInt(1)!=0,watched,state))}}}
    private fun issue(db:SQLiteDatabase,table:String,key:String){db.execSQL("INSERT OR REPLACE INTO migration_issues VALUES(?,?,?)",arrayOf(table,key,"Unresolved legacy record retained in original table"))}
    private fun aliasKeys(db:SQLiteDatabase,key:String)=db.rawQuery("SELECT k FROM aliases_v2 WHERE legacy=?",arrayOf(key)).use{c->buildList{while(c.moveToNext())add(c.getString(0))}}
    private fun putWork(db:SQLiteDatabase,w:Work){
        val prior=db.rawQuery("SELECT media_id,j FROM media_v2 WHERE k=?",arrayOf(w.key)).use{c->if(c.moveToFirst())c.getString(0) to Work.from(JSONObject(c.getString(1)))else null}
        val merged=prior?.second?.let{p->w.copy(overview=w.overview.ifBlank{p.overview},poster=w.poster.ifBlank{p.poster},original=w.original.ifBlank{p.original},aliases=(w.aliases+p.aliases).distinct(),backdrop=w.backdrop.ifBlank{p.backdrop},people=w.people.ifEmpty{p.people})}?:w
        db.insertWithOnConflict("media_v2",null,ContentValues().apply{put("k",w.key);put("media_id",prior?.first?:java.util.UUID.randomUUID().toString());put("j",merged.json().toString())},SQLiteDatabase.CONFLICT_REPLACE)
        db.execSQL("INSERT OR IGNORE INTO aliases_v2 VALUES(?,?)",arrayOf(w.legacyKey,w.key))
    }
    private fun episodeKey(w:Work,name:String,source:String="legacy")=w.key+"@"+w.season+"@"+(if(w.kind=="movie")"movie"else Identity.episode(name)?.let{"regular:$it"}?:"source:$source:${Identity.normalize(name)}")
    private fun putWatch(db:SQLiteDatabase,w:Watch,track:Boolean){
        putWork(db,w.work);val key=episodeKey(w.work,w.episode,w.sourceId.ifBlank{"legacy:${w.siteKey}|${w.road}"})
        db.execSQL("INSERT OR IGNORE INTO episodes_v2 VALUES(?,?,?,?,?)",arrayOf<Any>(key,java.util.UUID.randomUUID().toString(),w.work.key,w.work.season,w.episode))
        db.insertWithOnConflict("progress_v2",null,ContentValues().apply{put("k",key);put("work",w.work.key);put("updated",w.updated);put("position_ms",w.position);put("duration_ms",w.duration);put("j",w.json().toString())},SQLiteDatabase.CONFLICT_REPLACE)
        if(w.done&&w.work.kind=="movie")db.execSQL("INSERT OR REPLACE INTO state_v2 VALUES(?,?)",arrayOf(w.work.key,"COMPLETED"))
        else if(w.position>0)db.execSQL("INSERT OR IGNORE INTO state_v2 VALUES(?,?)",arrayOf(w.work.key,"WATCHING"))
        if(track)record(db,"progress",key)
    }
    private fun record(db:SQLiteDatabase,kind:String,key:String){val device=prefs.getString("device_id",null)?:java.util.UUID.randomUUID().toString().also{prefs.edit().putString("device_id",it).apply()};db.execSQL("INSERT INTO changes_v2(device_id,session_id,kind,entity,updated) VALUES(?,?,?,?,?)",arrayOf<Any>(device,sessionId,kind,key,System.currentTimeMillis()))}
    private val sessionId=java.util.UUID.randomUUID().toString()
    private inline fun transaction(action:(SQLiteDatabase)->Unit){val db=writableDatabase;db.beginTransaction();try{action(db);db.setTransactionSuccessful()}finally{db.endTransaction()};changes.value=changes.value+1}
    @Synchronized fun cache(w:Work){putWork(writableDatabase,w)}
    fun markedWorks()=readableDatabase.rawQuery("SELECT m.j FROM media_v2 m INNER JOIN state_v2 s ON s.k=m.k ORDER BY m.rowid DESC",null).use{c->buildList{while(c.moveToNext())add(Work.from(JSONObject(c.getString(0))))}}
    fun cached()=readableDatabase.rawQuery("SELECT j FROM media_v2",null).use{c->buildList{while(c.moveToNext())add(Work.from(JSONObject(c.getString(0))))}}
    fun resolve(key:String):Work?{val db=readableDatabase;val keys=aliasKeys(db,key);val resolved=if(keys.size==1)keys.single()else key;return db.rawQuery("SELECT j FROM media_v2 WHERE k=? OR media_id=?",arrayOf(resolved,resolved)).use{if(it.moveToFirst())Work.from(JSONObject(it.getString(0)))else null}}
    fun mediaId(work:Work):String{cache(work);return readableDatabase.rawQuery("SELECT media_id FROM media_v2 WHERE k=?",arrayOf(work.key)).use{it.moveToFirst();it.getString(0)}}
    fun favorite(work:Work)=readableDatabase.rawQuery("SELECT k FROM favorites_v2 WHERE k=?",arrayOf(work.key)).use{it.moveToFirst()}
    @Synchronized fun toggleFavorite(work:Work)=transaction{db->putWork(db,work);if(favorite(work))db.delete("favorites_v2","k=?",arrayOf(work.key))else db.execSQL("INSERT INTO favorites_v2 VALUES(?)",arrayOf(work.key));record(db,"favorite",work.key)}
    fun favorites()=readableDatabase.rawQuery("SELECT m.j FROM media_v2 m JOIN favorites_v2 f ON m.k=f.k",null).use{c->buildList{while(c.moveToNext())add(Work.from(JSONObject(c.getString(0))))}}
    fun favoriteEpisodes():List<Pair<Work,Int>> = episodeFlags(readableDatabase).filterValues{it.favorite}.keys.mapNotNull{k->val number=k.substringAfterLast("@regular:").toIntOrNull();val prefix=k.substringBeforeLast("@regular:");val season=prefix.substringAfterLast('@').toIntOrNull();val work=runCatching{Work.from(JSONObject(prefs.getString("episode_mark_work_$k",null)?:""))}.getOrNull()?:resolve(prefix.substringBeforeLast('@'));if(number!=null&&season!=null&&work!=null)work.copy(season=season) to number else null}
    @Synchronized fun save(w:Watch)=transaction{putWatch(it,w,true)}
    fun watches()=readableDatabase.rawQuery("SELECT j FROM progress_v2 ORDER BY updated DESC",null).use{c->buildList{while(c.moveToNext())add(Watch.from(JSONObject(c.getString(0))))}}
    fun last(work:Work)=readableDatabase.rawQuery("SELECT j FROM progress_v2 WHERE work=? ORDER BY updated DESC",arrayOf(work.key)).use{c->var found:Watch?=null;while(c.moveToNext()&&found==null){val w=Watch.from(JSONObject(c.getString(0)));if(w.work.season==work.season)found=w};found}
    fun progress(work:Work,episode:String,source:String="legacy")=readableDatabase.rawQuery("SELECT j FROM progress_v2 WHERE k=?",arrayOf(episodeKey(work,episode,source))).use{if(it.moveToFirst())Watch.from(JSONObject(it.getString(0)))else null}
    fun state(work:Work)=readableDatabase.rawQuery("SELECT state FROM state_v2 WHERE k=?",arrayOf(work.key)).use{if(it.moveToFirst())WatchState.valueOf(it.getString(0))else null}
    @Synchronized fun saveCatalog(work:Work,groups:List<CatalogGroup>){val db=writableDatabase;db.beginTransaction();try{
        putWork(db,work);val rows=JSONArray()
        groups.forEach{g->val names=g.road.episodes.map{it.name};rows.put(JSONObject().put("key",g.key).put("site",g.site.key).put("title",g.title).put("road",g.road.name).put("names",JSONArray(names)))
            names.forEach{name->val key=episodeKey(work,name,g.key);db.execSQL("INSERT OR IGNORE INTO episodes_v2 VALUES(?,?,?,?,?)",arrayOf<Any>(key,java.util.UUID.randomUUID().toString(),work.key,work.season,name));db.execSQL("INSERT OR REPLACE INTO episode_sources_v2 VALUES(?,?,?,?)",arrayOf(work.key,g.key,name,key))}}
        db.execSQL("INSERT OR REPLACE INTO catalog_v2 VALUES(?,?)",arrayOf(work.catalogKey(),rows.toString()));db.setTransactionSuccessful()
    }finally{db.endTransaction()}}
    fun loadCatalog(work:Work,sites:List<Site>):List<CatalogGroup> = readableDatabase.rawQuery("SELECT j FROM catalog_v2 WHERE work IN (?,?) ORDER BY CASE WHEN work=? THEN 0 ELSE 1 END LIMIT 1",arrayOf(work.catalogKey(),if(work.season==1)work.key else work.catalogKey(),work.catalogKey())).use{c->if(!c.moveToFirst())emptyList()else JSONArray(c.getString(0)).objects().mapNotNull{j->sites.firstOrNull{it.key==j.optString("site")}?.let{site->CatalogGroup(j.getString("key"),site,j.optString("title"),Road(j.optString("road"),j.arrayStrings("names").map{Episode(it,"")}))}}}
    @Synchronized fun setState(work:Work,state:WatchState)=transaction{db->putWork(db,work);db.execSQL("INSERT OR REPLACE INTO state_v2 VALUES(?,?)",arrayOf(work.key,state.name));record(db,"state",work.key)}
    @Synchronized fun clearState(work:Work)=transaction{db->db.delete("state_v2","k=?",arrayOf(work.key));record(db,"state_cleared",work.key)}
    @Synchronized fun hideContinue(work:Work)=transaction{db->db.execSQL("INSERT OR REPLACE INTO hidden_v2 VALUES(?)",arrayOf(work.key));record(db,"continue_hidden",work.key)}
    @Synchronized fun engage(work:Work)=transaction{db->db.delete("hidden_v2","k=?",arrayOf(work.key));db.execSQL("INSERT OR REPLACE INTO state_v2 VALUES(?,?)",arrayOf(work.key,"WATCHING"));record(db,"engage",work.key)}
    @Synchronized fun snapshot():UserSnapshot {val db=readableDatabase;val states=db.rawQuery("SELECT k,state FROM state_v2",null).use{c->buildMap{while(c.moveToNext())put(c.getString(0),WatchState.valueOf(c.getString(1)))}};val hidden=db.rawQuery("SELECT k FROM hidden_v2",null).use{c->buildSet{while(c.moveToNext())add(c.getString(0))}};return UserSnapshot(favorites().map{it.key}.toSet(),watches(),states,hidden,episodeFlags(db))}
    /** Local contract for a future transport; decimal seconds preserve every legacy millisecond. */
    @Synchronized fun syncSnapshot():JSONObject {
        val db=readableDatabase;val snapshot=snapshot();val works=JSONArray()
        db.rawQuery("SELECT k,media_id FROM media_v2",null).use{c->while(c.moveToNext()){val key=c.getString(0);works.put(JSONObject().put("media_id",c.getString(1)).put("external_key",key).put("favorite",key in snapshot.favorites).put("watch_state",snapshot.states[key]?.name?:JSONObject.NULL).put("continue_hidden",key in snapshot.hidden))}}
        val progress=JSONArray();db.rawQuery("SELECT e.episode_id,m.media_id,p.position_ms,p.duration_ms,p.updated FROM progress_v2 p JOIN episodes_v2 e ON p.k=e.k JOIN media_v2 m ON p.work=m.k",null).use{c->while(c.moveToNext())progress.put(JSONObject().put("episode_id",c.getString(0)).put("media_id",c.getString(1)).put("position_seconds",ProgressUnits.seconds(c.getLong(2))).put("duration_seconds",ProgressUnits.seconds(c.getLong(3))).put("updated_ms",c.getLong(4)))}
        val revision=db.rawQuery("SELECT COALESCE(MAX(revision),0) FROM changes_v2",null).use{it.moveToFirst();it.getLong(0)}
        val device=prefs.getString("device_id",null)?:java.util.UUID.randomUUID().toString().also{prefs.edit().putString("device_id",it).apply()}
        return JSONObject().put("schema",2).put("device_id",device).put("session_id",sessionId).put("revision",revision).put("works",works).put("progress",progress)
    }
    @Synchronized fun forget(work:Work)=transaction{it.delete("progress_v2","work=?",arrayOf(work.key));record(it,"history_deleted",work.key)}
    @Synchronized fun clearHistory()=transaction{it.delete("progress_v2",null,null);record(it,"history_cleared","all")}
    fun historySearch():List<String> =runCatching{JSONArray(prefs.getString("search_history","[]")).let{a->(0 until a.length()).map{a.getString(it)}}}.getOrDefault(emptyList())
    fun rememberSearch(value:String){prefs.edit().putString("search_history",JSONArray((listOf(value.trim())+historySearch()).filter{it.isNotEmpty()}.distinct().take(10)).toString()).apply()}
    fun removeSearch(value:String){prefs.edit().putString("search_history",JSONArray(historySearch().filter{it!=value}).toString()).apply()}
    fun clearSearch(){prefs.edit().remove("search_history").apply()}
}

class SecretStore(context:Context) {
    companion object { private val keyLock=Any() }
    private val prefs=context.getSharedPreferences("encrypted_credentials",Context.MODE_PRIVATE)
    fun names():Set<String> = prefs.all.keys.toSet()
    private fun key():SecretKey = synchronized(keyLock) {val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)};(store.getKey("ptvbox.credentials",null) as? SecretKey)?:KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply{init(KeyGenParameterSpec.Builder("ptvbox.credentials",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())}.generateKey()}
    fun save(name:String,value:String){val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());val encrypted=cipher.iv+cipher.doFinal(value.toByteArray());prefs.edit().putString(name,Base64.encodeToString(encrypted,Base64.NO_WRAP)).apply()}
    fun read(name:String):String=runCatching{val data=Base64.decode(prefs.getString(name,null)?:return "",Base64.NO_WRAP);val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,data.copyOfRange(0,12)));String(cipher.doFinal(data.copyOfRange(12,data.size)))}.getOrDefault("")
}
