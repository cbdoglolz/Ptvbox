package com.cbdog.ptvbox
import kotlinx.coroutines.CancellationException

/** The subscription's room ID is already platform$room. Keep the stable source/room identity,
 * resolve that exact room with its native module instead of consuming a short proxy response. */
suspend fun Sites.liveDetail054(entry:LiveEntry050):LiveEntry050 {
 if(entry.site.api=="csp_Live"&&entry.vod.id.matches(Regex("(?:huya|douyu|bili|cc)\\$[0-9]+"))){
  val native=all().firstOrNull{it.api.contains("Alllive")&&it.type==3}
  if(native!=null)try{val adapter=native.copy(key=entry.site.key,name=entry.site.name+" · 原生房间");val vod=detail(adapter,entry.vod.copy(id=entry.vod.id.replace('$','_'),roads=emptyList()));if(vod.roads.any{it.episodes.isNotEmpty()})return LiveEntry050(adapter,vod.copy(id=entry.vod.id,title=entry.vod.title,poster=entry.vod.poster.ifBlank{vod.poster}))}catch(e:CancellationException){throw e}catch(_:Exception){}
 }
 val query=if(entry.site.api.contains("Alllive")&&entry.vod.id.matches(Regex("(?:huya|douyu|bili|cc)\\$[0-9]+")))entry.vod.copy(id=entry.vod.id.replace('$','_'),roads=emptyList())else if(entry.site.type==3)entry.vod.copy(roads=emptyList())else entry.vod
 return entry.copy(vod=detail(entry.site,query).copy(id=entry.vod.id,title=entry.vod.title))
}
class LiveRetry054 {
 private var start=0L;var attempts=0;private set
 fun reset(now:Long){start=now;attempts=0}
 fun next(now:Long):Long? {if(now-start>90000)reset(now);if(attempts>=3)return null;attempts++;return attempts*1500L}
}
