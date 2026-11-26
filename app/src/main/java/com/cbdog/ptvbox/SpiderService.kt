package com.cbdog.ptvbox

import android.app.Service
import android.content.*
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.*
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** Separate process protects the player/UI from plugin worker-thread crashes. Same app UID;
 * user-provided plugins still have this app's permissions, so this is not a permission sandbox. */
class SpiderService:Service(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val bridge by lazy{SpiderBridge(this)}
    private val receiver by lazy{Messenger(object:Handler(Looper.getMainLooper()){
        override fun handleMessage(msg:Message){val reply=msg.replyTo?:return;val id=msg.arg1;val operation=msg.what;val data=msg.data
            scope.launch{val out=Bundle();try{
                val site=Site.from(JSONObject(data.getString("site")!!))
                PluginUi054.install()
                val result=when(operation){
                    7->{if(!BuildConfig.DEBUG)throw IOException("调试钩子未启用");val notices=data.getString("notices").orEmpty().split('\u001f').filter{it.isNotBlank()}.ifEmpty{listOf("QA plugin toast")};val toastDone=java.util.concurrent.CountDownLatch(1);Handler(Looper.getMainLooper()).post{runCatching{notices.forEach{android.widget.Toast.makeText(this@SpiderService,it,android.widget.Toast.LENGTH_SHORT).show()}};toastDone.countDown()};toastDone.await(5,java.util.concurrent.TimeUnit.SECONDS);JSONObject().put("noticeHook",PluginUi054.noticeHook).put("suppressed",PluginUi054.suppressed.get()).toString()}
                    6->{bridge.prepare(site);JSONObject().put("noticeHook",PluginUi054.noticeHook).put("suppressed",PluginUi054.suppressed.get()).toString()}
                    8->{if(!BuildConfig.DEBUG)throw IOException("调试钩子未启用");bridge.prepare(site);val method=android.os.Debug::class.java.getDeclaredMethod("getLoadedClasses").apply{isAccessible=true};val classes=(method.invoke(null) as Array<*>).filterIsInstance<Class<*>>().map{it.name};JSONArray(classes.filter{name->Regex("(?i)(toast|notify|notice|window|dialog|message|tip|advert|adsdk)").containsMatchIn(name)}.distinct().take(300)).toString()}
                    1->JSONArray(bridge.search(site,data.getString("query").orEmpty()).map(VodWire::encode)).toString()
                    2->VodWire.encode(bridge.detail(site,data.getString("id").orEmpty())).toString()
                    3->{val road=Road(data.getString("road").orEmpty(),emptyList());val parsed=bridge.player(site,road,Episode(data.getString("name").orEmpty(),data.getString("url").orEmpty()));JSONObject().put("url",parsed.first.url).put("header",JSONObject(parsed.second)).toString()}
                    4->bridge.browse(site,data.getString("category").orEmpty(),data.getInt("page",1))
                    else->throw IOException("未知插件操作")
                }
                if(result.toByteArray().size>700000)throw IOException("插件返回目录过大，超过本机安全传输上限")
                out.putString("result",result)
            }catch(e:Throwable){out.putString("error",e.message?.substringBefore(" on path")?.take(180)?:e.javaClass.simpleName)}
                runCatching{reply.send(Message.obtain(null,operation,id,0).apply{this.data=out})}
            }
        }
    })}
    override fun onBind(intent:Intent)=receiver.binder
    override fun onDestroy(){scope.cancel();bridge.close();super.onDestroy()
        // Third-party modules may leave their own threads/Toast tasks alive after destroy().
        // This service owns a dedicated process, so discard it once the last host unbinds.
        val name=if(Build.VERSION.SDK_INT>=28)android.app.Application.getProcessName()else runCatching{java.io.File("/proc/self/cmdline").readText().substringBefore('\u0000')}.getOrDefault("")
        if(name.endsWith(":spider"))android.os.Process.killProcess(android.os.Process.myPid())
    }
}
object VodWire {
    fun encode(v:Vod)=JSONObject().put("id",v.id).put("title",v.title).put("year",v.year).put("poster",v.poster).put("overview",v.overview).put("folder",v.folder).put("roads",JSONArray(v.roads.map{r->JSONObject().put("name",r.name).put("episodes",JSONArray(r.episodes.map{JSONObject().put("name",it.name).put("url",it.url)}))}))
    fun decode(j:JSONObject)=Vod(j.getString("id"),j.getString("title"),j.optString("year"),j.optString("poster"),j.optString("overview"),j.getJSONArray("roads").objects().map{r->Road(r.getString("name"),r.getJSONArray("episodes").objects().map{Episode(it.getString("name"),it.getString("url"))})},j.optBoolean("folder"))
}
class SpiderClient(input:Context){
    private val context=input.applicationContext?:input
    private val lock=Mutex();private var remote:Messenger?=null;private var bound=false
    private var connected=CompletableDeferred<Messenger>()
    private val serial=AtomicInteger();private val pending=ConcurrentHashMap<Int,CompletableDeferred<Bundle>>()
    private val reply=Messenger(object:Handler(Looper.getMainLooper()){override fun handleMessage(msg:Message){pending.remove(msg.arg1)?.complete(msg.data)}})
    private val connection=object:ServiceConnection{
        override fun onServiceConnected(name:ComponentName,binder:IBinder){val messenger=Messenger(binder);remote=messenger;connected.complete(messenger)}
        override fun onServiceDisconnected(name:ComponentName){remote=null;connected=CompletableDeferred();pending.values.forEach{it.completeExceptionally(IOException("插件进程退出，播放器保留；可重试当前站点"))};pending.clear()}
    }
    private suspend fun call(site:Site,op:Int,fill:Bundle.()->Unit):String {
        val target=lock.withLock{remote?:run{
            withContext(Dispatchers.Main){if(!bound){bound=context.bindService(Intent(context,SpiderService::class.java),connection,Context.BIND_AUTO_CREATE);if(!bound)throw IOException("插件服务无法启动")}}
            withTimeout(12000){connected.await()}
        }}
        val id=serial.incrementAndGet();val result=CompletableDeferred<Bundle>();pending[id]=result
        try{target.send(Message.obtain(null,op,id,0).apply{replyTo=reply;data=Bundle().apply{putString("site",site.json().toString());fill()}});val data=withTimeout(75000){result.await()};data.getString("error")?.let{throw IOException(it)};return data.getString("result").orEmpty()}
        finally{pending.remove(id)}
    }
    suspend fun browse(site:Site,category:String="",page:Int=1)=call(site,4){putString("category",category);putInt("page",page)}
    suspend fun prepare(site:Site)=JSONObject(call(site,6){})
    suspend fun noticeStatus(site:Site,vararg notices:String)=JSONObject(call(site,7){putString("notices",notices.joinToString("\u001f"))})
    suspend fun search(site:Site,q:String)=JSONArray(call(site,1){putString("query",q)}).objects().map(VodWire::decode)
    suspend fun detail(site:Site,id:String)=VodWire.decode(JSONObject(call(site,2){putString("id",id)}))
    suspend fun loadedClasses(site:Site)=JSONArray(call(site,8){})
    suspend fun player(site:Site,r:Road,e:Episode):Pair<Episode,Map<String,String>>{val j=JSONObject(call(site,3){putString("road",r.name);putString("name",e.name);putString("url",e.url)});val h=j.getJSONObject("header");return e.copy(url=j.getString("url")) to h.keys().asSequence().associateWith{h.optString(it)}}
    fun close(){if(bound)runCatching{context.unbindService(connection)};bound=false;remote=null;pending.values.forEach{it.cancel()};pending.clear()}
}
