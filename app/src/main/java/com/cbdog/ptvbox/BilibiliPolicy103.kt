package com.cbdog.ptvbox

import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

internal object BilibiliPolicy103 {
    // Search pubtime is the original release date; season.publish is the platform upload date.
    fun acceptsYear(year:String,releaseTimestamp:Long):Boolean {
        if(year.length!=4||releaseTimestamp !in -5364662400L..4133980799L)return true
        return Instant.ofEpochSecond(releaseTimestamp).atOffset(ZoneOffset.UTC).year.toString()==year
    }
    fun decode(bytes:ByteArray):String {
        val text=bytes.toString(Charsets.UTF_8).trimStart('\uFEFF',' ','\n','\r','\t')
        if(text.startsWith("<")||text.startsWith("{")||text.startsWith("["))return text
        // The legacy comment endpoint also serves raw DEFLATE without Content-Encoding.
        val inflater=Inflater(true)
        return try{InflaterInputStream(ByteArrayInputStream(bytes),inflater).use{MediaText053.bounded(it,16_000_000)}.toString(Charsets.UTF_8)}finally{inflater.end()}
    }
}
