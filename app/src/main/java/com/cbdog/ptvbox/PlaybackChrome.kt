package com.cbdog.ptvbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*

private val LocalControlRestore=staticCompositionLocalOf<Pair<String,()->Unit>>{"" to {}}
private val LocalCompactPlayer=staticCompositionLocalOf{false}
@Composable internal fun PlaybackChrome(m:PtvModel,menu:(String)->Unit,hide:()->Unit,pictureInPicture:()->Unit,toolbarModifier:Modifier=Modifier,restoreTarget:String="",restored:()->Unit={},onFocusState:(Boolean)->Unit={},defaultFocus:FocusRequester?=null){
    BoxWithConstraints(Modifier.fillMaxWidth().then(toolbarModifier).onFocusChanged{onFocusState(it.hasFocus)}){CompositionLocalProvider(LocalCompactPlayer provides (maxWidth<720.dp),LocalControlRestore provides (restoreTarget to restored)){Column(Modifier.fillMaxWidth().background(Color(0xe610111b)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        val audio=enabledAudioText(m)
        val subtitle=enabledSubtitleText(m)
        val danmaku=danmakuStatusLabel102(m.config.flag("danmaku_enabled",true),m.addons.danmaku.size)
        var scrubPreview by remember(m.current?.key){mutableStateOf<Long?>(null)}
        var keyScrubRev by remember(m.current?.key){mutableIntStateOf(0)}
        LaunchedEffect(keyScrubRev){val t=scrubPreview;if(t!=null){kotlinx.coroutines.delay(SeekPolicy057.DEBOUNCE_COMMIT_MS);if(scrubPreview==t){scrubPreview=null;m.seek051(t)}}}
        val shownPos=scrubPreview?:m.position
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(m.currentCandidate?.label.orEmpty(),color=Color.LightGray,fontSize=12.sp,maxLines=1,modifier=Modifier.weight(1f));Text("${clock(shownPos)} / ${clock(m.duration)}",color=if(scrubPreview!=null)PcAccent else Color.White,fontSize=12.sp,maxLines=1)}
        val fraction=if(m.duration>0)(shownPos.toFloat()/m.duration).coerceIn(0f,1f)else 0f
        Canvas(Modifier.fillMaxWidth().height(24.dp).semantics{contentDescription="播放进度";progressBarRangeInfo=ProgressBarRangeInfo(fraction,0f..1f);setProgress{if(m.duration>0)m.seek051((m.duration*it).toLong());true}}.focusable().onKeyEvent{if(it.type==KeyEventType.KeyDown&&it.key in listOf(Key.DirectionLeft,Key.DirectionRight)){val base=scrubPreview?:m.position;val next=(base+if(it.key==Key.DirectionLeft)-SeekPolicy057.INITIAL_STEP_MS else SeekPolicy057.INITIAL_STEP_MS).coerceIn(0,m.duration.coerceAtLeast(0));scrubPreview=next;keyScrubRev++;true}else false}.pointerInput(m.duration){awaitEachGesture{val down=awaitFirstDown();down.consume();var target=if(m.duration>0)(down.position.x/size.width*m.duration).toLong().coerceIn(0,m.duration)else null;scrubPreview=target;do{val e=awaitPointerEvent();val change=e.changes.first();change.consume();if(m.duration>0){target=(change.position.x/size.width*m.duration).toLong().coerceIn(0,m.duration);scrubPreview=target}}while(e.changes.any{it.pressed});scrubPreview=null;if(target!=null)m.seek051(target)}}){
            drawLine(Color.Gray.copy(alpha=.6f),Offset(0f,size.height/2),Offset(size.width,size.height/2),3.dp.toPx());drawLine(Color(0xffb7a0ff),Offset(0f,size.height/2),Offset(size.width*fraction,size.height/2),3.dp.toPx());drawCircle(Color(0xffc7b6ff),5.dp.toPx(),Offset(size.width*fraction,size.height/2))
        }
        val btnBg=Color(0x553c3d54)
        val btnHeight=if(LocalCompactPlayer.current)36.dp else 42.dp
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.Bottom,horizontalArrangement=Arrangement.spacedBy(8.dp)){
        FlowRow(Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            PlayerIcon("previous","上一集"){m.stepEpisode(-1)}
            PlayerIcon(if(m.playing)"pause"else"play",if(m.playing)"暂停"else"播放"){if(m.playing)m.player.pause()else m.player.play()}
            PlayerIcon("next","下一集"){m.stepEpisode(1)}
            PlayerIcon("stop","停止",m::stop)
            PlayerIcon("list",if(m.current?.provider=="live")"直播线路"else"选集"){menu("episodes")}
            InputButton({menu("sources")},Modifier.height(btnHeight).widthIn(min=56.dp,max=180.dp).then(if(defaultFocus!=null)Modifier.focusRequester(defaultFocus)else Modifier).then(playerRestore("播放线路")),shape=ButtonDefaults.shape(shape=androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),colors=ptvButtonColors(btnBg,Color.White),contentPadding=PaddingValues(12.dp,4.dp)){Text(m.currentCandidate?.site?.name?:"播放源",fontSize=12.sp,maxLines=1,softWrap=false)}
            InputButton({menu("speed")},Modifier.height(btnHeight).widthIn(min=48.dp).then(playerRestore("倍速")),shape=ButtonDefaults.shape(shape=androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),colors=ptvButtonColors(btnBg,Color.White),contentPadding=PaddingValues(11.dp,4.dp)){Text("${m.player.playbackParameters.speed}×",fontSize=12.sp,maxLines=1,softWrap=false)}
            InputButton({menu("scale")},Modifier.height(btnHeight).widthIn(min=48.dp).then(playerRestore("画面比例")),shape=ButtonDefaults.shape(shape=androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),colors=ptvButtonColors(btnBg,Color.White),contentPadding=PaddingValues(11.dp,4.dp)){Text(listOf("FIT","FILL","ZOOM")[m.resizeChoice],fontSize=12.sp,maxLines=1,softWrap=false)}
            PlayerIcon("cc","字幕"){menu("subtitles")}
            PlayerIcon("audio","音轨"){menu("audio")}
            DanmakuToggle(m,btnHeight){menu("danmaku")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
            PlayerIcon(if(m.muted051)"mute"else"volume",if(m.muted051)"取消静音"else"静音",m::toggleMute051)
            PlayerIcon("pip","小窗播放",pictureInPicture)
            PlayerIcon("hide","隐藏控制",hide)
        }
        }
    }
}}}
private fun enabledAudioText(m:PtvModel):String {
    val group=m.player.currentTracks.groups.filter{it.type==androidx.media3.common.C.TRACK_TYPE_AUDIO}.firstOrNull{g->(0 until g.length).any{g.isTrackSelected(it)}}
    if(group==null)return "音轨未报告"
    val index=(0 until group.length).first{group.isTrackSelected(it)};val f=group.getTrackFormat(index)
    return TrackLabels056.label(f.language,f.label,f.codecs,f.sampleMimeType,f.channelCount,index+1)
}
internal fun subtitleStatusLabel102(
    hasExternal:Boolean,
    externalEnabled:Boolean,
    embeddedDisabled:Boolean,
    hasSelectedEmbedded:Boolean
):String = when {
    hasExternal->if(externalEnabled)"外挂字幕 ON" else "外挂字幕 OFF"
    embeddedDisabled->"字幕 OFF"
    hasSelectedEmbedded->"字幕 ON"
    else->"字幕未报告"
}
internal fun danmakuStatusLabel102(enabled:Boolean,count:Int):String = when {
    !enabled->"弹幕 OFF"
    count>0->"弹幕 ON ${count}条"
    else->"弹幕 ON 0"
}
internal fun enabledSubtitleText(m:PtvModel):String {
    val embeddedDisabled=androidx.media3.common.C.TRACK_TYPE_TEXT in m.player.trackSelectionParameters.disabledTrackTypes
    val hasSelectedEmbedded=m.player.currentTracks.groups.filter{it.type==androidx.media3.common.C.TRACK_TYPE_TEXT}.any{g->(0 until g.length).any{g.isTrackSelected(it)}}
    return subtitleStatusLabel102(
        hasExternal=m.addons.hasExternalSubtitle,
        externalEnabled=m.addons.externalSubtitleEnabled&&m.addons.cues.isNotEmpty(),
        embeddedDisabled=embeddedDisabled,
        hasSelectedEmbedded=hasSelectedEmbedded
    )
}
@Composable private fun DanmakuToggle(m:PtvModel,height:androidx.compose.ui.unit.Dp,action:()->Unit){
    val enabled=remember(m.preferencesRevision){m.config.flag("danmaku_enabled",true)}
    val count=m.addons.danmaku.size
    val pillBg=if(enabled)Color(0x332ee08c)else Color(0x33ff5c67)
    val pillColor=if(enabled)Color(0xff5cf0a8)else Color(0xffff7a84)
    InputButton(action,Modifier.height(height).widthIn(min=72.dp).then(playerRestore("弹幕")).semantics{contentDescription="弹幕开关与数量"},shape=ButtonDefaults.shape(shape=androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),contentPadding=PaddingValues(10.dp,4.dp),colors=ptvButtonColors(Color(0x553c3d54),Color.White)){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
            Box(Modifier.border(1.2.dp,Color.White.copy(alpha=.88f),androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal=4.dp,vertical=1.dp)){Text("弹",color=Color.White,fontSize=10.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold)}
            Text(if(enabled)"ON" else "OFF",color=pillColor,fontSize=10.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,modifier=Modifier.background(pillBg,androidx.compose.foundation.shape.RoundedCornerShape(4.dp)).padding(horizontal=5.dp,vertical=1.dp))
            if(enabled)Text("$count",color=Color(0xffc9f7df),fontSize=11.sp,maxLines=1,softWrap=false)
        }
    }
}
@Composable private fun PlayerIcon(kind:String,label:String,action:()->Unit){InputButton(action,Modifier.size(if(LocalCompactPlayer.current)36.dp else 42.dp).selection051(kind=="mute").then(playerRestore(label)).semantics{contentDescription=label},shape=ButtonDefaults.shape(shape=androidx.compose.foundation.shape.RoundedCornerShape(7.dp)),contentPadding=PaddingValues(9.dp),colors=ptvButtonColors(Color(0x553c3d54),Color.White)){
    val color=LocalContentColor.current
    Canvas(Modifier.size(24.dp)){
        val w=size.width;val h=size.height;val stroke=Stroke(2.dp.toPx());fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(color,Offset(w*x1,h*y1),Offset(w*x2,h*y2),2.dp.toPx())
        when(kind){
            "play"->{drawPath(Path().apply{moveTo(w*.25f,h*.1f);lineTo(w*.85f,h*.5f);lineTo(w*.25f,h*.9f);close()},color)}
            "pause"->{drawRect(color,Offset(w*.2f,h*.15f),Size(w*.2f,h*.7f));drawRect(color,Offset(w*.6f,h*.15f),Size(w*.2f,h*.7f))}
            "stop"->drawRect(color,Offset(w*.15f,h*.15f),Size(w*.7f,h*.7f))
            "previous","next"->{val d=if(kind=="previous")-1 else 1;val p=Path().apply{moveTo(w*(.5f-d*.3f),h*.15f);lineTo(w*(.5f+d*.3f),h*.5f);lineTo(w*(.5f-d*.3f),h*.85f);close()};drawPath(p,color);line(if(d<0).1f else .9f,.15f,if(d<0).1f else .9f,.85f)}
            "back","forward"->{val d=if(kind=="back")-1 else 1;for(x in listOf(.3f,.7f)){val p=Path().apply{moveTo(w*(x-d*.2f),h*.15f);lineTo(w*(x+d*.2f),h*.5f);lineTo(w*(x-d*.2f),h*.85f);close()};drawPath(p,color)}}
            "list"->{for(y in listOf(.2f,.5f,.8f)){drawCircle(color,w*.04f,Offset(w*.1f,h*y));line(.25f,y,.95f,y)}}
            "source"->{for(y in listOf(.1f,.5f))drawRect(color,Offset(w*.05f,h*y),Size(w*.9f,h*.3f),style=stroke)}
            "cc"->{drawRect(color,Offset(w*.05f,h*.2f),Size(w*.9f,h*.6f),style=stroke);for(x in listOf(.3f,.7f)){line(x+.07f,.36f,x-.07f,.36f);line(x-.07f,.36f,x-.07f,.64f);line(x-.07f,.64f,x+.07f,.64f)}}
            "audio"->{for(x in listOf(.2f,.5f,.8f))line(x,.3f-(if(x==.5f).2f else 0f),x,.7f+(if(x==.5f).2f else 0f))}
            "danmaku"->{drawPath(Path().apply{moveTo(w*.1f,h*.15f);lineTo(w*.9f,h*.15f);lineTo(w*.9f,h*.7f);lineTo(w*.4f,h*.7f);lineTo(w*.2f,h*.9f);lineTo(w*.2f,h*.7f);lineTo(w*.1f,h*.7f);close()},color,style=stroke);line(.25f,.33f,.75f,.33f);line(.25f,.5f,.6f,.5f)}
            "mute","volume"->{drawPath(Path().apply{moveTo(w*.1f,h*.35f);lineTo(w*.3f,h*.35f);lineTo(w*.55f,h*.1f);lineTo(w*.55f,h*.9f);lineTo(w*.3f,h*.65f);lineTo(w*.1f,h*.65f);close()},color,style=stroke);if(kind=="mute"){line(.67f,.3f,.95f,.7f);line(.95f,.3f,.67f,.7f)}else drawArc(color,-60f,120f,false,Offset(w*.45f,h*.1f),Size(w*.5f,h*.8f),style=stroke)}
            "pip"->{drawRect(color,Offset(w*.12f,h*.18f),Size(w*.76f,h*.64f),style=stroke);line(.38f,.84f,.62f,.84f);line(.5f,.64f,.5f,.84f)}
            "hide"->{line(.1f,.3f,.5f,.7f);line(.5f,.7f,.9f,.3f)}
            else->for(x in listOf(.2f,.5f,.8f))drawCircle(color,w*.06f,Offset(w*x,h*.5f))
        }
    }
}}

@Composable private fun playerRestore(label:String):Modifier{val focus=remember{FocusRequester()};val pair=LocalControlRestore.current;val windowFocused=LocalWindowInfo.current.isWindowFocused;var attached by remember{mutableStateOf(false)};LaunchedEffect(pair.first,attached,windowFocused){if(attached&&windowFocused&&pair.first==label){withFrameNanos{};runCatching{focus.requestFocus()}}};return Modifier.focusRequester(focus).onGloballyPositioned{attached=true}.onFocusChanged{if(it.isFocused&&pair.first==label)pair.second()}.semantics{contentDescription=label}}
