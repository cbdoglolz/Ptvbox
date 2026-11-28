package com.cbdog.ptvbox

import android.app.Instrumentation
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/** Debug-only behavioral checks; use an explicitly selected isolated emulator. */
object FeaturesQa103 {
    suspend fun transport(i:Instrumentation,m:PtvModel):JSONObject=withContext(Dispatchers.IO){
        val old=m.db.secrets.read("proxy")
        val socket=java.net.ServerSocket(0,4,java.net.InetAddress.getByName("127.0.0.1"))
        val requests=java.util.concurrent.atomic.AtomicInteger()
        val data=byteArrayOf(0,0,0,24)+"ftypisom".toByteArray()+byteArrayOf(0,0,0,0)
        val worker=Thread{while(!socket.isClosed)try{socket.accept().use{peer->peer.soTimeout=3000;val reader=peer.getInputStream().bufferedReader();reader.readLine();while(!reader.readLine().isNullOrBlank()){};requests.incrementAndGet();peer.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Type: video/mp4\r\nContent-Length: ${data.size}\r\nConnection: close\r\n\r\n").toByteArray()+data)}}catch(_:Exception){}}
        worker.isDaemon=true;worker.start()
        try{
            m.db.secrets.save("proxy","http://127.0.0.1:${socket.localPort}");NetworkPolicy.apply(i.targetContext)
            val proxyUrl="http://127.0.0.2:${socket.localPort}/proxy-fixture.mp4"
            check(InternationalHttp103.bytes(proxyUrl,limit=4096,truncate=true,media=true).contentEquals(data))
            check(!InternationalHttp103.directFor(proxyUrl))
            m.db.secrets.save("proxy","http://127.0.0.1:9");NetworkPolicy.apply(i.targetContext)
            val directUrl="http://127.0.0.1:${socket.localPort}/direct-fixture.mp4"
            check(InternationalHttp103.bytes(directUrl,limit=4096,truncate=true,media=true).contentEquals(data))
            check(InternationalHttp103.directFor(directUrl))
            JSONObject().put("configuredProxyFallback",true).put("directWorksWithUnavailableProxy",true).put("verifiedRequests",requests.get())
        }finally{socket.close();m.db.secrets.save("proxy",old);NetworkPolicy.apply(i.targetContext)}
    }
    private fun render(i:Instrumentation,a:MainActivity,body:@Composable ()->Unit){i.runOnMainSync{a.setContent{CinemaTheme(false){Box(Modifier.fillMaxSize().background(LocalCinema.current.background)){body()}}}};Thread.sleep(500)}
    private fun value(i:Instrumentation,read:()->Boolean):Boolean{var result=false;i.runOnMainSync{result=read()};return result}
    private fun drag(i:Instrumentation,rect:android.graphics.Rect,dx:Float,dy:Float){
        val start=android.os.SystemClock.uptimeMillis();val x=rect.exactCenterX();val y=rect.exactCenterY()
        fun send(action:Int,part:Float){val props=arrayOf(android.view.MotionEvent.PointerProperties().apply{id=0;toolType=android.view.MotionEvent.TOOL_TYPE_FINGER});val coords=arrayOf(android.view.MotionEvent.PointerCoords().apply{this.x=x+dx*part;this.y=y+dy*part;pressure=1f;size=1f});val e=android.view.MotionEvent.obtain(start,android.os.SystemClock.uptimeMillis(),action,1,props,coords,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);check(i.uiAutomation.injectInputEvent(e,true));e.recycle()}
        send(android.view.MotionEvent.ACTION_DOWN,0f);for(n in 1..14){Thread.sleep(20);send(android.view.MotionEvent.ACTION_MOVE,n/14f)};send(android.view.MotionEvent.ACTION_UP,1f);Thread.sleep(350)
    }
    fun ui(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject {
        val metrics=i.targetContext.resources.displayMetrics
        val output=JSONObject().put("widthDp",metrics.widthPixels/metrics.density).put("heightDp",metrics.heightPixels/metrics.density);val oldSites=m.availableSites;val oldEnabled=m.enabledSiteKeys;val oldCredit=m.creditPage055
        val site=Site("qa103-http","测试HTTP","http://127.0.0.1:9",1)
        try{
            i.runOnMainSync{m.availableSites=listOf(site);m.enabledSiteKeys=setOf(site.key)}
            render(i,a){Sites040(m)}
            InputQa.clickLabel(i,"分类选源",idle=false);UiQa050.await{InputQa.hasLabel(i,"恢复本类默认")};UiQa050.shot(i,"qa103-sites-touch-route.png");UiQa054.key(i,KeyEvent.KEYCODE_BACK)
            InputQa.clickLabel(i,"更新 / 回退",idle=false);UiQa050.await{InputQa.hasLabel(i,"检查私人订阅更新")};UiQa050.shot(i,"qa103-sites-touch-updates.png");UiQa054.key(i,KeyEvent.KEYCODE_BACK)
            InputQa.clickLabel(i,"检测当前",idle=false);UiQa050.await{value(i){m.stationTotal==1}};output.put("headerFingerTaps",true)
            InputQa.clickLabel(i,"分类选源",mouse=true,idle=false);UiQa050.await{InputQa.hasLabel(i,"恢复本类默认")};UiQa054.key(i,KeyEvent.KEYCODE_BACK);output.put("headerMouseTap",true)
            fun focus(n:android.view.accessibility.AccessibilityNodeInfo?):Boolean {if(n==null)return false;if(n.contentDescription?.toString()=="分类选源"){var target:android.view.accessibility.AccessibilityNodeInfo?=n;while(target!=null&&!target.isFocusable)target=target.parent;if(target!=null)return target.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS)};for(k in 0 until n.childCount)if(focus(n.getChild(k)))return true;return false}
            check(focus(i.uiAutomation.rootInActiveWindow));UiQa054.key(i,KeyEvent.KEYCODE_DPAD_CENTER);UiQa050.await{InputQa.hasLabel(i,"恢复本类默认")};UiQa054.key(i,KeyEvent.KEYCODE_BACK);output.put("headerDpadConfirm",true)
            val w=Work("fixture","原作品");val page=CreditPage055(w,PersonCredit("1","测试人物"))
            val poster="android.resource://${i.targetContext.packageName}/${R.drawable.brand}"
            page.rows=(1..15).map{Work("credit$it","作品$it",poster=poster)}
            i.runOnMainSync{m.creditPage055=page}
            render(i,a){CreditWorksPage055(m)}
            val bounds=(1..5).map{UiQa054.rect(i,"作品$it")?:error("Missing credit $it")}
            check(bounds.all{kotlin.math.abs(it.top-bounds[0].top)<4})
            UiQa054.rect(i,"作品6")?.let{check(it.top>bounds[0].top+30)}
            UiQa050.shot(i,"qa103-credits-five-columns.png");output.put("creditColumns",5)
            i.runOnMainSync{m.current=Work("download-qa","下载测试")}
            // The download action must never stop or replace the currently playing session.
            val group=CatalogGroup("download",site,"Fixture",Road("10Gbps Download Only",listOf(Episode("正片","https://example.org/download"))))
            val monitor=i.addMonitor(android.content.IntentFilter(android.content.Intent.ACTION_VIEW).apply{addDataScheme("http");addDataScheme("https")},android.app.Instrumentation.ActivityResult(0,null),true)
            render(i,a){Column{SourceAction103(m,Work("download","Download"),group,group.road.episodes.single())}}
            UiQa050.shot(i,"qa103-download-action.png")
            try{InputQa.clickLabel(i,"下载",idle=false);Thread.sleep(250);check(monitor.hits>0);check(value(i){m.current?.id=="download-qa"});output.put("downloadOpensBrowserWithoutReplacingPlayback",true)}finally{i.removeMonitor(monitor)}
            return output
        }finally{i.runOnMainSync{m.stationCheck?.cancel();m.stationChecking=false;m.availableSites=oldSites;m.enabledSiteKeys=oldEnabled;m.creditPage055=oldCredit;m.stop()}}
    }
    suspend fun network(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject {
        val out=JSONObject()
        fun stage(name:String){out.put("stage",name);java.io.File(i.targetContext.getExternalFilesDir(null),"qa103-network-progress.json").writeText(out.toString(2));android.util.Log.i("PtvQa103",name)}
        val previousProxy=m.db.secrets.read("proxy")
        m.db.secrets.save("proxy","");NetworkPolicy.apply(i.targetContext)
        try{
        stage("bilibili-search")
        val work=Work("1292052","肖申克的救赎","movie","1994",facts=WorkFacts(imdbId="tt0111161"))
        val service=DanmakuService053(m.db.secrets)
        val matches=withContext(Dispatchers.IO){service.bilibili(MediaContext053(work,null))}
        check(matches.isNotEmpty()){"No movie danmaku match"}
        stage("bilibili-comments")
        val comments=withContext(Dispatchers.IO){service.comments(matches.first().id)}
        check(comments.size>1000){"Movie comments missing: ${comments.size}"}
        out.put("movieDanmakuProvider",if(matches.first().id.startsWith("bili:"))"Bilibili"else"Dandanplay").put("movieDanmakuCount",comments.size)
        stage("hdhub-lookup")
        val hd=m.sites.all().first{it.key=="nuvio_hdhub"}
        val vod=Stremio050.lookup(hd,work,"正片",m.metadata)
        out.put("hdhubVariants",vod.roads.size).put("downloadOnly",vod.roads.count{SourceDelivery103.downloadOnly(it.name)})
        stage("hdhub-media-probe")
        var playable:Candidate?=null
        for(road in vod.roads.filterNot{SourceDelivery103.downloadOnly(it.name)}.sortedBy{ResourceLabels040.bytes(it.name)}.take(5)){
            try{playable=withTimeout(22000){m.sites.probe(hd,road,road.episodes.first())};break}catch(_:Exception){}
        }
        check(playable!=null){"HDHUB has no verified media route"}
        out.put("hdhubMediaDirect",InternationalHttp103.directFor(playable!!.episode.url))
        stage("hdhub-first-frame")
        i.runOnMainSync{m.current=work;m.select(playable!!,0);m.player.volume=0f}
        UiQa050.await(45000){value(i){m.hasRenderedFrame()&&m.player.currentPosition>1000}}
        UiQa050.await(60000){value(i){m.addons.danmaku.size==9600}}
        UiQa054.key(i,KeyEvent.KEYCODE_DPAD_DOWN)
        UiQa050.shot(i,"qa103-hdhub-first-frame.png")
        i.runOnMainSync{out.put("hdhubFirstFrame",true).put("hdhubHeight",m.player.videoSize.height).put("automaticMovieDanmaku",m.addons.danmaku.size)}
        i.runOnMainSync{m.stop()}
        stage("onetouch-search-detail")
        val rows=OneTouch103.search("Queen of Tears");check(rows.isNotEmpty())
        val detail=OneTouch103.detail(rows.first());check(detail.roads.isNotEmpty())
        val one=m.sites.all().first{it.key=="nuvio_onetouchtv"};val road=detail.roads.first();val ep=road.episodes.first();check(ep.name=="第01集")
        stage("onetouch-probe")
        val candidate=m.sites.probe(one,road,ep)
        stage("onetouch-first-frame")
        i.runOnMainSync{m.current=Work("onetouch","泪之女王","tv","2024");m.select(candidate,0);m.player.volume=0f}
        UiQa050.await(45000){value(i){m.hasRenderedFrame()&&m.player.currentPosition>1000}}
        UiQa050.shot(i,"qa103-onetouch-first-frame.png");i.runOnMainSync{out.put("oneTouchEpisodes",road.episodes.size).put("oneTouchFirstFrame",true).put("oneTouchHeight",m.player.videoSize.height);m.stop()}
        stage("complete");return out
        }finally{m.db.secrets.save("proxy",previousProxy);NetworkPolicy.apply(i.targetContext)}
    }
    suspend fun mapping(m:PtvModel):JSONObject {
        val context=object:android.content.ContextWrapper(m.getApplication<android.app.Application>()){
            override fun getSharedPreferences(name:String,mode:Int)=super.getSharedPreferences("qa103-mapping-"+name,mode)
        }
        val db=Library(context,"qa103-mapping.db")
        val fixture=Metadata(db){path,_->
            val id=path.split('/').getOrNull(1).orEmpty()
            val dandadan=id=="240411"
            if(path.contains("/season/")){
                val season=path.substringAfterLast('/').toInt()
                val eps=if(dandadan)(1..12).map{n->EpisodeMeta("d-$season-$n",season,n,"第${n}集","番剧简介","","8",100,java.time.LocalDate.of(if(season==1)2024 else 2025,if(season==1)10 else 7,3).plusWeeks((n-1).toLong()).toString())}
                else (1..72).map{n->val block=(n-1)/24;EpisodeMeta("k-$n",1,n,"第${n}集","番剧简介","","8",100,java.time.LocalDate.of(2023+block,10,1).plusWeeks(((n-1)%24).toLong()).toString())}
                JSONObject().put("episodes",JSONArray(eps.map{e->JSONObject().put("id",e.id).put("season_number",e.season).put("episode_number",e.number).put("name",e.title).put("overview",e.overview).put("air_date",e.airDate)}))
            }else JSONObject().put("id",id).put("name",if(dandadan)"胆大当"else"药屋少女的呢喃").put("original_name","").put("seasons",JSONArray((1..if(dandadan)2 else 1).map{n->JSONObject().put("season_number",n).put("name","第${n}季").put("episode_count",if(dandadan)12 else 72).put("air_date",if(dandadan)"202${n+3}-10-03"else"2023-10-01")}))
        }
        val out=JSONObject().put("metadataEvidence","controlled TMDB-shaped response; live AniList identity/status and bundled mapping IDs");val tests=JSONArray()
        try{
        for(id in listOf("171018","185660","198966","161645","176301","195516")){
            val query="query { Media(id:$id,type:ANIME){id title{native romaji english}synonyms episodes format status startDate{year month day}coverImage{large}bannerImage} }"
            val item=JSONObject(Net.postJson("https://graphql.anilist.co",JSONObject().put("query",query).toString())).getJSONObject("data").getJSONObject("Media")
            val page=JSONObject().put("data",JSONObject().put("Page",JSONObject().put("media",JSONArray().put(item)).put("pageInfo",JSONObject().put("hasNextPage",false))))
            val work=AnimeSeason056.parse(page.toString()).first.single().work
            val normalized=fixture.identifyAnime103(work)
            val series=fixture.episodeSeries(normalized)
            check(series==null||series.episodes.all{it.season==work.season})
            check(series==null||series.episodes.firstOrNull()?.number==1)
            if(id=="198966"){check(work.season==3&&series?.episodes?.none{it.airDate.startsWith("2024")}!=false);check(fixture.identifyAnime103(work.copy(season=1,catalogSeason=1)).season==3)}
            tests.put(JSONObject().put("anilist",id).put("season",work.season).put("status",item.optString("status")).put("episodes",series?.episodes?.size?:0).put("firstDate",series?.episodes?.firstOrNull()?.airDate.orEmpty()).put("mapped",fixture.animeMapping103(work)!=null))
        }
        return out.put("animeCoordinateCases",tests)
        }finally{db.close()}
    }
}
