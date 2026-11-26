package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import kotlinx.coroutines.*

@Composable fun ProviderRatingRows(m:PtvModel,w:Work){val c=LocalCinema.current;var revision by remember(w.key){mutableIntStateOf(0)};var edit by remember{mutableStateOf("")};var query by remember{mutableStateOf("")};var choices by remember{mutableStateOf<List<Work>>(emptyList())};var error by remember{mutableStateOf("")};var loading by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();val series by m.details.series.collectAsState();val artwork by m.details.artwork.collectAsState()
 LaunchedEffect(w.key,w.kind,w.season,w.catalogSeason,revision,m.tmdbConfigured,series?.id,artwork){val matches=coroutineScope{listOf("douban","tmdb").map{p->async(Dispatchers.IO){p to runCatching{m.metadata.ratingWork(w,p)}.getOrNull()}}.awaitAll()};m.linkedRatings=m.linkedRatings+(w.ratingViewKey to (m.linkedRatings[w.ratingViewKey].orEmpty()+matches))}
 Row(Modifier.fillMaxWidth().background(c.surface.copy(alpha=.82f),RoundedCornerShape(8.dp)).padding(5.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
  for(p in listOf("douban","tmdb")){val data=m.linkedRatings[w.ratingViewKey]?.get(p);val matched=data!=null;val tint=if(!matched)c.muted else if(p=="douban")PcGreen else PcCyan
   Row(Modifier.weight(1f).height(36.dp),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
    Box(Modifier.width(40.dp).height(24.dp),contentAlignment=androidx.compose.ui.Alignment.CenterStart){Text(if(p=="douban")"豆瓣"else"TMDB",color=tint,fontSize=12.sp,lineHeight=24.sp,maxLines=1)}
    Box(Modifier.widthIn(min=34.dp).height(24.dp),contentAlignment=androidx.compose.ui.Alignment.Center){Text(data?.rating?:"-",color=tint,fontSize=18.sp,lineHeight=24.sp,maxLines=1)}
    Box(Modifier.weight(1f).height(24.dp),contentAlignment=androidx.compose.ui.Alignment.CenterStart){Text(if(data!=null&&data.votes>0)"${data.votes}人"else if(data!=null)"暂无人数"else if(p=="tmdb"&&!m.tmdbConfigured)"无Key"else"未关联",color=c.muted,fontSize=10.sp,lineHeight=24.sp,maxLines=1)}
    MatchIcon056("✎","手动匹配${if(p=="douban")"豆瓣"else"TMDB"}"){edit=p;query=w.title;choices=emptyList();error=""}
    MatchIcon056("⌫","清除${if(p=="douban")"豆瓣"else"TMDB"}匹配"){m.metadata.clearRatingLink(w,p);scope.launch{m.artworkQueue.invalidate(w)};if(p=="tmdb")m.details.clearSeries();m.linkedRatings=m.linkedRatings+(w.ratingViewKey to (m.linkedRatings[w.ratingViewKey].orEmpty()+(p to null)));revision++}
   }
  }
 }
 if(edit.isNotBlank())Dialog({edit=""}){Column(Modifier.width(580.dp).heightIn(max=470.dp).background(c.surface,RoundedCornerShape(12.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  PcSection("${if(edit=="douban")"豆瓣"else"TMDB"} · 手动匹配");Entry(query,{query=it},"输入片名或作品数字ID")
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Action(if(loading)"搜索中…"else"搜索",{if(!loading)scope.launch{loading=true;val provider=edit;try{choices=withContext(Dispatchers.IO){m.metadata.providerCandidates(w,provider,query)};error=if(choices.isEmpty())"没有找到候选，可尝试原名、别名或数字ID"else""}catch(_:Exception){error="查询失败，请检查网络和密钥；原关联保留"}finally{loading=false}}});Action("关闭",{edit=""})}
  if(error.isNotBlank())Text(error,color=PcAccent,fontSize=12.sp)
  LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(choices,key={it.key}){choice->Action("${choice.title} · ${choice.year} · ${choice.rating} · ID ${choice.id}",{val p=edit;m.metadata.saveRatingLink(w,p,choice.id);scope.launch{m.artworkQueue.invalidate(w)};m.linkedRatings=m.linkedRatings+(w.ratingViewKey to(m.linkedRatings[w.ratingViewKey].orEmpty()+(p to choice)));if(p=="tmdb"&&w.kind=="tv")m.details.linkSeries(choice.id);m.details.enrich(choice);scope.launch{try{val fresh=withContext(Dispatchers.IO){m.metadata.details(choice)};if(m.db.prefs.getString(m.metadata.ratingStorageKey(w,p),null)==choice.id){m.linkedRatings=m.linkedRatings+(w.ratingViewKey to(m.linkedRatings[w.ratingViewKey].orEmpty()+(p to fresh)));if(m.details.work.value?.key==w.key)m.details.enrich(fresh)}}catch(e:CancellationException){throw e}catch(_:Exception){m.message("评分已关联，部分人物资料暂未刷新")}};edit="";revision++},Modifier.fillMaxWidth())}}
 }}
}

@Composable private fun MatchIcon056(icon:String,label:String,click:()->Unit){
 InputButton(click,Modifier.size(34.dp).semantics{contentDescription=label},shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(7.dp)),contentPadding=PaddingValues(4.dp)){Text(icon,fontSize=16.sp,maxLines=1)}
}
