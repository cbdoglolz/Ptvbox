package com.cbdog.ptvbox

import android.widget.Toast
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicInteger

/** Installed in both the app and dedicated :spider process. Suppress plugin Toast queues; Compose owns player notices.
 * Reflection is optional: unavailable framework internals are reported, never treated as success. */
object PluginUi054 {
 @Volatile var noticeHook=false;private set
 val suppressed=AtomicInteger()
 @Volatile var authActivity:PluginAuthActivity054?=null
 @Volatile private var toastProxy:Any?=null
 private fun replaceStatic(field:java.lang.reflect.Field,value:Any):Boolean {
  runCatching{field.isAccessible=true;field.set(null,value);return true}
  return runCatching{
   val unsafeClass=Class.forName("sun.misc.Unsafe");val unsafe=unsafeClass.getDeclaredField("theUnsafe").apply{isAccessible=true}.get(null)
   val base=unsafeClass.getMethod("staticFieldBase",java.lang.reflect.Field::class.java).invoke(unsafe,field)
   val offset=unsafeClass.getMethod("staticFieldOffset",java.lang.reflect.Field::class.java).invoke(unsafe,field) as Long
   unsafeClass.getMethod("putObject",Any::class.java,Long::class.javaPrimitiveType,Any::class.java).invoke(unsafe,base,offset,value)
   true
  }.getOrDefault(false)
 }
 @Synchronized fun install(){if(noticeHook)return;noticeHook=runCatching{
  val type=Class.forName("android.app.INotificationManager");val get=Toast::class.java.declaredMethods.firstOrNull{it.name=="getService"&&it.parameterCount==0}.apply{this?.isAccessible=true}?:error("service method");val original=get.invoke(null)?:error("service");val field=Toast::class.java.declaredFields.firstOrNull{it.type==type}?:Toast::class.java.getDeclaredField("sService").apply{isAccessible=true}
  val bridge=Proxy.newProxyInstance(type.classLoader,arrayOf(type)){_,method,args->if(ToastProxyPolicy.shouldBlock(method.name)){if(!method.name.startsWith("cancel",true))suppressed.incrementAndGet();when(method.returnType){java.lang.Boolean.TYPE->true;java.lang.Integer.TYPE->0;java.lang.Long.TYPE->0L;else->null}}else try{method.invoke(original,*(args?:emptyArray()))}catch(e:java.lang.reflect.InvocationTargetException){throw e.targetException}}
  toastProxy=bridge;check(replaceStatic(field,bridge)){"hidden API field blocked"};true
 }.getOrDefault(false)}
}
internal object ToastProxyPolicy {
 fun shouldBlock(method:String)=method.contains("toast",true)&&(method.startsWith("enqueue",true)||method.startsWith("cancel",true))
}
object CloudPolicy054 {
 private val siteNameRegex=Regex("网盘|云盘|玩偶|木偶|蜡笔|虎斑|闪电|二小|欧歌|至臻|多多|小米|优汐|盘他|盘搜|盘地|夸克|阿里|小雅|天翼|百度|迅雷|115|123|(?i)\\b(?:uc|alist|alipan|quark|wogg|pan)\\b")
 private val siteApiRegex=Regex("(?i)^csp_(?:Ali|Quark|UC|Wogg|Wobg|Muou|Labi|Huban|XiaoMi|ErXiao|Ouge|ZhiZhen|Duoduo|Ypan|Bpan|Pan|AList|Push)")
 private val roadRegex=Regex("网盘|云盘|夸克|阿里|优汐|天翼|百度|迅雷|移动云|115|123|(?i)(?:^|[^a-z])(?:uc|quark|ali|alipan|alist|pan)(?:[^a-z]|$)")
 fun needsAuthorization(site:Site)=siteNameRegex.containsMatchIn(site.name)||siteApiRegex.containsMatchIn(site.api)
 fun isCloudRoad(roadName:String)=roadRegex.containsMatchIn(roadName)
}
