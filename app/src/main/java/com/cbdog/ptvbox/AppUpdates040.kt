package com.cbdog.ptvbox

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.tv.material3.Text
import kotlinx.coroutines.*
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

data class AppUpdate040Info(val version:String,val codeHint:String,val assetUrl:String,val digest:String,val filename:String,val notes:String)
class AppUpdates040(private val context:Context,private val db:Library){
    val cache=File(context.cacheDir,"updates").apply{mkdirs()}
    private fun headers():Map<String,String>{val token=db.secrets.read("source_update_token");return if(token.isBlank())emptyMap()else mapOf("Authorization" to "Bearer $token")}
    suspend fun check():AppUpdate040Info {
        val root=Net.json("https://api.github.com/repos/cbdoglolz/Ptvbox/releases/latest",headers())
        require(!root.optBoolean("draft")&&!root.optBoolean("prerelease"))
        val assets=root.getJSONArray("assets").objects();val asset=assets.firstOrNull{it.optString("name").endsWith("-ARM.apk",true)}?:assets.first{it.optString("name").endsWith(".apk",true)}
        val filename=asset.getString("name");var digest=asset.optString("digest").removePrefix("sha256:")
        if(!digest.matches(Regex("[a-fA-F0-9]{64}"))){
            val sums=assets.firstOrNull{it.optString("name") in listOf("SHA256SUMS.txt","RELEASE-SHA256SUMS.txt")}?:error("版本未提供SHA-256校验，未下载")
            val text=Net.text(sums.getString("url"),headers()+mapOf("Accept" to "application/octet-stream"));digest=text.lineSequence().map{it.trim().split(Regex("\\s+"),limit=2)}.firstOrNull{it.size==2&&it[1].removePrefix("*")==filename}?.first().orEmpty()
        }
        require(digest.matches(Regex("[a-fA-F0-9]{64}"))){"版本校验格式无效"}
        return AppUpdate040Info(root.optString("tag_name"),root.optString("id"),asset.getString("url"),digest.lowercase(),filename,root.optString("body"))
    }
    fun cached(info:AppUpdate040Info)=File(cache,"${info.digest}.apk")
    fun validate(file:File,info:AppUpdate040Info,checkActive:()->Unit={}):String {
        require(file.isFile&&file.length() in 1..80_000_000)
        val digest=MessageDigest.getInstance("SHA-256");file.inputStream().use{stream->val buffer=ByteArray(65536);while(true){checkActive();val n=stream.read(buffer);if(n<0)break;digest.update(buffer,0,n)}}
        require(digest.digest().joinToString(""){"%02x".format(it)}==info.digest){"安装包SHA-256不匹配"}
        @Suppress("DEPRECATION") val flags=if(android.os.Build.VERSION.SDK_INT>=28)PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION") val archive=context.packageManager.getPackageArchiveInfo(file.absolutePath,flags)?:error("不是可识别APK")
        @Suppress("DEPRECATION") val installed=context.packageManager.getPackageInfo(context.packageName,flags)
        fun certificates(p:android.content.pm.PackageInfo):Set<String>{
            @Suppress("DEPRECATION") val signatures=if(android.os.Build.VERSION.SDK_INT>=28)p.signingInfo?.apkContentsSigners else p.signatures
            return signatures.orEmpty().map{sha256(it.toByteArray())}.toSet()
        }
        require(archive.packageName==context.packageName&&certificates(archive).isNotEmpty()&&certificates(archive)==certificates(installed)){"包名或签名不匹配，拒绝覆盖"}
        @Suppress("DEPRECATION") val next=if(android.os.Build.VERSION.SDK_INT>=28)archive.longVersionCode else archive.versionCode.toLong()
        require(next>BuildConfig.VERSION_CODE){"没有比当前版本更新的安装包"}
        return archive.versionName.orEmpty()
    }
    suspend fun download(info:AppUpdate040Info,progress:(Int)->Unit):File=withContext(Dispatchers.IO){
        val target=cached(info)
        if(target.exists()){progress(0);try{withTimeout(8000){validate(target,info){ensureActive()}};progress(100);return@withContext target}catch(_:TimeoutCancellationException){}catch(e:CancellationException){throw e}catch(_:Exception){}}
        val partial=File(cache,"${info.digest}.part.apk");var url=info.assetUrl;var auth=headers()+mapOf("Accept" to "application/octet-stream")
        try{
            repeat(6){
                coroutineContext.ensureActive();val req=Request.Builder().url(url).header("User-Agent",NetworkPolicy.userAgent).apply{auth.forEach{(k,v)->header(k,v)}}.build()
                val call=NetworkPolicy.client().newCall(req)
                val response=call.execute()
                response.use{r->
                    if(r.code in 300..399){val next=r.header("Location")?.let{r.request.url.resolve(it)}?:error("下载跳转无效");require(next.scheme=="https");if(next.host!=r.request.url.host||next.port!=r.request.url.port)auth=emptyMap();url=next.toString();return@repeat}
                    require(r.isSuccessful){"HTTP ${r.code}"};val size=r.body.contentLength();require(size<=80_000_000)
                    var written=0L;var tick=0L
                    partial.outputStream().use{out->r.body.byteStream().use{input->val buffer=ByteArray(65536);while(true){coroutineContext.ensureActive();val n=input.read(buffer);if(n<0)break;written+=n;require(written<=80_000_000);out.write(buffer,0,n);val now=System.currentTimeMillis();if(now-tick>200){tick=now;progress(if(size>0)(written*95/size).toInt()else -1)}}}}
                    validate(partial,info);if(target.exists())target.delete();check(partial.renameTo(target));progress(100);return@withContext target
                }
            };error("下载跳转过多")
        }finally{partial.delete()}
    }
}

