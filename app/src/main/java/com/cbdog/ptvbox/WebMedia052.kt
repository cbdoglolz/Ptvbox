package com.cbdog.ptvbox
import android.content.Context
import android.webkit.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException

/** Bounded public playback-page resolver. Every captured URL is subsequently media-probed. */
object WebMedia052 {
 private val gate=Semaphore(2)
 fun accessPage(html:String)=html.contains("cloudflareaccess.com",true)||html.contains("/cdn-cgi/access/",true)||html.contains("Cloudflare Access",true)
 fun mediaUrl(url:String)=url.startsWith("http")&&Regex("(?i)\\.(m3u8|mp4|mkv|flv|mpd)([?#]|$)").containsMatchIn(url)&&!Regex("(?i)(/ads?/|advert|/promo)").containsMatchIn(url)
 fun embedded(html:String,base:String):List<String>{val text=html.replace("\\/","/").replace("&amp;","&");val doc=org.jsoup.Jsoup.parse(text,base);return (doc.select("video[src],source[src],iframe[src]").map{it.absUrl("src")}+Regex("""https?://[^\s"'<>\\]+\.(?:m3u8|mp4|mkv|flv)(?:\?[^\s"'<>\\]*)?""",RegexOption.IGNORE_CASE).findAll(text).map{it.value}.toList()).filter{it.startsWith("http")}.distinct().take(8)}
 suspend fun resolve(context:Context,url:String,headers:Map<String,String>,html:String=""):Pair<String,Map<String,String>> = gate.withPermit {
  if(accessPage(html))throw IOException("播放接口被Cloudflare Access登录拦截，需要放行播放接口")
  for(child in embedded(html,url)){val h=headers+mapOf("Referer" to url);try{val data=Net.bytes(child,h+mapOf("Range" to "bytes=0-4095"),4096,true);if(MediaBytes052.accept(data))return@withPermit child to h}catch(e:CancellationException){throw e}catch(_:Exception){}}
  try{withTimeout(16000){val found=CompletableDeferred<Pair<String,Map<String,String>>>();var view:WebView?=null
   try{withContext(Dispatchers.Main){val w=WebView(context);view=w;w.settings.apply{javaScriptEnabled=true;domStorageEnabled=true;allowFileAccess=false;allowContentAccess=false;mediaPlaybackRequiresUserGesture=false;javaScriptCanOpenWindowsAutomatically=false;setSupportMultipleWindows(false);userAgentString=headers.entries.firstOrNull{it.key.equals("User-Agent",true)}?.value?:NetworkPolicy.userAgent};w.webViewClient=object:WebViewClient(){
    override fun shouldOverrideUrlLoading(v:WebView,r:WebResourceRequest):Boolean{if(r.url.host.orEmpty().endsWith("cloudflareaccess.com")){found.completeExceptionally(IOException("播放接口被Cloudflare Access登录拦截，需要放行播放接口"));return true};return r.url.scheme !in listOf("https","http")}
    override fun shouldInterceptRequest(v:WebView,r:WebResourceRequest):WebResourceResponse? {val u=r.url.toString();if(mediaUrl(u)&&!found.isCompleted){val h=(headers.filterKeys{it.equals("User-Agent",true)}+r.requestHeaders.filterKeys{it.lowercase()in listOf("user-agent","referer","origin","authorization")}).toMutableMap();h.putIfAbsent("Referer",url);CookieManager.getInstance().getCookie(u)?.let{h["Cookie"]=it};found.complete(u to h)};return null}
    override fun onPageFinished(v:WebView,u:String){v.evaluateJavascript("document.querySelectorAll('video').forEach(v=>{v.muted=true;v.play().catch(()=>{})});",null)}
   };w.loadUrl(url,headers.filterKeys{!it.equals("Range",true)});w.onResume()};found.await()}
   finally{withContext(NonCancellable+Dispatchers.Main){view?.apply{stopLoading();loadUrl("about:blank");onPause();removeAllViews();destroy()}}}
  }}catch(e:TimeoutCancellationException){if(!currentCoroutineContext().isActive)throw e;throw IOException("网页播放页16秒内未提供有效媒体，请切换本场其他线路")}
 }
}
object MediaBytes052 {
 fun hls(b:ByteArray)=b.toString(Charsets.UTF_8).trimStart('\uFEFF',' ','\n','\r').startsWith("#EXTM3U")
 fun mkv(b:ByteArray)=b.size>=4&&b.take(4).map{it.toInt()and 255}==listOf(0x1a,0x45,0xdf,0xa3)
 fun accept(b:ByteArray)=hls(b)||(b.size>12&&(String(b,4,4)=="ftyp"||String(b,0,3)=="FLV"||mkv(b)||(b.size>188&&(b[0].toInt()and 255)==0x47&&(b[188].toInt()and 255)==0x47)))
}
