package com.cbdog.ptvbox

import androidx.media3.datasource.*
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Resolve chained Matroska SeekHeads without downloading media or estimating byte positions. */
object MkvSeek051 {
 data class Patch(val bytes:Map<Long,Byte>)
 private data class Element(val id:Long,val data:Int,val end:Int)
 private fun number(b:ByteArray,p:Int,id:Boolean=false):Pair<Long,Int>{require(p in b.indices);val first=b[p].toInt() and 255;require(first!=0);val n=Integer.numberOfLeadingZeros(first)-24+1;require(n<=8&&p+n<=b.size);var v=(first and if(id)255 else (255 ushr n)).toLong();for(i in 1 until n)v=(v shl 8)or(b[p+i].toLong()and 255);return v to p+n}
 private fun elements(b:ByteArray,start:Int,end:Int):List<Element>{val out=mutableListOf<Element>();var p=start;while(p<end){val(id,q)=number(b,p,true);val(size,data)=number(b,q);require(size<=Int.MAX_VALUE&&data+size<=end);val next=data+size.toInt();out+=Element(id,data,next);p=next};return out}
 private fun uint(b:ByteArray,e:Element):Long {require(e.end-e.data in 1..8);return (e.data until e.end).fold(0L){v,n->(v shl 8)or(b[n].toLong()and 255)}}
 private fun refs(b:ByteArray,root:Int):List<Pair<Element,Element>>{val(id,q)=number(b,root,true);require(id==0x114d9b74L);val(n,p)=number(b,q);require(n<=65536);return elements(b,p,p+n.toInt()).filter{it.id==0x4dbbL}.mapNotNull{val es=elements(b,it.data,it.end);val a=es.firstOrNull{e->e.id==0x53abL};val z=es.firstOrNull{e->e.id==0x53acL};if(a==null||z==null)null else a to z}}
 fun patch(head:ByteArray,load:(Long)->ByteArray):Patch? {if(head.size<52||!head.take(4).toByteArray().contentEquals(byteArrayOf(0x1a,0x45,0xdf.toByte(),0xa3.toByte())))return null
  var p=0;var segment=0L
  while(p<head.size){val(id,q)=number(head,p,true);val(n,d)=number(head,q);if(id==0x18538067L){segment=d.toLong();p=d;break};require(n<Int.MAX_VALUE);p=d+n.toInt()}
  if(segment==0L)return null
  val roots=refs(head,p);if(roots.any{uint(head,it.first)==0x1c53bb6bL})return null
  val chain=roots.firstOrNull{uint(head,it.first)==0x114d9b74L}?:return null
  var offset=uint(head,chain.second);val seen=mutableSetOf<Long>();var cues:Long?=null
  repeat(4){if(cues==null){require(seen.add(offset));val b=load(segment+offset);val r=refs(b,0);cues=r.firstOrNull{uint(b,it.first)==0x1c53bb6bL}?.let{uint(b,it.second)};if(cues==null)offset=r.firstOrNull{uint(b,it.first)==0x114d9b74L}?.let{uint(b,it.second)}?:return null}}
  val target=cues?:return null;val width=chain.second.end-chain.second.data;require(width>=1&&(width==8||target ushr(width*8)==0L));require(chain.first.end-chain.first.data==4)
  val changes=mutableMapOf<Long,Byte>();byteArrayOf(0x1c,0x53,0xbb.toByte(),0x6b).forEachIndexed{n,b->changes[(chain.first.data+n).toLong()]=b};repeat(width){n->changes[(chain.second.data+n).toLong()]=(target ushr((width-1-n)*8)).toByte()};return Patch(changes)
 }
 fun load(c:Candidate):Patch? {val client=NetworkPolicy.client().newBuilder().callTimeout(12,TimeUnit.SECONDS).followRedirects(true).build()
  fun range(start:Long):ByteArray {val req=Request.Builder().url(c.episode.url).header("User-Agent",NetworkPolicy.userAgent).apply{c.site.headers.forEach{(k,v)->header(k,v)}}.header("Range","bytes=$start-${start+65535}").header("Accept-Encoding","identity").build();client.newCall(req).execute().use{r->if(r.code!=206||!r.header("Content-Range").orEmpty().startsWith("bytes $start-"))throw IOException("线路不支持索引范围读取");return r.body.byteStream().use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(4096);while(out.size()<65536){val n=input.read(buffer,0,minOf(buffer.size,65536-out.size()));if(n<0)break;out.write(buffer,0,n)};out.toByteArray()}}}
  return patch(range(0),::range)
 }
 fun factory(base:DataSource.Factory,patch:Patch)=DataSource.Factory {val source=base.createDataSource();object:DataSource by source {var position=0L;override fun open(dataSpec:DataSpec):Long{position=dataSpec.position;return source.open(dataSpec)};override fun read(buffer:ByteArray,offset:Int,length:Int):Int{val n=source.read(buffer,offset,length);if(n>0){patch.bytes.forEach{(at,b)->if(at>=position&&at<position+n)buffer[offset+(at-position).toInt()]=b};position+=n};return n}}}
}
