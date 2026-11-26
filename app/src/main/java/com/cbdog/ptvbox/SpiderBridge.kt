package com.cbdog.ptvbox

import android.content.Context
import com.github.catvod.crawler.Spider
import dalvik.system.DexClassLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile

/** Native Android execution of the user's own TVBox dex JAR, not a desktop Java converter. */
class SpiderBridge(inputContext:Context){
    private val context=inputContext.applicationContext?:inputContext
    private val loaders=ConcurrentHashMap<String,ClassLoader>()
    private val spiders=ConcurrentHashMap<String,Spider>()
    private val locks=ConcurrentHashMap<String,Any>()
    private val proxies=java.util.concurrent.CopyOnWriteArrayList<SpiderProxy>()
    suspend fun prepare(site:Site){loader(site.jar)}
    private suspend fun loader(reference:String):ClassLoader=withContext(Dispatchers.IO){
        require(reference.isNotBlank()){"订阅没有配置Spider Jar"}
        synchronized(locks.getOrPut(reference){Any()}){
            loaders[reference]?.let{return@synchronized it}
            PluginUi054.install()
            val url=reference.substringBefore(";md5;")
            val expected=reference.substringAfter(";md5;","").take(32)
            val index=File(context.filesDir,"spider-index").apply{mkdirs()}.resolve(sha256(reference.toByteArray())+".json")
            fun valid(file:File):Boolean=runCatching{file.isFile&&file.length() in 1..32_000_000&&(!expected.matches(Regex("[0-9a-fA-F]{32}"))||MessageDigest.getInstance("MD5").digest(file.readBytes()).joinToString(""){"%02x".format(it)}.equals(expected,true))&&ZipFile(file).use{it.getEntry("classes.dex")!=null}}.getOrDefault(false)
            val cached=runCatching{val j=JSONObject(index.readText());val hash=j.getString("hash");require(hash.matches(Regex("[0-9a-f]{64}")));File(context.codeCacheDir,"spiders/$hash/plugin.jar").takeIf{valid(it)&&sha256(it.readBytes())==hash}?.let{it to j.getLong("time")}}.getOrNull()
            val jar=if(cached!=null&&System.currentTimeMillis()-cached.second<15*60*1000L)cached.first else {
                val data=try{kotlinx.coroutines.runBlocking{Net.bytes(url,mapOf("User-Agent" to "Mozilla/5.0"),limit=32_000_000,timeoutSeconds=45)}}catch(e:Exception){if(cached!=null)cached.first.readBytes()else throw e}
                if(expected.matches(Regex("[0-9a-fA-F]{32}")))check(MessageDigest.getInstance("MD5").digest(data).joinToString(""){"%02x".format(it)}.equals(expected,true)){"Spider Jar校验不一致，请更新订阅"}
                val hash=sha256(data);val target=File(context.codeCacheDir,"spiders/$hash").apply{mkdirs()}.resolve("plugin.jar")
                if(!target.exists())target.outputStream().use{out->check(target.setReadOnly());out.write(data)}
                check(valid(target)){"下载内容不是有效Android Spider Jar"}
                index.writeText(JSONObject().put("hash",hash).put("time",System.currentTimeMillis()).toString());target
            }
            val dir=jar.parentFile!!
            val native=File(dir,"native").apply{mkdirs()}
            ZipFile(jar).use{zip->
                check(zip.getEntry("classes.dex")!=null){"下载内容不是Android Spider Jar"}
                // Keep the names expected by user-supplied native guard loaders; never extract arbitrary paths.
                for(name in listOf("ftyguard_v7","ftyguard_v8")){val entry=zip.getEntry("assets/$name.so")?:continue;check(entry.size in 1..8_000_000);val target=File(native,"lib$name.so");if(!target.exists())target.outputStream().use{out->check(target.setReadOnly());zip.getInputStream(entry).use{it.copyTo(out)}}}
            }
            val loader=DexClassLoader(jar.path,context.codeCacheDir.path,native.path,context.classLoader)
            runCatching{val c=loader.loadClass("com.github.catvod.spider.Init");c.getMethod("init",Context::class.java).invoke(null,context)}.getOrElse{throw IOException("Spider初始化失败：${rootType(it)}")}
            val payload=runCatching{loader.loadClass("com.github.catvod.spider.Init").getMethod("loader").invoke(null) as ClassLoader}.getOrDefault(loader)
            runCatching{val proxy=SpiderProxy(loader);proxies.add(proxy);proxy.setPort(payload)}
            runCatching{payload.loadClass("com.github.catvod.spider.InitOrigin").getMethod("init",Context::class.java).invoke(null,context)}
            proxies.forEach{it.setPort(payload)}
            loaders[reference]=loader;loader
        }
    }
    private suspend fun spider(site:Site):Spider {
        val key=site.key+"|"+site.jar+"|"+site.ext
        spiders[key]?.let{return it};val loader=loader(site.jar)
        return withContext(Dispatchers.IO){synchronized(locks.getOrPut(key){Any()}){
            spiders[key]?:try{
                val name=site.api.removePrefix("csp_");val c=loader.loadClass(if(name.contains('.'))name else "com.github.catvod.spider.$name")
                val s=c.getDeclaredConstructor().newInstance() as Spider;s.siteKey=site.key;s.init(context,site.ext);spiders[key]=s;s
            }catch(e:Throwable){throw IOException("Spider站点初始化失败：${rootType(e)}")}
        }}
    }
    suspend fun browse(site:Site,category:String="",page:Int=1):String {val s=spider(site);return withContext(Dispatchers.IO){synchronized(s){if(category.isBlank())s.homeContent(true)else s.categoryContent(category,page.toString(),false,hashMapOf())}}}
    suspend fun search(site:Site,keyword:String):List<Vod>{val s=spider(site);return withContext(Dispatchers.IO){try{synchronized(s){JSONObject(s.searchContent(keyword,false,"1")).optJSONArray("list")?.objects()?.map(VodParser::parse).orEmpty()}}catch(e:Throwable){throw IOException("Spider搜索失败：${rootType(e)}")}}}
    suspend fun detail(site:Site,id:String):Vod {val s=spider(site);return withContext(Dispatchers.IO){try{synchronized(s){JSONObject(s.detailContent(listOf(id))).getJSONArray("list").getJSONObject(0).let(VodParser::parse)}}catch(e:Throwable){throw IOException("Spider详情失败：${rootType(e)}")}}}
    suspend fun player(site:Site,road:Road,episode:Episode):Pair<Episode,Map<String,String>> {val s=spider(site);return withContext(Dispatchers.IO){try{synchronized(s){val j=JSONObject(s.playerContent(road.name,episode.url,emptyList()));if(j.optString("url").isBlank()||j.optString("url")=="null")throw IOException("插件返回空播放地址："+com.github.catvod.crawler.SpiderDebug.takeError());val headers=j.opt("header").let{if(it is JSONObject)it else if(it is String&&it.startsWith("{"))JSONObject(it)else JSONObject()};episode.copy(url=j.getString("url")) to headers.keys().asSequence().associateWith{headers.optString(it)}}}catch(e:Throwable){if(e is IOException)throw e;throw IOException("Spider播放解析失败：${rootType(e)}")}}}
    companion object {private fun rootType(error:Throwable):String {var e=error;repeat(8){e.cause?.let{e=it}};val extra=if(BuildConfig.DEBUG&&(e is UnsatisfiedLinkError||e is ClassNotFoundException))e.message.orEmpty().replace(Regex("/data/[^ ]+"),"<private-path>")else"";return e.javaClass.simpleName+" "+extra+" @ "+e.stackTrace.take(3).joinToString(" ← "){it.className+"."+it.methodName}}}
    fun close(){proxies.forEach{it.close()};proxies.clear();spiders.values.forEach{runCatching{it.destroy()}};spiders.clear()}
}
