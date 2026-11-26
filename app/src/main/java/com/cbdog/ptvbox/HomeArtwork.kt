package com.cbdog.ptvbox

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

/** Public poster thumbnails only. Never exposes the database, credentials or private files. */
class HomeArtwork(private val context:Context){
    companion object { private val downloads=Semaphore(4) }
    fun trim(protectedNames:Set<String>){
        val files=File(context.filesDir,"home-art").listFiles()?.filter{it.isFile&&it.name.matches(Regex("[0-9a-f]{64}\\.jpg"))}.orEmpty()
        var total=files.sumOf{it.length()}
        for(file in files.sortedBy{it.lastModified()}){if(total<=100L*1024*1024)break;if(file.name !in protectedNames){val size=file.length();if(file.delete())total-=size}}
    }
    suspend fun uri(work:Work):Uri?=uriFromUrl(work.poster)
    suspend fun uriFromUrl(rawUrl:String,landscape:Boolean=false):Uri?=withContext(Dispatchers.IO){
        if(rawUrl.isBlank())return@withContext null
        val source=Uri.parse(rawUrl);val host=source.host.orEmpty().lowercase()
        val allowed=source.scheme=="https"&&(host.matches(Regex("img[0-9]*\\.doubanio\\.com"))||host=="image.tmdb.org"||host.endsWith(".bgm.tv")||host.endsWith(".anilist.co"))
        if(!allowed)return@withContext null
        val hash=MessageDigest.getInstance("SHA-256").digest((rawUrl+(if(landscape)"#16x9"else"")).toByteArray()).joinToString(""){"%02x".format(it)}
        val dir=File(context.filesDir,"home-art").apply{mkdirs()};val target=File(dir,"$hash.jpg")
        if(!target.isFile)downloads.withPermit{
            if(!target.isFile)runCatching{
                val bytes=Net.bytes(rawUrl,mapOf("Referer" to "https://movie.douban.com/","User-Agent" to "Mozilla/5.0"),2_500_000)
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                check(bounds.outWidth in 16..8192&&bounds.outHeight in 16..8192)
                val maxW=if(landscape)960 else 640;val maxH=if(landscape)540 else 960
                val options=BitmapFactory.Options().apply{var sample=1;while(bounds.outWidth/sample>maxW||bounds.outHeight/sample>maxH)sample*=2;inSampleSize=sample}
                val bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)?:error("Invalid poster")
                val temporary=File.createTempFile("poster-",".tmp",dir)
                try{temporary.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,88,it))};if(!temporary.renameTo(target)&&!target.isFile)error("Poster save failed")}finally{temporary.delete();bitmap.recycle()}
            }
        }
        if(target.isFile){target.setLastModified(System.currentTimeMillis());Uri.Builder().scheme("content").authority("${context.packageName}.homeart").appendPath("art").appendPath(target.name).build()}else null
    }
}

class HomeArtworkProvider:ContentProvider(){
    override fun onCreate()=true
    override fun getType(uri:Uri)="image/jpeg"
    override fun openFile(uri:Uri,mode:String):ParcelFileDescriptor {
        if(mode!="r"||uri.pathSegments.size!=2||uri.pathSegments[0]!="art"||!uri.pathSegments[1].matches(Regex("[0-9a-f]{64}\\.jpg")))throw FileNotFoundException("Unknown public artwork")
        val dir=File(requireNotNull(context).filesDir,"home-art").canonicalFile;val file=File(dir,uri.pathSegments[1]).canonicalFile
        if(file.parentFile!=dir||!file.isFile)throw FileNotFoundException("Artwork unavailable")
        return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
    }
    override fun query(uri:Uri,projection:Array<out String>?,selection:String?,selectionArgs:Array<out String>?,sortOrder:String?):Cursor?=null
    override fun insert(uri:Uri,values:ContentValues?):Uri?=throw UnsupportedOperationException("Read-only public artwork")
    override fun update(uri:Uri,values:ContentValues?,selection:String?,selectionArgs:Array<out String>?):Int=throw UnsupportedOperationException("Read-only public artwork")
    override fun delete(uri:Uri,selection:String?,selectionArgs:Array<out String>?):Int=throw UnsupportedOperationException("Read-only public artwork")
}
