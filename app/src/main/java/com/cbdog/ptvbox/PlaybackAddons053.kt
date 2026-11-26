package com.cbdog.ptvbox

import androidx.compose.runtime.*
import androidx.media3.common.C
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

/** Independent jobs survive closing panels, but never leak results across works/episodes. */
class PlaybackAddons053(private val m:PtvModel,private val scope:CoroutineScope){
 var results by mutableStateOf<List<Subtitle053>>(emptyList());private set
 var files by mutableStateOf<List<Subtitle053>>(emptyList());private set
 var searching by mutableStateOf(false);private set
 var downloading by mutableStateOf(false);private set
 var badge by mutableStateOf("");private set
 var status by mutableStateOf("");private set
 var cues by mutableStateOf<List<TextCue053>>(emptyList());private set
 var selected by mutableStateOf("");private set
 var selectedTitle by mutableStateOf("");private set
 var delayMs by mutableLongStateOf(0);private set
 var danmaku by mutableStateOf<List<Danmaku053>>(emptyList());private set
 var danmakuStatus by mutableStateOf("");private set
 var matches by mutableStateOf<List<DanmakuMatch053>>(emptyList());private set
 private var context:MediaContext053?=null;private var job:Job?=null;private var download:Job?=null;private var dmJob:Job?=null;private var badgeJob:Job?=null
 private val service=OnlineSubtitles053(m.db.secrets);private val dm=DanmakuService053(m.db.secrets)
 private fun file(c:MediaContext053,suffix:String)=File(m.db.userMediaDir,MessageDigest.getInstance("SHA-256").digest((c.key+suffix).toByteArray()).joinToString(""){"%02x".format(it)}+".json")
 fun bind(work:Work,episode:String){val linked=m.linkedRatings[work.ratingViewKey]?.get("tmdb");val enriched=if(linked!=null)work.enriched(linked)else work;val c=MediaContext053(enriched,if(work.kind=="tv")Identity.episode(episode)else null,linked?.id?:if(work.provider=="tmdb")work.id else "");if(context?.key==c.key)return;clear();context=c;m.player.trackSelectionParameters=m.player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,false).build()
  scope.launch{val saved=withContext(Dispatchers.IO){runCatching{JSONObject(file(c,"subtitle").readText())}.getOrNull()};if(context?.key!=c.key)return@launch;if(saved!=null){val text=saved.optString("text");val parsed=withContext(Dispatchers.Default){MediaText053.cues(text)};if(context?.key==c.key&&parsed.isNotEmpty()){cues=parsed;selected=saved.optString("id");selectedTitle=saved.optString("title");delayMs=saved.optLong("delay");disableEmbedded();announce("已恢复上次字幕：$selectedTitle")}}}
  if(work.provider!="live"&&m.config.flag("danmaku_enabled",true))searchDanmaku()
 }
 fun clear(){job?.cancel();download?.cancel();dmJob?.cancel();badgeJob?.cancel();context=null;results=emptyList();files=emptyList();searching=false;downloading=false;badge="";status="";cues=emptyList();selected="";selectedTitle="";delayMs=0;danmaku=emptyList();matches=emptyList();danmakuStatus=""}
 private fun announce(text:String){badgeJob?.cancel();badge=text;badgeJob=scope.launch{delay(7000);badge=""}}
 private fun disableEmbedded(){m.player.trackSelectionParameters=m.player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT,true).build()}
 fun off(){cues=emptyList();selected="";selectedTitle="";disableEmbedded();context?.let{c->scope.launch(Dispatchers.IO){file(c,"subtitle").delete()}}}
 fun embedded(){cues=emptyList();selected="";selectedTitle="";context?.let{c->scope.launch(Dispatchers.IO){file(c,"subtitle").delete()}}}
 fun search(){if(searching)return;val c=context?:return;job=scope.launch{searching=true;files=emptyList();results=emptyList();status="正在搜索中文字幕…";badgeJob?.cancel();badge="正在搜索字幕";val errors=mutableListOf<String>();try{val providers=service.configured();if(providers.isEmpty()){status="请先在字幕服务设置中保存SubDL、ASSRT或OpenSubtitles凭据"}else{
  supervisorScope{providers.map{p->async{try{val found=withTimeout(60000){withContext(Dispatchers.IO){service.searchExpanded(p,c)}};if(context?.key==c.key){results=(results+found).distinctBy{it.key};status="$p 返回${found.size}条"}}catch(e:TimeoutCancellationException){errors+="$p：搜索超时，可稍后重试"}catch(e:CancellationException){throw e}catch(e:Exception){errors+="$p：${mediaError053(e)}"}}}.awaitAll()};status="发现${results.size}条"+(if(errors.isNotEmpty())" · "+errors.joinToString("；")else" · 请核对年份、季集和压制版本")
 };announce("字幕搜索完成，发现${results.size}条，可在字幕中查看") }finally{searching=false}}
 }
 fun choose(s:Subtitle053){if(downloading)return;val c=context?:return;download=scope.launch{downloading=true;status=if(s.pack)"正在读取字幕包文件…"else"正在下载字幕…";try{if(s.pack){files=withContext(Dispatchers.IO){service.files(s,c)};status="先选字幕包，再选对应文件 · ${files.size}个文件"}else{val downloaded=withContext(Dispatchers.IO){service.download(s)};val acceptable=downloaded.filter{c.accepts(it.first)};require(acceptable.isNotEmpty()){"包内没有符合当前季集的字幕"};if(acceptable.size==1&&!s.archive)install(c,s,acceptable.first().second)else{pendingTexts=acceptable.toMap();files=acceptable.map{(name,_)->Subtitle053("缓存",s.key+name,name,s.detail,"memory:$name")};status="已下载，请选择对应字幕文件"}}}catch(e:CancellationException){throw e}catch(e:Exception){status="字幕加载失败：${mediaError053(e)}";announce(status)}finally{downloading=false}}}
 private var pendingTexts:Map<String,String> = emptyMap()
 fun chooseFile(s:Subtitle053){if(s.url.startsWith("memory:")){val c=context?:return;val text=pendingTexts[s.url.removePrefix("memory:")]?:return;scope.launch{install(c,s,text)}}else choose(s)}
 private suspend fun install(c:MediaContext053,s:Subtitle053,text:String){val parsed=withContext(Dispatchers.Default){MediaText053.cues(text)};require(parsed.isNotEmpty()){"字幕中没有有效时间轴"};if(context?.key!=c.key)return;cues=parsed;selected=s.key;selectedTitle=s.title;delayMs=0;disableEmbedded();withContext(Dispatchers.IO){file(c,"subtitle").writeText(JSONObject().put("text",text).put("id",selected).put("title",selectedTitle).put("delay",0).toString())};status="字幕已加载，共${parsed.size}句";announce("字幕已加载：${s.title}")}
 fun local(bytes:ByteArray,name:String){val c=context?:return;scope.launch{try{val parsed=withContext(Dispatchers.IO){MediaText053.unpack(bytes,name)};require(parsed.size==1){"本地压缩包请先解压，再选择字幕文件"};install(c,Subtitle053("本地",name,name),parsed.first().second)}catch(e:Exception){status="导入失败：${mediaError053(e)}";announce(status)}}}
 fun adjust(ms:Long){delayMs=(delayMs+ms).coerceIn(-3_600_000,3_600_000);persistDelay()}
 fun align(cue:TextCue053){delayMs=m.player.currentPosition-cue.start;persistDelay();announce("已将该句对准当前画面")}
 private fun persistDelay(){val c=context?:return;val value=delayMs;scope.launch(Dispatchers.IO){runCatching{val f=file(c,"subtitle");val j=JSONObject(f.readText());j.put("delay",value);f.writeText(j.toString())}}}
 fun searchDanmaku(){val c=context?:return;if(c.work.provider=="live")return;dmJob?.cancel();dmJob=scope.launch{danmakuStatus="正在匹配弹幕…";try{val cache=withContext(Dispatchers.IO){runCatching{JSONObject(file(c,"danmaku").readText())}.getOrNull()};if(cache!=null&&System.currentTimeMillis()-cache.optLong("created")<86400000){danmaku=withContext(Dispatchers.Default){MediaText053.danmaku(cache.getString("data"))};danmakuStatus="缓存弹幕 ${danmaku.size}条";return@launch};matches=withContext(Dispatchers.IO){dm.matches(c)};if(matches.size==1)loadDanmaku(matches.first())else danmakuStatus=if(matches.isEmpty())"未找到同作品同季集弹幕，可手动重试或导入"else"找到${matches.size}个候选，请核对后选择"}catch(e:CancellationException){throw e}catch(e:Exception){danmakuStatus="弹幕匹配失败：${mediaError053(e)}"}}}
 fun loadDanmaku(match:DanmakuMatch053){val c=context?:return;scope.launch{try{val list=withContext(Dispatchers.IO){dm.comments(match.id)};if(context?.key!=c.key)return@launch;danmaku=list;danmakuStatus="${match.title} ${match.episode} · ${list.size}条";val arr=JSONArray(list.map{JSONObject().put("p","${it.time/1000.0},${it.mode},${it.color},0").put("m",it.text)});withContext(Dispatchers.IO){file(c,"danmaku").writeText(JSONObject().put("created",System.currentTimeMillis()).put("data",JSONObject().put("comments",arr).toString()).toString())}}catch(e:CancellationException){throw e}catch(e:Exception){danmakuStatus="弹幕加载失败：${mediaError053(e)}"}}}
 fun localDanmaku(bytes:ByteArray){scope.launch{try{danmaku=withContext(Dispatchers.Default){MediaText053.danmaku(MediaText053.decode(bytes))};danmakuStatus="已导入${danmaku.size}条弹幕"}catch(_:Exception){danmakuStatus="弹幕文件无效，支持JSON或B站XML"}}}
}
