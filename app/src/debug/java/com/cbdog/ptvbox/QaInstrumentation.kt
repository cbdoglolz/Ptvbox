package com.cbdog.ptvbox

import androidx.activity.compose.setContent
import android.app.Instrumentation
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import android.view.View
import android.view.ViewGroup
import android.view.SurfaceView
import android.view.PixelCopy

/** Debug APK only; never shipped in release APKs. Own app's audio is muted before playback. */
class QaInstrumentation:Instrumentation(){
    private lateinit var args:Bundle
    private fun launch053():Activity {val app=targetContext.applicationContext as android.app.Application;val ready=java.util.concurrent.CountDownLatch(1);var found:Activity?=null;val callback=object:android.app.Application.ActivityLifecycleCallbacks{
        override fun onActivityResumed(a:Activity){if(a is MainActivity){found=a;ready.countDown()}}
        override fun onActivityCreated(a:Activity,b:Bundle?){};override fun onActivityStarted(a:Activity){};override fun onActivityPaused(a:Activity){};override fun onActivityStopped(a:Activity){};override fun onActivitySaveInstanceState(a:Activity,b:Bundle){};override fun onActivityDestroyed(a:Activity){}
    };app.registerActivityLifecycleCallbacks(callback);try{targetContext.startActivity(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));check(ready.await(15,java.util.concurrent.TimeUnit.SECONDS)){"Activity did not resume"};return found!!}finally{app.unregisterActivityLifecycleCallbacks(callback)}}
    override fun onCreate(arguments:Bundle?){args=arguments?:Bundle();super.onCreate(arguments);start()}
    override fun onStart(){
        uiAutomation.serviceInfo=uiAutomation.serviceInfo.apply{flags=flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS}
        val result=JSONObject();var activity:Activity?=null;var model:PtvModel?=null
        try{
            if(args.getString("mode")=="backup054"){val data=BackupQa054.run(targetContext);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="notice054"){val data=UiQa054.notice(targetContext);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="addons053"){val data=AddonsQa053.contracts(targetContext);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="metadata040"){val data=MetadataQa040.run(targetContext);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="data040"){val data=DataQa040.run(targetContext);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="config022"){
                val data=ConfigQa022.run(targetContext);File(targetContext.filesDir,"qa-config022.json").writeText(data.toString(2));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="source022"){
                val data=SourceQa022.run(targetContext,args.getString("configUrl")!!);File(targetContext.filesDir,"qa-source022.json").writeText(data.toString(2));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="v2-data"){
                val data=V2Qa.data(targetContext);File(targetContext.filesDir,"qa-v2-data.json").writeText(data.toString(2));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="v2-home"){
                val data=V2Qa.home(targetContext);File(targetContext.filesDir,"qa-v2-home.json").writeText(data.toString(2));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode") in listOf("show-watch","cleanup-watch")){
                val db=Library(targetContext);val home=HomeChannels(targetContext,db)
                val prior=db.watches().first{it.work.title=="肖申克的救赎"&&it.height==1080}
                val work=runBlocking{Metadata(db).details(Work("1292052","肖申克的救赎","movie","1994"))}
                val w=prior.copy(work=work,updated=System.currentTimeMillis(),done=args.getString("mode")=="cleanup-watch")
                if(!w.done){db.prefs.edit().putFloat("volume",0f).apply();db.save(w);home.resumeEngaged(work)}
                home.publishWatch(w);val present=home.watchIds().containsKey("continue|${work.key}");db.close()
                val out=Bundle();out.putString("result","Watch Next present=$present; actual prior playback=${prior.position}ms; release data untouched");finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="cleanup-home"){
                val db=Library(targetContext);val home=HomeChannels(targetContext,db)
                val approved=args.getString("ids","")!!.split(',').mapNotNull{it.toLongOrNull()}.toSet()
                val ids=home.channelIds().values.filter{it in approved}
                ids.forEach{targetContext.contentResolver.delete(androidx.tvprovider.media.tv.TvContractCompat.buildChannelUri(it),null,null)}
                db.prefs.edit().putBoolean("qa_home_disabled",true).apply();targetContext.getSystemService(android.app.job.JobScheduler::class.java).cancel(8120);db.close()
                val out=Bundle();out.putString("result","Removed only ${ids.size} approved QA channels; release channels and app data preserved");finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode") in listOf("v2-ui","input","privateplay022","remote022","ui030","ui040","ui050","stremio050","livefixture050","privatebrowse050","playerui040","playerui022","playerui051","page051","seek051","faultseek051")){val setup=Library(targetContext);try{setup.prefs.edit().putBoolean("qa_home_disabled",true).putFloat("volume",0f).commit()}finally{setup.close()}}
            activity=launch053()
            runOnMainSync{model=ViewModelProvider(activity as MainActivity)[PtvModel::class.java];model!!.player.volume=0f}
            val m=model!!;runOnMainSync{result.put("audioMuted",m.player.volume==0f)}
            if(args.getString("mode") in listOf("ui103","network103","mapping103","transport103","search103","related103","searchRemote103","relatedUi103")){
                val oldIncognito=m.config.incognito
                try{runOnMainSync{m.config.set("incognito",true)};val data=when(args.getString("mode")){"relatedUi103"->SearchQa103.relatedUi(this,m,activity as MainActivity);"searchRemote103"->SearchQa103.remote(this,m);"search103"->SearchQa103.search(this,m,activity as MainActivity);"related103"->SearchQa103.related(this,m);"ui103"->FeaturesQa103.ui(this,m,activity as MainActivity);"network103"->runBlocking{FeaturesQa103.network(this@QaInstrumentation,m,activity as MainActivity)};"transport103"->runBlocking{FeaturesQa103.transport(this@QaInstrumentation,m)};else->runBlocking{FeaturesQa103.mapping(m)}};File(targetContext.getExternalFilesDir(null),"qa103-${args.getString("mode")}.json").writeText(data.toString(2));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
                finally{runOnMainSync{m.stop();m.config.set("incognito",oldIncognito)}}
            }
            if(args.getString("mode") in listOf("episodes060","settings060","notice060","gestures060","routing060")){val data=when(args.getString("mode")){"episodes060"->FeaturesQa060.episodes(this,m,activity as MainActivity);"settings060"->FeaturesQa060.settings(this,m,activity as MainActivity);"gestures060"->FeaturesQa060.gestures(this,m,activity as MainActivity);"routing060"->FeaturesQa060.routing(this,m,activity as MainActivity);else->FeaturesQa060.notice(this)};runOnMainSync{m.stop();(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="noticeExact100"){val data=FeaturesQa060.exactPrivateNotice(this);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="subtitle060"){val data=FeaturesQa060.publicSubtitle(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="noticeMain060"){val data=FeaturesQa060.mainNotice(this);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="privateClasses090"){val data=FeaturesQa060.privateClasses(this,m,args.getString("privateApi"));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="privateSearch090"){val data=FeaturesQa060.privateSearch(this,m,args.getString("privateTitles","肖申克的救赎"));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="privateDirect090"){val data=FeaturesQa060.privateDirect(this,m,activity as MainActivity,args.getString("privateApi")!!,args.getString("privateTitle","今天是太阳雨"),args.getString("privateYear","2026"));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="privatePlayback090"){val data=FeaturesQa060.privatePlayback(this,m,activity as MainActivity,args.getString("privateApi"),args.getString("privateTitle","肖申克的救赎"),args.getString("privateYear","1994"));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="cache055"){val data=FeaturesQa055.cache(this,m);runOnMainSync{(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode") in listOf("ui056","layout056","catalog056","sources056","animeplay056")){val data=when(args.getString("mode")){"ui056"->FeaturesQa056.ui(this,m,activity as MainActivity);"layout056"->FeaturesQa056.layout(this,m,activity as MainActivity);"catalog056"->runBlocking{FeaturesQa056.catalog()};"animeplay056"->runBlocking{FeaturesQa056.playGiri(this@QaInstrumentation,m)};else->runBlocking{FeaturesQa056.sources(this@QaInstrumentation,m)}};runOnMainSync{m.stop();(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode") in listOf("layout057","history057","danmaku057","douban057","anime2017","hdhubDirect057","player057")){val data=when(args.getString("mode")){
                "layout057"->FeaturesQa057.layout(this,m,activity as MainActivity)
                "history057"->FeaturesQa057.history(this,m,activity as MainActivity)
                "danmaku057"->FeaturesQa057.danmaku(this,m,activity as MainActivity)
                "douban057"->runBlocking{FeaturesQa057.douban(m)}
                "anime2017"->runBlocking{FeaturesQa057.anime2017()}
                "hdhubDirect057"->runBlocking{FeaturesQa057.hdhubDirect(this@QaInstrumentation,m)}
                else->runBlocking{FeaturesQa057.player(this@QaInstrumentation,m,activity as MainActivity)}
            };runOnMainSync{m.stop();(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode") in listOf("metadata055","search055","episodes055","credits055")){val data=when(args.getString("mode")){"metadata055"->FeaturesQa055.metadata(this,m);"search055"->FeaturesQa055.search(this,m,activity as MainActivity);"episodes055"->FeaturesQa055.episodes(this,m,activity as MainActivity);else->FeaturesQa055.credits(this,m,activity as MainActivity)};runOnMainSync{(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="hub054"){val data=UiQa054.hub(this,m,activity as MainActivity);runOnMainSync{(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="portalDom054"){val data=PortalDomQa054.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode") in listOf("ui054","authfixture054","authreal054")){val data=if(args.getString("mode")=="ui054")UiQa054.run(this,m,activity as MainActivity)else UiQa054.auth(this,m,args.getString("mode")=="authreal054");runOnMainSync{(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="ordinary030"){
                val all=m.sites.all();val prior=all.associateWith{m.sites.enabled(it)}
                val site=all.first{it.name.contains("魔都")&&it.group!="私人订阅"}
                m.db.prefs.edit().apply{all.forEach{putBoolean("site_${it.key}",it.key==site.key)}}.commit()
                try{
                    val work=Work("2156663","西游记","tv","1986")
                    runOnMainSync{m.play(work,"第01集",true)}
                    var frame=false;val until=System.currentTimeMillis()+100000
                    while(System.currentTimeMillis()<until){Thread.sleep(500);runOnMainSync{frame=m.hasRenderedFrame()&&m.player.currentPosition>2000};if(frame)break}
                    result.put("firstFrame",frame).put("site",site.name)
                    runOnMainSync{result.put("height",m.player.videoSize.height).put("position",m.player.currentPosition)}
                    if(frame){
                        Thread.sleep(4500);runOnMainSync{m.saveProgress()};Thread.sleep(600)
                        val saved=m.db.last(work)!!;runOnMainSync{m.stop();m.play(work)}
                        var resumed=false;val retryUntil=System.currentTimeMillis()+60000
                        while(System.currentTimeMillis()<retryUntil){Thread.sleep(500);runOnMainSync{resumed=m.hasRenderedFrame()&&m.player.currentPosition>=saved.position};if(resumed)break}
                        runOnMainSync{result.put("resumeRestored",resumed).put("resumeHeight",m.player.videoSize.height).put("resumeSitePreserved",m.currentCandidate?.site?.key==saved.siteKey)}
                        uiAutomation.takeScreenshot()?.let{b->File(targetContext.getExternalFilesDir(null),"qa-ordinary-play.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
                    }
                    val out=Bundle();out.putString("result",result.toString());finish(if(frame&&result.optBoolean("resumeRestored"))Activity.RESULT_OK else Activity.RESULT_CANCELED,out);return
                }finally{m.db.prefs.edit().apply{prior.forEach{(s,enabled)->putBoolean("site_${s.key}",enabled)}}.commit()}
            }
            if(args.getString("mode")=="privatebrowse050"){
                val items=runBlocking{kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){m.sites.all().filter{it.type==3&&LiveParser050.isPortal(it)}}};val records=org.json.JSONArray()
                for(site in items.take(4)){val row=JSONObject().put("site",site.name).put("apiClass",site.api);try{val root=runBlocking{kotlinx.coroutines.withTimeout(60000){m.sites.browse(site)}};val classes=root.optJSONArray("class")?.objects().orEmpty();row.put("categories",classes.size);val category=classes.firstOrNull()?.optString("type_id");val page=if(category!=null)runBlocking{kotlinx.coroutines.withTimeout(60000){m.sites.browse(site,category,1)}}else root;val rows=page.optJSONArray("list")?.objects().orEmpty();row.put("items",rows.size);if(rows.isNotEmpty()){val v=runBlocking{kotlinx.coroutines.withTimeout(45000){m.sites.detail(site,VodParser.parse(rows.first()))}};row.put("roads",v.roads.size).put("lines",v.roads.sumOf{it.episodes.size})};row.put("browseResponded",true)}catch(e:Exception){row.put("browseResponded",false).put("error",e.message?.substringBefore(" @")?.take(110))};records.put(row)}
                result.put("sources",records);val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode") in listOf("live054","liveprobe054")){val data=LiveQa054.run(this,m,if(args.getString("mode")=="liveprobe054")1 else 16);runOnMainSync{(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="anime053"){val data=UiQa053.anime(this,m);runOnMainSync{(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="ui053"){val data=UiQa053.run(this,m,activity!!,args.getString("mediaUrl")!!);runOnMainSync{(activity as MainActivity).setContent{}};val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="hdhub1080-052"){val data=HdhubQa052.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="portalDom052"){val data=PortalDomQa052.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="resume052"){val data=ResumeQa052.run(this,m,args.getString("api")!!);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="portal052"){val data=PortalQa052.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="platforms052"){args.getString("configUrl")?.let{runBlocking{m.db.secrets.save("source_config",Net.text(it))}};val data=PlatformsQa052.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="networklive052"){args.getString("configUrl")?.let{runBlocking{m.db.secrets.save("source_config",Net.text(it))}};val data=LiveQa052.run(this,m,args.getString("apiClass")?:"csp_Live");val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="live052"){val data=LiveQa052.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="page051"){val data=UiQa051.pagination(this,m,args.getString("api")!!);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="seek051"){
                val all=m.sites.all();val prior=all.associateWith{m.sites.enabled(it)};val site=all.first{it.type==8};m.db.prefs.edit().apply{all.forEach{putBoolean("site_${it.key}",it==site)}}.commit()
                try{val work=Work("qa-odyssey051","奥德赛","movie","2026",facts=WorkFacts(imdbId="tt33764258"));runOnMainSync{m.play(work,"正片",true)};try{UiQa050.await(150000){var ok=false;runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition>2000};ok}}catch(e:Exception){runOnMainSync{result.put("phase","before-seek").put("checked",m.checked).put("total",m.total).put("candidates",m.candidates.size).put("loading",m.streamLoading).put("notice",m.notice).put("states",org.json.JSONArray(m.siteStates.map{it.text})).put("playerErrors",org.json.JSONArray(m.playbackFailures052))};throw e};runOnMainSync{result.put("firstFrame",true).put("hevcMain10Advertised",DecodeSupport052.main10).put("height",m.player.videoSize.height).put("seekable",m.player.isCurrentMediaItemSeekable).put("duration",m.player.duration).put("route",m.currentCandidate?.road?.take(200)).put("site",m.currentCandidate?.site?.key).put("mime",m.currentCandidate?.mimeType);m.seek051(180000)};UiQa050.await(60000){var ok=false;runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition in 175000..200000};ok};runOnMainSync{result.put("seekPosition",m.player.currentPosition).put("seekDidNotRestart",m.player.currentPosition>=175000).put("indexedSeekable",m.player.isCurrentMediaItemSeekable)};Thread.sleep(1800);runOnMainSync{check(m.player.currentPosition>180000&&m.player.isCurrentMediaItemSeekable);m.seek051(60000)};UiQa050.await(45000){var ok=false;runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition in 60001..80000};ok};runOnMainSync{result.put("backwardSeek",true);m.select(m.currentCandidate!!,120000,false)};UiQa050.await(45000){var ok=false;runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition in 120001..140000};ok};runOnMainSync{result.put("sameSourceResumeAtTwoMinutes",true)};UiQa050.shot(this,"qa051-odyssey-seek.png");val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return}finally{m.db.prefs.edit().apply{prior.forEach{(s,v)->putBoolean("site_${s.key}",v)}}.commit()}
            }
            if(args.getString("mode")=="ui050"){val data=UiQa050.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="stremio050"){
                val all=m.sites.all();val prior=all.associateWith{m.sites.enabled(it)};val h=all.first{it.type==8};m.db.prefs.edit().apply{all.forEach{putBoolean("site_${it.key}",it.key==h.key)}}.commit()
                try{val work=Work("qa-hdhub","肖申克的救赎","movie","1994",facts=WorkFacts(imdbId="tt0111161"));runOnMainSync{m.play(work,"正片",true)};UiQa050.await(150000){var ok=false;runOnMainSync{ok=m.hasRenderedFrame()&&m.player.currentPosition>2000};ok};runOnMainSync{result.put("firstFrame",m.hasRenderedFrame()).put("height",m.player.videoSize.height).put("variants",m.candidates.size).put("sitePreserved",m.currentCandidate?.site?.key==h.key)};UiQa050.shot(this,"qa050-hdhub-play.png");val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return}finally{m.db.prefs.edit().apply{prior.forEach{(s,v)->putBoolean("site_${s.key}",v)}}.commit()}
            }
            if(args.getString("mode")=="livefixture050"){
                val url=args.getString("mediaUrl")!!;val entry=LiveEntry050(Site("qa-live","QA直播","https://localhost",1,searchable=false),Vod("qa-match","沙特 VS 韩国 · 中文解说","","","",listOf(Road("中文",listOf(Episode("原生",url))),Road("English",listOf(Episode("1080P",url))))));runOnMainSync{m.openLive050(entry);m.playLive050(entry)};UiQa050.await(20000){var ok=false;runOnMainSync{ok=m.hasRenderedFrame()&&m.candidates.size==2};ok};runOnMainSync{check(m.current?.provider=="live");check(m.episodeNames.all{!it.contains("第01集")});check(m.total==2);result.put("liveFirstFrame",true).put("allLiveLinesVisible",m.episodeNames.size==2).put("noVodSiteSearch",m.siteStates.isEmpty())};runOnMainSync{m.back()};Thread.sleep(400);check(InputQa.hasLabel(this,"全部直播线路"));runOnMainSync{check(m.screen=="detail"&&m.detail?.provider=="live")};result.put("backToMatchDetail",true);UiQa050.shot(this,"qa050-live-detail.png");val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="ui040"){val data=UiQa040.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return}
            if(args.getString("mode")=="ui030"){
                val data=UiQa030.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode") in listOf("playerui022","playerui030","playerui040","playerui051","faultseek051")){
                runOnMainSync{m.current=Work("qa-player-controls","播放器输入验收");m.select(Candidate(Site("qa-local","QA media fixture","http://127.0.0.1",1),"QA",Episode("正片",args.getString("mediaUrl")!!),mimeType="video/mp4"))}
                val start=System.currentTimeMillis();var frame=false
                while(System.currentTimeMillis()-start<30000){Thread.sleep(500);runOnMainSync{frame=m.hasRenderedFrame()&&m.player.currentPosition>2000};if(frame)break};check(frame)
                if(args.getString("mode")=="playerui051"){result.put("interaction",UiQa051.player(this,m,activity!!));val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return}
                if(args.getString("mode")=="faultseek051"){runBlocking{Net.text(args.getString("armUrl")!!)};val first=m.currentCandidate!!;runOnMainSync{m.candidates=listOf(first,first.copy(road="备用",episode=first.episode.copy(url=args.getString("fallbackUrl")!!)));m.seek051(35000)};UiQa050.await(60000){var ok=false;runOnMainSync{ok=m.currentCandidate?.road=="备用"&&m.hasRenderedFrame()&&m.player.currentPosition>=33000};ok};runOnMainSync{result.put("seekFailureUsesFallback",m.currentCandidate?.road=="备用").put("position",m.player.currentPosition).put("targetPreserved",m.player.currentPosition>=33000)};val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return}
                result.put("controls",InputQa.player(this,m));if(args.getString("mode")=="playerui040")result.put("backgroundService",UiQa040.player(this,m,activity!!));if(args.getString("mode")=="playerui030")result.put("remoteAndLifecycle",UiQa030.player(this,m,activity!!))
                uiAutomation.takeScreenshot()?.let{b->File(targetContext.getExternalFilesDir(null),"qa-player-ui.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
                val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="remote022"){
                val data=RemoteFlowQa.run(this,m);val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="privateplay022"){
                runBlocking{m.db.secrets.save("source_config",Net.text(args.getString("configUrl")!!))}
                val all=m.sites.all();val prior=all.associateWith{m.sites.enabled(it)};m.db.prefs.edit().apply{all.forEach{putBoolean("site_${it.key}",it.api=="csp_AppgzGuard")}}.commit()
                try{
                    val indexDir=File(targetContext.filesDir,"spider-index");val indexBefore=indexDir.listFiles().orEmpty().associate{it.name to it.lastModified()}
                    val w=Work("1292052","肖申克的救赎",year="1994")
                    runOnMainSync{m.play(w,"正片",true)};val start=System.currentTimeMillis();var frame=false
                    while(System.currentTimeMillis()-start<120000){Thread.sleep(1000);var failed=false;runOnMainSync{frame=m.hasRenderedFrame()&&m.player.videoSize.height>0&&m.player.currentPosition>1500;failed=m.total>0&&m.checked>=m.total&&!m.searching&&m.candidates.isEmpty()};if(frame||failed)break}
                    if(frame)Thread.sleep(7000)
                    runOnMainSync{result.put("privateOnly",true).put("firstFrame",frame).put("height",m.player.videoSize.height).put("position",m.player.currentPosition).put("checked",m.checked).put("total",m.total).put("candidates",m.candidates.size)}
                    result.put("states",org.json.JSONArray(m.siteStates.map{org.json.JSONObject().put("apiClass",it.site.api).put("state",it.text)}))
                    if(frame&&args.getString("playerUi")=="1")result.put("playerUi",InputQa.player(this,m))
                    uiAutomation.takeScreenshot()?.let{b->File(targetContext.getExternalFilesDir(null),"qa-private-play.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
                    if(frame){
                        runOnMainSync{m.saveProgress()};Thread.sleep(600)
                        val saved=m.db.last(w)!!;runOnMainSync{m.stop();m.play(w)}
                        val until=System.currentTimeMillis()+90000;var resumed=false
                        while(System.currentTimeMillis()<until){Thread.sleep(500);runOnMainSync{resumed=m.hasRenderedFrame()&&m.player.currentPosition>=saved.position};if(resumed)break}
                        runOnMainSync{result.put("resumeRestored",resumed).put("resumeSitePreserved",m.currentCandidate?.site?.key==saved.siteKey).put("resumeHeight",m.player.videoSize.height)}
                    }
                    result.put("verifiedModuleCacheReused",indexBefore.isNotEmpty()&&indexBefore.all{(name,time)->File(indexDir,name).lastModified()==time})
                    File(targetContext.filesDir,"qa-private-play.json").writeText(result.toString(2));val out=Bundle();out.putString("result",result.toString());finish(if(frame)Activity.RESULT_OK else Activity.RESULT_CANCELED,out);return
                }finally{m.db.prefs.edit().apply{prior.forEach{(s,enabled)->putBoolean("site_${s.key}",enabled)}}.commit()}
            }
            if(args.getString("mode")=="input"){
                val data=InputQa.run(this,activity as MainActivity,m);File(targetContext.filesDir,"qa-input.json").writeText(data.toString(2));val out=Bundle();out.putString("result",data.toString());finish(Activity.RESULT_OK,out);return
            }
            if(args.getString("mode")=="v2-ui"){
                fun shot(name:String){uiAutomation.takeScreenshot()?.let{bitmap->File(targetContext.filesDir,name).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}}
                check(!m.tmdbConfigured);Thread.sleep(10000);shot("qa-v2-home.png")
                repeat(4){sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_LEFT)};repeat(16){sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_UP)};sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_CENTER);Thread.sleep(1500)
                result.put("drawerOpenedByDpadScreenshot","qa-v2-drawer.png");shot("qa-v2-drawer.png");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);Thread.sleep(1500);shot("qa-v2-drawer-closed.png")
                val rows=runBlocking{m.metadata.search("西游记")};val w=rows.firstOrNull{it.year=="1986"}?.copy(kind="tv")?:Work("2156663","西游记","tv","1986")
                runOnMainSync{m.open(w)};val start=System.currentTimeMillis();var ready=false
                while(System.currentTimeMillis()-start<100000){Thread.sleep(500);runOnMainSync{ready=m.details.catalog.value.groups.isNotEmpty()};if(ready)break}
                check(ready){"No independently loaded catalog"};Thread.sleep(2500)
                runOnMainSync{result.put("detailDidNotStartPlayback",m.current==null&&m.player.mediaItemCount==0);check(m.current==null&&m.player.mediaItemCount==0);result.put("noTmdbKey",!m.tmdbConfigured)}
                val groups=m.details.catalog.value.groups;result.put("catalogGroups",groups.size).put("catalogEpisodes",groups.sumOf{it.road.episodes.size});shot("qa-v2-detail.png")
                runOnMainSync{if(w.key !in m.user.favorites)m.toggleFavorite(w);m.setWatchState(w,WatchState.PLANNED)};Thread.sleep(1000);check(m.user.favorites.contains(w.key));check(m.user.states[w.key]==WatchState.PLANNED);result.put("favoriteStateImmediate",true)
                repeat(10){sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN)};Thread.sleep(1500);shot("qa-v2-catalog.png")
                val episode=groups.first().road.episodes.first().name;runOnMainSync{m.play(w,episode,true)};val playStart=System.currentTimeMillis();var frame=false
                while(System.currentTimeMillis()-playStart<100000){Thread.sleep(500);runOnMainSync{frame=m.hasRenderedFrame()&&m.player.currentPosition>1500};if(frame)break}
                result.put("realPlaybackFirstFrame",frame);runOnMainSync{result.put("height",m.player.videoSize.height).put("position",m.player.currentPosition).put("checked",m.checked).put("total",m.total).put("candidateCount",m.candidates.size);if(frame)m.saveProgress();m.back();result.put("backToDetail",m.screen=="detail");check(m.screen=="detail");check(m.player.mediaItemCount>0);result.put("browseRetainsPlayback",true)}
                Thread.sleep(1200);runOnMainSync{m.back();result.put("backToHome",m.screen=="home")};shot("qa-v2-return-home.png")
                File(targetContext.filesDir,"qa-v2-ui.json").writeText(result.toString(2));val out=Bundle();out.putString("result",result.toString());finish(if(frame)Activity.RESULT_OK else Activity.RESULT_CANCELED,out);return
            }
            if(args.getString("mode")=="home"){
                val timeout=System.currentTimeMillis()+150000;var refreshing=true
                while(refreshing&&System.currentTimeMillis()<timeout){Thread.sleep(500);runOnMainSync{refreshing=m.syncingHome}}
                result.put("channels",org.json.JSONObject(m.home.channelIds())).put("status",m.homeStatus)
                val prior=m.db.watches().firstOrNull{it.work.title=="肖申克的救赎"}
                if(prior!=null){val actual=runBlocking{m.metadata.details(Work("1292052","肖申克的救赎","movie","1994"))};val w=prior.copy(work=actual);m.db.save(w);m.home.publishWatch(w);val ids=m.home.watchIds();result.put("watchNextInserted",ids.containsKey("continue|${actual.key}"));runOnMainSync{m.handleHomeLink(HomeCatalog.intent(targetContext,actual).data!!);result.put("detailDeepLink",m.detail?.key==actual.key&&m.screen=="detail")};m.home.publishWatch(w.copy(done=true));result.put("watchNextCompletedRemoved",!m.home.watchIds().containsKey("continue|${actual.key}"))}
                File(targetContext.filesDir,"qa-home.json").writeText(result.toString(2));val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_OK,out);return
            }
            val rows=runBlocking{m.metadata.feed("douban","tv","热门",1)};check(rows.size>0);check(rows.any{it.poster.startsWith("https://")});result.put("doubanFeed",rows.size)
            val prior=m.db.favorites().map{it.key};val fixture=Work("qa-only","独立QA收藏");m.db.toggleFavorite(fixture);check(m.db.favorite(fixture));m.db.toggleFavorite(fixture);check(m.db.favorites().map{it.key}==prior);result.put("favoritePersistence",true)
            val title=args.getString("title","西游记")!!;val year=args.getString("year","1986")!!
            val kind=args.getString("kind","tv")!!;val work=Work("qa-$title",title,kind,year)
            runOnMainSync{m.play(work,"第01集",true)}
            val start=System.currentTimeMillis();var frame=false
            while(System.currentTimeMillis()-start<150000){Thread.sleep(1000);runOnMainSync{frame=m.hasRenderedFrame()&&m.player.videoSize.height>0&&m.player.currentPosition>1500};if(frame)break}
            if(frame){
                runOnMainSync{m.player.seekTo(60000)};Thread.sleep(12000)
                uiAutomation.takeScreenshot()?.let{bitmap->File(targetContext.filesDir,"qa-play.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}}
                val latch=java.util.concurrent.CountDownLatch(1)
                runOnMainSync{
                    fun find(v:View):SurfaceView?=if(v is SurfaceView)v else if(v is ViewGroup)(0 until v.childCount).firstNotNullOfOrNull{find(v.getChildAt(it))}else null
                    val surface=find(activity!!.window.decorView)
                    if(surface!=null&&surface.width>0){val bitmap=android.graphics.Bitmap.createBitmap(surface.width,surface.height,android.graphics.Bitmap.Config.ARGB_8888);PixelCopy.request(surface,bitmap,{status->result.put("pixelCopy",status);if(status==PixelCopy.SUCCESS){File(targetContext.filesDir,"qa-video.png").outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};latch.countDown()},android.os.Handler(android.os.Looper.getMainLooper()))}else latch.countDown()
                };latch.await(8,java.util.concurrent.TimeUnit.SECONDS)
            }
            runOnMainSync{result.put("firstFrame",frame).put("height",m.player.videoSize.height).put("position",m.player.currentPosition).put("checked",m.checked).put("total",m.total).put("candidates",m.candidates.size).put("site",m.currentCandidate?.site?.name.orEmpty());m.saveProgress();m.back();result.put("backToDetails",m.screen=="detail");m.screen="home";result.put("backgroundRetained",m.player.mediaItemCount>0)}
            if(frame){check(m.db.last(work)?.position?:0>0);result.put("progressSaved",true)
                val saved=m.db.last(work)!!;runOnMainSync{m.play(work)};val retryStart=System.currentTimeMillis();var resumed=false
                while(System.currentTimeMillis()-retryStart<90000){Thread.sleep(1000);runOnMainSync{resumed=m.hasRenderedFrame()&&m.player.currentPosition>=saved.position};if(resumed)break}
                runOnMainSync{result.put("resumeRestored",resumed).put("resumeSitePreserved",m.currentCandidate?.site?.key==saved.siteKey).put("resumeHeight",m.player.videoSize.height)}
            }
            result.put("states",org.json.JSONArray(m.siteStates.map{JSONObject().put("site",it.site.name).put("state",it.text)}))
            File(targetContext.filesDir,"qa-result.json").writeText(result.toString(2))
            val output=Bundle();output.putString("result",result.toString());finish(if(frame)Activity.RESULT_OK else Activity.RESULT_CANCELED,output)
        }catch(e:Throwable){uiAutomation.takeScreenshot()?.let{b->File(targetContext.getExternalFilesDir(null),"qa-failure.png").outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)}};result.put("failure",e.javaClass.simpleName+": "+e.message+" @ "+e.stackTrace.take(3).joinToString(" <- "){it.className.substringAfterLast('.')+":"+it.lineNumber});File(targetContext.filesDir,"qa-result.json").writeText(result.toString(2));val out=Bundle();out.putString("result",result.toString());finish(Activity.RESULT_CANCELED,out)}
        finally{if(args.getString("mode") !in listOf("ui053","anime053","live054","liveprobe054","ui054","authfixture054","authreal054","metadata055","search055","episodes055","credits055","cache055","ui056","layout056","catalog056","sources056","animeplay056","layout057","history057","danmaku057","douban057","anime2017","hdhubDirect057","player057","episodes060","settings060","notice060"))runOnMainSync{model?.stop();activity?.finish()}}
    }
}

