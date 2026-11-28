package com.cbdog.ptvbox

import android.app.Instrumentation
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject

internal object SearchQa103 {
    private fun render(i:Instrumentation,a:MainActivity,body:@Composable ()->Unit){i.runOnMainSync{a.setContent{CinemaTheme(false){Box(Modifier.fillMaxSize().background(LocalCinema.current.background).padding(12.dp)){body()}}}};Thread.sleep(350)}
    private fun focus(i:Instrumentation,label:String){
        fun walk(n:AccessibilityNodeInfo?):Boolean{if(n==null)return false
            if(n.text?.toString()==label||n.contentDescription?.toString()==label){var t:AccessibilityNodeInfo?=n;while(t!=null&&!t.isFocusable)t=t.parent;return t?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)==true}
            for(k in 0 until n.childCount)if(walk(n.getChild(k)))return true
            return false
        }
        check(walk(i.uiAutomation.rootInActiveWindow)){"Cannot focus $label"};Thread.sleep(150)
    }
    private fun focused(i:Instrumentation,label:String):Boolean{
        fun contains(n:AccessibilityNodeInfo?):Boolean{if(n==null)return false;if(n.text?.toString()==label||n.contentDescription?.toString()==label)return true;for(k in 0 until n.childCount)if(contains(n.getChild(k)))return true;return false}
        fun walk(n:AccessibilityNodeInfo?):Boolean{if(n==null)return false;if(n.isFocused&&contains(n))return true;for(k in 0 until n.childCount)if(walk(n.getChild(k)))return true;return false}
        return walk(i.uiAutomation.rootInActiveWindow)||i.uiAutomation.windows.any{walk(it.root)}
    }
    fun search(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
        val dm=i.targetContext.resources.displayMetrics;val out=JSONObject().put("widthDp",dm.widthPixels/dm.density).put("heightDp",dm.heightPixels/dm.density);val oldHistory=m.searchHistory
        fun stage(s:String){java.io.File(i.targetContext.getExternalFilesDir(null),"qa103-search-stage.txt").writeText(s)}
        try{
            i.runOnMainSync{m.stop();m.enterSearch055();m.searchHistory=emptyList()}
            render(i,a){TvSearch055(m){}}
            UiQa050.await{InputQa.hasLabel(i,"A")};stage("keys")
            check(SearchText055.keys.all{InputQa.hasLabel(i,it)}){"Keyboard paginated/clipped"}
            focus(i,"远程搜索");UiQa054.key(i,20);check(focused(i,"A"));out.put("remoteDownToKeyboard",true)
            focus(i,"语音搜索");UiQa054.key(i,20);check(focused(i,"A"));out.put("voiceDownToKeyboard",true)
            for(k in listOf("W","J","Z","D","Y"))InputQa.clickLabel(i,k,mouse=k=="J",idle=false)
            i.runOnMainSync{check(m.query=="WJZDY"){"Typed query: ${m.query}"}}
            stage("suggestions");UiQa050.await(10000){InputQa.hasLabel(i,"玩具总动员5")}
            // Right edge of every keyboard row enters the suggestions, and left returns to that key.
            val edge=UiQa054.rect(i,"指定搜索源 · 影视资料")!!.right
            val first=runBlocking{m.searchAssist055.suggest("WJZDY",emptyMap())}.names.mapNotNull{name->UiQa054.rect(i,name)?.takeIf{it.left>edge}?.let{name to it}}.sortedWith(compareBy<Pair<String,android.graphics.Rect>>{it.second.top}.thenBy{it.second.left}).first().first
            for(k in SearchText055.keys.chunked(8).map{it.last()}){
                focus(i,k);UiQa054.key(i,22);check(focused(i,first)){"Right from $k did not enter hint $first; node=${i.uiAutomation.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)}"}
                UiQa054.key(i,21);UiQa050.await(3000){focused(i,k)}
            }
            out.put("allKeyboardRowsReachHintsAndReturn",true);UiQa050.shot(i,"qa103-c3-search-hints.png")
            focus(i,"H");UiQa054.key(i,22);UiQa054.key(i,23)
            stage("submit");UiQa050.await(30000){var ready=false;i.runOnMainSync{ready=!m.busy&&m.searchResults.isNotEmpty()};ready}
            UiQa050.await{focused(i,"返回联想")};out.put("dpadHintSubmitsCatalogSearch",true)
            focus(i,"H");UiQa054.key(i,22);check(focused(i,"返回联想"));UiQa054.key(i,23)
            UiQa050.await{InputQa.hasLabel(i,"联想搜索")};out.put("resultsReturnToHints",true)
            InputQa.clickLabel(i,"清空输入",idle=false);UiQa050.await{InputQa.hasLabel(i,"热门搜索")}
            i.runOnMainSync{m.editQuery055("来自深渊")};UiQa050.await(10000){InputQa.hasLabel(i,"来自深渊")}
            InputQa.clickLabel(i,"空格",idle=false);InputQa.clickLabel(i,"删除",idle=false)
            i.runOnMainSync{check(m.query=="来自深渊")};out.put("chineseInputSpaceDeleteClear",true)
            stage("rapid edits");i.runOnMainSync{m.editQuery055("WJZDY");m.editQuery055("XSK");m.editQuery055("来自深渊")}
            UiQa050.await(10000){InputQa.hasLabel(i,"来自深渊")};Thread.sleep(2200)
            check(!InputQa.hasLabel(i,"玩具总动员5"));out.put("obsoleteHintsDoNotReplaceCurrentQuery",true)
            val t=android.os.SystemClock.elapsedRealtime();runBlocking{m.searchAssist055.observe("来自深渊",emptyMap()).first()}
            out.put("warmHintFirstEmissionMs",android.os.SystemClock.elapsedRealtime()-t)
            stage("source selector")
            InputQa.clickLabel(i,"指定搜索源 · 影视资料",idle=false)
            UiQa050.await{InputQa.hasLabel(i,"豆瓣资料")};InputQa.clickLabel(i,"豆瓣资料",idle=false)
            UiQa050.await{InputQa.hasLabel(i,"指定搜索源 · 豆瓣资料")}
            InputQa.clickLabel(i,"搜索",idle=false)
            UiQa050.await(30000){var ready=false;i.runOnMainSync{ready=!m.busy&&m.searchResults.any{it.provider=="douban"&&it.title.contains("来自深渊")}};ready}
            out.put("providerSelectionSubmitsSearch",true)
            InputQa.clickLabel(i,"返回联想",idle=false);InputQa.clickLabel(i,"清空输入",idle=false)
            InputQa.clickLabel(i,"搜索",idle=false);i.runOnMainSync{check(!m.busy&&m.query.isBlank())}
            i.runOnMainSync{m.searchHistory=listOf("来自深渊")}
            UiQa050.await{InputQa.hasLabel(i,"来自深渊")};InputQa.clickLabel(i,"来自深渊",mouse=true,idle=false)
            UiQa050.await(30000){var ready=false;i.runOnMainSync{ready=!m.busy&&m.query=="来自深渊"&&m.searchResults.isNotEmpty()};ready}
            out.put("emptySubmitIgnoredAndHistoryMouseSearch",true)
            InputQa.clickLabel(i,"返回联想",idle=false)
            i.runOnMainSync{m.searchHistory=emptyList()}
            stage("QR")
            InputQa.clickLabel(i,"远程搜索",idle=false);UiQa050.await(15000){InputQa.hasLabel(i,"远程搜索二维码")}
            val qr=UiQa054.rect(i,"远程搜索二维码")!!;val widthDp=qr.width()/i.targetContext.resources.displayMetrics.density
            check(widthDp in 150f..160f);UiQa050.shot(i,"qa103-c3-search-qr.png");out.put("qrSizeDp",widthDp)
            UiQa054.key(i,4);check(!InputQa.hasLabel(i,"远程搜索二维码"));UiQa050.await{focused(i,"远程搜索")};out.put("qrDismissRestoresSearch",true)
            // Exercise the exact production voice content without pretending an emulator has a microphone service.
            var open by mutableStateOf(true)
            render(i,a){if(open)SearchVoiceDialog103("正在收听，请说出片名"){open=false}}
            UiQa050.await{InputQa.hasLabel(i,"正在收听，请说出片名")};UiQa050.shot(i,"qa103-c3-search-voice.png")
            InputQa.clickLabel(i,"关闭",idle=false);UiQa050.await{!InputQa.hasLabel(i,"语音搜索")}
            out.put("compactVoiceDialogTouchClose",true).put("speechServiceAvailable",android.speech.SpeechRecognizer.isRecognitionAvailable(i.targetContext))
            stage("complete");return out
        }catch(e:Exception){UiQa050.shot(i,"qa103-c3-search-failure.png");throw e}finally{i.runOnMainSync{m.searchHistory=oldHistory;m.stopRemoteConfig();m.enterSearch055()}}
    }
    fun related(i:Instrumentation,m:PtvModel):JSONObject=runBlocking{
        val out=JSONObject()
        val films=DoubanSearch055.search("来自深渊").filter{it.kind=="movie"}
        val film=films.firstOrNull{it.title.contains("深魂")}?:films.first()
        val enriched=film.copy(genres=listOf("动画"),original="メイドインアビス 深き魂の黎明")
        val rows=withTimeout(20000){m.metadata.moreRelated(enriched,1)}
        check(rows.any{it.kind=="tv"&&SeriesLinks103.matches(film,it)}){"Abyss movie has no TV link"}
        val tv=rows.first{it.kind=="tv"&&SeriesLinks103.matches(film,it)}
        check(tv.poster.isNotBlank()){ "Related TV poster missing" }
        out.put("liveRelatedTvPosterPresent",true)
        val reverse=withTimeout(20000){m.metadata.moreRelated(tv.copy(genres=listOf("动画")),1)}
        check(reverse.any{it.kind=="movie"&&SeriesLinks103.matches(tv,it)}){"TV has no movie link"}
        out.put("liveAbyssMovieToTv",true).put("liveAbyssTvToMovie",true).put("movieRecommendations",rows.size).put("tvRecommendations",reverse.size)
        val raw=JSONObject(Net.postJson("https://graphql.anilist.co",JSONObject().put("query",SeriesLinks103.relationQuery()).put("variables",JSONObject().put("id",100643)).toString()))
        val links=SeriesLinks103.parseRelations(raw)
        check(links.any{it.id=="97986"&&it.kind=="tv"})
        out.put("liveAniListExplicitFilmToTv",true).put("explicitRelations",links.size)
        val fixture=Metadata(m.db){path,_->
            if(path=="search/multi")JSONObject("""{"results":[{"id":103001,"media_type":"tv","name":"联动测试系列","first_air_date":"2020-01-01"},{"id":103002,"media_type":"person","name":"联动测试系列"}]}""")
            else throw java.io.IOException("Recommendation provider fixture offline")
        }
        val tmdbRows=fixture.moreRelated(Work("103000","联动测试系列：电影","movie",provider="tmdb"),1)
        check(tmdbRows.any{it.id=="103001"&&it.kind=="tv"&&it.provider=="tmdb"})
        check(tmdbRows.none{it.id=="103002"})
        out.put("tmdbMixedSearchRetainsTvKind",true).put("sameKindFailureDoesNotHideSeries",true)
        out
    }
    fun remote(i:Instrumentation,m:PtvModel):JSONObject{
        var view:android.webkit.WebView?=null
        i.runOnMainSync{m.stopRemoteConfig();m.enterSearch055();m.startRemoteConfig("127.0.0.1")}
        try{
            UiQa050.await(10000){m.remoteUrl.isNotBlank()}
            i.runOnMainSync{
                view=android.webkit.WebView(i.targetContext).apply{
                    settings.javaScriptEnabled=true
                    webViewClient=object:android.webkit.WebViewClient(){
                        override fun onReceivedSslError(v:android.webkit.WebView,h:android.webkit.SslErrorHandler,e:android.net.http.SslError){
                            val bytes=android.net.http.SslCertificate.saveState(e.certificate).getByteArray("x509-certificate")
                            val fingerprint=bytes?.let{java.security.MessageDigest.getInstance("SHA-256").digest(it).joinToString(":"){b->"%02X".format(b)}}
                            if(e.url.startsWith(m.remoteUrl.substringBefore('#'))&&fingerprint==m.remoteFingerprint)h.proceed()else h.cancel()
                        }
                    }
                    loadUrl(m.remoteUrl+"&page=search")
                }
            }
            fun js(code:String):String{val done=java.util.concurrent.CompletableFuture<String>();i.runOnMainSync{view!!.evaluateJavascript(code){done.complete(it)}};return done.get(8,java.util.concurrent.TimeUnit.SECONDS)}
            UiQa050.await(15000){js("document.querySelector('details[open] summary')?.textContent")=="\"远程搜索\""}
            check(js("document.getElementById('toggle').hidden && document.getElementById('stop').hidden")=="true")
            check(js("document.getElementById('query').closest('details').hidden")=="false")
            js("document.getElementById('query').value='来自深渊';document.getElementById('query').dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))")
            UiQa050.await(12000){var ok=false;i.runOnMainSync{ok=m.query=="来自深渊"&&m.searchRevision055>0};ok}
            UiQa050.await(30000){var ok=false;i.runOnMainSync{ok=!m.busy&&m.searchResults.any{it.title.contains("来自深渊")}};ok}
            return JSONObject().put("searchOnlyPhonePortal",true).put("liveHttpsEnterSubmitsToTv",true).put("remoteCatalogResults",true).put("localCertificatePinned",true)
        }finally{i.runOnMainSync{view?.destroy();m.stopRemoteConfig();m.enterSearch055()}}
    }
    fun relatedUi(i:Instrumentation,m:PtvModel,a:MainActivity):JSONObject{
        val film=runBlocking{DoubanSearch055.search("来自深渊").first{it.kind=="movie"&&it.title.contains("深魂")}}
        try{
            i.runOnMainSync{m.open(film)}
            render(i,a){CinemaDetail040(m)}
            UiQa050.await(90000){m.details.related.value.any{it.kind=="tv"&&SeriesLinks103.matches(film,it)}}
            val tv=m.details.related.value.first{it.kind=="tv"&&SeriesLinks103.matches(film,it)}
            fun scroll(n:AccessibilityNodeInfo?):Boolean{if(n==null)return false
                if(n.isScrollable&&n.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true
                for(k in 0 until n.childCount)if(scroll(n.getChild(k)))return true
                return false
            }
            var tries=0
            while(!InputQa.hasLabel(i,"关联与相似推荐")&&tries++<18){scroll(i.uiAutomation.rootInActiveWindow);Thread.sleep(400)}
            check(InputQa.hasLabel(i,"关联与相似推荐"))
            UiQa050.await(10000){InputQa.hasLabel(i,tv.title)}
            Thread.sleep(3500)
            UiQa050.shot(i,"qa103-c3-abyss-related-detail.png")
            InputQa.clickLabel(i,tv.title,idle=false)
            UiQa050.await{var ok=false;i.runOnMainSync{ok=m.detail?.key==tv.key&&m.detail?.kind=="tv"};ok}
            return JSONObject().put("realMovieDetailLoadsTvRecommendation",true).put("touchRelatedTvOpensTvDetail",true)
        }finally{i.runOnMainSync{m.details.cancelLoad()}}
    }
}
