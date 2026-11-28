package com.cbdog.ptvbox

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.*
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable internal fun CinemaPlayer(m:PtvModel){CinemaTheme(false){
 var overlay by remember{mutableStateOf(PlayerOverlay())};var interaction by remember{mutableLongStateOf(0)}
 val root=remember{FocusRequester()};val tools=remember{FocusRequester()};val context=LocalContext.current;val activity=generateSequence(context){(it as? android.content.ContextWrapper)?.baseContext}.filterIsInstance<android.app.Activity>().firstOrNull()
 var restoreControl by remember{mutableStateOf("")};var panelTrail by remember{mutableStateOf<List<String>>(emptyList())}
 var heldAt by remember{mutableLongStateOf(0)};var restoreSpeed by remember{mutableStateOf<Float?>(null)}
 var seekDirection by remember{mutableIntStateOf(0)};var seekStartedAt by remember{mutableLongStateOf(0)}
 var seekRevision by remember{mutableIntStateOf(0)};var seekHolding by remember{mutableStateOf(false)}
  var chromeFocused by remember{mutableStateOf(false)};var touchBoost by remember{mutableStateOf(false)}
 val series by m.details.series.collectAsState()
 fun closePanel(){if(panelTrail.isNotEmpty()){overlay=overlay.copy(panel=panelTrail.last());panelTrail=panelTrail.dropLast(1)}else{restoreControl=when(overlay.panel){"sources"->"播放线路";"episodes"->if(m.current?.provider=="live")"直播线路"else"选集";"audio"->"音轨";"subtitles"->"字幕";"volume"->"音量";"speed"->"倍速";"scale"->"画面比例";else->""};overlay=overlay.copy(panel="")};interaction=android.os.SystemClock.uptimeMillis()}
 fun back(){seekDirection=0;seekHolding=false;if(overlay.controls||overlay.panel.isNotBlank()||overlay.previewMs!=null){overlay=PlayerOverlay(controls=false);panelTrail=emptyList();restoreControl=""}else m.back()}
 fun commitSeek(){val target=overlay.previewMs;seekDirection=0;seekHolding=false;overlay=overlay.copy(previewMs=null,controls=false);if(target!=null)m.seek051(target)}
 fun queueSeekDelta(deltaMs:Long){if(m.current?.provider=="live")return;val base=overlay.previewMs?:m.position;val max=m.duration.coerceAtLeast(0);val next=if(max>0)(base+deltaMs).coerceIn(0,max)else(base+deltaMs).coerceAtLeast(0);overlay=overlay.copy(controls=false,previewMs=next);seekRevision++}
 fun enterSystemPip103(){val a=activity?:return m.message("当前环境没有可用Activity，无法进入小窗");if(android.os.Build.VERSION.SDK_INT<26||!a.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)){m.message("当前设备或电视系统不支持系统小窗");return};val width=m.player.videoSize.width.takeIf{it>0}?:16;val height=m.player.videoSize.height.takeIf{it>0}?:9;runCatching{check(a.enterPictureInPictureMode(android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational((width.toFloat()/height*1000).toInt().coerceIn(420,2390),1000)).build())){"系统拒绝进入画中画"}}.onFailure{m.message("小窗启动失败：${it.message?.take(60) ?: "系统限制"}")}}
 fun open(name:String){if(overlay.panel.isNotBlank())panelTrail=panelTrail+overlay.panel;restoreControl="";overlay=overlay.open(name);interaction=android.os.SystemClock.uptimeMillis()}
 BackHandler{back()}
 LaunchedEffect(overlay){if(BuildConfig.DEBUG)android.util.Log.d("PtvOverlay","STATE controls=${overlay.controls} panel=${overlay.panel} preview=${overlay.previewMs!=null} trail=${panelTrail.size}")}
 DisposableEffect(Unit){activity?.window?.decorView?.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;onDispose{activity?.window?.decorView?.systemUiVisibility=View.SYSTEM_UI_FLAG_VISIBLE}}
 LaunchedEffect(m.pipMode){if(m.pipMode){seekDirection=0;seekHolding=false;overlay=PlayerOverlay(controls=false);panelTrail=emptyList()}}
 LaunchedEffect(seekDirection){if(seekDirection==0)return@LaunchedEffect;delay(320);while(isActive&&seekDirection!=0){val held=android.os.SystemClock.elapsedRealtime()-seekStartedAt;delay(SeekPolicy057.repeatDelayMs(held));if(seekDirection==0)break;val step=SeekPolicy057.stepForHeld(android.os.SystemClock.elapsedRealtime()-seekStartedAt);overlay=overlay.copy(previewMs=((overlay.previewMs?:m.position)+seekDirection*step).coerceIn(0,m.duration.coerceAtLeast(0)));seekRevision++}}
 LaunchedEffect(seekRevision,seekHolding){if(overlay.previewMs!=null&&!seekHolding){delay(SeekPolicy057.DEBOUNCE_COMMIT_MS);if(overlay.previewMs!=null&&!seekHolding)commitSeek()}}
 LaunchedEffect(overlay.controls,overlay.panel){withFrameNanos{};runCatching{if(overlay.panel.isBlank()){if(overlay.controls){if(restoreControl.isBlank())tools.requestFocus()}else root.requestFocus()}}}
 LaunchedEffect(overlay,m.playing,interaction){if(overlay.controls&&overlay.panel.isBlank()&&overlay.previewMs==null&&m.playing){delay(5000);overlay=overlay.copy(controls=false)}}
 BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).focusRequester(root).onPreviewKeyEvent{e->
  val down=e.type==KeyEventType.KeyDown;val up=e.type==KeyEventType.KeyUp;if(down)interaction=android.os.SystemClock.uptimeMillis()
  if(e.key==Key.Back){if(up)back();true}
  else if(e.key in listOf(Key.MediaPlayPause,Key.MediaPause,Key.MediaPlay)){if(down){if(e.key==Key.MediaPause)m.player.pause()else if(e.key==Key.MediaPlay)m.player.play()else if(m.player.playWhenReady)m.player.pause()else m.player.play();overlay=overlay.copy(controls=true)};true}
  else if(overlay.panel.isNotBlank())false
  else if(overlay.controls&&!chromeFocused){
   when(e.key){Key.DirectionCenter,Key.Enter->{if(down){if(m.playing)m.player.pause()else m.player.play()};true};else->false}
  }
  else if(overlay.previewMs!=null){when(e.key){
   Key.DirectionLeft,Key.DirectionRight->{val direction=if(e.key==Key.DirectionLeft)-1 else 1;if(down&&e.nativeKeyEvent.repeatCount==0){seekDirection=direction;seekHolding=true;seekStartedAt=android.os.SystemClock.elapsedRealtime();queueSeekDelta(direction*m.config.seekStepMs)};if(up&&seekDirection==direction){seekDirection=0;seekHolding=false;seekRevision++};true}
   Key.DirectionCenter,Key.Enter->{if(up)commitSeek();true};else->false}}
  else if(!overlay.controls){when(e.key){
   Key.DirectionLeft,Key.DirectionRight->{if(down&&e.nativeKeyEvent.repeatCount==0){val direction=if(e.key==Key.DirectionLeft)-1 else 1;seekDirection=direction;seekHolding=true;seekStartedAt=android.os.SystemClock.elapsedRealtime();queueSeekDelta(direction*m.config.seekStepMs)};if(up){seekDirection=0;seekHolding=false;seekRevision++};true}
   Key.DirectionUp,Key.DirectionDown->{if(down)overlay=overlay.copy(controls=true);true}
   Key.DirectionCenter,Key.Enter->{if(down){if(e.nativeKeyEvent.repeatCount==0)heldAt=android.os.SystemClock.uptimeMillis();if(android.os.SystemClock.uptimeMillis()-heldAt>=450&&restoreSpeed==null){restoreSpeed=m.player.playbackParameters.speed;m.player.setPlaybackSpeed(2.5f)}};if(up){val old=restoreSpeed;if(old!=null){m.player.setPlaybackSpeed(old);restoreSpeed=null}else{if(m.playing)m.player.pause()else m.player.play();overlay=overlay.copy(controls=false)}};true}
   else->false
  }}else false
 }.focusable().pointerInput(Unit){awaitPointerEventScope{while(true){val e=awaitPointerEvent(PointerEventPass.Initial);if(e.changes.any{it.pressed})interaction=android.os.SystemClock.uptimeMillis()}}}){
  val compact=maxHeight<430.dp
  PlayerSurface040(m)
  MediaOverlay053(m)
   PlayerTouchLayer060(
     m,
     onShowControls={overlay=overlay.copy(controls=true)},
     onPreview={seekHolding=true;overlay=overlay.copy(previewMs=it)},
     onCommit={seekHolding=false;overlay=overlay.copy(previewMs=null);if(it!=null)m.seek051(it)},
     onToggle={if(m.playing)m.player.pause()else m.player.play()},
     onSeekDelta={queueSeekDelta(it)},
     onBoost={touchBoost=it}
   )
  if(overlay.controls){
   Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().background(Color(0xb8101019)).padding(if(compact)10.dp else 18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
    val current=m.current;val episodeNumber=Identity.episode(m.currentEpisode);val episodeMeta=series?.episodes?.firstOrNull{it.season==current?.season&&it.number==episodeNumber};val logo=current?.let{m.presentations[it.ratingViewKey]?.logo}.orEmpty()
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     if(logo.isNotBlank())AsyncImage(logo,current?.title,Modifier.widthIn(max=260.dp).height(if(compact)38.dp else 52.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit)else Text(current?.title.orEmpty(),color=Color.White,fontSize=if(compact)17.sp else 23.sp,maxLines=1)
     if(current?.kind=="tv")Text("第${current.season}季 · ${episodeNumber?.let{"第${it}集"}?:m.currentEpisode}",color=Color(0xffd8c8ff),fontSize=if(compact)12.sp else 14.sp,maxLines=1)
     episodeMeta?.title?.takeIf{it.isNotBlank()&&!Identity.sameEpisode(it,m.currentEpisode)}?.let{Text(it,color=Color.White,fontSize=if(compact)12.sp else 16.sp,maxLines=1,modifier=Modifier.weight(1f))}
    }
    if(!compact)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){
     Text("已检测 ${m.checked}/${m.total} · ${m.candidates.size}条候选",color=PcAccent,fontSize=12.sp)
     val canContinue=!m.searching&&m.checked<m.total&&m.current?.provider!="live"
     if(m.searching||canContinue){
      val showResume=canContinue||m.pausedScan
      val scanBtnColors=androidx.tv.material3.ButtonDefaults.colors(
       containerColor=if(showResume)Color(0xD95C3A0B)else Color(0xC0181D30),
       contentColor=if(showResume)Color(0xFFFDE68A)else Color(0xFFD6DFEE),
       focusedContainerColor=Color(0xFF2D3C70),
       focusedContentColor=Color.White,
       pressedContainerColor=Color(0xFF3A4B88),
       pressedContentColor=Color.White
      )
      InputButton(
       {if(canContinue)m.continueScan()else m.pausedScan=!m.pausedScan},
       Modifier.border(1.dp,if(showResume)Color(0xFFF59E0B)else Color(0x66586580),RoundedCornerShape(8.dp)),
       shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),
       focusRadius=8.dp,
       scale=androidx.tv.material3.ButtonDefaults.scale(1f,1.05f,.98f),
       colors=scanBtnColors,
       contentPadding=PaddingValues(horizontal=13.dp,vertical=6.dp)
      ){Text(if(showResume)"▶ 继续搜索"else"暂停搜索",fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold,maxLines=1)}
     }
     Text(m.notice,color=Color(0xffded6ef),fontSize=12.sp,maxLines=2,modifier=Modifier.weight(1f))
    }
    if(!compact){
     val subtitleText=enabledSubtitleText(m)
     val danmakuText=danmakuStatusLabel102(m.config.flag("danmaku_enabled",true),m.addons.danmaku.size)
     val infoLine=listOf(m.playbackCapability060.compactSummary,subtitleText,danmakuText,m.playbackCapability060.warning).filter{it.isNotBlank()}.joinToString(" · ")
     Text(infoLine,color=if(m.playbackCapability060.warning.isBlank())Color(0xffc5d8e8)else Color(0xffffc56e),fontSize=11.sp,maxLines=1)
    }
   }
   Box(Modifier.align(Alignment.BottomCenter)){PlaybackChrome(m,{open(it)},{overlay=overlay.copy(controls=false)},::enterSystemPip103,Modifier,restoreControl,{m.lastRestoredControl=restoreControl;restoreControl=""},{chromeFocused=it},defaultFocus=tools)}
   }else{
   if(m.notice.isNotBlank())Text(m.notice,color=PcAccent,fontSize=12.sp,maxLines=2,modifier=Modifier.align(Alignment.TopEnd).padding(top=if(m.addons.badge.isNotBlank())76.dp else 16.dp,end=16.dp).widthIn(max=420.dp).background(Color(0xc0201a32),RoundedCornerShape(8.dp)).padding(10.dp))
  }
  SubtitleBadge053(m,Modifier.align(Alignment.TopEnd).padding(top=if(overlay.controls)if(compact)62.dp else 110.dp else 12.dp,end=18.dp))
  if(m.streamLoading)Text("连接 / 缓冲中…",color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color(0xbb11111b),RoundedCornerShape(8.dp)).padding(16.dp))
  if(m.current!=null&&m.current?.provider!="live"&&m.duration>0&&!m.playing&&!m.streamLoading&&overlay.previewMs==null)Column(Modifier.align(Alignment.Center).background(Color(0xb312111b),RoundedCornerShape(10.dp)).padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){Text("已暂停",color=Color.White,fontSize=16.sp);Text("${clock(m.position)} / ${clock(m.duration)}",color=PcAccent,fontSize=22.sp)}
  overlay.previewMs?.let{time->val deltaSec=(time-m.position)/1000L;val deltaText=if(deltaSec>=0)"+${deltaSec}秒"else"${deltaSec}秒";Column(Modifier.align(Alignment.Center).background(Color(0xdd151321),RoundedCornerShape(12.dp)).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){Text("${clock(time)} / ${clock(m.duration)}",color=Color.White,fontSize=30.sp);Text("$deltaText · 停顿自动跳转 · 确认立刻跳 · 返回取消",color=PcAccent,fontSize=13.sp)}}
  restoreSpeed?.let{Text("临时 2.5×",color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color(0xaa11111b)).padding(14.dp))}
   if(touchBoost)Text("长按 2× 播放",color=Color.White,modifier=Modifier.align(Alignment.Center).background(Color(0xaa11111b)).padding(14.dp))
 }
 if(overlay.panel.isNotBlank())PlayerPanel(m,overlay.panel,{name->open(name)},::closePanel,::back)
}}

