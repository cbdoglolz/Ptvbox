package com.cbdog.ptvbox

import java.io.*
import java.net.*
import java.lang.reflect.Method
import java.util.concurrent.*

/** TVBox's loopback /proxy transport, deliberately distinct from the authenticated LAN settings server. */
class SpiderProxy(loader:ClassLoader):Closeable {
    private val method:Method=loader.loadClass("com.github.catvod.spider.Proxy").getMethod("proxy",Map::class.java)
    private val socket:ServerSocket=(9978..9999).firstNotNullOfOrNull{port->runCatching{ServerSocket(port,8,InetAddress.getByName("127.0.0.1"))}.getOrNull()}?:throw IOException("本机播放代理端口已占用")
    val port get()=socket.localPort
    private val pool=ThreadPoolExecutor(8,8,10,TimeUnit.SECONDS,ArrayBlockingQueue(32)){r->Thread(r,"ptv-spider-proxy").apply{isDaemon=true}}
    init{Thread({while(!socket.isClosed){try{val peer=socket.accept();try{pool.execute{peer.use{runCatching{serve(it)}}}}catch(_:RejectedExecutionException){peer.close()}}catch(_:IOException){break}}},"ptv-proxy-accept").apply{isDaemon=true;start()}}
    private fun serve(peer:Socket){peer.soTimeout=15000;val reader=peer.getInputStream().bufferedReader();val request=reader.readLine()?.takeIf{it.length<8192}?.split(' ')?:return;if(request.size!=3||request[0]!="GET")return
        val headers=mutableMapOf<String,String>();var size=0
        while(true){val line=reader.readLine()?:return;if(line.isEmpty())break;size+=line.length;if(size>8192)return;if(line.contains(':'))headers[line.substringBefore(':').lowercase()]=line.substringAfter(':').trim()}
        val origin=headers["origin"];if(origin!=null&&!origin.startsWith("http://127.0.0.1:"))return
        val uri=URI("http://127.0.0.1"+request[1]);if(uri.path!="/proxy")return
        val params=uri.rawQuery.orEmpty().split('&').filter{it.isNotEmpty()}.associate{URLDecoder.decode(it.substringBefore('='),"UTF-8") to URLDecoder.decode(it.substringAfter('=',""),"UTF-8")}.toMutableMap()
        headers["range"]?.let{params["Range"]=it;params["range"]=it}
        if(params["do"]=="ck"){peer.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".toByteArray());return}
        val result=method.invoke(null,params) as? Array<*>?:return;if(result.size<3)return;val code=(result[0] as? Number)?.toInt()?:502;val mime=result[1]?.toString()?.takeIf{!it.contains('\r')&&!it.contains('\n')}?:"application/octet-stream"
        val stream=result[2] as? InputStream?:return
        stream.use{input->val out=peer.getOutputStream();val extra=(result.getOrNull(3) as? Map<*,*>).orEmpty().entries.filter{it.key?.toString()?.lowercase() in listOf("content-range","accept-ranges","location")&&it.value?.toString()?.none{c->c=='\r'||c=='\n'}==true}.joinToString(""){"${it.key}: ${it.value}\r\n"};out.write("HTTP/1.1 $code OK\r\nContent-Type: $mime\r\n$extra Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n".replace("\r\n Transfer","\r\nTransfer").toByteArray());val buffer=ByteArray(32768);while(true){val count=input.read(buffer);if(count<0)break;if(count==0)continue;out.write((count.toString(16)+"\r\n").toByteArray());out.write(buffer,0,count);out.write("\r\n".toByteArray());out.flush()};out.write("0\r\n\r\n".toByteArray());out.flush()}
    }
    fun setPort(loader:ClassLoader){runCatching{val c=loader.loadClass("com.github.catvod.spider.ProxyOrigin");for(name in listOf("yq","port","proxyPort","serverPort"))runCatching{val f=c.getDeclaredField(name);if(f.type==Int::class.javaPrimitiveType&&java.lang.reflect.Modifier.isStatic(f.modifiers)){f.isAccessible=true;f.setInt(null,port)}}}}
    override fun close(){runCatching{socket.close()};pool.shutdownNow()}
}
