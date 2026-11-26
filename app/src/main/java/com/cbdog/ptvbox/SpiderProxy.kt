package com.cbdog.ptvbox

import java.io.*
import java.net.*
import java.lang.reflect.Method
import java.util.concurrent.*

/** TVBox's loopback /proxy transport, deliberately distinct from the authenticated LAN settings server. */
class SpiderProxy(loader:ClassLoader):Closeable {
    private data class CachedPlaylist(val code:Int,val mime:String,val body:ByteArray,val cachedAt:Long)
    private val method:Method=loader.loadClass("com.github.catvod.spider.Proxy").getMethod("proxy",Map::class.java)
    private val socket:ServerSocket=(9978..9999).firstNotNullOfOrNull{port->runCatching{ServerSocket(port,8,InetAddress.getByName("127.0.0.1"))}.getOrNull()}?:throw IOException("本机播放代理端口已占用")
    val port get()=socket.localPort
    private val pool=ThreadPoolExecutor(8,8,10,TimeUnit.SECONDS,ArrayBlockingQueue(32)){r->Thread(r,"ptv-spider-proxy").apply{isDaemon=true}}
    private val playlistCache=ConcurrentHashMap<String,CachedPlaylist>()
    init{Thread({while(!socket.isClosed){try{val peer=socket.accept();try{pool.execute{peer.use{runCatching{serve(it)}}}}catch(_:RejectedExecutionException){peer.close()}}catch(_:IOException){break}}},"ptv-proxy-accept").apply{isDaemon=true;start()}}
    private fun writeBytes(out:OutputStream,code:Int,mime:String,bytes:ByteArray,extra:String=""){
        out.write("HTTP/1.1 $code OK\r\nContent-Type: $mime\r\n${extra}Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes);out.flush()
    }
    private fun serve(peer:Socket){peer.soTimeout=15000;val reader=peer.getInputStream().bufferedReader();val request=reader.readLine()?.takeIf{it.length<8192}?.split(' ')?:return;if(request.size!=3||request[0]!="GET")return
        val headers=mutableMapOf<String,String>();var size=0
        while(true){val line=reader.readLine()?:return;if(line.isEmpty())break;size+=line.length;if(size>8192)return;if(line.contains(':'))headers[line.substringBefore(':').lowercase()]=line.substringAfter(':').trim()}
        val origin=headers["origin"];if(origin!=null&&!origin.startsWith("http://127.0.0.1:"))return
        val uri=URI("http://127.0.0.1"+request[1]);if(uri.path!="/proxy")return
        val cacheKey=uri.rawQuery.orEmpty()
        val now=System.currentTimeMillis()
        playlistCache[cacheKey]?.takeIf{now-it.cachedAt<30*60*1000L}?.let{hit->writeBytes(peer.getOutputStream(),hit.code,hit.mime,hit.body);return}
        val params=cacheKey.split('&').filter{it.isNotEmpty()}.associate{URLDecoder.decode(it.substringBefore('='),"UTF-8") to URLDecoder.decode(it.substringAfter('=',""),"UTF-8")}.toMutableMap()
        headers["range"]?.let{params["Range"]=it;params["range"]=it}
        if(params["do"]=="ck"){peer.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok".toByteArray());return}
        val result=method.invoke(null,params) as? Array<*>?:return;if(result.size<3)return;val code=(result[0] as? Number)?.toInt()?:502;val mime=result[1]?.toString()?.takeIf{!it.contains('\r')&&!it.contains('\n')}?:"application/octet-stream"
        val stream=result[2] as? InputStream?:return
        val extra=(result.getOrNull(3) as? Map<*,*>).orEmpty().entries.filter{it.key?.toString()?.lowercase() in listOf("content-range","accept-ranges","location")&&it.value?.toString()?.none{c->c=='\r'||c=='\n'}==true}.joinToString(""){"${it.key}: ${it.value}\r\n"}
        val maybePlaylist=mime.contains("mpegurl",true)||mime.contains("text",true)||params["do"].orEmpty().contains("m3u8",true)||params["url"].orEmpty().contains(".m3u8",true)
        stream.use{input->
            val out=peer.getOutputStream()
            if(maybePlaylist&&code==200){
                val baos=ByteArrayOutputStream()
                val buf=ByteArray(16384);var totalRead=0;var overflow=false
                while(true){val n=input.read(buf);if(n<0)break;if(n==0)continue;totalRead+=n;if(totalRead>2_000_000){overflow=true;break};baos.write(buf,0,n)}
                if(!overflow){
                    var rawBytes=baos.toByteArray()
                    val text=rawBytes.toString(Charsets.UTF_8)
                    if(text.trimStart().startsWith("#EXTM3U")){
                        val totalSec=Regex("#EXTINF:([0-9.]+)").findAll(text).sumOf{it.groupValues[1].toDoubleOrNull()?:0.0}
                        val hasEnd=text.contains("#EXT-X-ENDLIST")
                        val isVod=hasEnd||totalSec>=180.0
                        if(isVod){
                            if(!hasEnd){rawBytes=(text.trimEnd()+"\n#EXT-X-ENDLIST\n").toByteArray(Charsets.UTF_8)}
                            if(playlistCache.size>=64)playlistCache.clear()
                            playlistCache[cacheKey]=CachedPlaylist(code,mime,rawBytes,now)
                        }
                    }
                    writeBytes(out,code,mime,rawBytes,extra)
                    return
                }
                // Fallback if unexpectedly huge
                out.write("HTTP/1.1 $code OK\r\nContent-Type: $mime\r\n${extra}Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n".toByteArray())
                val first=baos.toByteArray();if(first.isNotEmpty()){out.write((first.size.toString(16)+"\r\n").toByteArray());out.write(first);out.write("\r\n".toByteArray())}
                while(true){val count=input.read(buf);if(count<0)break;if(count==0)continue;out.write((count.toString(16)+"\r\n").toByteArray());out.write(buf,0,count);out.write("\r\n".toByteArray());out.flush()}
                out.write("0\r\n\r\n".toByteArray());out.flush();return
            }
            out.write("HTTP/1.1 $code OK\r\nContent-Type: $mime\r\n$extra Transfer-Encoding: chunked\r\nConnection: close\r\n\r\n".replace("\r\n Transfer","\r\nTransfer").toByteArray());val buffer=ByteArray(32768);while(true){val count=input.read(buffer);if(count<0)break;if(count==0)continue;out.write((count.toString(16)+"\r\n").toByteArray());out.write(buffer,0,count);out.write("\r\n".toByteArray());out.flush()};out.write("0\r\n\r\n".toByteArray());out.flush()
        }
    }
    fun setPort(loader:ClassLoader){runCatching{val c=loader.loadClass("com.github.catvod.spider.ProxyOrigin");for(name in listOf("yq","port","proxyPort","serverPort"))runCatching{val f=c.getDeclaredField(name);if(f.type==Int::class.javaPrimitiveType&&java.lang.reflect.Modifier.isStatic(f.modifiers)){f.isAccessible=true;f.setInt(null,port)}}}}
    override fun close(){playlistCache.clear();runCatching{socket.close()};pool.shutdownNow()}
}