internal object PlayerTouch060 {
    const val SEEK_STEP_MS=5_000L
    fun zone(x:Float,width:Float):String=when{x<width*.3f->"left";x>width*.7f->"right";else->"center"}
    fun target(base:Long,dragX:Float,width:Float,duration:Long):Long=if(width<=0f||duration<=0)base else (base+(dragX/width*duration).toLong()).coerceIn(0,duration)
}

@Composable internal fun PlayerTouchLayer060(
    m:PtvModel,
    onShowControls:()->Unit,
    onPreview:(Long)->Unit,
    onCommit:(Long?)->Unit,
    onToggle:()->Unit,
    onSeek:((Long)->Unit)?=null,
    onBoost:(Boolean)->Unit,
    onSeekDelta:((Long)->Unit)?=null
){
    var originalSpeed by remember{mutableStateOf(1f)}
    var boosting by remember{mutableStateOf(false)}
    var dragBase by remember{mutableStateOf<Long?>(null)}
    var dragX by remember{mutableStateOf(0f)}
    var dragPreview by remember{mutableStateOf<Long?>(null)}
    DisposableEffect(m.current?.key){
        onDispose{if(boosting)m.player.setPlaybackSpeed(originalSpeed)}
    }
    Box(
        Modifier.fillMaxSize()
            .pointerInput(m.current?.key,m.duration){
                detectTapGestures(
                    onTap={onShowControls()},
                    onDoubleTap={offset->
                        when(PlayerTouch060.zone(offset.x,size.width.toFloat())){
                            "left"->if(m.current?.provider!="live"){onSeekDelta?.invoke(-PlayerTouch060.SEEK_STEP_MS)?:onSeek?.invoke((m.player.currentPosition-PlayerTouch060.SEEK_STEP_MS).coerceAtLeast(0))}
                            "right"->if(m.current?.provider!="live"){onSeekDelta?.invoke(PlayerTouch060.SEEK_STEP_MS)?:onSeek?.invoke((m.player.currentPosition+PlayerTouch060.SEEK_STEP_MS).coerceAtMost(m.player.duration.coerceAtLeast(0)))}
                            else->onToggle()
                        }
                    },
                    onLongPress={
                        if(!boosting){
                            originalSpeed=m.player.playbackParameters.speed
                            boosting=true
                            m.player.setPlaybackSpeed(2f)
                            onBoost(true)
                        }
                    },
                    onPress={
                        tryAwaitRelease()
                        if(boosting){m.player.setPlaybackSpeed(originalSpeed);boosting=false;onBoost(false)}
                    }
                )
            }
            .pointerInput(m.current?.key,m.duration){
                detectDragGestures(
                    onDragStart={if(m.current?.provider=="live")onShowControls() else{dragBase=m.player.currentPosition;dragX=0f;dragPreview=null}},
                    onDrag={change,amount->
                        val base=dragBase
                        if(base!=null){
                            change.consume()
                            dragX+=amount.x
                            val target=PlayerTouch060.target(base,dragX,size.width.toFloat(),m.player.duration.coerceAtLeast(0))
                            dragPreview=target
                            onPreview(target)
                        }
                    },
                    onDragEnd={val target=dragPreview;dragBase=null;dragX=0f;dragPreview=null;onCommit(target)},
                    onDragCancel={dragBase=null;dragX=0f;dragPreview=null;onCommit(null)}
                )
            }
    )
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable internal fun PlayerSurface040(m:PtvModel){
    m.preferencesRevision
    AndroidView(
        factory={ctx->PlayerView(ctx).apply{player=m.player;useController=false;keepScreenOn=true}},
        update={it.player=m.player;applySubtitleStyle040(it.subtitleView,m.config);it.resizeMode=listOf(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL,androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM)[m.resizeChoice]},
        modifier=Modifier.fillMaxSize(),
        onRelease={it.player=null}
    )
}
@Composable private fun PcChipFloating(label:String,modifier:Modifier,click:()->Unit){Box(modifier){PcChip(label,false,click)}}
 @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
 @Composable private fun PlayerPanel(m:PtvModel,panel:String,open:(String)->Unit,close:()->Unit,dismiss:()->Unit){if(panel=="subtitles"){SubtitlePanel053(m,close);return};if(panel=="danmaku"){DanmakuPanel053(m,close);return};val episodeSeries by m.details.series.collectAsState();val detailWork by m.details.work.collectAsState();val titles=mapOf("sources" to "播放线路","episodes" to if(m.current?.provider=="live")"直播线路"else"选集","audio" to "音轨","subtitles" to "字幕","volume" to "音量","speed" to "倍速","scale" to "画面比例","preference" to "自动选源");val focus=remember{FocusRequester()};val listState=rememberLazyListState();LaunchedEffect(Unit){withFrameNanos{};runCatching{focus.requestFocus()}}
 val displayedEpisodes=remember(m.episodeNames,episodeSeries,detailWork?.key,detailWork?.season,m.current?.key,m.current?.season,m.currentEpisode){
  val cur=m.current
  val seriesFallback=if(cur?.kind=="tv"&&detailWork?.key==cur.key&&detailWork?.season==cur.season)episodeSeries?.episodes.orEmpty().filter{it.season==cur.season}.sortedBy{it.number}.map{"第%02d集".format(it.number)}else emptyList()
  if(cur?.provider!="live"&&cur?.kind=="tv"&&seriesFallback.size>m.episodeNames.size){
   val merged=if(m.currentEpisode.isNotBlank()&&seriesFallback.none{Identity.sameEpisode(it,m.currentEpisode)})seriesFallback+m.currentEpisode else seriesFallback
   merged.distinct().sortedBy{Identity.episode(it)?:Int.MAX_VALUE}
  }else m.episodeNames
 }
 LaunchedEffect(panel,displayedEpisodes.size){if(panel=="episodes"){val idx=displayedEpisodes.indexOfFirst{Identity.sameEpisode(it,m.currentEpisode)};if(idx>0)listState.scrollToItem((idx-1).coerceAtLeast(0))}}
 val availableTracks=remember(m.trackRevision,panel){val type=if(panel=="audio")C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT;m.player.currentTracks.groups.filter{it.type==type}.flatMap{g->(0 until g.length).map{g to it}}}
 val isCompact=panel in listOf("speed","scale","preference")
 Dialog(dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){Column(Modifier.widthIn(max=if(isCompact)360.dp else 690.dp).fillMaxWidth(if(isCompact).46f else .82f).heightIn(max=if(isCompact)260.dp else 470.dp).background(Color(0xeb181823),RoundedCornerShape(12.dp)).border(1.dp,Color(0xff494358),RoundedCornerShape(12.dp)).padding(if(isCompact)14.dp else 18.dp),verticalArrangement=Arrangement.spacedBy(if(isCompact)10.dp else 12.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Text(titles[panel]?:panel,color=Color.White,fontSize=if(isCompact)17.sp else 20.sp)
    if(panel=="episodes")PcTag("已加载 ${displayedEpisodes.size} 集")
   }
   if(panel=="sources")Row(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalAlignment=Alignment.CenterVertically){
    val canResumeMore=!m.searching&&m.checked<m.total&&m.current?.provider!="live"
    if(canResumeMore||m.pausedScan)PcChip("▶ 继续搜索",true,{m.continueScan()})
    PcChip(if(m.preference=="speed")"速度优先"else"高清优先",m.preference=="quality",{m.changePreference(if(m.preference=="speed")"quality"else"speed")})
    PcChip(if(m.current?.provider=="live")"↻ 刷新频道"else"↻ 重新检测",false,m::refreshSources)
   }
   if(panel=="audio")PcChip("音频优先级设置",false,{close();m.settingsSection="playback";m.utility("settings")})
  }
  if(panel=="speed"){
   FlowRow(Modifier.focusRequester(focus),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    listOf(.5f,.75f,1f,1.25f,1.5f,2f,2.5f).forEach{speed->PcChip("${speed}×",m.player.playbackParameters.speed==speed,{m.player.setPlaybackSpeed(speed);close()})}
   }
  }else LazyColumn(Modifier.focusRequester(focus),state=listState,verticalArrangement=Arrangement.spacedBy(8.dp)){
   when(panel){
    "sources"->{items(m.liveRouteErrors054.toList()){(name,error)->Text("$name · $error",color=Color.Gray,fontSize=12.sp)};item{Text("已检测 ${m.checked}/${m.total} · 关闭站点立即作用于后续搜索",color=PcAccent,fontSize=12.sp)};items(m.candidates,key={it.identity}){candidate->val activeSource=m.currentCandidate?.identity==candidate.identity;val badges=SourceBadgeParser102.parse("${candidate.site.name} ${candidate.road} ${candidate.episode.name}",candidate.height);val sizeLabel=ResourceLabels040.size(candidate.bytes);Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){InputButton({m.select(candidate);close()},Modifier.weight(1f).selection051(activeSource),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(9.dp)),focusRadius=9.dp,colors=ptvButtonColors(if(activeSource)Color(0xff42316b)else LocalCinema.current.raised,Color.White),contentPadding=PaddingValues(horizontal=12.dp,vertical=9.dp)){Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(5.dp)){if(badges.isNotEmpty())SourceMediaBadges102(badges);Text("${if(activeSource)"✓ "else""}${candidate.label}",color=Color.White,fontSize=13.5.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=2);Text(listOfNotNull(candidate.height.takeIf{it>0}?.let{"${it}P"},sizeLabel.takeIf{it.isNotBlank()},"${candidate.delay}ms",if(!DecodeSupport052.compatible(candidate))"本机未报告HEVC 10bit支持，可手动尝试"else null).joinToString(" · "),color=Color(0xffc9c5dc),fontSize=11.sp,maxLines=2)}};if(CloudPolicy054.needsAuthorization(candidate.site))PcChip("网盘扫码",false,{m.authorizeCloud054(candidate.site)});PcChip(if(m.siteEnabled(candidate.site))"关闭站点"else"启用站点",false,{m.toggleSite(candidate.site)})}};items(m.siteStates.filter{!it.usable},key={it.site.key}){s->Row(verticalAlignment=Alignment.CenterVertically){Text("${s.site.name} · ${s.text}",color=Color.Gray,fontSize=12.sp,modifier=Modifier.weight(1f));if(CloudPolicy054.needsAuthorization(s.site))PcChip("网盘扫码",false,{m.authorizeCloud054(s.site)})}}}
     "episodes"->{if(displayedEpisodes.isEmpty())item{Text(if(m.searching)"正在读取集数…"else"当前片源没有提供分集",color=PcAccent)};items(displayedEpisodes,key={it}){ep->val number=Identity.episode(ep);val meta=episodeSeries?.episodes?.firstOrNull{it.number==number};val progress=m.user.watches.firstOrNull{w->w.work.key==m.current?.key&&w.work.season==m.current?.season&&Identity.episode(w.episode)==number};val title=meta?.title?.takeIf{it.isNotBlank()&&!Identity.sameEpisode(it,ep)}?.let{"$ep · $it"}?:ep;val rating=when{meta?.rating.isNullOrBlank()||meta?.rating=="-"->"评分未公开";else->"${meta.rating}分 · ${meta.votes}人"};val runtime=meta?.runtime?.takeIf{it>0}?.let{"${it}分钟"}?:progress?.duration?.takeIf{it>0}?.let{clock(it)}?:"时长未公开";val resume=progress?.let{" · 上次 ${clock(it.position)} / ${clock(it.duration)}"}.orEmpty();val airDate=meta?.airDate.orEmpty();val selectedEp=Identity.sameEpisode(ep,m.currentEpisode);InputButton({m.chooseEpisode(ep,true);close()},Modifier.fillMaxWidth().selection051(selectedEp),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),focusRadius=8.dp,colors=ptvButtonColors(if(selectedEp)Color(0xff4d3a77)else LocalCinema.current.raised,Color.White),contentPadding=PaddingValues(horizontal=14.dp,vertical=10.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){Text("${if(selectedEp)"✓ "else""}$title",color=Color.White,fontSize=14.sp,maxLines=2);Text("$rating · $runtime$resume",color=Color(0xffc9c5dc),fontSize=11.sp,maxLines=2)};if(airDate.isNotBlank())Text(airDate,color=PcCyan,fontSize=12.sp,maxLines=1)}}}}
    "scale"->itemsIndexed(listOf("FIT · 自适应，完整显示","FILL · 拉伸填满","ZOOM · 放大裁切")){index,label->Action("${if(m.resizeChoice==index)"✓ "else""}$label",{m.resizeChoice=index;m.db.prefs.edit().putInt("resize_choice",index).apply();m.message("画面 ${label.substringBefore(' ')}");close()},Modifier.fillMaxWidth(),selected=m.resizeChoice==index)}
    "preference"->items(listOf("speed" to "速度优先","quality" to "高清优先")){(value,label)->Action(label,{m.changePreference(value);close()},Modifier.fillMaxWidth(),selected=m.preference==value)}
    "audio","subtitles"->{val type=if(panel=="audio")C.TRACK_TYPE_AUDIO else C.TRACK_TYPE_TEXT;val tracks=availableTracks;if(tracks.isEmpty())item{Text(if(type==C.TRACK_TYPE_AUDIO)"当前文件未报告可选音轨"else"当前文件没有内嵌字幕；在线字幕接入仍待完成",color=PcAccent,fontSize=13.sp)};items(tracks){(g,index)->val f=g.getTrackFormat(index);Action("${TrackLabels056.label(f.language,f.label,f.codecs,f.sampleMimeType,f.channelCount,index+1)}",{if(type==C.TRACK_TYPE_AUDIO)f.language?.let(m::persistAudio);m.player.trackSelectionParameters=m.player.trackSelectionParameters.buildUpon().setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup,listOf(index))).setTrackTypeDisabled(type,false).build()},Modifier.fillMaxWidth(),selected=g.isTrackSelected(index))};if(type==C.TRACK_TYPE_TEXT)item{Action("关闭字幕",{m.player.trackSelectionParameters=m.player.trackSelectionParameters.buildUpon().setTrackTypeDisabled(type,true).build()},selected=m.player.trackSelectionParameters.disabledTrackTypes.contains(type))}}
    else->Unit
   }
  }
 }}
}


