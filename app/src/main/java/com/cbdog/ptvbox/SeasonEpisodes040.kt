package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

@Composable internal fun SeasonEpisodes040(m:PtvModel,w:Work){
    val series by m.details.series.collectAsState();val status by m.details.seriesStatus.collectAsState();val loading by m.details.seriesLoading.collectAsState();val c=LocalCinema.current
    var manual by remember{mutableStateOf(false)};var id by remember{mutableStateOf("")};var focusedEpisode by remember(w.key){mutableStateOf<EpisodeMeta?>(null)};var expanded by remember(w.key){mutableStateOf(false)};var episodeInteracted by remember(w.key){mutableStateOf(false)}
    val resumeEpNum=remember(w.key,w.season,m.user.watches){m.user.watches.firstOrNull{it.work.key==w.key&&it.work.season==w.season}?.let{Identity.episode(it.episode)}}
    val epRowState=rememberLazyListState()
    val resumeEpFocus=remember(w.key,w.season){FocusRequester()}
    var autoFocusedResume by remember(w.key,w.season){mutableStateOf(false)}
    LaunchedEffect(focusedEpisode?.number){expanded=false}
    LaunchedEffect(m.seasonCorrectionRequest){if(m.seasonCorrectionRequest>0)manual=true}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
        PcSection("季与单集",status)
        series?.let{show->
            LaunchedEffect(show.id,show.episodes){
                val resumeIdx=resumeEpNum?.let{num->show.episodes.indexOfFirst{it.number==num}}?:-1
                if(resumeIdx>=0&&!autoFocusedResume){
                    val ep=show.episodes[resumeIdx]
                    focusedEpisode=ep;episodeInteracted=true;autoFocusedResume=true
                    epRowState.scrollToItem((resumeIdx-1).coerceAtLeast(0))
                    withFrameNanos{}
                    runCatching{resumeEpFocus.requestFocus()}
                }else{
                    val selected=focusedEpisode?.number?.let{number->show.episodes.firstOrNull{it.number==number}}
                    focusedEpisode=selected?:show.episodes.firstOrNull{it.overview.isNotBlank()}?:show.episodes.firstOrNull()
                }
            }
            if(show.seasons.size>1)LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(3.dp)){
                items(show.seasons,key={it.number}){season->InputButton({m.details.selectSeason(season.number)},Modifier.width(124.dp),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(10.dp)),contentPadding=PaddingValues(5.dp),colors=ptvButtonColors(if(w.season==season.number)c.raised else c.surface,c.text)){
                    Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                        if(season.poster.isNotBlank())AsyncImage(season.poster,season.name,Modifier.fillMaxWidth().height(174.dp),contentScale=ContentScale.Fit)
                        Text("${if(w.season==season.number)"✓ "else""}${season.name}",color=c.text,fontSize=13.sp,maxLines=2)
                        Text("${season.year} · ${season.count}集",color=c.muted,fontSize=11.sp)
                    }
                }}
            }else show.seasons.firstOrNull()?.let{season->Text("${season.name} · ${season.year} · ${season.count}集",color=PcAccent,fontSize=13.sp)}
            LazyRow(state=epRowState,horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(4.dp)){
                items(show.episodes,key={it.number}){episode->val focusReq=if(resumeEpNum!=null&&episode.number==resumeEpNum)resumeEpFocus else null;EpisodeCard054(m,w,episode,selected=episodeInteracted&&focusedEpisode?.number==episode.number,cardFocusRequester=focusReq){focusedEpisode=it;episodeInteracted=true}}
            }
            focusedEpisode?.let{episode->EpisodeSynopsis060(episode,expanded,{expanded=!expanded})}
        }
        if(series==null&&loading)LazyRow(horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(4.dp)){items(6){EpisodeSkeleton055()}}
    }
    if(manual)Dialog({manual=false}){SettingsCard("校正TMDB季集关联","不影响豆瓣关联、收藏与进度；没有Key也可通过普通来源目录选择集数。"){
        Entry(id,{id=it},"数字TMDB剧集ID")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Action("确认关联",{if(id.isNotBlank()){m.details.linkSeries(id.trim());manual=false}});Action("配置密钥",{manual=false;m.settingsSection="metadata";m.utility("settings")});Action("关闭",{manual=false})}
    }}
}

@Composable private fun EpisodeSynopsis060(episode:EpisodeMeta,expanded:Boolean,toggle:()->Unit,modifier:Modifier=Modifier){
    val c=LocalCinema.current
    val text=episode.overview.trim()
    val expandable=text.length>72
    var focused by remember(episode.number){mutableStateOf(false)}
    Column(modifier,verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(
            Modifier.background(androidx.compose.ui.graphics.Color(0xCC241C3D),RoundedCornerShape(7.dp))
                .padding(horizontal=10.dp,vertical=5.dp),
            verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(8.dp)
        ){
            Text("第${episode.number}集简介",color=androidx.compose.ui.graphics.Color(0xFFE9D5FF),fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,maxLines=1)
            if(episode.title.isNotBlank())Text("· ${episode.title}",color=androidx.compose.ui.graphics.Color.White,fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        InputButton({if(expandable)toggle()},Modifier.fillMaxWidth().heightIn(min=58.dp,max=if(expanded)210.dp else 96.dp).onFocusChanged{focused=it.isFocused}.semantics{contentDescription="第${episode.number}集简介"+(if(focused)" · 当前焦点" else "")},shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),colors=ptvButtonColors(c.raised,c.text),contentPadding=PaddingValues(10.dp,8.dp)){
            Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(4.dp)){
                Text(
                    text.ifBlank{"本集暂未提供简介"},
                    color=if(text.isBlank())c.muted else c.text,
                    fontSize=13.sp,
                    lineHeight=18.sp,
                    maxLines=if(expanded)Int.MAX_VALUE else 2,
                    overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if(expandable)Text(if(expanded)"点按折叠简介" else "点按展开简介",color=PcAccent,fontSize=11.sp,maxLines=1)
            }
        }
    }
}
