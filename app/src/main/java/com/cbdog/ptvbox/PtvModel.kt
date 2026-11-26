package com.cbdog.ptvbox

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.DefaultHttpDataSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@androidx.annotation.OptIn(UnstableApi::class)
class PtvModel(app:Application):AndroidViewModel(app) {
    val config=TvPreferences(app)
    var preferencesRevision by mutableIntStateOf(0)
    internal var seasonCorrectionRequest by mutableIntStateOf(0)
    var configurationValues052 by mutableStateOf<Map<String,String>>(emptyMap())
    var savedCredentials by mutableStateOf<Set<String>>(emptySet())
    var settingsSection by mutableStateOf(config.text("settings_section","remote"))
    var playbackFailures052 by mutableStateOf<List<String>>(emptyList())
    var health by mutableStateOf<Map<String,SiteHealth040>>(emptyMap())
    var stationChecked by mutableIntStateOf(0);var stationTotal by mutableIntStateOf(0);var stationChecking by mutableStateOf(false)
    var stationCheck:Job?=null
    var sourcePreview by mutableStateOf<SubscriptionPreview?>(null);var sourceUpdateText by mutableStateOf("");var sourceUpdating by mutableStateOf(false)
    val detailTrail=mutableListOf<Work>()

    val db=Library(app);val sites=Sites(app,db);val metadata=Metadata(db)
    val searchAssist055 by lazy{SearchAssist055(db,metadata)};var searchRevision055 by mutableIntStateOf(0);private var queryJob055:Job?=null;private var queryGeneration055=0
    val liveBrowseCache050=mutableMapOf<String,LivePage050>()
    var liveEntry by mutableStateOf<LiveEntry050?>(null)
    var presentations by mutableStateOf<Map<String,Presentation050>>(emptyMap())
    val artworkQueue=PresentationQueue050(this)
    var markedWorks by mutableStateOf<List<Work>>(emptyList())
    val users=UserRepository(db)
    val details=DetailViewModel(metadata,EpisodeCatalogRepository(sites,db))
    val home=HomeChannels(app,db)
    var linkedRatings by mutableStateOf<Map<String,Map<String,Work?>>>(emptyMap())
    var user by mutableStateOf(UserSnapshot())
    var favoriteWorks by mutableStateOf<List<Work>>(emptyList())
    var favoriteEpisodes by mutableStateOf<List<Pair<Work,Int>>>(emptyList())
    var searchHistory by mutableStateOf<List<String>>(emptyList())
    var availableSites by mutableStateOf<List<Site>>(emptyList())
    var enabledSiteKeys by mutableStateOf<Set<String>>(emptySet())
    var siteScoresRevision by mutableIntStateOf(0)
    fun siteScore(site:Site):Float{siteScoresRevision;return SiteScores102.score(db,site.key)}
    fun resetSiteScores(){SiteScores102.resetAll(db,availableSites.map{it.key});siteScoresRevision++;message("已清零所有站点计分")}
    private fun recordSiteScore(site:Site,delta:Float){val w=current?:return;if(SiteScores102.recordOnce(db,w.key,w.season,currentEpisode,site.key,delta))siteScoresRevision++}
    var tmdbConfigured by mutableStateOf(false)
    var keyStatus by mutableStateOf(db.prefs.getString("tmdb_status","").orEmpty())
    var remoteUrl by mutableStateOf("");private set
    var remoteFingerprint by mutableStateOf("");private set
    private var configServer:LocalConfigServer?=null
    private val backupPortal054 by lazy{BackupPortal054(db)}
    private var configExpiry:Job?=null
    var dashboard by mutableStateOf<Map<String,List<Work>>>(emptyMap())
    private var dashboardJob:Job?=null
    private val dashboardCache=mutableMapOf<String,Map<String,List<Work>>>()
    var homeStatus by mutableStateOf(db.prefs.getString("home_sync_status","").orEmpty())
    var syncingHome by mutableStateOf(false)
    private var lastHomeWrite=0L
    private fun createPlayer()=PlaybackOwner.obtain(getApplication<Application>())
    var player=createPlayer();private set
    private var playerReleased=false
    var volume by mutableFloatStateOf(db.prefs.getFloat("volume",.75f))
    var trackRevision by mutableIntStateOf(0)
    internal var playbackCapability060 by mutableStateOf(PlaybackCapability060(display=PlaybackCapabilities060.display(app)))
    var lightTheme by mutableStateOf(db.prefs.getBoolean("light_theme",false))
    var pipMode by mutableStateOf(false)
    fun toggleTheme(){lightTheme=!lightTheme;db.prefs.edit().putBoolean("light_theme",lightTheme).apply()}
    var restoreFocus by mutableStateOf("")
    var lastRestoredControl by mutableStateOf("")
    var includeUpcoming by mutableStateOf(db.prefs.getBoolean("include_upcoming",false))
    fun toggleUpcoming(){includeUpcoming=!includeUpcoming;db.prefs.edit().putBoolean("include_upcoming",includeUpcoming).apply();loadFeed();dashboardCache.clear();loadDashboard()}
    var screen by mutableStateOf("home");var origin="home";var provider by mutableStateOf("douban");var kind by mutableStateOf("tv");var category by mutableStateOf("热门")
    var feedHasMore by mutableStateOf(true)
    var playbackReturnScreen="detail"
    var playbackReturnWork:Work?=null
    val roadEpisodes051=mutableMapOf<String,List<Episode>>()
    var muted051 by mutableStateOf(db.prefs.getBoolean("player_muted",false))
    private var unmutedVolume051=db.prefs.getFloat("unmuted_volume",.75f)
    private var seekTarget051=0L
    var feed by mutableStateOf<List<Work>>(emptyList());var searchResults by mutableStateOf<List<Work>>(emptyList());var query by mutableStateOf("");var detail by mutableStateOf<Work?>(null)
    var current by mutableStateOf<Work?>(null);var currentCandidate by mutableStateOf<Candidate?>(null);var candidates by mutableStateOf<List<Candidate>>(emptyList());var siteStates by mutableStateOf<List<SiteStatus>>(emptyList())
    var currentEpisode by mutableStateOf("正片");var episodeNames by mutableStateOf<List<String>>(emptyList());var checked by mutableIntStateOf(0);var total by mutableIntStateOf(0);var searching by mutableStateOf(false);var pausedScan by mutableStateOf(false)
    var notice by mutableStateOf("");var busy by mutableStateOf(false);var error by mutableStateOf("");var playing by mutableStateOf(false);var position by mutableLongStateOf(0);var duration by mutableLongStateOf(0)
    var resizeChoice by mutableIntStateOf(db.prefs.getInt("resize_choice",0).coerceIn(0,2))
    fun cycleResize(){resizeChoice=(resizeChoice+1)%3;db.prefs.edit().putInt("resize_choice",resizeChoice).apply();message("画面 "+listOf("FIT","FILL","ZOOM")[resizeChoice])}
    var preference by mutableStateOf(db.prefs.getString("source_preference","speed")?:"speed");var focusRow="grid";var focusWork="";var streamLoading by mutableStateOf(false)
    val addons by lazy{PlaybackAddons053(this,viewModelScope)}
    var liveRouteErrors054 by mutableStateOf<Map<String,String>>(emptyMap())
    private val liveRetry054=LiveRetry054();private var liveRecovery054:Job?=null
    private fun recoverLive054(){if(current?.provider!="live"||!player.playWhenReady||liveRecovery054?.isActive==true)return;val entry=liveEntry?:return;if(!PlaybackOwner.foreground&&!config.background)return;val pause=liveRetry054.next(android.os.SystemClock.elapsedRealtime());if(pause==null){streamLoading=false;message("直播线路连续中断，请刷新频道或切换其他线路");return};liveRecovery054=viewModelScope.launch{streamLoading=true;message("直播连接中断，正在重连 ${liveRetry054.attempts}/3");delay(pause);if(current?.key==entry.work.key)playLive050(entry,recovering=true)}}

