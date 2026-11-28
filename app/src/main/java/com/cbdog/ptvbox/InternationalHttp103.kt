package com.cbdog.ptvbox

import kotlinx.coroutines.CancellationException
import java.util.concurrent.ConcurrentHashMap

/** Preserve the proven direct route. Only fall back to the owner's configured proxy after failure. */
internal object InternationalHttp103 {
    private val directRoutes=ConcurrentHashMap<String,Boolean>()
    fun directFor(url:String)=directRoutes[url]?:true
    suspend fun bytes(url:String,headers:Map<String,String> = emptyMap(),limit:Int=4_000_000,truncate:Boolean=false,media:Boolean=false):ByteArray {
        val preferred=directFor(url)
        suspend fun read(direct:Boolean)=Net.bytes(url,headers,limit,truncate,timeoutSeconds=12,direct=direct).also{if(media&&!MediaBytes052.accept(it))throw java.io.IOException("国际线路返回网页而非媒体")}
        try{return read(preferred).also{remember(url,preferred)}}catch(e:CancellationException){throw e}catch(first:Exception){
            if(NetworkPolicy.client().proxy==null)throw first
            return read(!preferred).also{remember(url,!preferred)}
        }
    }
    private fun remember(url:String,direct:Boolean){if(directRoutes.size>1024)directRoutes.clear();directRoutes[url]=direct}
}
