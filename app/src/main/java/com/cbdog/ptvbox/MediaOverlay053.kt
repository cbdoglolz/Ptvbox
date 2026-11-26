package com.cbdog.ptvbox

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import kotlinx.coroutines.isActive

private fun <T> upper053(items:List<T>,time:Long,key:(T)->Long):Int{var lo=0;var hi=items.size;while(lo<hi){val mid=(lo+hi)/2;if(key(items[mid])<=time)lo=mid+1 else hi=mid};return lo}
private class DanmakuPlan102(val lanes:IntArray,val widths:FloatArray,val speedPxPerSec:Float,val maxLifetimeMs:Long)
private fun buildDanmakuPlan102(list:List<Danmaku053>,paint:Paint,screenW:Float,lanesCount:Int,durationSec:Float,gapPx:Float,travelPercent:Int):DanmakuPlan102 {
 val count=list.size
 val lanes=IntArray(count){-1}
 val widths=FloatArray(count)
 val speed=(screenW+paint.textSize*12f)/durationSec.coerceAtLeast(1f)
 val laneFreeAt=LongArray(lanesCount){Long.MIN_VALUE}
 var topFreeAt=Long.MIN_VALUE;var bottomFreeAt=Long.MIN_VALUE
 var maxLife=(durationSec*1000f).toLong()+4000L
 for(i in 0 until count){
  val d=list[i]
  val w=paint.measureText(d.text);widths[i]=w
  val fixed=d.mode in listOf(4,5)
  if(fixed){
   if(d.mode==4){if(d.time>=bottomFreeAt){lanes[i]=lanesCount+1;bottomFreeAt=d.time+4000L}}
   else{if(d.time>=topFreeAt){lanes[i]=lanesCount;topFreeAt=d.time+4000L}}
  }else{
   val start=i%lanesCount
   var chosen=-1
   for(step in 0 until lanesCount){
    val cand=(start+step)%lanesCount
    if(d.time>=laneFreeAt[cand]){chosen=cand;break}
   }
   if(chosen>=0){
    lanes[i]=chosen
    val clearMs=((w+gapPx)/speed*1000f).toLong().coerceAtLeast(250L)
    laneFreeAt[chosen]=d.time+clearMs
    val travelDist=if(travelPercent>=100)screenW+w else screenW*(travelPercent.coerceIn(25,100)/100f)
    val life=((travelDist/speed)*1000f).toLong()+500L
    if(life>maxLife)maxLife=life
   }
  }
 }
 return DanmakuPlan102(lanes,widths,speed,maxLife)
}
@Composable internal fun MediaOverlay053(m:PtvModel){val a=m.addons;val s=m.config;m.preferencesRevision
 var smoothPosMs by remember{mutableLongStateOf(m.player.currentPosition.coerceAtLeast(0))}
 val hasOverlay=(a.externalSubtitleEnabled&&a.cues.isNotEmpty())||(a.danmaku.isNotEmpty()&&s.flag("danmaku_enabled",true))
 val running=hasOverlay&&m.playing
 LaunchedEffect(m.current?.key,m.currentEpisode,m.position){
  val p=m.player.currentPosition.coerceAtLeast(0)
  if(kotlin.math.abs(smoothPosMs-p)>450L||!m.playing)smoothPosMs=p
 }
 LaunchedEffect(running){
  if(running){
   var lastNanos=0L
   while(isActive){
    withFrameNanos{nowNanos->
     val playerPos=m.player.currentPosition.coerceAtLeast(0)
     if(lastNanos==0L){smoothPosMs=playerPos;lastNanos=nowNanos}
     else{
      val dtMs=((nowNanos-lastNanos)/1_000_000L).coerceIn(0L,100L)
      lastNanos=nowNanos
      val speed=m.player.playbackParameters.speed.coerceIn(0.25f,3f)
      val predicted=smoothPosMs+(dtMs*speed).toLong()
      smoothPosMs=if(kotlin.math.abs(predicted-playerPos)>450L)playerPos else predicted
     }
    }
   }
  }
 }
 val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG)};val dmPaint=remember{Paint(Paint.ANTI_ALIAS_FLAG)}
 val dmSizeSp=s.number("danmaku_size",20).coerceIn(12,40)
 val dmAreaPct=s.number("danmaku_area",50)
 val dmDuration=s.number("danmaku_duration",8).coerceIn(4,20).toFloat()
 val dmTravel=s.number("danmaku_travel_percent",100).coerceIn(25,100)
 val dmDensity=s.number("danmaku_density",12).coerceIn(1,20)
 val dmAlpha=s.number("danmaku_alpha",80).coerceIn(10,100)
 val dmDelay=s.number("danmaku_delay",0)*1000L
 val dmColor=remember(s.text("danmaku_color","#FFFFFF")){runCatching{android.graphics.Color.parseColor(s.text("danmaku_color","#FFFFFF"))}.getOrDefault(android.graphics.Color.WHITE)}
 val density=androidx.compose.ui.platform.LocalDensity.current
 val config=androidx.compose.ui.platform.LocalConfiguration.current
 val screenWpx=with(density){config.screenWidthDp.dp.toPx().coerceAtLeast(640f)}
 val screenHpx=with(density){config.screenHeightDp.dp.toPx().coerceAtLeast(360f)}
 val plan=remember(a.danmaku,dmSizeSp,dmAreaPct,dmDuration,dmTravel,screenWpx,screenHpx){
  dmPaint.typeface=Typeface.DEFAULT_BOLD
  dmPaint.textSize=with(density){dmSizeSp.sp.toPx()}
  val area=DanmakuLayout056.area(screenHpx,dmAreaPct)
  val lanesCount=DanmakuLayout056.lanes(area,dmPaint.textSize)
  buildDanmakuPlan102(a.danmaku,dmPaint,screenWpx,lanesCount,dmDuration,with(density){40.dp.toPx()},dmTravel)
 }
 Canvas(Modifier.fillMaxSize()){
  val t=smoothPosMs;val native=drawContext.canvas.nativeCanvas
  if(a.danmaku.isNotEmpty()&&s.flag("danmaku_enabled",true)){
   dmPaint.typeface=Typeface.DEFAULT_BOLD;dmPaint.textSize=dmSizeSp.sp.toPx();dmPaint.setShadowLayer(2.dp.toPx(),0f,0f,android.graphics.Color.BLACK)
   dmPaint.color=(dmAlpha*255/100 shl 24) or (dmColor and 0xffffff)
   val area=DanmakuLayout056.area(size.height,dmAreaPct);val lanesCount=DanmakuLayout056.lanes(area,dmPaint.textSize)
   val now=t-dmDelay
   val startIdx=upper053(a.danmaku,now-plan.maxLifetimeMs){it.time}.coerceAtLeast(0)
   val end=upper053(a.danmaku,now){it.time}
   val maxDraw=dmDensity*3;var drawn=0
   val minX=if(dmTravel>=100)Float.NEGATIVE_INFINITY else size.width*(1f-dmTravel/100f)
   for(i in startIdx until end){
    val lane=plan.lanes[i];if(lane<0)continue
    val d=a.danmaku[i];val elapsed=(now-d.time)/1000f;if(elapsed<0f)continue
    val w=plan.widths[i]
    if(lane>=lanesCount){
     if(elapsed>4f)continue
     val y=if(lane==lanesCount+1)(area-dmPaint.textSize*.3f).coerceAtLeast(dmPaint.textSize)else dmPaint.textSize
     native.drawText(d.text,(size.width-w)/2f,y,dmPaint)
    }else{
     val x=size.width-elapsed*plan.speedPxPerSec
     if(x+w<0f||x<minX)continue
     val y=dmPaint.textSize+lane*dmPaint.textSize*1.5f
     native.drawText(d.text,x,y,dmPaint)
    }
    if(++drawn>=maxDraw)break
   }
  }
  val now=t-a.delayMs;val end=if(a.externalSubtitleEnabled)upper053(a.cues,now){it.start}else 0;val lines=if(a.externalSubtitleEnabled)a.cues.subList((end-12).coerceAtLeast(0),end).filter{now<it.end}.flatMap{it.text.lines()}.take(6)else emptyList()
  if(lines.isNotEmpty()){paint.typeface=Typeface.create(s.text("subtitle_font","sans-serif"),if(s.flag("subtitle_bold",true))Typeface.BOLD else Typeface.NORMAL);paint.textSize=s.number("subtitle_size",24).coerceIn(12,64).sp.toPx().coerceAtMost(size.width/12);paint.textAlign=Paint.Align.CENTER;paint.setShadowLayer(2.dp.toPx(),0f,1.dp.toPx(),android.graphics.Color.BLACK);val h=paint.textSize*1.25f;val base=size.height*.86f-(lines.size-1)*h
   lines.forEachIndexed{i,line->val width=paint.measureText(line);val y=base+i*h;paint.color=(s.number("subtitle_background_alpha",40).coerceIn(0,100)*255/100 shl 24) or (runCatching{android.graphics.Color.parseColor(s.text("subtitle_background_color","#000000"))}.getOrDefault(0) and 0xffffff);paint.clearShadowLayer();native.drawRoundRect((size.width-width)/2-8.dp.toPx(),y-paint.textSize,size.width/2+width/2+8.dp.toPx(),y+paint.textSize*.25f,4.dp.toPx(),4.dp.toPx(),paint);paint.color=s.subtitleColor();paint.setShadowLayer(2.dp.toPx(),0f,0f,android.graphics.Color.BLACK);native.drawText(line,size.width/2,y,paint)}
  }
 }
}
@Composable internal fun SubtitleBadge053(m:PtvModel,modifier:Modifier=Modifier){val a=m.addons;if(a.badge.isBlank())return;var bright by remember{mutableStateOf(true)};LaunchedEffect(a.searching){while(a.searching){kotlinx.coroutines.delay(1100);bright=!bright};bright=true};Text(a.badge,color=Color(0xffe0c9ff).copy(alpha=if(bright)1f else .55f),fontSize=12.sp,maxLines=2,modifier=modifier.widthIn(max=440.dp).background(Color(0x9a22152f),RoundedCornerShape(8.dp)).border(1.dp,Color(0xffb27af8),RoundedCornerShape(8.dp)).padding(9.dp))}