@Composable internal fun AppUpdate040(m:PtvModel){
    val context=LocalContext.current;val scope=rememberCoroutineScope();val updater=remember{AppUpdates040(context,m.db)}
    var info by remember{mutableStateOf<AppUpdate040Info?>(null)};var status by remember{mutableStateOf("只检测已发布的ATV版本；草稿不会作为更新。")};var task by remember{mutableStateOf<Job?>(null)};var progress by remember{mutableIntStateOf(0)};var ready by remember{mutableStateOf<File?>(null)};var savedUri by remember{mutableStateOf<Uri?>(null)}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")){uri->val file=ready;if(uri!=null&&file!=null)scope.launch{withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri,"w")!!.use{out->file.inputStream().use{it.copyTo(out)}}};savedUri=uri;status="安装包已保存到所选位置"}}
    val locate=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){}
    Text(status,color=PcAccent,fontSize=13.sp);if(progress>0)Text("下载 / 校验：$progress%",color=PcGreen,fontSize=13.sp)
    FlowRow(horizontalArrangement=Arrangement.spacedBy(androidx.compose.ui.unit.Dp(8f)),verticalArrangement=Arrangement.spacedBy(androidx.compose.ui.unit.Dp(6f))){
        Action("检查更新",{if(task?.isActive!=true)task=scope.launch{status="正在检查公开版本…";try{val result=withContext(Dispatchers.IO){updater.check()};info=result;status="${result.version}\n${result.notes}"}catch(_:Exception){status="暂无可验证的新版本，或仓库权限/网络暂不可用。当前安装保持。"}}})
        if(info!=null)Action("下载新版本 / 检查本地包",{if(task?.isActive!=true)task=scope.launch{status="正在检查本地安装包（最多8秒），随后下载…";try{ready=updater.download(info!!){value->scope.launch{progress=value}};status="下载和包名/签名校验通过，可安装"}catch(e:CancellationException){status="已取消；当前应用保持";throw e}catch(_:Exception){status="下载或校验失败，未覆盖当前安装"}}})
        if(task?.isActive==true)Action("取消",{task?.cancel()})
        if(ready!=null){
            Action("安装更新（系统确认）",{runCatching{val uri=FileProvider.getUriForFile(context,"${context.packageName}.updates",ready!!);context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))}.onFailure{status="请在系统允许此来源安装，或保存APK后手动安装"}})
            Action("保存安装包到文件夹",{runCatching{export.launch(info?.filename?:"Ptvbox-update.apk")}.onFailure{status="此电视未提供文件选择器"}})
        }
        if(savedUri!=null)Action("定位下载文件",{runCatching{locate.launch(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/vnd.android.package-archive").putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI,savedUri))}.onFailure{status="请在系统文件管理器打开刚才所选目录"}})
    }
}
