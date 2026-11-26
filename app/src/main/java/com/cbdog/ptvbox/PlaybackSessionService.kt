package com.cbdog.ptvbox

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.*

/** Process-wide player ownership. The service never retains an Activity or ViewModel. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object PlaybackOwner {
    private var value:ExoPlayer?=null
    private var template:Watch?=null
    private val writes=CoroutineScope(SupervisorJob()+Dispatchers.IO.limitedParallelism(1))
    @Volatile var saveError=""
    private var rendered=false
    var foreground=true
    var serviceRunning=false
    fun isCurrent(player:ExoPlayer)=value===player
    fun obtain(context:Context):ExoPlayer {
        value?.let{return it}
        val config=TvPreferences(context)
        val buffer=DefaultLoadControl.Builder().setBufferDurationsMs(10_000,30_000,1000,2000).setTargetBufferBytes(config.budget.bufferMb*1024*1024).setPrioritizeTimeOverSizeThresholds(false).build()
        return ExoPlayer.Builder(context.applicationContext,DefaultRenderersFactory(context).setEnableDecoderFallback(true)).setLoadControl(buffer).build().also{p->
            value=p;p.volume=config.prefs.getFloat("volume",.75f)
            p.addListener(object:Player.Listener{override fun onRenderedFirstFrame(){rendered=true}})
        }
    }
    fun source(work:Work,candidate:Candidate){rendered=false;template=Watch(work,candidate.episode.name,candidate.site.key,candidate.road,candidate.height,0,0,sourceId=candidate.sourceId)}
    fun save(context:Context){
        val p=value?:return;val old=template?:return
        if(old.work.provider=="live"||!rendered||p.currentPosition<1000||TvPreferences(context).incognito)return
        val snapshot=old.copy(position=p.currentPosition,duration=p.duration.coerceAtLeast(0),height=p.videoSize.height.takeIf{it>0}?:old.height,updated=System.currentTimeMillis())
        val app=context.applicationContext;writes.launch{val db=Library(app);try{db.save(snapshot)}catch(_:Exception){saveError="后台进度暂未保存，请检查可用存储空间"}finally{db.close()}}
    }
    fun clear(){template=null;rendered=false}
    fun release(player:ExoPlayer){if(value===player){value=null;template=null;rendered=false;player.release()}}
    fun startService(context:Context){context.startService(Intent(context,PlaybackSessionService::class.java))}
    fun stopService(context:Context){context.stopService(Intent(context,PlaybackSessionService::class.java))}
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlaybackSessionService:MediaSessionService(){
    private var session:MediaSession?=null
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    override fun onCreate(){
        super.onCreate();PlaybackOwner.serviceRunning=true
        val player=PlaybackOwner.obtain(this)
        val intent=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        session=MediaSession.Builder(this,player).setSessionActivity(intent).build();addSession(session!!)
        scope.launch{while(isActive){delay(15_000);if(!PlaybackOwner.foreground)PlaybackOwner.save(this@PlaybackSessionService)}}
    }
    override fun onGetSession(controllerInfo:MediaSession.ControllerInfo):MediaSession?=session
    override fun onTaskRemoved(rootIntent:Intent?){PlaybackOwner.save(this);pauseAllPlayersAndStopSelf()}
    override fun onDestroy(){
        scope.cancel();PlaybackOwner.save(this);val p=session?.player;session?.release();session=null;PlaybackOwner.serviceRunning=false
        if(!PlaybackOwner.foreground&&p is ExoPlayer)PlaybackOwner.release(p)
        super.onDestroy()
    }
}
