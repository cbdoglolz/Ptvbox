package com.cbdog.ptvbox

import org.json.JSONObject
import org.json.JSONArray
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipInputStream

/** Surface actionable stages, never a transport exception containing an authenticated URL/header. */
fun mediaError053(e:Throwable):String {val text=e.message.orEmpty();val safe=listOf("字幕","下载","压缩包","解压","返回内容","拒绝","请先配置","OpenSubtitles","SubDL","ASSRT","登录返回","弹幕服务","B站弹幕","包内没有","本地压缩包").any{text.startsWith(it)}&&!text.contains(Regex("(?i)https?://|bearer|api.key|password|token="));return if(safe)text.take(180)else when(e){is java.net.SocketTimeoutException->"连接超时，请稍后重试";is java.net.UnknownHostException->"域名解析失败，请检查网络设置";is javax.net.ssl.SSLException->"TLS连接失败，请检查网络或服务状态";is java.net.ConnectException->"网络连接失败，请稍后重试";else->"请求未完成（${e.javaClass.simpleName}），请检查配置、网络或文件格式"}}

data class TextCue053(val start:Long,val end:Long,val text:String)
data class Danmaku053(val time:Long,val text:String,val mode:Int=1,val color:Int=0xffffff)
/** Text-only subtitles. Embedded bitmap subtitles remain in Media3; no stream reload is needed for timing adjustment. */
object MediaText053 {
 fun bounded(input:java.io.InputStream,max:Int):ByteArray {val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(out.size()+n<=max){"文件超过允许大小"};out.write(buffer,0,n)};return out.toByteArray()}
 fun decode(bytes:ByteArray):String {require(bytes.size<=5_000_000){"字幕文件超过5MB"};return when{bytes.size>=2&&bytes[0]==0xff.toByte()&&bytes[1]==0xfe.toByte()->bytes.toString(Charsets.UTF_16LE);bytes.size>=2&&bytes[0]==0xfe.toByte()&&bytes[1]==0xff.toByte()->bytes.toString(Charsets.UTF_16BE);else->runCatching{Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()}.getOrElse{bytes.toString(charset("GB18030"))}}.trimStart('\uFEFF')}
 fun unpack(bytes:ByteArray,name:String):List<Pair<String,String>> {require(bytes.size<=10_000_000){"下载文件过大"};if(bytes.size>3&&bytes[0]==0x50.toByte()&&bytes[1]==0x4b.toByte()) {var total=0;var entries=0;val out=mutableListOf<Pair<String,String>>();ZipInputStream(bytes.inputStream()).use{zip->while(true){val e=zip.nextEntry?:break;require(++entries<=100){"压缩包文件过多"};if(e.isDirectory)continue;val data=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=zip.read(buffer);if(n<0)break;total+=n;require(total<=20_000_000&&data.size()+n<=5_000_000){"解压文件过大"};data.write(buffer,0,n)};if(e.name.substringAfterLast('.').lowercase() in listOf("srt","ass","ssa","vtt")){val text=decode(data.toByteArray());if(cues(text).isNotEmpty())out+=e.name.substringAfterLast('/').substringAfterLast('\\') to text}}};require(out.isNotEmpty()){"压缩包内没有可用文字字幕"};return out};val text=decode(bytes);require(cues(text).isNotEmpty()){"返回内容不是有效字幕（支持SRT/ASS/SSA/VTT；RAR请先解压）"};return listOf(name to text)}
 private fun time(s:String):Long? {val p=s.trim().replace(',','.').split(':');if(p.size !in 2..3)return null;return runCatching{val seconds=p.last().toDouble();val minutes=p[p.size-2].toLong();val hours=if(p.size==3)p[0].toLong()else 0L;((hours*3600+minutes*60+seconds)*1000).toLong()}.getOrNull()}
 fun cues(text:String):List<TextCue053>{if(text.length>5_000_000||text.trimStart().startsWith("<html",true)||text.contains("<!DOCTYPE",true))return emptyList();val out=mutableListOf<TextCue053>();val lines=text.replace("\r","").lines();var i=0
  while(i<lines.size&&out.size<50000){val line=lines[i++];if(line.startsWith("Dialogue:",true)){val p=line.substringAfter(':').split(',',limit=10);if(p.size==10){val a=time(p[1]);val b=time(p[2]);if(a!=null&&b!=null&&b>a)out+=TextCue053(a,b,clean(p[9].replace("\\N","\n").replace("\\n","\n")))}}else if("-->" in line){val p=line.split("-->",limit=2);val a=time(p[0]);val b=time(p[1].trim().substringBefore(' '));val body=mutableListOf<String>();while(i<lines.size&&lines[i].isNotBlank())body+=lines[i++];if(a!=null&&b!=null&&b>a)out+=TextCue053(a,b,clean(body.joinToString("\n")))}}
  return out.filter{it.start>=0&&it.text.isNotBlank()}.sortedBy{it.start}
 }
 private fun clean(s:String)=s.replace(Regex("\\{[^}]*\\}"),"").replace(Regex("<[^>]*>"),"").replace("&amp;","&").replace("&lt;","<").replace("&gt;",">").take(2000)
 fun danmaku(text:String):List<Danmaku053>{require(text.length<=16_000_000);val out=mutableListOf<Danmaku053>();if(text.trimStart().startsWith('<')){require(!text.contains("<!DOCTYPE",true)&&!text.contains("<!ENTITY",true));val nodes=org.jsoup.Jsoup.parse(text,"",org.jsoup.parser.Parser.xmlParser()).getElementsByTag("d");for(n in nodes.take(60000)){val p=n.attr("p").split(',');val t=p.firstOrNull()?.toDoubleOrNull()?:continue;out+=Danmaku053((t*1000).toLong(),n.wholeText().take(160),p.getOrNull(1)?.toIntOrNull()?:1,p.getOrNull(3)?.toIntOrNull()?:0xffffff)}}else{val root=JSONObject(text);val arr=root.optJSONArray("comments")?:root.optJSONArray("danmaku")?:JSONArray();for(j in arr.objects().take(60000)){val p=j.optString("p").split(',');val t=p.firstOrNull()?.toDoubleOrNull()?:j.optDouble("time",-1.0);val body=j.optString("m",j.optString("text"));if(t>=0&&body.isNotBlank())out+=Danmaku053((t*1000).toLong(),body.take(160),p.getOrNull(1)?.toIntOrNull()?:1,p.getOrNull(2)?.toIntOrNull()?:0xffffff)}};return out.filter{it.time>=0&&it.text.isNotBlank()}.sortedBy{it.time}}
}
data class MediaContext053(val work:Work,val episode:Int?,val tmdbId:String=if(work.provider=="tmdb")work.id else ""){
 val names get()=(listOf(work.title,work.original)+work.aliases).map{Identity.searchTitle(it)}.filter{it.isNotBlank()}.distinct().take(5)
 val key get()=work.key+"|S${work.season}|E${episode?:0}"
 fun accepts(name:String,season:Int?=null,ep:Int?=null):Boolean {
  val s=season?.takeIf{it>0}?:Identity.season(name);val e=ep?.takeIf{it>0}?:Identity.episode(name)
  if(work.kind=="tv"&&(s!=null&&s!=work.season||e!=null&&episode!=null&&e!=episode))return false
  val years=Regex("(?<!\\d)(?:19|20)\\d{2}(?!\\d)").findAll(name).map{it.value}.toList();if(work.kind=="movie"&&work.year.length==4&&years.isNotEmpty()&&work.year !in years)return false
  val all=Identity.normalize(names.joinToString(" "));val normalizedName=Identity.normalize(name);for(term in listOf("2045","arise","stand alone complex","making of","the making","ova","preview","trailer"))if(normalizedName.contains(Identity.normalize(term))&&!all.contains(Identity.normalize(term)))return false
  return true
 }
 fun exactName(name:String)=names.any{Identity.base(it)==Identity.base(name)}
}
