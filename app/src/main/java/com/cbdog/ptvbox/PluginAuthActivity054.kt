package com.cbdog.ptvbox

import android.os.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.Text
import kotlinx.coroutines.*
import org.json.JSONObject

data class AuthQr054(val key:Int,val bitmap:Bitmap,val provider:String,val time:Long)
/** Same UID and plugin process, with a real Activity for native QR dialogs. Never exported.
 * Existing plugin cookies/files stay in place; no borrowed sessions or credential extraction. */
class PluginAuthActivity054:ComponentActivity(){
 private lateinit var db:Library;private lateinit var sites:Sites;private lateinit var site:Site;private lateinit var work:Work
 private var succeeded=false;private var closed=false;private var task:Job?=null;private var watch:Job?=null
 private var codes by mutableStateOf<List<AuthQr054>>(emptyList());private var selected by mutableIntStateOf(0);private var status by mutableStateOf("正在向网盘请求二维码…");private var now by mutableLongStateOf(0);private var started=0L
 private var nativeActions by mutableStateOf<List<Pair<String,View>>>(emptyList());private val hidden=mutableMapOf<View,Pair<Int,Float>>()
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);PluginUi054.install();PluginUi054.authActivity=this
  try{site=Site.from(JSONObject(intent.getStringExtra("site")!!));work=Work.from(JSONObject(intent.getStringExtra("work")!!));db=Library(this);sites=Sites(this,db)}catch(_:Exception){finish();return}
  setContent{CinemaTheme(false){val qr=codes.getOrNull(selected);BoxWithConstraints(Modifier.fillMaxSize().background(Color(0x990a0912)),contentAlignment=Alignment.Center){val qrSize=minOf(230.dp,maxHeight*.42f);Column(Modifier.widthIn(max=660.dp).fillMaxWidth(.9f).heightIn(max=maxHeight*.95f).verticalScroll(rememberScrollState()).background(Color(0xf0201c30),RoundedCornerShape(14.dp)).border(1.dp,PcAccent,RoundedCornerShape(14.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
   PcSection("网盘扫码授权 · ${site.name}","${work.title} · ${intent.getStringExtra("episode").orEmpty()}")
   Text("${qr?.provider?:"正在识别网盘"} · ${if(codes.isEmpty())0 else selected+1}/${codes.size}",color=PcAccent,fontSize=16.sp)
   Text(if(qr!=null&&status.startsWith("请使用"))"请使用上方标注的网盘客户端扫码授权"else status,color=Color.White,fontSize=12.sp,maxLines=3)
   if(qr!=null)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){PcChip("◀ 上一个",false,{if(codes.isNotEmpty())selected=(selected-1+codes.size)%codes.size});Image(qr.bitmap.asImageBitmap(),"${qr.provider}登录二维码",Modifier.padding(10.dp).size(qrSize).background(Color.White));PcChip("下一个 ▶",false,{if(codes.isNotEmpty())selected=(selected+1)%codes.size})}
   if(now-(qr?.time?:started)>=30000)Text("二维码可能已过期，请点击刷新二维码",color=Color(0xffffbb8f),fontSize=13.sp)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){nativeActions.filterNot{it.first.contains("刷新")}.forEach{(name,view)->PcChip(name,false,{view.performClick()})};PcChip("刷新二维码",false,{if(now-started>=3000){codes=emptyList();selected=0;val refresh=nativeActions.firstOrNull{it.first.contains("刷新")};if(refresh!=null){started=SystemClock.elapsedRealtime();refresh.second.performClick()}else request()}});PcChip("关闭授权",false,{notifyClosed054();finish()})}
   Text("请确认网盘名称再扫码。授权状态由网盘管理，软件保留已有登录文件，不主动清除会话。",color=Color.LightGray,fontSize=11.sp)
  }}}}
  watch=lifecycleScope.launch{while(isActive){now=SystemClock.elapsedRealtime();captureDialogs();delay(750)}}
 }
 override fun onResume(){super.onResume();if(task==null)request()}
 private fun request(){task?.cancel();started=SystemClock.elapsedRealtime();status="正在向网盘请求二维码…";task=lifecycleScope.launch{try{
  val epName=intent.getStringExtra("episode").orEmpty();val direct=intent.getStringExtra("url");val roadName=intent.getStringExtra("road").orEmpty()
  val pair=withContext(Dispatchers.IO){if(!direct.isNullOrBlank())Road(roadName,emptyList()) to Episode(epName,direct)else{
   var hits=sites.search(site,work.title);if(hits.none{Identity.matches(work,it)}&&work.original.isNotBlank())hits=hits+sites.search(site,work.original)
   val item=hits.firstOrNull{Identity.matches(work,it)}?:throw java.io.IOException("未找到当前作品");val vod=sites.detail(site,item);vod.roads.flatMap{r->r.episodes.filter{work.kind=="movie"||Identity.sameEpisode(it.name,epName)}.map{r to it}}.firstOrNull()?:throw java.io.IOException("未找到当前单集")
  }}
  val candidate=withTimeout(120000){withContext(Dispatchers.IO){sites.probe(site,pair.first,pair.second,interactive=true)}}
  val c=JSONObject().put("site",candidate.site.json()).put("road",candidate.road).put("episodeName",candidate.episode.name).put("url",candidate.episode.url).put("height",candidate.height).put("bytes",candidate.bytes).put("delay",candidate.delay).put("mime",candidate.mimeType)
  succeeded=true;sendBroadcast(Intent(BuildConfig.APPLICATION_ID+".AUTH_RESULT054").setPackage(BuildConfig.APPLICATION_ID).putExtra("request",intent.getStringExtra("request")).putExtra("workKey",work.key).putExtra("candidate",c.toString()));status="授权及线路验证成功";delay(500);finish()
 }catch(e:CancellationException){if(e is TimeoutCancellationException)status="扫码或线路请求超时，可刷新二维码"else throw e}catch(_:Exception){status=if(codes.isEmpty())"未收到可显示的二维码，可刷新重试或检查该网盘规则"else"请扫码后尝试刷新；未改变现有播放"}}}
 private fun roots():List<View> = if(Build.VERSION.SDK_INT>=29)android.view.inspector.WindowInspector.getGlobalWindowViews()else runCatching{val c=Class.forName("android.view.WindowManagerGlobal");val instance=c.getDeclaredMethod("getInstance").invoke(null);@Suppress("UNCHECKED_CAST")(c.getDeclaredField("mViews").apply{isAccessible=true}.get(instance) as List<View>).toList()}.getOrDefault(emptyList())
 private fun captureDialogs(){for(root in roots().filter{it!==window.decorView}){val texts=mutableListOf<String>();val images=mutableListOf<ImageView>();val buttons=mutableListOf<Pair<String,View>>()
  fun visit(v:View){if(v is TextView){val t=v.text.toString();texts+=t;if(v.isClickable&&Regex("刷新|切换|夸克|阿里|UC|115|123|百度|天翼|移动").containsMatchIn(t)&&t.length<25)buttons+=t to v};if(v is ImageView)images+=v;if(v is ViewGroup)for(i in 0 until v.childCount)visit(v.getChildAt(i))};visit(root)
  val provider=listOf("夸克","阿里","UC","115","123","百度","天翼","移动","迅雷").firstOrNull{name->texts.any{it.contains(name)}}?.let{"${it}网盘"}?:site.name
  for(image in images){val bitmap=qr(image.drawable)?:continue;var hash=1;for(y in 0 until bitmap.height step maxOf(1,bitmap.height/32))for(x in 0 until bitmap.width step maxOf(1,bitmap.width/32))hash=31*hash+bitmap.getPixel(x,y);if(codes.none{it.key==hash}){val imageProvider=listOf("夸克","阿里","UC","115","123","百度","天翼","移动","迅雷").firstOrNull{image.contentDescription?.contains(it)==true}?.let{"${it}网盘"}?:provider;codes=(codes+AuthQr054(hash,bitmap,imageProvider,SystemClock.elapsedRealtime())).takeLast(8);selected=codes.lastIndex;status="请使用${provider}客户端扫码授权"}}
  if(images.isNotEmpty()&&codes.isNotEmpty()){nativeActions=buttons.distinctBy{it.first}.take(8);val lp=root.layoutParams as? WindowManager.LayoutParams?:continue;if(root !in hidden){hidden[root]=lp.flags to lp.alpha;lp.flags=lp.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;lp.alpha=0f;runCatching{windowManager.updateViewLayout(root,lp)}}}
 }}
 private fun qr(drawable:Drawable?):Bitmap?=runCatching{drawable?:return null;val w=drawable.intrinsicWidth;val h=drawable.intrinsicHeight;if(w<100||h<100||w.toFloat()/h !in .8f..1.2f)return null;val bitmap=Bitmap.createBitmap(300,300,Bitmap.Config.ARGB_8888);drawable.setBounds(0,0,300,300);drawable.draw(Canvas(bitmap));var monochrome=0;var dark=0;var total=0;for(y in 0 until 300 step 7)for(x in 0 until 300 step 7){val p=bitmap.getPixel(x,y);val r=android.graphics.Color.red(p);val g=android.graphics.Color.green(p);val b=android.graphics.Color.blue(p);total++;if(maxOf(r,g,b)-minOf(r,g,b)<30)monochrome++;if(r+g+b<180)dark++};if(monochrome<total*.8||dark<total*.1||dark>total*.8)null else bitmap}.getOrNull()
 private fun notifyClosed054(){if(!succeeded&&!closed){closed=true;sendBroadcast(Intent(BuildConfig.APPLICATION_ID+".AUTH_CLOSED054").setPackage(BuildConfig.APPLICATION_ID).putExtra("request",intent.getStringExtra("request")))}}
 override fun onDestroy(){notifyClosed054();PluginUi054.authActivity=null;watch?.cancel();task?.cancel();for((view,values)in hidden){runCatching{val lp=view.layoutParams as WindowManager.LayoutParams;lp.flags=values.first;lp.alpha=values.second;windowManager.updateViewLayout(view,lp)}};if(::sites.isInitialized)sites.close();if(::db.isInitialized)db.close();super.onDestroy()}
}
