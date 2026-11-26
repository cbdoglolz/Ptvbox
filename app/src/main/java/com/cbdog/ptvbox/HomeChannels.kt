package com.cbdog.ptvbox

import android.app.job.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.tvprovider.media.tv.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class HomeChannel(val key:String,val title:String,val provider:String,val kind:String,val category:String="热门")
object HomeCatalog {
    val channels=listOf(
        HomeChannel("continue-watching","继续观看","local","continue"),
        HomeChannel("douban-tv","热门剧集","douban","tv"),
        HomeChannel("douban-movie","热门电影","douban","movie"),
        HomeChannel("anime-hot","近期热门番剧","douban","tv","动画"),
        HomeChannel("netflix-tv","Netflix 精选","netflix","tv")
    )
    fun forCredentials(hasTmdbKey:Boolean)=channels.filter{it.provider!="tmdb"||hasTmdbKey}
    fun intent(context:Context,work:Work?=null,resume:Boolean=false,channel:String?=null):Intent {
        val uri=Uri.Builder().scheme("ptvbox").authority("home").apply{
            work?.let{appendQueryParameter("work",it.key)}
            if(resume)appendQueryParameter("resume","1")
            channel?.let{appendQueryParameter("channel",it)}
        }.build()
        return Intent(Intent.ACTION_VIEW,uri,context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    fun schedule(context:Context){context.getSystemService(JobScheduler::class.java).schedule(JobInfo.Builder(8120,ComponentName(context,HomeSyncJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(6*60*60*1000L).setPersisted(true).build())}
}

/** TvProvider owns the home screen. This class only changes rows belonging to this APK. */
class HomeChannels(private val context:Context,private val db:Library){
    private val resolver=context.contentResolver
    private val helper=PreviewChannelHelper(context)
    private val artwork=HomeArtwork(context)
    private val fallbackArt get()=Uri.parse("android.resource://${context.packageName}/${R.drawable.brand}")
    companion object { private val lock=Mutex() }
    fun available()=runCatching{resolver.acquireContentProviderClient(TvContractCompat.AUTHORITY)?.use{true}?:false}.getOrDefault(false)
    private fun ownIds(uri:Uri):Map<String,Long> = resolver.query(uri,arrayOf("_id","internal_provider_id","package_name"),null,null,null)?.use{c->buildMap{while(c.moveToNext())if(c.getString(2)==context.packageName)put(c.getString(1).orEmpty(),c.getLong(0))}}?:emptyMap()
    fun channelIds()=ownIds(TvContractCompat.Channels.CONTENT_URI)
    fun watchIds()=ownIds(TvContractCompat.WatchNextPrograms.CONTENT_URI)
    fun channelIntent(id:Long)=Intent(TvContractCompat.ACTION_REQUEST_CHANNEL_BROWSABLE).putExtra(TvContractCompat.EXTRA_CHANNEL_ID,id)
    private fun episodeStillUrl(w:Watch):String {
        if(w.work.kind=="tv"){
            val epNum=Identity.episode(w.episode)
            if(epNum!=null){
                val files=db.episodeCacheDir055.listFiles()?.filter{it.isFile&&it.name.endsWith(".json")}.orEmpty()
                for(f in files){
                    val hit=runCatching{
                        val j=org.json.JSONObject(f.readText())
                        val workKey=j.optString("workKey")
                        val matchesWork=workKey==w.work.key||workKey==w.work.legacyKey||j.optString("title")==w.work.title
                        if(!matchesWork)null else {
                            val eps=j.optJSONArray("episodes")
                            var found:String?=null
                            if(eps!=null)for(i in 0 until eps.length()){
                                val e=eps.optJSONObject(i)?:continue
                                if(e.optInt("season",w.work.season)==w.work.season&&e.optInt("number")==epNum){
                                    val still=e.optString("still").ifBlank{e.optString("still_path")}
                                    if(still.startsWith("https://")){found=still;break}
                                }
                            }
                            found
                        }
                    }.getOrNull()
                    if(!hit.isNullOrBlank())return hit
                }
            }
        }
        return w.work.backdrop.ifBlank{w.work.poster}
    }
    private suspend fun publishContinueChannel(existing:Map<String,Long>):String {
        val spec=HomeCatalog.channels.first{it.key=="continue-watching"}
        val activeWatches=db.watches().filter{!it.work.id.startsWith("qa-")&&!it.done&&it.duration>0&&(it.duration-it.position)>=10000}.distinctBy{it.work.key}.take(16)
        val id=existing[spec.key]?:helper.publishChannel(PreviewChannel.Builder().setDisplayName(spec.title).setDescription("Ptvbox 最近观看（16:9横向单集封面）").setInternalProviderId(spec.key).setAppLinkIntentUri(HomeCatalog.intent(context,channel=spec.key).toUri(Intent.URI_INTENT_SCHEME).let(Uri::parse)).setLogo(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources,R.drawable.brand),128,128,true)).build())
        check(id>=0){"频道未成功创建"}
        val old=ownIds(TvContractCompat.buildPreviewProgramsUriForChannel(id));val keep=mutableSetOf<String>()
        val artUris=coroutineScope{activeWatches.map{w->async{artwork.uriFromUrl(episodeStillUrl(w),landscape=true)?:artwork.uri(w.work)}}.awaitAll()}
        for((index,w) in activeWatches.withIndex()){
            val key="${spec.key}|${w.work.key}";keep.add(key)
            val subtitle=if(w.work.kind=="tv")"第${w.work.season}季 · ${w.episode} · ${clock(w.position)} / ${clock(w.duration)}" else "已看到 ${clock(w.position)} / ${clock(w.duration)}"
            val p=PreviewProgram.Builder().setChannelId(id).setInternalProviderId(key).setContentId(w.work.key).setType(if(w.work.kind=="movie")TvContractCompat.PreviewPrograms.TYPE_MOVIE else TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE).setTitle(w.work.title).setDescription(subtitle).setPosterArtUri(artUris[index]?:fallbackArt).setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9).setIntentUri(Uri.parse(HomeCatalog.intent(context,w.work,resume=true).toUri(Intent.URI_INTENT_SCHEME))).setDurationMillis(w.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).setLastPlaybackPositionMillis(w.position.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).setWeight(activeWatches.size-index).build()
            old[key]?.let{resolver.update(TvContractCompat.buildPreviewProgramUri(it),p.toContentValues(),null,null)}?:helper.publishPreviewProgram(p)
        }
        old.filterKeys{it !in keep}.values.forEach{resolver.delete(TvContractCompat.buildPreviewProgramUri(it),null,null)}
        return "${spec.title}：${activeWatches.size}部（16:9横版）"
    }
    suspend fun sync():List<String> = lock.withLock {
        withContext(Dispatchers.IO){
            if(!available())return@withContext listOf("当前桌面没有提供ATV频道接口")
            val notes=mutableListOf<String>();val existing=channelIds();val hasKey=db.secrets.read("tmdb").isNotBlank()
            val validKeys=HomeCatalog.channels.map{it.key}.toSet()
            existing.filterKeys{it !in validKeys}.values.forEach{runCatching{resolver.delete(TvContractCompat.buildChannelUri(it),null,null)}}
            for(spec in HomeCatalog.channels){
                if(spec.provider=="local"&&spec.kind=="continue"){runCatching{notes.add(publishContinueChannel(existing))}.onFailure{notes.add("${spec.title}：暂不可用")};continue}
                if(spec.provider=="tmdb"&&!hasKey){existing[spec.key]?.let{resolver.delete(TvContractCompat.buildChannelUri(it),null,null)};continue}
                try{
                    val rows=Metadata(db).feed(spec.provider,spec.kind,spec.category,1).take(20)
                    if(rows.isEmpty()){notes.add("${spec.title}：暂无资料，保留已有频道");continue}
                    val id=existing[spec.key]?:helper.publishChannel(PreviewChannel.Builder().setDisplayName(spec.title).setDescription("Ptvbox影视推荐").setInternalProviderId(spec.key).setAppLinkIntentUri(HomeCatalog.intent(context,channel=spec.key).toUri(Intent.URI_INTENT_SCHEME).let(Uri::parse)).setLogo(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.resources,R.drawable.brand),128,128,true)).build())
                    check(id>=0){"频道未成功创建"}
                    val old=ownIds(TvContractCompat.buildPreviewProgramsUriForChannel(id));val keep=mutableSetOf<String>()
                    val posters=coroutineScope{rows.map{w->async{artwork.uri(w)}}.awaitAll()}
                    for((index,w) in rows.withIndex()){
                        val key="${spec.key}|${w.key}";keep.add(key)
                        val p=PreviewProgram.Builder().setChannelId(id).setInternalProviderId(key).setContentId(w.key).setType(if(w.kind=="movie")TvContractCompat.PreviewPrograms.TYPE_MOVIE else TvContractCompat.PreviewPrograms.TYPE_TV_SERIES).setTitle(w.title).setDescription(w.overview.ifBlank{"${w.year} · ${w.provider.uppercase()} ${w.rating}"}).setPosterArtUri(posters[index]?:fallbackArt).setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_2_3).setIntentUri(Uri.parse(HomeCatalog.intent(context,w).toUri(Intent.URI_INTENT_SCHEME))).setWeight(rows.size-index).build()
                        old[key]?.let{resolver.update(TvContractCompat.buildPreviewProgramUri(it),p.toContentValues(),null,null)}?:helper.publishPreviewProgram(p)
                    }
                    old.filterKeys{it !in keep}.values.forEach{resolver.delete(TvContractCompat.buildPreviewProgramUri(it),null,null)}
                    notes.add("${spec.title}：${rows.size}部")
                }catch(e:CancellationException){throw e}catch(e:Exception){notes.add("${spec.title}：刷新暂不可用，保留原内容")}
            }
            runCatching{
                val used=mutableSetOf<String>()
                for(uri in listOf(TvContractCompat.PreviewPrograms.CONTENT_URI,TvContractCompat.WatchNextPrograms.CONTENT_URI))resolver.query(uri,arrayOf("poster_art_uri","package_name"),null,null,null)?.use{c->while(c.moveToNext())if(c.getString(1)==context.packageName){val art=Uri.parse(c.getString(0).orEmpty());if(art.authority=="${context.packageName}.homeart")art.lastPathSegment?.let(used::add)}}
                artwork.trim(used)
            }
            db.prefs.edit().putLong("home_sync",System.currentTimeMillis()).putInt("home_art_version",1).putString("home_sync_status",notes.joinToString("\n")).apply();notes
        }
    }
    fun resumeEngaged(work:Work){db.prefs.edit().remove("home_dismiss_${work.key}").remove("home_dismiss_${work.legacyKey}").apply()}
    fun publishWatch(w:Watch){
        if(w.work.id.startsWith("qa-"))return
        runCatching{
            val key="continue|${w.work.key}";val ids=watchIds();val legacy=ids["continue|${w.work.legacyKey}"].takeIf{db.resolve(w.work.legacyKey)?.key==w.work.key};val id=ids[key]?:legacy
            if(ids[key]!=null&&legacy!=null&&ids[key]!=legacy)resolver.delete(TvContractCompat.buildWatchNextProgramUri(legacy),null,null)
            if(w.done||w.duration<=0||(w.duration-w.position)<10000){id?.let{resolver.delete(TvContractCompat.buildWatchNextProgramUri(it),null,null)};return}
            if((db.prefs.getBoolean("home_dismiss_${w.work.key}",false)||db.prefs.getBoolean("home_dismiss_${w.work.legacyKey}",false)))return
            val poster=runBlocking{artwork.uriFromUrl(episodeStillUrl(w),landscape=true)?:artwork.uri(w.work)}?:fallbackArt
            val p=WatchNextProgram.Builder().setWatchNextType(TvContractCompat.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE).setType(if(w.work.kind=="movie")TvContractCompat.PreviewPrograms.TYPE_MOVIE else TvContractCompat.PreviewPrograms.TYPE_TV_EPISODE).setTitle(w.work.title).setDescription("${w.episode} · 已看到 ${clock(w.position)}").setInternalProviderId(key).setContentId(w.work.key).setPosterArtUri(poster).setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9).setIntentUri(Uri.parse(HomeCatalog.intent(context,w.work,true).toUri(Intent.URI_INTENT_SCHEME))).setDurationMillis(w.duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).setLastPlaybackPositionMillis(w.position.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).setLastEngagementTimeUtcMillis(w.updated).setSeasonNumber(w.work.season).apply{Identity.episode(w.episode)?.let{setEpisodeNumber(it)}}.build()
            id?.let{resolver.update(TvContractCompat.buildWatchNextProgramUri(it),p.toContentValues(),null,null)}?:helper.publishWatchNextProgram(p)
            runCatching{runBlocking{publishContinueChannel(channelIds())}}
        }.onFailure{db.prefs.edit().putString("home_watch_status","系统接下来播放暂不可用；本地进度已保存").apply()}
    }
    fun dismiss(id:Long){val row=watchIds().entries.firstOrNull{it.value==id}?:return;val workKey=row.key.removePrefix("continue|");db.prefs.edit().putBoolean("home_dismiss_$workKey",true).apply();resolver.delete(TvContractCompat.buildWatchNextProgramUri(id),null,null)}
}

class HomeProgramReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){if(intent.action==TvContractCompat.ACTION_WATCH_NEXT_PROGRAM_BROWSABLE_DISABLED){val id=intent.getLongExtra(TvContractCompat.EXTRA_WATCH_NEXT_PROGRAM_ID,-1);if(id>=0){val pending=goAsync();CoroutineScope(Dispatchers.IO).launch{val db=Library(context);try{HomeChannels(context,db).dismiss(id)}finally{db.close();pending.finish()}}}}}}
class HomeSyncJob:JobService(){private var job:Job?=null;override fun onStartJob(params:JobParameters):Boolean{job=CoroutineScope(Dispatchers.IO).launch{val db=Library(this@HomeSyncJob);try{HomeChannels(this@HomeSyncJob,db).sync()}finally{db.close();jobFinished(params,false)}};return true};override fun onStopJob(params:JobParameters):Boolean{job?.cancel();return true}}
