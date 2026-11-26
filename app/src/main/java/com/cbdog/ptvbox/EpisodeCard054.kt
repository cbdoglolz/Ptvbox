package com.cbdog.ptvbox
import androidx.compose.foundation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

@Composable internal fun EpisodeBadge054(state:WatchState,modifier:Modifier=Modifier){val colors=WatchVisuals056.colors(state);val brush=Brush.linearGradient(colors);val wash=Brush.linearGradient(colors.map{it.copy(alpha=.28f)});Canvas(modifier.size(34.dp).semantics{contentDescription="观看状态："+when(state){WatchState.COMPLETED->"已看";WatchState.WATCHING->"在看";WatchState.PLANNED->"想看"}}){
 val w=size.width;val h=size.height;drawCircle(wash,w*.48f);drawCircle(brush,w*.43f,style=Stroke(2.dp.toPx()));when(state){WatchState.COMPLETED->drawPath(Path().apply{moveTo(w*.28f,h*.51f);lineTo(w*.43f,h*.66f);lineTo(w*.71f,h*.34f)},brush,style=Stroke(3.dp.toPx(),cap=StrokeCap.Round));WatchState.WATCHING->{drawOval(brush,Offset(w*.27f,h*.4f),Size(w*.46f,h*.2f),style=Stroke(2.dp.toPx()));drawCircle(brush,w*.075f,Offset(w*.5f,h*.5f))};WatchState.PLANNED->drawPath(Path().apply{moveTo(w*.5f,h*.72f);cubicTo(w*.16f,h*.5f,w*.25f,h*.28f,w*.5f,h*.39f);cubicTo(w*.75f,h*.28f,w*.84f,h*.5f,w*.5f,h*.72f);close()},brush)}
}}
@Composable internal fun EpisodeCard054(m:PtvModel,w:Work,ep:EpisodeMeta,selected:Boolean=false,onFocused:(EpisodeMeta)->Unit={}){val c=LocalCinema.current;val mark=m.user.episodeMarks[episodeRef(w,ep.number)]?:EpisodeMark();val progress=m.user.watches.firstOrNull{it.work.key==w.key&&it.work.season==w.season&&Identity.episode(it.episode)==ep.number};var menu by remember(ep.number,w.key,w.season){mutableStateOf(false)};var restoreMarkFocus by remember(ep.number,w.key,w.season){mutableStateOf(false)};val markFocus=remember(ep.number,w.key,w.season){androidx.compose.ui.focus.FocusRequester()}
 LaunchedEffect(menu,restoreMarkFocus){if(!menu&&restoreMarkFocus){androidx.compose.runtime.withFrameNanos{};runCatching{markFocus.requestFocus()};restoreMarkFocus=false}}
 var focused by remember{mutableStateOf(false)};val active=focused||selected;val cardScale by animateFloatAsState(if(active)1.035f else 1f,label="episode-card-scale")
 Box(Modifier.width(248.dp).height(244.dp).graphicsLayer{scaleX=cardScale;scaleY=cardScale}.onFocusChanged{focused=it.hasFocus;if(it.hasFocus)onFocused(ep)}.drawWithContent{
   drawContent()
   if(active){val inset=.75.dp.toPx();drawRoundRect(brush=SelectionBrush051,topLeft=Offset(inset,inset),size=Size(size.width-inset*2,size.height-inset*2),cornerRadius=CornerRadius(10.dp.toPx(),10.dp.toPx()),style=Stroke(1.5.dp.toPx()))}
 }.semantics{contentDescription="单集卡片 ${ep.number}"}){
 Column(Modifier.fillMaxSize().background(c.surface,RoundedCornerShape(10.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
  InputButton({if(active)m.play(w,"第%02d集".format(ep.number))else onFocused(ep)},Modifier.fillMaxWidth().weight(1f).semantics{contentDescription=if(active)"第${ep.number}集 · 已选中，再次点击播放"else"第${ep.number}集 · 点击选中查看简介"},shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),scale=androidx.tv.material3.ButtonDefaults.scale(1f,1f,1f),colors=ptvButtonColors(Color.Transparent,c.text),contentPadding=PaddingValues(1.dp)){
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)){
    Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).background(c.raised,RoundedCornerShape(8.dp))){if(ep.still.isNotBlank())AsyncImage(ep.still,ep.title,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)else Text(if(ep.pending)"剧照加载中…"else"第${ep.number}集",color=c.muted,modifier=Modifier.align(Alignment.Center))}
    val fullTitle="S${ep.season}E${ep.number} · ${ep.title}"
    Text(fullTitle,color=c.text,fontSize=when{fullTitle.length<=22->13.5.sp;fullTitle.length<=34->12.sp;else->10.5.sp},lineHeight=when{fullTitle.length<=34->15.sp;else->13.sp},maxLines=2,softWrap=true,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.heightIn(min=28.dp,max=34.dp).semantics{contentDescription=fullTitle})
    Text((if(ep.rating=="-"||ep.rating.isBlank())"本集评分未公开" else "${ep.rating} · ${ep.votes}人评分")+(if(ep.runtime>0)" · ${ep.runtime}分钟"else"")+(if(ep.airDate.isBlank())""else" · ${ep.airDate}"),color=PcGreen,fontSize=11.sp,maxLines=1)
   }
  }
   Row(Modifier.fillMaxWidth().height(34.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
    Text(progress?.let{"继续 · ${clock(it.position)} / ${clock(it.duration)}"}?:"未播放",color=if(progress==null)c.muted else PcAccent,fontSize=10.5.sp,maxLines=1,modifier=Modifier.weight(1f))
    InputButton({menu=true},Modifier.size(34.dp).focusRequester(markFocus).semantics{contentDescription="第${ep.number}集观看标记"},shape=androidx.tv.material3.ButtonDefaults.shape(CircleShape),colors=ptvButtonColors(c.raised,mark.watchState?.let{WatchVisuals056.colors(it).first()}?:c.text),scale=androidx.tv.material3.ButtonDefaults.scale(1f,1f,1f),contentPadding=PaddingValues(0.dp)){
     val symbol=when(mark.watchState){WatchState.COMPLETED->"✓";WatchState.WATCHING->"●";WatchState.PLANNED->"♥";null->"标"}
     Text(symbol,style=if(mark.watchState==null)TextStyle(color=c.text,fontSize=12.sp)else TextStyle(brush=Brush.horizontalGradient(WatchVisuals056.colors(mark.watchState)),fontSize=15.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold))
    }
   }
 }
 }
if(menu)WatchPicker056("第${ep.number}集 · 观看标记",mark.watchState,{state->m.users.write{if(state==null)markEpisode(w,ep.number,clearState=true)else markEpisode(w,ep.number,state=state)};menu=false;restoreMarkFocus=true},{menu=false;restoreMarkFocus=true})

}

@Composable internal fun EpisodeSkeleton055(){val c=LocalCinema.current;Column(Modifier.width(248.dp).height(244.dp).semantics{contentDescription="单集资料占位"}.background(c.surface,RoundedCornerShape(10.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).background(c.raised,RoundedCornerShape(8.dp)));Text("正在读取集数…",color=c.muted,fontSize=13.sp);repeat(2){Box(Modifier.fillMaxWidth(if(it==1).7f else 1f).height(12.dp).background(c.raised,RoundedCornerShape(4.dp)))};Spacer(Modifier.weight(1f));Row(Modifier.fillMaxWidth().height(34.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.weight(1f).height(10.dp).background(c.raised,RoundedCornerShape(4.dp)));Spacer(Modifier.width(8.dp));Box(Modifier.size(34.dp).background(c.raised,CircleShape))}}}
