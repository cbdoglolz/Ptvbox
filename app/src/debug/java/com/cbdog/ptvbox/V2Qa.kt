package com.cbdog.ptvbox

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.io.File

/** Synthetic, debug-only migration tests. Never opens the owner's production library. */
object V2Qa {
    fun home(context:Context):JSONObject = kotlinx.coroutines.runBlocking {
        val db=Library(context);try{
            val home=HomeChannels(context,db);val notes=home.sync();val ids=home.channelIds();check(ids.keys.containsAll(HomeCatalog.forCredentials(false).map{it.key}))
            val work=Metadata(db).details(Work("1292052","肖申克的救赎",year="1994"));db.cache(work)
            val watch=Watch(work,"正片","qa-home-site","1080p",1080,123456,500000)
            home.resumeEngaged(work);home.publishWatch(watch);val currentId=home.watchIds()["continue|${work.key}"]?:error("Watch Next missing")
            context.contentResolver.update(androidx.tvprovider.media.tv.TvContractCompat.buildWatchNextProgramUri(currentId),android.content.ContentValues().apply{put("internal_provider_id","continue|${work.legacyKey}")},null,null)
            check(db.resolve(work.legacyKey)?.key==work.key);home.publishWatch(watch)
            check(home.watchIds()["continue|${work.key}"]==currentId);check(!home.watchIds().containsKey("continue|${work.legacyKey}"))
            home.publishWatch(watch.copy(done=true));check(!home.watchIds().containsKey("continue|${work.key}"))
            JSONObject().put("channelKeys",org.json.JSONArray(ids.keys)).put("channelCount",ids.size).put("legacyWork",work.legacyKey).put("resolvedWork",work.key).put("watchNextLegacyRowReused",true).put("watchNextCompletionRemoved",true).put("notes",org.json.JSONArray(notes))
        }finally{db.close()}
    }
    fun data(context:Context):JSONObject {
        val name="qa-v2-${System.nanoTime()}.db";val path=context.getDatabasePath(name).apply{parentFile!!.mkdirs()}
        val a=Work("77","合成电影",provider="tmdb");val b=Work("77","合成剧集","tv",provider="tmdb")
        val recovered=Work("88","仅在进度里的作品","tv",season=2)
        val watch=Watch(recovered,"第03集","synthetic-site","1080p",1080,1234567,7654321,10001)
        fun legacy(path:File,collision:Boolean=false){SQLiteDatabase.openOrCreateDatabase(path,null).use{db->
            db.execSQL("CREATE TABLE works(k TEXT PRIMARY KEY,j TEXT NOT NULL)");db.execSQL("CREATE TABLE favorites(k TEXT PRIMARY KEY)");db.execSQL("CREATE TABLE progress(k TEXT PRIMARY KEY,work TEXT NOT NULL,updated INTEGER NOT NULL,j TEXT NOT NULL)")
            listOf(a,b).forEachIndexed{i,w->db.execSQL("INSERT INTO works VALUES(?,?)",arrayOf("legacy-$i",w.json().toString()))}
            db.execSQL("INSERT INTO works VALUES('bad','not json')")
            db.execSQL("INSERT INTO favorites VALUES(?)",arrayOf(a.legacyKey));db.execSQL("INSERT INTO favorites VALUES(?)",arrayOf(recovered.legacyKey))
            db.execSQL("INSERT INTO progress VALUES(?,?,?,?)",arrayOf<Any>("legacy-episode",recovered.legacyKey,watch.updated,watch.json().toString()))
            db.execSQL("INSERT INTO progress VALUES('broken','bad',2,'{')")
            if(collision)db.execSQL("CREATE TABLE media_v2(k TEXT)")
            db.version=1
        }}
        legacy(path)
        var db=Library(context,name);check(db.readableDatabase.version==3)
        check(db.favorites().single().key==recovered.key);check(db.last(recovered)==watch)
        check(db.resolve(a.legacyKey)==null);check(db.resolve(recovered.legacyKey)?.key==recovered.key)
        val id=db.mediaId(recovered);check(db.resolve(id)?.key==recovered.key)
        check(db.mediaId(a)!=db.mediaId(b))
        check(db.readableDatabase.rawQuery("SELECT COUNT(*) FROM works",null).use{it.moveToFirst();it.getInt(0)}==3)
        check(db.readableDatabase.rawQuery("SELECT COUNT(*) FROM migration_issues",null).use{it.moveToFirst();it.getInt(0)}==3)
        check(File(context.filesDir,"migration-backups/$name.v1.json").exists())
        val epId=db.readableDatabase.rawQuery("SELECT episode_id FROM episodes_v2",null).use{it.moveToFirst();it.getString(0)}
        val sync=db.syncSnapshot().getJSONArray("progress").getJSONObject(0);check(sync.getString("position_seconds")=="1234.567");check(ProgressUnits.milliseconds(sync.getString("duration_seconds"))==watch.duration)
        db.close();db=Library(context,name);check(db.mediaId(recovered)==id);check(db.watches().size==1)
        db.save(watch.copy(episode="S02E03",updated=10002));check(db.watches().size==1)
        check(db.readableDatabase.rawQuery("SELECT episode_id FROM episodes_v2",null).use{it.moveToFirst();it.getString(0)}==epId)
        db.toggleFavorite(recovered);db.setState(recovered,WatchState.COMPLETED);check(!db.favorite(recovered));check(db.state(recovered)==WatchState.COMPLETED)
        db.setState(recovered,WatchState.WATCHING);db.hideContinue(recovered);check(db.snapshot().continues.isEmpty());check(db.watches().size==1);db.engage(recovered);check(db.snapshot().continues.size==1)
        val site=Site("stable","合成源","https://example.test/api");db.saveCatalog(recovered,listOf(CatalogGroup("group",site,"目录",Road("中文",listOf(Episode("第03集","https://example.test/private?token=DO_NOT_STORE"))))))
        check(db.loadCatalog(recovered,listOf(site)).single().road.episodes.single().url.isEmpty())
        val otherSeason=recovered.copy(season=1,title="其他季",year="2001")
        check(db.loadCatalog(otherSeason,listOf(site)).isEmpty())
        db.saveCatalog(otherSeason,listOf(CatalogGroup("other",site,"其他季",Road("中文",listOf(Episode("第01集",""))))))
        check(db.loadCatalog(recovered,listOf(site)).single().road.episodes.single().name=="第03集")
        check(db.loadCatalog(otherSeason,listOf(site)).single().road.episodes.single().name=="第01集")
        check(!db.readableDatabase.rawQuery("SELECT j FROM catalog_v2",null).use{it.moveToFirst();it.getString(0)}.contains("DO_NOT_STORE"));db.close()
        db=Library(context,name);db.save(watch.copy(episode="特别篇",sourceId="site-A|vod-1|road"));db.save(watch.copy(episode="特别篇",sourceId="site-B|vod-9|road"));check(db.watches().count{it.episode=="特别篇"}==2);db.close()
        db=Library(context,name);db.markEpisode(recovered,3,favorite=true,watched=true);db.close();db=Library(context,name);check(db.snapshot().episodeMarks[episodeRef(recovered,3)]==EpisodeMark(true,true));check(db.snapshot().episodeMarks[episodeRef(recovered.copy(season=1),3)]==null);check(db.favoriteEpisodes().single().second==3);db.cache(recovered.copy(season=1,title="Other season changed",year="1900"));check(db.favoriteEpisodes().single().first.title==recovered.title);check(db.favoriteEpisodes().single().first.season==recovered.season);db.close()
        val failureName="qa-rollback-${System.nanoTime()}.db";val failurePath=context.getDatabasePath(failureName);legacy(failurePath,true)
        val failing=Library(context,failureName);check(runCatching{failing.writableDatabase}.isFailure);failing.close()
        SQLiteDatabase.openDatabase(failurePath.path,null,SQLiteDatabase.OPEN_READONLY).use{raw->check(raw.version==1);check(raw.rawQuery("SELECT COUNT(*) FROM progress",null).use{it.moveToFirst();it.getInt(0)}==2);check(raw.rawQuery("SELECT name FROM sqlite_master WHERE name='favorites_v2'",null).use{!it.moveToFirst()})}
        return JSONObject().put("migration",true).put("reopenWithoutDuplicates",true).put("uuidStable",true).put("decimalSecondsExact",true).put("ambiguousAliasNotMerged",true).put("malformedRecordsRetained",true).put("privateBackup",true).put("failedUpgradeRolledBack",true).put("catalogHasNoPlaybackSecrets",true).put("favoriteStateIndependent",true).put("continueHideKeepsHistory",true).put("unknownEpisodesKeepSourceIdentity",true).put("episodeMarksPersistAndSeparateSeasons",true).put("catalogSeasonsIsolated",true).put("episodeFavoriteRetainsWorkSnapshot",true)
    }
}
