package com.cbdog.ptvbox

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.os.SystemClock
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.Closeable
import java.math.BigInteger
import java.net.*
import java.security.*
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.*
import javax.security.auth.x500.X500Principal

/** Explicitly started, short-lived HTTPS configuration session. Saved values are shown as configured by the owner. */
class LocalConfigServer(
    private val context:android.content.Context,
    private val status:()->JSONObject,
    private val apply:suspend (JSONObject)->JSONObject,
    private val backup:suspend (JSONObject)->JSONObject={JSONObject().put("error",true).put("message","备份入口不可用")}
):Closeable {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val clients=ConcurrentHashMap.newKeySet<Socket>()
    private val count=AtomicInteger()
    private val backupBusy=java.util.concurrent.atomic.AtomicBoolean(false)
    private var socket:SSLServerSocket?=null
    private var deadline=0L
    private var secret=ByteArray(0)
    var url:String="";private set
    var fingerprint:String="";private set
    var lastError:String="";private set
    val port get()=socket?.localPort?:0
    val running get()=socket?.isClosed==false&&SystemClock.elapsedRealtime()<deadline
    fun start(advertiseHost:String=""):String {
        check(socket==null){"会话已启动"}
        val random=SecureRandom();secret=ByteArray(32).also(random::nextBytes)
        val identity=LocalTlsIdentity.get(context)
        val alias="local-tls"
        val chain=arrayOf(identity.second)
        fingerprint=MessageDigest.getInstance("SHA-256").digest(chain[0].encoded).joinToString(":"){"%02X".format(it)}
        val manager=object:X509ExtendedKeyManager(){
            override fun chooseServerAlias(k:String?,i:Array<out java.security.Principal>?,s:Socket?)=if(k?.contains("RSA")==true)alias else null
            override fun chooseEngineServerAlias(k:String?,i:Array<out java.security.Principal>?,engine:SSLEngine?)=chooseServerAlias(k,i,null)
            override fun getServerAliases(k:String?,i:Array<out java.security.Principal>?)=if(k=="RSA")arrayOf(alias)else null
            override fun getCertificateChain(a:String?)=if(a==alias)chain else null
            override fun getPrivateKey(a:String?)=if(a==alias)identity.first else null
            override fun chooseClientAlias(k:Array<out String>?,i:Array<out java.security.Principal>?,s:Socket?):String?=null
            override fun getClientAliases(k:String?,i:Array<out java.security.Principal>?):Array<String>?=null
        }
        val tls=SSLContext.getInstance("TLS").apply{init(arrayOf(manager),null,random)}
        val server=(tls.serverSocketFactory.createServerSocket(0,4,InetAddress.getByName("0.0.0.0")) as SSLServerSocket).apply{enabledProtocols=supportedProtocols.filter{it=="TLSv1.2"||it=="TLSv1.3"}.toTypedArray();soTimeout=500}
        socket=server;deadline=SystemClock.elapsedRealtime()+10*60_000
        val host=advertiseHost.ifBlank{lanAddress()};require(host.matches(Regex("[0-9.]+"))){"请使用本机IPv4地址"}
        val token=Base64.getUrlEncoder().withoutPadding().encodeToString(secret)
        url="https://$host:${server.localPort}/#$token"
        scope.launch{while(running){try{val peer=server.accept();if(count.incrementAndGet()>4){count.decrementAndGet();peer.close();continue};clients.add(peer);launch{try{handle(peer)}catch(e:Exception){lastError=if(e is SSLException)generateSequence<Throwable>(e){it.cause}.take(4).joinToString("; "){it.javaClass.simpleName+": "+it.message.orEmpty().take(220)}else e.javaClass.simpleName}finally{clients.remove(peer);count.decrementAndGet();peer.close()}}}catch(_:SocketTimeoutException){}catch(_:Exception){break}};close()}
        return url
    }
    private suspend fun handle(peer:Socket){
        peer.soTimeout=8000;val input=peer.getInputStream();val bytes=ArrayList<Byte>();var tail=""
        while(bytes.size<8192){val b=input.read();if(b<0)return;bytes.add(b.toByte());tail=(tail+b.toChar()).takeLast(4);if(tail=="\r\n\r\n")break}
        if(tail!="\r\n\r\n")return
        val lines=bytes.toByteArray().toString(Charsets.UTF_8).split("\r\n");val request=lines[0].split(' ');if(request.size!=3)return
        val headers=lines.drop(1).filter{it.contains(':')}.associate{it.substringBefore(':').trim().lowercase() to it.substringAfter(':').trim()}
        val host=headers["host"].orEmpty();val origin=headers["origin"]
        // Reject cross-origin browser submissions and DNS rebinding; no wildcard Host acceptance.
        val allowed=(listOf(lanAddress(),"127.0.0.1","localhost",URI(url.substringBefore('#')).host)).any{host=="$it:$port"}
        if(!allowed||(origin!=null&&origin!="https://$host")){reply(peer,403,"application/json","{}");return}
        val method=request[0];val path=request[1]
        if(method=="GET"&&path=="/"){reply(peer,200,"text/html; charset=utf-8",context.assets.open("config-portal.html").bufferedReader().use{it.readText()});return}
        val supplied=headers["authorization"]?.removePrefix("Bearer ").orEmpty()
        val expected=Base64.getUrlEncoder().withoutPadding().encodeToString(secret)
        if(!running||!MessageDigest.isEqual(supplied.toByteArray(),expected.toByteArray())){reply(peer,403,"application/json","{\"message\":\"配对无效或已过期，请重新扫码\"}");return}
        if(method=="GET"&&path=="/api/status"){reply(peer,200,"application/json",status().toString());return}
        val isBackup=path=="/api/backup"
        if(method!="POST"||path !in listOf("/api/config","/api/backup")||headers["content-type"]?.substringBefore(';')!="application/json"||headers.containsKey("transfer-encoding")){reply(peer,400,"application/json","{}");return}
        val size=headers["content-length"]?.toIntOrNull()?:-1;if(size !in 2..if(isBackup)22_000_000 else 131072){reply(peer,413,"application/json","{}");return}
        if(isBackup&&!backupBusy.compareAndSet(false,true)){reply(peer,429,"application/json","{\"message\":\"备份正在处理，请稍候\"}");return}
        try{if(isBackup)peer.soTimeout=30000
        val body=ByteArray(size);var n=0;while(n<size){val got=input.read(body,n,size-n);if(got<0)return;n+=got}
        val result=try{val j=JSONObject(body.toString(Charsets.UTF_8));if(isBackup)backup(j)else apply(j)}catch(_:Exception){JSONObject().put("error",true).put("message",if(isBackup)"迁移操作失败：文件损坏、版本不支持或格式不兼容；原有数据未清空"else"配置未能保存，请在电视检查状态；现有配置保留")}
        reply(peer,200,"application/json",result.toString())
        }finally{if(isBackup)backupBusy.set(false)}
    }
    private fun reply(s:Socket,code:Int,type:String,body:String){val b=body.toByteArray();val h="HTTP/1.1 $code ${if(code==200)"OK"else"Rejected"}\r\nContent-Type: $type\r\nContent-Length: ${b.size}\r\nCache-Control: no-store\r\nReferrer-Policy: no-referrer\r\nX-Frame-Options: DENY\r\nContent-Security-Policy: default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'\r\nConnection: close\r\n\r\n";s.getOutputStream().apply{write(h.toByteArray());write(b);flush()}}
    override fun close(){socket?.let{runCatching{it.close()}};clients.toList().forEach{runCatching{it.close()}};secret.fill(0);scope.cancel();url=""}
    companion object {
        fun lanAddress():String=NetworkInterface.getNetworkInterfaces().toList().filter{it.isUp&&!it.isLoopback}.flatMap{it.inetAddresses.toList()}.filterIsInstance<Inet4Address>().firstOrNull{it.isSiteLocalAddress}?.hostAddress?:"127.0.0.1"
    }
}