    private var authRequest054="";private var authWork054:Work?=null;private var authEpisode054="";private var authWasPaused054=false
    fun authorizeCloud054(site:Site,road:Road?=null,episode:Episode?=null,work:Work?=current?:detail){val w=work?:return;authWasPaused054=pausedScan;pausedScan=true;authWork054=w;authEpisode054=episode?.name?:currentEpisode;val request=java.util.UUID.randomUUID().toString();authRequest054=request
        viewModelScope.launch{try{withContext(Dispatchers.IO){sites.prepareAuth054(site)};if(authRequest054!=request)return@launch;getApplication<Application>().startActivity(android.content.Intent(getApplication<Application>(),PluginAuthActivity054::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("site",site.json().toString()).putExtra("work",w.json().toString()).putExtra("episode",authEpisode054).putExtra("road",road?.name).putExtra("url",episode?.url).putExtra("request",request))}catch(_:Exception){cloudClosed054(request);message("网盘授权入口暂未打开，请重试")}}
    }
    fun cloudClosed054(request:String){if(request!=authRequest054)return;authRequest054="";pausedScan=authWasPaused054}
    fun cloudResult054(request:String,key:String,json:String){if(request!=authRequest054||authWork054?.key!=key)return;val w=authWork054?:return;val ep=authEpisode054;cloudClosed054(request);runCatching{val j=org.json.JSONObject(json);val s=Site.from(j.getJSONObject("site"));val c=Candidate(s,j.getString("road"),Episode(j.getString("episodeName"),j.getString("url")),j.optInt("height"),j.optLong("bytes"),j.optLong("delay"),mimeType=j.optString("mime"));db.prefs.edit().putLong("cloud_authenticated_${s.key}",System.currentTimeMillis()).apply();val start=if(current?.key==w.key&&Identity.sameEpisode(currentEpisode,ep))player.currentPosition else 0L;if(current?.key!=w.key){saveProgress();playbackReturnScreen=screen;playbackReturnWork=detail;current=w;detail=w;currentEpisode=ep;oldWatch=null};candidates=(candidates+c).distinctBy{it.identity};select(c,start,false);message("网盘授权及播放地址已就绪")}.onFailure{message("网盘授权结果无效，请重试")}}
    private var feedPage=0;private var feedJob:Job?=null;private var sourceJob:Job?=null;private var startupJob:Job?=null;private var generation=0;private var qualityJob:Job?=null;private var initialStart=false;private var rendered=false;private var automaticSelection=false;private val tried=mutableSetOf<String>();private var resume=0L;private var oldWatch:Watch?=null
    private val feedCache=mutableMapOf<String,Pair<List<Work>,Int>>()
    private fun feedKey()="$provider|$kind|$category|$includeUpcoming"
    init {
        attachPlayer()

        viewModelScope.launch{while(isActive){if(!playerReleased){position=player.currentPosition.coerceAtLeast(0);duration=player.duration.coerceAtLeast(0);if(playing&&PlaybackOwner.foreground){saveProgress();applySkip()}};delay(3000)}}
        viewModelScope.launch{users.errors.collect{message(it)}}
        viewModelScope.launch{users.loadError.collect{if(it.isNotBlank())message(it)}}
        viewModelScope.launch{users.state.collect{user=it;val favorites=withContext(Dispatchers.IO){db.favorites() to db.favoriteEpisodes()};favoriteWorks=favorites.first;favoriteEpisodes=favorites.second;markedWorks=withContext(Dispatchers.IO){db.markedWorks()}}}
        viewModelScope.launch{details.series.collect{s->val w=current;if(s!=null&&w!=null&&w.kind=="tv"&&w.provider!="live"&&details.work.value?.key==w.key&&details.work.value?.season==w.season){episodeNames=Episodes051.names(w.kind,currentEpisode,currentCandidate,candidates,roadEpisodes051,seriesFallbackEpisodes(w))}}}
        viewModelScope.launch(Dispatchers.IO){DecodeSupport052.main10}
        viewModelScope.launch{refreshConfiguration()}
        loadFeed()
        loadDashboard()
        if(!BuildConfig.DEBUG||!db.prefs.getBoolean("qa_home_disabled",false)){
            runCatching{HomeCatalog.schedule(app)}
            if(db.prefs.getInt("home_art_version",0)<1||System.currentTimeMillis()-db.prefs.getLong("home_sync",0)>6*60*60*1000L)syncHome()
        }
    }
    private fun attachPlayer(){player.volume=db.prefs.getFloat("volume",.75f);player.addListener(object:Player.Listener{
            override fun onIsPlayingChanged(isPlaying:Boolean){playing=isPlaying}
            override fun onRenderedFirstFrame(){val actual=player.videoSize.height;val expected=oldWatch?.height?:0;if(!ResumePolicy040.decodedAccepts(expected,actual,automaticSelection)){val failed=currentCandidate;if(failed!=null)candidates=candidates.map{if(it.identity==failed.identity)it.copy(height=actual)else it};PlaybackOwner.clear();failCandidate("该线路实际${actual}P低于上次${expected}P，继续查找；可手动选择替代");return};rendered=true;streamLoading=false;startupJob?.cancel()}
            override fun onPlaybackStateChanged(state:Int){streamLoading=state==Player.STATE_BUFFERING;if(state==Player.STATE_ENDED){if(current?.provider=="live")recoverLive054()else{saveProgress(true);message("本集播放完毕，可选择下一集")}}}
            override fun onPlayerError(e:PlaybackException){if(current?.provider=="live"){recoverLive054();return};val now=android.os.SystemClock.elapsedRealtime();val causes=generateSequence<Throwable>(e){it.cause}.take(10).toList();val status=causes.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.responseCode;if(now-lastSeekAt057<5000L&&seekRecoveryCount057<1&&status !in listOf(401,403,404,410)){seekRecoveryCount057++;val target=seekTarget051.coerceAtLeast(0);player.seekTo(target);player.prepare();player.play();return};streamLoading=false;failCandidate("线路播放失败：${if(status!=null)"HTTP $status · "else""}${e.errorCodeName}")}
            override fun onVolumeChanged(volume:Float){this@PtvModel.volume=volume;db.prefs.edit().putFloat("volume",volume).apply()}
            override fun onTracksChanged(tracks:Tracks){trackRevision++;playbackCapability060=PlaybackCapabilities060.inspect(tracks,getApplication());restoreAudio();applyAutoSubtitle(tracks)}
        })
    }
    private fun ensurePlayer(){if(playerReleased||!PlaybackOwner.isCurrent(player)){runCatching{player.stop();player.release()};player=createPlayer();playerReleased=false;attachPlayer()}}
    private val utilityTrail=mutableListOf<String>()
    fun utility(target:String){if(screen!=target){utilityTrail+=screen;screen=target}}
    fun onForeground(){PlaybackOwner.foreground=true;if(PlaybackOwner.saveError.isNotBlank()){message(PlaybackOwner.saveError);PlaybackOwner.saveError=""};if(!PlaybackOwner.isCurrent(player)){val previous=current;playerReleased=true;current=null;playing=false;streamLoading=false;if(screen=="player"&&previous!=null){detail=previous;screen="detail"}}}
    fun settingChanged(key:String,value:Any){when(value){is Boolean->db.prefs.edit().putBoolean(key,value).apply();is Int->db.prefs.edit().putInt(key,value).apply();is String->db.prefs.edit().putString(key,value).apply()};if(key.startsWith("subtitle_")&&key!="subtitle_style_override")db.prefs.edit().putBoolean("subtitle_style_override",true).apply();preferencesRevision++;if(key=="light_theme"){lightTheme=value as Boolean};if(key=="background_playback"){if(value==true&&current!=null)PlaybackOwner.startService(getApplication<Application>())else if(value==false)PlaybackOwner.stopService(getApplication<Application>())}}
    fun applySkip(){if(current?.provider=="live")return;if(!config.flag("auto_skip"))return;val head=config.number("skip_intro",0)*1000L;val tail=config.number("skip_outro",0)*1000L;if(head>0&&position<head&&duration>head)player.seekTo(head);if(tail>0&&duration>tail+head&&position>=duration-tail){val at=episodeNames.indexOfFirst{Identity.sameEpisode(it,currentEpisode)};if(at>=0&&at+1<episodeNames.size)stepEpisode(1)else{player.seekTo(duration);player.pause()}}}
    fun saveCredential(name:String,value:String){if(name=="tmdb"){saveTmdb(value);return};require(name in SettingsInventory.serviceSecrets+listOf("proxy"));viewModelScope.launch{try{withContext(Dispatchers.IO){require(value.length<=8192);db.secrets.save(name,value.trim())};if(name=="proxy")NetworkPolicy.apply(getApplication<Application>());refreshConfiguration();preferencesRevision++;message("已保存并明文显示")}catch(_:Exception){message("凭据未保存，请检查格式或系统加密状态")}}}
    fun clearCredential(name:String){viewModelScope.launch{withContext(Dispatchers.IO){db.secrets.save(name,"");if(name=="tmdb")db.secrets.save("tmdb_pending","")};if(name=="proxy")NetworkPolicy.apply(getApplication<Application>());configurationChanged();preferencesRevision++;message("已删除该项配置")}}
    fun applyNetwork(proxy:String,ua:String){viewModelScope.launch{try{withContext(Dispatchers.IO){NetworkPolicy.validateProxy(proxy);require(ua.isNotBlank()&&ua.length<=512&&!ua.contains('\n')&&!ua.contains('\r'));db.secrets.save("proxy",proxy);db.prefs.edit().putString("user_agent",ua).commit();NetworkPolicy.apply(getApplication<Application>())};configurationValues052=withContext(Dispatchers.IO){ConfigPortal052.localValues(db)};preferencesRevision++;message("网络设置已应用；当前媒体保持，新的请求使用新设置")}catch(_:Exception){message("网络设置无效，检查代理格式和UA")}}}
    fun syncHome(){if(syncingHome)return;viewModelScope.launch{syncingHome=true;try{homeStatus=home.sync().joinToString("\n")}catch(e:Exception){homeStatus="系统首页频道暂不可用，稍后可重试"}finally{syncingHome=false}}}
    private suspend fun refreshConfiguration(){val config=withContext(Dispatchers.IO){Triple(sites.all().map{it to sites.enabled(it)},db.historySearch(),db.secrets.read("tmdb").isNotBlank())};availableSites=config.first.map{it.first};enabledSiteKeys=config.first.filter{it.second}.map{it.first.key}.toSet();searchHistory=config.second;tmdbConfigured=config.third;configurationValues052=withContext(Dispatchers.IO){ConfigPortal052.localValues(db)};savedCredentials=withContext(Dispatchers.IO){(SettingsInventory.serviceSecrets+"tmdb").filter{db.secrets.read(it).isNotBlank()||it=="tmdb"&&db.secrets.read("tmdb_pending").isNotBlank()}.toSet()}}
    private suspend fun storeTmdb(value:String):String=withContext(Dispatchers.IO){
        val text=try{if(metadata.validateCredential(value.trim())){db.secrets.save("tmdb",value.trim());db.secrets.save("tmdb_pending","");"TMDB密钥验证成功，已启用"}else"TMDB验证未通过，未覆盖现有密钥"}catch(e:Exception){if(e is IllegalArgumentException||e.message?.contains("HTTP 401")==true||e.message?.contains("HTTP 403")==true)"TMDB凭据格式或认证错误，未覆盖现有密钥"else{db.secrets.save("tmdb_pending",value.trim());"网络暂不可用，密钥已加密保存待验证，尚未启用"}}
        if(db.secrets.read("tmdb")==value.trim()||db.secrets.read("tmdb_pending")==value.trim())ConfigHistory.remember(db,"tmdb",value.trim())
        db.prefs.edit().putString("tmdb_status",text).apply();text
    }
    internal suspend fun configurationChanged(){refreshConfiguration();keyStatus=db.prefs.getString("tmdb_status","").orEmpty();detail?.takeIf{screen=="detail"}?.let(details::load);dashboardCache.clear();loadDashboard();if(provider=="tmdb")loadFeed();if(current!=null)refreshSources()}
    fun saveTmdb(value:String){viewModelScope.launch{keyStatus="正在验证…";val result=storeTmdb(value);configurationChanged();message(result);if(tmdbConfigured)syncHome()}}
    fun testTmdb(value:String){viewModelScope.launch{
        keyStatus="正在验证…"
        val result=withContext(Dispatchers.IO){
            runCatching{metadata.validateCredential(value.trim())}.fold(
                {if(it)"TMDB测试通过，尚未改变已保存密钥" else "TMDB测试未通过"},
                {"TMDB测试未完成：${it.message?.replace('\n',' ')?.take(80)?:"网络或认证不可用"}"}
            )
        }
        db.prefs.edit().putString("tmdb_status",result).apply();keyStatus=result;preferencesRevision++;message(result)
    }}
    fun deleteTmdbHistory(value:String){viewModelScope.launch{withContext(Dispatchers.IO){ConfigHistory.remove(db,"tmdb",value)};preferencesRevision++;message("已删除历史TMDB Key")}}
    fun clearStoredConfiguration(name:String){require(name in listOf("subscription","live_subscription"));viewModelScope.launch{withContext(Dispatchers.IO){db.secrets.save(name,"")};configurationChanged();message(if(name=="subscription")"已删除点播订阅地址，私人站点将移除" else "已删除直播订阅地址")}}
    fun retryTmdb(){viewModelScope.launch{val value=withContext(Dispatchers.IO){db.secrets.read("tmdb_pending").ifBlank{db.secrets.read("tmdb")}};if(value.isNotBlank())saveTmdb(value)else message("尚未保存密钥")}}
    fun importSources(value:String){viewModelScope.launch{try{val count=withContext(Dispatchers.IO){sites.import(value.trim())};configurationChanged();message("已导入${count}个私人站点；Jar需首次初始化，JS规则单独显示状态")}catch(e:Exception){message("导入失败：${e.message?.let{Regex("""HTTP \d{3}""").find(it)?.value}?:"无法读取订阅或解析配置"}；原配置保留")}}}
    fun startRemoteConfig(host:String=""){if(configServer!=null)return;viewModelScope.launch{
        var server:LocalConfigServer?=null
        try{server=LocalConfigServer(getApplication<Application>(),status={ConfigPortal052.snapshot(db,config.budget).put("tmdbSaved",db.secrets.read("tmdb").isNotBlank()).put("tmdbHistory",org.json.JSONArray(ConfigHistory.items(db,"tmdb"))).put("subscriptionSaved",db.secrets.read("source_config").isNotBlank()).put("savedServices",org.json.JSONArray(SettingsInventory.serviceSecrets.filter{db.secrets.read(it).isNotBlank()})).put("title",current?.title.orEmpty()).put("playing",playing)},apply={j->
            val results=mutableListOf<String>();val key=j.optString("tmdb").trim();val subscription=j.optString("subscription").trim()
            if(key.isNotBlank())results+=storeTmdb(key)
            if(subscription.isNotBlank())try{val count=withContext(Dispatchers.IO){sites.import(subscription)};results+="订阅已导入${count}站"}catch(e:Exception){results+="订阅导入失败，原订阅保留"}
            val credentialInput=j.optJSONObject("credentials")
            if(credentialInput!=null)withContext(Dispatchers.IO){for(name in SettingsInventory.serviceSecrets){val value=credentialInput.optString(name);if(value.isNotBlank()){if(value.length<=8192){db.secrets.save(name,value.trim());results+="服务凭据已加密保存"}else results+="有一项凭据过长，原值保留"}}}
            val inputs=j.optJSONObject("fields")
            if(inputs!=null){for(f in ConfigPortal052.fields){if(!inputs.has(f.id))continue;val value=inputs.optString(f.id).trim();if(value.isBlank())continue
                try{ConfigPortal052.validate(f,value);when(f.id){
                    "tmdb"->results+=storeTmdb(value)
                    "subscription"->{val count=withContext(Dispatchers.IO){sites.import(value)};results+="点播订阅已导入${count}站"}
                    "live_subscription"->withContext(Dispatchers.IO){db.secrets.save(f.id,value);results+="直播地址已保存"}
                    else->if(f.id in ConfigPortal052.secretStorage)withContext(Dispatchers.IO){db.secrets.save(f.id,value);results+="${f.title}已保存"}else withContext(Dispatchers.Main){settingChanged(f.id,when(f.kind){"bool"->value.toBooleanStrict();"int"->value.toInt();else->value});if(f.id=="source_preference")preference=value;if(f.id=="resize_choice")resizeChoice=value.toInt();results+="${f.title}已保存"}
                }}catch(e:CancellationException){throw e}catch(_:Exception){results+="${f.title}保存失败，原值保留"}
            };withContext(Dispatchers.IO){NetworkPolicy.apply(getApplication<Application>())}}
            val deletes=j.optJSONArray("deletes")?.objects()?.map{it.toString()}.orEmpty()
            for(id in deletes){when(id){
                "tmdb"->{withContext(Dispatchers.IO){db.secrets.save("tmdb","");db.secrets.save("tmdb_pending","")};results+="TMDB Key已删除"}
                "subscription"->{withContext(Dispatchers.IO){db.secrets.save("subscription","")};results+="点播订阅地址已删除"}
                "live_subscription"->{withContext(Dispatchers.IO){db.secrets.save("live_subscription","")};results+="直播订阅地址已删除"}
                else->{val field=ConfigPortal052.fields.firstOrNull{it.id==id};if(id in ConfigPortal052.secretStorage){withContext(Dispatchers.IO){db.secrets.save(id,"")};results+="${field?.title?:id}已删除"}}
            }}
            withContext(Dispatchers.Main){
                val settings=j.optJSONObject("settings")
                if(settings!=null){val profile=settings.optString("device_profile");if(profile in listOf("auto","economy","balanced","performance"))settingChanged("device_profile",profile);val parallel=settings.optInt("search_parallel");if(parallel in 1..20)settingChanged("search_parallel",parallel)}
                when(j.optString("command")){"toggle"->if(current!=null&&PlaybackOwner.isCurrent(player)){if(player.playWhenReady)player.pause()else player.play();results+="播放状态已切换"}else results+="没有可控制的播放会话";"stop"->{stop();results+="播放已停止"};"search"->{val query=j.optString("query").take(200);if(query.isNotBlank()){search(query);results+="电视已搜索该片名"}};"test_tmdb"->{val candidate=j.optString("testKey").trim();if(candidate.isNotBlank()){val valid=withContext(Dispatchers.IO){runCatching{metadata.validateCredential(candidate)}.getOrDefault(false)};results+=if(valid)"TMDB测试通过，尚未保存" else "TMDB测试未通过"}};"delete_tmdb_history"->{val old=j.optString("historyKey");if(old.isNotBlank()){withContext(Dispatchers.IO){ConfigHistory.remove(db,"tmdb",old)};results+="历史TMDB Key已删除"}}}
                configurationChanged();message("手机配置：${results.joinToString("；")}")
            }
            org.json.JSONObject().put("message",results.ifEmpty{listOf("未填写新内容，现有配置保持不变")}.joinToString("；"))
        },backup={j->val r=backupPortal054.apply(j);if(j.optString("action")=="import")withContext(Dispatchers.Main){configurationChanged()};r});val url=withContext(Dispatchers.IO){server.start(host)};configServer=server;remoteUrl=url;remoteFingerprint=server.fingerprint;configExpiry=viewModelScope.launch{delay(600_000);stopRemoteConfig()}
        }catch(e:Exception){server?.close();message("配置会话启动失败：请检查网络及本机IPv4地址")}
    }}
    fun stopRemoteConfig(){backupPortal054.clear();configExpiry?.cancel();configServer?.close();configServer=null;remoteUrl="";remoteFingerprint=""}
    fun siteEnabled(site:Site)=site.key in enabledSiteKeys
    fun toggleSite(site:Site){sites.toggle(site);enabledSiteKeys=if(sites.enabled(site))enabledSiteKeys+site.key else enabledSiteKeys-site.key}
    fun testSite(site:Site)=checkSites(listOf(site))
    fun saveLiveConfiguration(value:String){viewModelScope.launch{try{withContext(Dispatchers.IO){val uri=java.net.URI(value.trim());require(uri.scheme in listOf("http","https")&&uri.host!=null);db.secrets.save("live_subscription",value.trim())};configurationValues052=withContext(Dispatchers.IO){ConfigPortal052.localValues(db)};preferencesRevision++;message("直播地址已加密保存，可到直播与体育读取频道")}catch(_:Exception){message("直播地址格式无效，原配置保留")}}}
    fun legacyTestSite(site:Site){if(!site.searchable){message("${site.name}：订阅标为不参与点播搜索，配置已保留");return};viewModelScope.launch{val ok=withContext(Dispatchers.IO){runCatching{sites.search(site,"测试")}.isSuccess};message(if(ok)"${site.name} 接口可响应，尚不代表可播放"else"${site.name} 接口不可用或需插件")}}
    fun loadDashboard(){dashboardJob?.cancel();val selected=provider;dashboard=dashboardCache[selected].orEmpty();dashboardJob=viewModelScope.launch{val cache=withContext(Dispatchers.IO){db.cached().filter{selected!="netflix"&&it.provider==selected}};if(dashboard.isEmpty())dashboard=mapOf("热门推荐" to cache.take(20));listOf(Triple("最新剧集","tv","最新"),Triple("最新电影","movie","最新"),Triple("热门推荐","tv","热门"),Triple("专题推荐 · 动漫","tv","动画")).map{(title,k,c)->launch{try{val rows=withContext(Dispatchers.IO){metadata.feed(selected,k,c,1,includeUpcoming)};if(selected==provider){dashboard=dashboard+(title to rows);dashboardCache[selected]=dashboard}}catch(e:CancellationException){throw e}catch(e:Exception){/* Retain the last useful row. */}}}.joinAll()}}
    fun handleHomeLink(uri:android.net.Uri){if(uri.scheme!="ptvbox"||uri.host!="home")return;val key=uri.getQueryParameter("work");if(key!=null)viewModelScope.launch{val work=withContext(Dispatchers.IO){db.resolve(key)};if(work!=null){origin="home";if(uri.getQueryParameter("resume")=="1")play(work)else open(work)}else message("该资料需重新查找，请回首页刷新")}
        else uri.getQueryParameter("channel")?.let{id->HomeCatalog.channels.firstOrNull{it.key==id}?.let{changeFeed(it.provider,it.kind,it.category);screen="library"}}}
    fun message(value:String){
        if(NoticePolicy057.suppressPluginNoise(value,searching,streamLoading,currentCandidate!=null))return
        notice=value;viewModelScope.launch{delay(4000);if(notice==value)notice=""}
    }
    fun loadFeed(more:Boolean=false){if(more&&busy)return;feedJob?.cancel();feedJob=viewModelScope.launch{busy=true;error="";try{val page=if(more)feedPage+1 else 1;val rows=withContext(Dispatchers.IO){metadata.feed(provider,kind,category,page,includeUpcoming)};feedHasMore=rows.any{row->!more||feed.none{it.key==row.key}};feed=(if(more)feed+rows else rows).distinctBy{it.key};feedPage=page;if(rows.isEmpty())message("当前没有更多条目")}catch(e:CancellationException){throw e}catch(e:Exception){if(feed.isEmpty())feed=withContext(Dispatchers.IO){db.cached().filter{it.kind==kind}};error="资料暂不可用，已有缓存保留；可以使用搜索或按资源站浏览"}finally{busy=false}}}
    fun changeFeed(p:String=provider,k:String=kind,c:String=category){feedJob?.cancel();if(feed.isNotEmpty())feedCache[feedKey()]=feed to feedPage;val target=if(screen=="library")"library"else"home";val effectiveCategory=if(p=="netflix"&&c=="TOP250")"热门"else c;provider=p;kind=k;category=effectiveCategory;screen=target;feedHasMore=true;val cached=feedCache[feedKey()];feed=cached?.first?:emptyList();feedPage=cached?.second?:0;busy=false;if(cached==null)loadFeed();loadDashboard()}
    var searchFilterKind056 by mutableStateOf("")
    var searchFilterGenre056 by mutableStateOf("")
    var searchFilterPage056 by mutableIntStateOf(0)
    var searchFilterMore056 by mutableStateOf(false)
    fun filterSearch056(kind:String,label:String){
        val normalized=DetailFact056.normalize(label);if(normalized.isBlank())return
        queryJob055?.cancel();queryGeneration055++;val token=queryGeneration055
        searchFilterKind056=kind;searchFilterGenre056=normalized;searchFilterPage056=0;searchFilterMore056=true;query=normalized;screen="search";searchRevision055++;busy=true
        val normKey=Identity.normalize(normalized)
        val cached=runCatching{db.cached()}.getOrDefault(emptyList()).filter{w->
            (w.kind==kind||kind.isBlank())&&(w.year==normalized||w.genres.any{Identity.normalize(DetailFact056.display(it))==normKey}||w.facts.countries.any{Identity.normalize(DetailFact056.display(it))==normKey})
        }.take(20)
        searchResults=cached
        queryJob055=viewModelScope.launch{
            try{
                val remote=withContext(Dispatchers.IO){
                    val primary=runCatching{metadata.feed("douban",kind,"筛选:$normalized",1,includeUpcoming)}.getOrDefault(emptyList())
                    if(primary.isNotEmpty())primary else{
                        val altKind=if(kind=="movie")"tv"else"movie"
                        runCatching{metadata.feed("douban",altKind,"筛选:$normalized",1,includeUpcoming)}.getOrDefault(emptyList())
                    }
                }
                if(token!=queryGeneration055)return@launch
                searchFilterPage056=1
                searchFilterMore056=remote.size>=18
                searchResults=(remote+cached).distinctBy{it.key}.take(20)
                if(searchResults.isEmpty())message("未找到「$normalized」相关分类作品，可尝试输入片名或切换搜索源")
            }catch(e:CancellationException){throw e}catch(_:Exception){
                if(token==queryGeneration055&&searchResults.isEmpty())message("分类加载暂不可用，请稍后重试")
            }finally{if(token==queryGeneration055)busy=false}
        }
    }
    fun loadMoreFilterSearch056(){
        if(busy||!searchFilterMore056||searchFilterGenre056.isBlank())return
        val token=queryGeneration055;val nextPage=searchFilterPage056+1;val kind=searchFilterKind056.ifBlank{"tv"};val normalized=searchFilterGenre056
        busy=true
        queryJob055=viewModelScope.launch{
            try{
                val remote=withContext(Dispatchers.IO){runCatching{metadata.feed("douban",kind,"筛选:$normalized",nextPage,includeUpcoming)}.getOrDefault(emptyList())}
                if(token!=queryGeneration055)return@launch
                searchFilterPage056=nextPage
                val added=remote.filter{r->searchResults.none{it.key==r.key}}
                searchFilterMore056=added.isNotEmpty()&&remote.size>=18
                searchResults=(searchResults+added).distinctBy{it.key}
            }catch(e:CancellationException){throw e}catch(_:Exception){searchFilterMore056=false}finally{if(token==queryGeneration055)busy=false}
        }
    }
    @Deprecated("Details now open the cross-filter search page.") fun browseDetailFact056(kind:String,label:String)=filterSearch056(kind,label)
    var creditPage055 by mutableStateOf<CreditPage055?>(null)
    private val browseTrail055=mutableListOf<BrowseReturn055>();private var creditJob055:Job?=null
    private fun rememberBrowse055(){browseTrail055+=BrowseReturn055(screen,details.work.value?.takeIf{it.key==detail?.key}?:detail,creditPage055,focusRow,focusWork)}
    fun openCredit055(w:Work,person:PersonCredit?=null,company:CompanyCredit?=null){rememberBrowse055();details.cancelLoad();creditPage055=CreditPage055(w,person,company);screen="credits";loadCredit055()}
    fun loadCredit055(refresh:Boolean=false){val target=creditPage055?:return;if(target.loading&&!refresh||!target.more&&!refresh)return;creditJob055?.cancel();if(refresh){target.page=0;target.more=true};val page=target.page+1;target.loading=true;target.error="";creditJob055=viewModelScope.launch{try{val rows=withContext(Dispatchers.IO){if(target.person!=null)metadata.personWorks(target.person,target.person.provider.ifBlank{target.owner.provider},page)else if(target.company!!.role=="播出平台")metadata.companyWorks(target.company,"tv",page)else coroutineScope{listOf("movie","tv").map{k->async{metadata.companyWorks(target.company,k,page)}}.awaitAll().flatten()}};if(creditPage055!==target)return@launch;target.more=if(target.person!=null)target.person.provider=="douban"&&rows.size>=20 else rows.any{r->target.rows.none{it.key==r.key}};target.page=page;target.rows=(if(refresh)rows else target.rows+rows).distinctBy{it.key}.sortedByDescending{it.releaseDate.ifBlank{it.year}}}catch(e:CancellationException){throw e}catch(_:Exception){target.error="作品暂未加载，请检查网络、TMDB Key或人物关联；原作品保留"}finally{target.loading=false}}}
    private fun backBrowse055():Boolean {val prior=browseTrail055.removeLastOrNull()?:return false;creditJob055?.cancel();screen=prior.screen;detail=prior.work;creditPage055=prior.credit;focusRow=prior.row;focusWork=prior.focus;restoreFocus="${prior.row}|${prior.focus}";if(screen=="detail")prior.work?.let(details::load);return true}
    fun open(work:Work){if(screen !in listOf("detail","credits","player")){browseTrail055.clear();creditJob055?.cancel();creditPage055=null};if(work.provider=="live"){restoreLive050(work,false);return};if(screen=="credits"||screen=="detail"&&detail?.key!=work.key)rememberBrowse055();focusWork=work.key;if(screen!="detail"&&screen!="player"&&screen!="credits")origin=screen;detail=work;screen="detail";users.write{cache(work)};details.load(work)}
    fun enterSearch055(){queryJob055?.cancel();queryGeneration055++;query="";searchResults=emptyList();searchRevision055=0;screen="search"}
    private fun beginQuery055(value:String):Int{queryJob055?.cancel();queryGeneration055++;searchRevision055++;query=value;screen="search";searchResults=emptyList();busy=true;if(!config.incognito){users.write{rememberSearch(value)};searchHistory=(listOf(value)+searchHistory).distinct().take(10)};return queryGeneration055}
    fun search(value:String=query){val text=value.trim().take(120);if(text.isBlank())return;val token=beginQuery055(text);queryJob055=viewModelScope.launch{try{val direct=withContext(Dispatchers.IO){metadata.search(text)};if(token!=queryGeneration055)return@launch;searchResults=direct;if(direct.isEmpty()&&SearchText055.initialQuery(text)){val names=searchAssist055.suggest(text,emptyMap()).names.take(6);val gate=Semaphore(3);names.map{name->launch{gate.withPermit{try{val rows=withContext(Dispatchers.IO){withTimeout(12000){metadata.search(name)}};if(token==queryGeneration055)searchResults=(searchResults+rows).distinctBy{it.key}}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e}catch(_:Exception){}}}}.joinAll()};if(searchResults.isEmpty()&&token==queryGeneration055)message("未找到作品，可尝试完整片名、英文原名或资源站搜索")}catch(e:CancellationException){throw e}catch(_:Exception){if(token==queryGeneration055)message("搜索暂不可用，可尝试其他搜索源")}finally{if(token==queryGeneration055)busy=false}}}
    fun searchSites(value:String,siteKey:String?=null){val text=value.trim().take(120);if(text.isBlank())return;val token=beginQuery055(text);queryJob055=viewModelScope.launch{val gate=Semaphore(config.parallel);try{withContext(Dispatchers.IO){sites.ordered().filter{sites.enabled(it)&&it.searchable&&!LiveParser050.isPortal(it)&&(siteKey==null||it.key==siteKey)}}.map{site->launch{gate.withPermit{try{val hits=withContext(Dispatchers.IO){withTimeout(18000){sites.search(site,text)}};if(token!=queryGeneration055||!sites.enabled(site))return@withPermit;val rows=hits.map{v->Work(v.id,v.title,if(v.roads.any{r->r.episodes.size>1})"tv"else"movie",v.year,v.poster,v.overview,provider="site_${site.key}",season=Identity.season(v.title)?:1)};searchResults=(searchResults+rows).distinctBy{it.key};withContext(Dispatchers.IO){rows.forEach(db::cache)}}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e}catch(_:Exception){}}}}.joinAll();if(token==queryGeneration055)message("资源站搜索完成，${searchResults.size}条结果")}finally{if(token==queryGeneration055)busy=false}}}

    fun toggleFavorite(work:Work){users.write{toggleFavorite(work)};message(if(work.key in user.favorites)"已取消收藏"else"已收藏")}
    fun hasRenderedFrame()=rendered
    fun setWatchState(work:Work,state:WatchState){users.write{setState(work,state)};if(state==WatchState.COMPLETED){val watch=user.lastByWork[work.key];if(watch!=null)viewModelScope.launch(Dispatchers.IO){home.publishWatch(watch.copy(done=true))}}}
    fun clearWatchState(work:Work){users.write{clearState(work)}}
    fun watchState(work:Work,done:Boolean)=setWatchState(work,if(done)WatchState.COMPLETED else WatchState.WATCHING)
    fun hideContinue(work:Work){users.write{hideContinue(work)};message("已从继续观看移除，历史记录保留")}
    fun play(work:Work,episode:String?=null,fromBeginning:Boolean=false){
        if(screen!="player"){playbackReturnScreen=screen;playbackReturnWork=detail.takeIf{screen=="detail"}}
        if(work.provider=="live"){restoreLive050(work,true);return}
        viewModelScope.launch{users.ready.first{it};if(users.loadError.value.isNotBlank()){message(users.loadError.value);return@launch};user=users.state.value;playReady(work,episode,fromBeginning)}
    }
    fun openLive050(entry:LiveEntry050){if(screen!="detail"&&screen!="player")origin="live";liveEntry=entry;detail=entry.work;screen="detail"}
    private fun restoreLive050(w:Work,play:Boolean){viewModelScope.launch{val entry=withContext(Dispatchers.IO){runCatching{LiveEntry050.from(org.json.JSONObject(db.secrets.read("live_entry_${w.id}")))}.getOrNull()};if(entry!=null){openLive050(entry);if(play)playLive050(entry)}else message("请回直播与体育重新选择频道；不会转入点播搜索")}}
    fun playLive050(entry:LiveEntry050,preferred:String="",refreshOnly:Boolean=false,recovering:Boolean=false){
        if(!recovering){liveRetry054.reset(android.os.SystemClock.elapsedRealtime());liveRecovery054?.cancel()};liveRouteErrors054=emptyMap()
        sourceJob?.cancel();qualityJob?.cancel();startupJob?.cancel();generation++;val token=generation
        if(!refreshOnly){if(screen!="player"){playbackReturnScreen=screen;playbackReturnWork=detail.takeIf{screen=="detail"}};seekTarget051=0;saveProgress();ensurePlayer();player.stop();player.clearMediaItems();PlaybackOwner.clear();oldWatch=null;rendered=false;current=entry.work;detail=entry.work;liveEntry=entry;currentEpisode=entry.vod.title;position=0;duration=0;origin="live";currentCandidate=null;resume=0;tried.clear();screen="player";streamLoading=true}
        searching=true;checked=0;candidates=emptyList();siteStates=emptyList();episodeNames=emptyList()
        sourceJob=viewModelScope.launch{
            try{val resolved=withContext(Dispatchers.IO){sites.liveDetail054(entry)};if(token!=generation)return@launch;liveEntry=resolved
                val routes=resolved.vod.roads.flatMap{r->r.episodes.map{r to it}}.distinctBy{it.first.name+"|"+it.second.url};total=routes.size;episodeNames=routes.map{it.first.name+" · "+it.second.name}.distinct()
                if(!config.incognito)withContext(Dispatchers.IO){db.secrets.save("live_last",resolved.json().toString());db.secrets.save("live_entry_${entry.work.id}",resolved.json().toString())}
                for((road,ep) in routes){if(token!=generation)return@launch
                    try{val c=withTimeout(35000){sites.probe(resolved.site,road,ep)}.let{it.copy(episode=it.episode.copy(name=road.name+" · "+ep.name),sourceId=entry.site.key+"|"+entry.vod.id+"|"+road.name)};if(token!=generation)return@launch;candidates=(candidates+c).distinctBy{it.identity};if(!refreshOnly&&currentCandidate==null&&(preferred.isBlank()||preferred==road.name+"|"+ep.name))select(c,0,false)}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e}catch(e:Exception){val reason=Regex("HTTP \\d{3}").find(e.message.orEmpty())?.value?:if(e.message.orEmpty().contains("Access"))"播放接口需要访问授权"else"直播解析或媒体访问失败";liveRouteErrors054=liveRouteErrors054+(road.name+" · "+ep.name to reason)};checked++
                }
                if(!refreshOnly&&currentCandidate==null&&candidates.isNotEmpty())select(candidates.first(),0,false)
                if(candidates.isEmpty()){streamLoading=false;message("此频道暂无通过检测的直播线路，请刷新频道或更换直播源；未搜索点播")}
            }catch(e:CancellationException){throw e}catch(e:Exception){streamLoading=false;message("直播详情读取失败，请返回刷新频道")}finally{if(token==generation)searching=false}
        }
    }
    private fun seriesFallbackEpisodes(work:Work):List<String>{
        if(work.kind!="tv")return emptyList()
        val matchingSeries=details.series.value?.takeIf{details.work.value?.key==work.key&&details.work.value?.season==work.season}
        val fromMeta=matchingSeries?.episodes.orEmpty().filter{it.season==work.season}.sortedBy{it.number}.map{"第%02d集".format(it.number)}
        if(fromMeta.size>1)return fromMeta
        if(work.episodeCount>1)return (1..work.episodeCount.coerceAtMost(500)).map{"第%02d集".format(it)}
        return emptyList()
    }
    private fun playReady(work:Work,episode:String?,fromBeginning:Boolean){
        roadEpisodes051.clear();seekTarget051=0
        ensurePlayer();player.setPlaybackSpeed(config.speed);if(!config.incognito){home.resumeEngaged(work);users.write{engage(work)}}
        saveProgress();player.stop();player.clearMediaItems();rendered=false;oldWatch=if(fromBeginning)null else user.watches.firstOrNull{it.work.key==work.key&&it.work.season==work.season&&(episode==null||Identity.sameEpisode(it.episode,episode))};current=work;detail=work;currentEpisode=episode?:oldWatch?.episode?:if(work.kind=="movie")"正片"else"第01集";resume=if(oldWatch?.episode==currentEpisode)oldWatch?.position?:0 else 0;currentCandidate=null;initialStart=false;tried.clear();candidates=emptyList();episodeNames=seriesFallbackEpisodes(work);if(work.kind=="tv"&&(details.work.value?.key!=work.key||details.work.value?.season!=work.season||details.series.value==null))details.load(work);screen="player";scan(false)
    }
    private val scannedKeys052=java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var manualSubtitlePick102=false
    private var lastWorkingWorkKey102=""
    private var lastWorkingSite102=""
    private var lastWorkingRoad102=""
    fun refreshSources(){if(current==null)return;if(current?.provider=="live"){liveEntry?.let{playLive050(it,refreshOnly=true)};return};scan(true)}
    fun continueScan(){
        if(current==null||current?.provider=="live")return
        if(searching){pausedScan=false;message("已继续搜索其余站点");return}
        if(checked<total)scan(background=true,resumeRemaining=true)
        else message("所有站点已检测完成")
    }
    fun changePreference(value:String){preference=value;db.prefs.edit().putString("source_preference",value).apply();message("已切换优先级，刷新后后台检测；当前播放保留")}
    private fun scan(background:Boolean,resumeRemaining:Boolean=false){
        sourceJob?.cancel();qualityJob?.cancel();qualityJob=null;generation++;val session=generation;val began=android.os.SystemClock.elapsedRealtime();val work=current?:return;WebMedia052.suppressDuringPlayback=background&&currentCandidate!=null;pausedScan=false;searching=true;if(!resumeRemaining){checked=0;candidates=emptyList();scannedKeys052.clear()};notice=""
        sourceJob=viewModelScope.launch{
            val routeType=SourceRoutePolicy.classify(work)
            val allActive=withContext(Dispatchers.IO){sites.ordered(work).filter{sites.enabled(it)&&it.searchable&&!LiveParser050.isPortal(it)}}
            val active=if(routeType==ContentRouteType.ANIME)allActive else allActive.filterNot{it.group=="动画"}.ifEmpty{allActive}
            total=active.size
            val existingStates=if(resumeRemaining)siteStates.associateBy{it.site.key}else emptyMap()
            if(!resumeRemaining)siteStates=active.map{SiteStatus(it)}
            val gate=Semaphore(config.parallel);val cached=oldWatch;val completed052=java.util.concurrent.ConcurrentHashMap.newKeySet<String>().apply{if(resumeRemaining)addAll(scannedKeys052)};val hitCache052=java.util.concurrent.ConcurrentHashMap<String,List<Vod>>();var priorFinished052=cached==null||background||resumeRemaining
            val stateMap=java.util.concurrent.ConcurrentHashMap<String,SiteStatus>().apply{active.forEach{put(it.key,existingStates[it.key]?:SiteStatus(it))}}
            val dirty=java.util.concurrent.atomic.AtomicBoolean(false)
            val scoreDirty=java.util.concurrent.atomic.AtomicBoolean(false)
            val routeRanks=withContext(Dispatchers.Default){SourceRoutePolicy.rankMap(work,active,config)}
            val scoreMap=withContext(Dispatchers.IO){active.associate{it.key to SiteScores102.score(db,it.key)}}
            val preferredSiteKey102=cached?.siteKey?.takeIf{it.isNotBlank()}?:lastWorkingSite102.takeIf{lastWorkingWorkKey102==work.key}.orEmpty()
            val preferredRoad102=cached?.road?.takeIf{it.isNotBlank()}?:lastWorkingRoad102.takeIf{lastWorkingWorkKey102==work.key}.orEmpty()
            val ordered=withContext(Dispatchers.Default){active.sortedWith(compareBy<Site>{if(it.key==preferredSiteKey102&&preferredSiteKey102.isNotBlank())0 else 1}.thenBy{routeRanks[it.key]?:999}.thenByDescending{scoreMap[it.key]?:0f}.thenBy{it.name})}
            val stopLimit=if(resumeRemaining)0 else config.sourceStopCount
            val waveStop=java.util.concurrent.atomic.AtomicBoolean(false)
            val absEpisodeNum=if(work.kind=="tv"&&work.season==1)Identity.episode(currentEpisode)else null
            val splitHint102=if(absEpisodeNum!=null&&absEpisodeNum>12){
                val liveSeries=details.series.value?.takeIf{details.work.value?.key==work.key&&details.work.value?.season==work.season}
                val diskSeries=liveSeries?:withContext(Dispatchers.IO){runCatching{val f=metadata.episodeCache055(work);if(f.isFile&&f.length()<2_000_000)EpisodeRail055.from(org.json.JSONObject(f.readText()))else null}.getOrNull()}
                SplitSeason102.fromSeries(diskSeries,work.season,absEpisodeNum)
            }else null
            fun flushUi(){
                if(session!=generation)return
                if(dirty.getAndSet(false)){siteStates=active.map{stateMap[it.key]?:SiteStatus(it)};checked=completed052.size}
                if(scoreDirty.getAndSet(false)){siteScoresRevision++}
            }
            val flusher=launch{while(isActive&&session==generation){delay(300);flushUi()}}
            fun markSite(site:Site,text:String,candidate:Candidate?=null){stateMap[site.key]=SiteStatus(site,text,candidate!=null,candidate);dirty.set(true)}
            fun markScore(site:Site,delta:Float){if(SiteScores102.recordOnce(db,work.key,work.season,currentEpisode,site.key,delta))scoreDirty.set(true)}
            suspend fun inspect052(site:Site,priorPhase:Boolean=false)=withContext(Dispatchers.IO){
                if(!priorPhase&&!background&&waveStop.get())return@withContext
                gate.withPermit{
                    while(pausedScan)delay(150);if(session!=generation||!sites.enabled(site)||(!priorPhase&&!background&&waveStop.get()))return@withPermit
                    if(!priorPhase&&CloudPolicy054.needsAuthorization(site)&&System.currentTimeMillis()-db.prefs.getLong("cloud_authenticated_${site.key}",0)>30L*86400000){
                        markSite(site,"网盘待扫码授权");markScore(site,0.5f);completed052.add(site.key);scannedKeys052.add(site.key);dirty.set(true);return@withPermit
                    }
                    markSite(site,"搜索中…")
                    try{
                        val primaryQuery=if(work.kind=="tv"&&work.season==1)Identity.searchTitle(work.title).takeIf{it.length>=2}?:work.title else work.title
                        var hits=hitCache052[site.key]?:if(site.type==8)listOf(Stremio050.lookup(site,work,currentEpisode,metadata))else {
                            val id=Variant052.previousId(cached).takeIf{site.key==cached?.siteKey};val prior=id?.let{runCatching{sites.detail(site,Vod(it,work.title,work.year,work.poster,work.overview,emptyList()))}.getOrNull()};if(prior!=null)listOf(prior)else sites.search(site,primaryQuery)
                        }
                        for(alias in (if(site.type==8)emptyList()else Identity.searchQueries(work).filter{it!=primaryQuery}.take(3)))if(hits.none{Identity.matches(work,it)})hits=hits+sites.search(site,alias)
                        hitCache052[site.key]=hits
                        val matching=hits.filter{Identity.matches(work,it)}.distinctBy{it.id}.sortedByDescending{Identity.matchScore(work,it)}.take(5)
                        var successful=false;var probeFailure=""
                        suspend fun probeCandidate102(vod:Vod,road:Road,ep:Episode,roadLabel:String=road.name,displayEp:String=ep.name):Boolean{
                            if(!priorPhase&&CloudPolicy054.isCloudRoad(road.name)&&System.currentTimeMillis()-db.prefs.getLong("cloud_authenticated_${site.key}",0)>30L*86400000){probeFailure="需要扫码授权，请点击网盘扫码";return false}
                            val probed=try{sites.probe(site,road,ep)}catch(e:CancellationException){throw e}catch(e:Exception){probeFailure=when{e.message.orEmpty().contains("需要扫码")->"需要扫码授权，请点击网盘扫码";e.message.orEmpty().contains("空播放地址")->"插件返回空播放地址";e.message.orEmpty().contains("网页解析")->"需要网页解析";else->Regex("""HTTP \d{3}""").find(e.message.orEmpty())?.value?:"媒体探测失败"};return false}
                            val candidate=probed.copy(road=roadLabel,episode=probed.episode.copy(name=displayEp),sourceId="${site.key}|${vod.id}|${road.name}")
                            if(session!=generation||!sites.enabled(site))return false;successful=true
                            markSite(site,"探测 ${candidate.delay} ms",candidate)
                            return withContext(Dispatchers.Main){
                                if(session!=generation||!sites.enabled(site))return@withContext false
                                candidates=(candidates+candidate).distinctBy{it.identity}.sortedWith(compareBy<Candidate>{if(it.bytes>0)it.bytes else Long.MAX_VALUE}.thenByDescending{if(preference=="quality")it.height else 0}.thenBy{it.delay})
                                if(!background&&currentCandidate==null&&candidate.identity !in tried&&(DecodeSupport052.compatible(candidate)||cached!=null&&candidate.site.key==cached.siteKey&&Variant052.same(candidate.road,cached.road))){
                                    if(cached!=null&&priorFinished052){nextCandidate()}else if(cached==null&&preference=="quality"){if(qualityJob==null)qualityJob=viewModelScope.launch{delay((began+6000-android.os.SystemClock.elapsedRealtime()).coerceAtLeast(0));if(session==generation&&currentCandidate==null)nextCandidate()}}else if(cached==null||candidate.site.key==cached.siteKey&&Variant052.same(candidate.road,cached.road)&&(candidate.height==0||cached.height==0||candidate.height>=cached.height))select(candidate,resume,true)
                                    if(priorPhase&&currentCandidate!=null)return@withContext true
                                }
                                if(!background&&stopLimit>0&&currentCandidate!=null&&candidates.size>=stopLimit)waveStop.set(true)
                                false
                            }
                        }
                        val detailedDirect=mutableListOf<Pair<Int,Vod>>()
                        for(row in matching){val vod=sites.detail(site,row);detailedDirect+=SplitSeason102.detectVodSeason(vod.title) to vod;vod.roads.forEach{r->roadEpisodes051[site.key+"|"+vod.id+"|"+r.name]=r.episodes;roadEpisodes051[site.key+"|"+r.name]=r.episodes};
                            suspend fun probeRoad052(road:Road):Boolean{
                                val ep=Identity.pick(road,currentEpisode,work.kind)?:return false
                                return probeCandidate102(vod,road,ep)
                            }
                            val roads=vod.roads.sortedBy{if(site.key==preferredSiteKey102&&preferredRoad102.isNotBlank()&&Variant052.same(it.name,preferredRoad102))0 else 1}
                            if(site.type==8&&!priorPhase){val roadGate052=Semaphore(3);coroutineScope{roads.map{road->launch{roadGate052.withPermit{probeRoad052(road)}}}.joinAll()}}
                            else for(road in roads){if(probeRoad052(road))return@withPermit;if(successful&&!background)break}
                        }
                        if(!successful&&absEpisodeNum!=null&&absEpisodeNum>12&&site.type!=8){
                            val activeSplitHint=splitHint102?:SplitSeason102.fromSeries(details.series.value?.takeIf{details.work.value?.key==work.key&&details.work.value?.season==work.season},work.season,absEpisodeNum)
                            var splitVods=SplitSeason102.seriesVods(work,hits)
                            val needSeason=activeSplitHint?.splitSeason
                            if((needSeason!=null&&splitVods.none{it.first==needSeason})||(needSeason==null&&splitVods.none{it.first>=2})){
                                for(sq in SplitSeason102.splitQueries(work,activeSplitHint,absEpisodeNum).filter{it!=primaryQuery}.take(2)){
                                    if(session!=generation||!sites.enabled(site))break
                                    val extra=withTimeoutOrNull(8000){runCatching{sites.search(site,sq)}.getOrDefault(emptyList())}.orEmpty()
                                    if(extra.isNotEmpty()){hits=(hits+extra).distinctBy{it.id};hitCache052[site.key]=hits;splitVods=SplitSeason102.seriesVods(work,hits)}
                                    if(needSeason!=null&&splitVods.any{it.first==needSeason})break
                                    if(needSeason==null&&splitVods.any{it.first>=2})break
                                }
                            }
                            val alreadyDetailed=detailedDirect.associateBy{it.second.id}.toMutableMap()
                            if(activeSplitHint!=null){
                                val targetVods=splitVods.filter{it.first==activeSplitHint.splitSeason}.map{it.second}.take(2)
                                val localName="第%02d集".format(activeSplitHint.localEpisode)
                                for(row in targetVods){
                                    if(successful)break
                                    val vod=alreadyDetailed[row.id]?.second?:withTimeoutOrNull(8000){runCatching{sites.detail(site,row)}.getOrNull()}?.also{alreadyDetailed[row.id]=activeSplitHint.splitSeason to it}?:continue
                                    val roads=vod.roads.sortedBy{if(site.key==preferredSiteKey102&&preferredRoad102.isNotBlank()&&Variant052.same(it.name,preferredRoad102))0 else 1}
                                    for(road in roads){
                                        val ep=Identity.pick(road,currentEpisode,"tv")?:Identity.pick(road,localName,"tv")?:continue
                                        val tag="${road.name} · S${activeSplitHint.splitSeason}E%02d".format(activeSplitHint.localEpisode)
                                        if(probeCandidate102(vod,road,ep,roadLabel=tag,displayEp=currentEpisode))return@withPermit
                                        if(successful&&!background)break
                                    }
                                }
                            }
                            if(!successful&&splitVods.any{it.first>=2}){
                                val bySeason=splitVods.groupBy{it.first}.toSortedMap().entries.take(4).mapNotNull{(sNum,list)->
                                    val chosen=list.first().second
                                    val vod=alreadyDetailed[chosen.id]?.second?:withTimeoutOrNull(8000){runCatching{sites.detail(site,chosen)}.getOrNull()}?.also{alreadyDetailed[chosen.id]=sNum to it}
                                    vod?.let{sNum to it}
                                }
                                SplitSeason102.resolveByCumulative(bySeason,absEpisodeNum)?.let{(vod,sNum,localEp)->
                                    val localName="第%02d集".format(localEp)
                                    val roads=vod.roads.sortedBy{if(site.key==preferredSiteKey102&&preferredRoad102.isNotBlank()&&Variant052.same(it.name,preferredRoad102))0 else 1}
                                    for(road in roads){
                                        val ep=Identity.pick(road,localName,"tv")?:Identity.pick(road,currentEpisode,"tv")?:continue
                                        val tag=if(sNum>1)"${road.name} · S${sNum}E%02d".format(localEp)else road.name
                                        if(probeCandidate102(vod,road,ep,roadLabel=tag,displayEp=currentEpisode))return@withPermit
                                        if(successful&&!background)break
                                    }
                                }
                            }
                        }
                        if(matching.isEmpty()&&!successful){markSite(site,"NA · 没有匹配作品");markScore(site,-1f);return@withPermit}
                        if(successful)markScore(site,1f)
                        else if(probeFailure.contains("扫码")||CloudPolicy054.needsAuthorization(site)){markSite(site,probeFailure.ifBlank{"网盘待扫码授权"});markScore(site,0.5f)}
                        else{markSite(site,probeFailure.ifBlank{"NA · 当前集数无可播线路"});markScore(site,-1f)}
                    }catch(e:CancellationException){throw e}catch(e:Exception){markScore(site,-1f);markSite(site,e.message.orEmpty().let{msg->Regex("""HTTP \d{3}""").find(msg)?.value?:if(msg.contains("JS规则"))"JS规则暂未适配"else if(msg.contains("Spider"))msg.substringBefore(" @").substringBefore(" on path").take(100)else if(BuildConfig.DEBUG)"连接/解析失败 · ${e.javaClass.simpleName} · ${e.message.orEmpty().replace(Regex("""https?://[^\s]+"""),"<url>").take(100)}"else "连接/解析失败"})}
                    finally{if(session==generation){completed052.add(site.key);scannedKeys052.add(site.key);dirty.set(true)}}
                }
            }
            try{
                val priorSite=if(resumeRemaining)null else ordered.firstOrNull{it.key==preferredSiteKey102&&preferredSiteKey102.isNotBlank()}
                if(priorSite!=null&&!background){message(if(cached!=null)"先恢复上次站点与播放版本…"else"优先检测当前工作站点…");try{withTimeout(25000){inspect052(priorSite,true)}}catch(e:TimeoutCancellationException){markSite(priorSite,"上次片源恢复超时，按优先级查找替代")} }
                priorFinished052=true
                if(!background&&currentCandidate==null&&cached!=null)nextCandidate()
                val waves=ordered.filter{it.key!=priorSite?.key&&(!resumeRemaining||it.key !in completed052)}.groupBy{routeRanks[it.key]?:999}.toSortedMap()
                if(background||candidates.isEmpty())for((_,wave) in waves){
                    if(session!=generation||(!background&&waveStop.get()))break
                    val jobs=wave.map{site->launch(Dispatchers.IO){inspect052(site)}}
                    val stopper=if(!background)launch{while(isActive&&!waveStop.get())delay(250);if(waveStop.get())jobs.forEach{it.cancel()}}else null
                    jobs.joinAll()
                    stopper?.cancel()
                    flushUi()
                    if(!background&&stopLimit>0&&candidates.size>=stopLimit)break
                    if(!background&&candidates.isNotEmpty())break
                }
            }finally{flusher.cancel();flushUi();val needsProxy=currentCandidate?.episode?.url?.let{it.startsWith("http://127.0.0.1")||it.startsWith("http://localhost")}==true;sites.releaseIdleSpider(needsProxy)}
            if(session==generation){searching=false;if(currentCandidate==null&&!background)nextCandidate();if(candidates.isEmpty())message("没有找到当前集数的可用线路，可更换片名或查看站点状态")else if(background)message("检测完成，${candidates.size}条候选，请手动选择切换")}
        }
    }
    private fun updateSite(site:Site,text:String,candidate:Candidate?=null){siteStates=siteStates.map{if(it.site.key==site.key)SiteStatus(site,text,candidate!=null,candidate)else it}}
    private val seekPatches051=mutableMapOf<String,MkvSeek051.Patch>()
    private var seekJob051:Job?=null
    private var lastSeekAt057:Long=0L
    private var seekRecoveryCount057:Int=0
    fun select(candidate:Candidate,start:Long=position,automatic:Boolean=false){
        seekJob051?.cancel();seekRecoveryCount057=0;manualSubtitlePick102=false
        val work=current?:return;automaticSelection=automatic;WebMedia052.suppressDuringPlayback=true;ensurePlayer();startupJob?.cancel();saveProgress();player.stop();player.clearMediaItems();tried.add(candidate.identity);playbackCapability060=PlaybackCapability060(display=PlaybackCapabilities060.display(getApplication()));currentCandidate=candidate;lastWorkingWorkKey102=work.key;lastWorkingSite102=candidate.site.key;lastWorkingRoad102=candidate.road.substringBefore(" · S");currentEpisode=candidate.episode.name;if(work.provider!="live")episodeNames=Episodes051.names(work.kind,currentEpisode,candidate,candidates,roadEpisodes051,seriesFallbackEpisodes(work));resume=start;rendered=false;streamLoading=true;initialStart=false
        val audioHints=TrackLanguagePolicy102.exoLanguageHints(config.audioPrimaryLang,config.audioSecondaryLang)
        val subHints=TrackLanguagePolicy102.exoLanguageHints(config.subtitlePrimaryLang,config.subtitleSecondaryLang)
        player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO).clearOverridesOfType(C.TRACK_TYPE_TEXT).apply{if(audioHints.isNotEmpty())setPreferredAudioLanguages(*audioHints);if(config.subtitlePrimaryLang=="off")setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true)else{setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false);if(subHints.isNotEmpty())setPreferredTextLanguages(*subHints)}}.build()
        addons.bind(work,candidate.episode.name)
        PlaybackOwner.source(work,candidate)
        val baseClient=if(candidate.site.type==8)NetworkPolicy.directClient()else NetworkPolicy.client()
        val http=androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(StreamHttp054.client(baseClient)).setUserAgent(NetworkPolicy.userAgent).setDefaultRequestProperties(candidate.site.headers)
        val item=MediaItem.Builder().setMediaMetadata(MediaMetadata.Builder().setTitle(work.title).setSubtitle(candidate.episode.name).apply{if(work.poster.startsWith("https://"))setArtworkUri(android.net.Uri.parse(work.poster))}.build()).setUri(candidate.episode.url).apply{if(candidate.mimeType.isNotBlank())setMimeType(candidate.mimeType)}.build()
        player.setMediaSource(DefaultMediaSourceFactory(seekPatches051[candidate.episode.url]?.let{MkvSeek051.factory(http,it)}?:http).createMediaSource(item),start.coerceAtLeast(0));player.prepare();player.play();if(start>0&&candidate.mimeType=="video/x-matroska"&&candidate.episode.url !in seekPatches051)repairSeek051(candidate,start);if(config.background)PlaybackOwner.startService(getApplication<Application>());screen="player"
        startupJob=viewModelScope.launch{delay(config.startupSeconds*1000L);if(!rendered&&currentCandidate?.identity==candidate.identity&&player.playWhenReady)failCandidate("此线路${config.startupSeconds}秒未出画面，尝试其他来源")}
    }
    private fun failCandidate(reason:String){resume=maxOf(resume,Episodes051.fallbackPosition(player.currentPosition,seekTarget051));startupJob?.cancel();val failed=currentCandidate;currentCandidate=null;player.stop();if(failed!=null){updateSite(failed.site,reason);playbackFailures052=(playbackFailures052+"${failed.site.name} · ${failed.road.take(130)} · $reason").takeLast(30)};message(reason);nextCandidate()}
    private fun nextCandidate(){val preferred=oldWatch;val next=candidates.filter{it.identity !in tried&&(DecodeSupport052.compatible(it)||preferred!=null&&it.site.key==preferred.siteKey&&Variant052.same(it.road,preferred.road))&&ResumePolicy040.eligible(preferred?.height?:0,it.height)}.sortedWith(compareBy<Candidate>{if(it.site.key==preferred?.siteKey&&Variant052.same(it.road,preferred.road))0 else 1}.thenBy{if(preferred!=null&&it.height>0&&it.height<preferred.height)1 else 0}.thenByDescending{if(preference=="quality")it.height else 0}.thenBy{if(it.bytes>0)it.bytes else Long.MAX_VALUE}.thenBy{it.delay}).firstOrNull();if(next!=null)select(next,resume,true)else if(!searching){streamLoading=false;message(if(preferred!=null)"未找到上次${preferred.height}P可用版本，请手动选择替代线路"else if(candidates.isNotEmpty()&&candidates.none{DecodeSupport052.compatible(it)})"本机未报告这些HEVC 10bit版本的解码支持，请在播放线路中选择H.264或手动尝试"else"所有已检测线路均未成功起播，请在播放线路查看具体错误并重新检测")}}
    fun playFromCatalog(work:Work,group:CatalogGroup,episode:Episode){
        if(CloudPolicy054.needsAuthorization(group.site)&&System.currentTimeMillis()-db.prefs.getLong("cloud_authenticated_${group.site.key}",0)>30L*86400000){authorizeCloud054(group.site,group.road,episode,work);return}
        if(screen!="player"){playbackReturnScreen=screen;playbackReturnWork=detail.takeIf{screen=="detail"}};roadEpisodes051.clear();roadEpisodes051[group.key]=group.road.episodes;roadEpisodes051[group.site.key+"|"+group.road.name]=group.road.episodes;seekTarget051=0
        saveProgress();sourceJob?.cancel();qualityJob?.cancel();generation++;val token=generation;ensurePlayer();player.stop();player.clearMediaItems();oldWatch=null;current=work;detail=work;currentEpisode=episode.name;currentCandidate=null;rendered=false;streamLoading=true;resume=0;position=0;duration=0;tried.clear();candidates=emptyList();siteStates=emptyList();episodeNames=group.road.episodes.map{it.name};screen="player"
        sourceJob=viewModelScope.launch{try{val candidate=withTimeout(config.startupSeconds*1000L){withContext(Dispatchers.IO){sites.probe(group.site,group.road,episode).copy(sourceId=group.key)}};if(token==generation){candidates=listOf(candidate);select(candidate,0,false)}}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;streamLoading=false;message("所选线路解析超时，可重新检测或手动换源")}catch(_:Exception){streamLoading=false;message("所选线路暂不可用，可重新检测或手动换源")}}
    }
    fun stepEpisode(direction:Int){val index=episodeNames.indexOfFirst{Identity.sameEpisode(it,currentEpisode)};val next=episodeNames.getOrNull(index+direction);if(index>=0&&next!=null)chooseEpisode(next,true)else message("没有可用的${if(direction>0)"下一集"else"上一集"}")}
    fun chooseEpisode(name:String,begin:Boolean=false){val active=currentCandidate;val eps=active?.let{roadEpisodes051[it.sourceId]?:roadEpisodes051[it.site.key+"|"+it.road]};val requested=eps?.firstOrNull{it.name==name};if(current?.kind=="movie"&&active!=null&&requested!=null&&eps.size>1){val work=current?:return;playFromCatalog(work,CatalogGroup(active.sourceId,active.site,work.title,Road(active.road,eps)),requested);return};if(current?.provider=="live"){candidates.firstOrNull{it.episode.name==name}?.let{select(it,0,false)}?:message("该线路尚未通过解析，可刷新直播线路");return};val work=current?:detail?:return;play(work,name,begin)}
    fun saveProgress(done:Boolean=false,forcePublish:Boolean=false){if(current?.provider=="live")return;if(playerReleased||!PlaybackOwner.isCurrent(player)||config.incognito)return;val work=current?:return;val candidate=currentCandidate?:return;if(!rendered||player.currentPosition<1000)return;val watch=Watch(work,currentEpisode,candidate.site.key,candidate.road,player.videoSize.height.takeIf{it>0}?:candidate.height,player.currentPosition,player.duration.coerceAtLeast(0),done=done,sourceId=candidate.sourceId);users.progress(watch);if(done||forcePublish||System.currentTimeMillis()-lastHomeWrite>15000){lastHomeWrite=System.currentTimeMillis();viewModelScope.launch(Dispatchers.IO){home.publishWatch(watch)}}}
    fun stop(){WebMedia052.suppressDuringPlayback=false;liveRecovery054?.cancel();addons.clear();qualityJob?.cancel();val previous=current;saveProgress(forcePublish=true);startupJob?.cancel();sourceJob?.cancel();generation++;if(!playerReleased){player.stop();player.clearMediaItems()};PlaybackOwner.clear();PlaybackOwner.stopService(getApplication<Application>());sites.releaseIdleSpider(false);current=null;currentCandidate=null;position=0;duration=0;playing=false;searching=false;streamLoading=false;if(previous!=null){detail=previous;screen="detail"};message("播放已停止")}
    fun showPlayer(){if(current!=null&&!playerReleased){if(screen!="player"){playbackReturnScreen=screen;playbackReturnWork=detail.takeIf{screen=="detail"}};detail=current;screen="player"}}
    fun onExternalBackground(){if(!config.background)liveRecovery054?.cancel();qualityJob?.cancel();PlaybackOwner.foreground=false;if(config.background&&current!=null&&PlaybackOwner.serviceRunning){saveProgress(forcePublish=true);return};sites.releaseIdleSpider(false);if(playerReleased)return;val previous=current;saveProgress(forcePublish=true);startupJob?.cancel();sourceJob?.cancel();generation++;searching=false;player.stop();player.clearMediaItems();PlaybackOwner.release(player);PlaybackOwner.stopService(getApplication<Application>());playerReleased=true;playing=false;streamLoading=false;addons.clear();current=null;currentCandidate=null;if(previous!=null){detail=previous;if(screen=="player")screen="detail"}}
    fun back(){if(screen in listOf("detail","credits")&&backBrowse055())return;screen=when(screen){"credits"->{detail=creditPage055?.owner;detail?.let(details::load);"detail"};"settings","sites"->utilityTrail.removeLastOrNull()?:"home";"player"->{detail=playbackReturnWork?:current;if(playbackReturnScreen=="detail"&&detail!=null&&details.work.value?.key!=detail?.key)details.load(detail!!);restoreFocus="$focusRow|$focusWork";playbackReturnScreen.takeUnless{it=="player"}?:"detail"};"detail"->{if(detailTrail.isNotEmpty()){val prior=detailTrail.removeAt(detailTrail.lastIndex);detail=prior;details.load(prior);return};details.cancelLoad();restoreFocus="$focusRow|$focusWork";origin.takeUnless{it=="detail"||it=="player"}?:"home"};else->"home"}}
    fun toggleMute051(){if(!muted051){unmutedVolume051=player.volume.takeIf{it>0}?:volume.takeIf{it>0}?:.75f;player.volume=0f}else player.volume=unmutedVolume051;muted051=!muted051;db.prefs.edit().putBoolean("player_muted",muted051).putFloat("unmuted_volume",unmutedVolume051).apply()}
    fun seek051(target:Long){if(current?.provider=="live")return;if(!player.isCurrentMediaItemSeekable){val c=currentCandidate;if(c!=null&&c.mimeType=="video/x-matroska")repairSeek051(c,target)else message("此线路未提供可快进索引，请换可快进的版本");return};seekTarget051=if(player.duration>0)target.coerceIn(0,player.duration)else target.coerceAtLeast(0);resume=seekTarget051;position=seekTarget051;lastSeekAt057=android.os.SystemClock.elapsedRealtime();seekRecoveryCount057=0;runCatching{player.setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)};player.seekTo(seekTarget051)}
    private fun repairSeek051(c:Candidate,target:Long){if(seekJob051?.isActive==true)return;val token=generation;message("正在读取此线路快进索引…");seekJob051=viewModelScope.launch{val patch=try{withContext(Dispatchers.IO){MkvSeek051.load(c)}}catch(e:CancellationException){throw e}catch(_:Exception){null};if(token!=generation||currentCandidate?.identity!=c.identity)return@launch;if(patch==null){if(!player.isCurrentMediaItemSeekable)message("此线路没有可用的快进索引，请切换其他版本");return@launch};if(seekPatches051.size>=12)seekPatches051.clear();seekPatches051[c.episode.url]=patch;seekJob051=null;seekTarget051=target.coerceAtLeast(0);select(c,seekTarget051,false)}}
    fun markManualSubtitle102(){manualSubtitlePick102=true}
    fun persistAudio(language:String){db.prefs.edit().putString("audio_${current?.key}",language).apply();player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setPreferredAudioLanguage(language).build()}
    private fun restoreAudio(){
        val saved=db.prefs.getString("audio_${current?.key}",db.prefs.getString("audio_${current?.legacyKey}","")).orEmpty()
        if(saved.isNotBlank()){
            player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setPreferredAudioLanguage(saved).build()
            return
        }
        val picked=TrackLanguagePolicy102.pickTrack(player.currentTracks,C.TRACK_TYPE_AUDIO,config.audioPrimaryLang,config.audioSecondaryLang)?:return
        if(!picked.first.isTrackSelected(picked.second)){
            player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(picked.first.mediaTrackGroup,listOf(picked.second))).build()
        }
    }
    private fun applyAutoSubtitle(tracks:Tracks){
        if(manualSubtitlePick102||addons.cues.isNotEmpty())return
        if(config.subtitlePrimaryLang=="off"){
            if(C.TRACK_TYPE_TEXT !in player.trackSelectionParameters.disabledTrackTypes){
                player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true).build()
            }
            return
        }
        val picked=TrackLanguagePolicy102.pickTrack(tracks,C.TRACK_TYPE_TEXT,config.subtitlePrimaryLang,config.subtitleSecondaryLang)?:return
        if(!picked.first.isTrackSelected(picked.second)||C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes){
            player.trackSelectionParameters=player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).setOverrideForType(TrackSelectionOverride(picked.first.mediaTrackGroup,listOf(picked.second))).build()
        }
    }
    override fun onCleared(){liveRecovery054?.cancel();addons.clear();qualityJob?.cancel();stationCheck?.cancel();saveProgress();stopRemoteConfig();details.dispose();sites.close();if(!playerReleased&&!(config.background&&PlaybackOwner.serviceRunning))PlaybackOwner.release(player);users.close()}
}

