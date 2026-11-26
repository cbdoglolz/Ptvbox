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
@Composable internal fun MediaOverlay053(m:PtvModel){val a=m.addons;val s=m.config;m.preferencesRevision;var frame by remember{mutableLongStateOf(0)};val active=a.cues.isNotEmpty()||a.danmaku.isNotEmpty();LaunchedEffect(active){if(active)while(isActive){withFrameNanos{frame=it}}}
 val paint=remember{Paint(Paint.ANTI_ALIAS_FLAG)};val dmPaint=remember{Paint(Paint.ANTI_ALIAS_FLAG)}
 Canvas(Modifier.fillMaxSize()){
  frame;val t=m.player.currentPosition.coerceAtLeast(0);val native=drawContext.canvas.nativeCanvas
  if(a.danmaku.isNotEmpty()&&s.flag("danmaku_enabled",true)){
   dmPaint.typeface=Typeface.DEFAULT_BOLD;dmPaint.textSize=s.number("danmaku_size",20).coerceIn(12,40).sp.toPx();dmPaint.setShadowLayer(2.dp.toPx(),0f,0f,android.graphics.Color.BLACK)
   val area=DanmakuLayout056.area(size.height,s.number("danmaku_area",50));val lanes=DanmakuLayout056.lanes(area,dmPaint.textSize);val duration=s.number("danmaku_duration",8).coerceIn(4,20).toFloat();val travelPercent=s.number("danmaku_travel_percent",100).coerceIn(25,100);val shift=s.number("danmaku_delay",0)*1000L;val now=t-shift;val end=upper053(a.danmaku,now){it.time};val occupied=mutableSetOf<Int>();var drawn=0;val color=runCatching{android.graphics.Color.parseColor(s.text("danmaku_color","#FFFFFF"))}.getOrDefault(android.graphics.Color.WHITE)
   for(i in end-1 downTo (end-600).coerceAtLeast(0)){val d=a.danmaku[i];val elapsed=(now-d.time)/1000f;if(elapsed>40)break;val width=dmPaint.measureText(d.text);val fixed=d.mode in listOf(4,5);if(elapsed<0||if(fixed)elapsed>4 else !DanmakuLayout056.visible(elapsed,duration))continue
    val lane=if(d.mode==4)lanes+1 else if(d.mode==5)lanes else i%lanes;if(!occupied.add(lane))continue;if(drawn++>=s.number("danmaku_density",12).coerceIn(1,20))break
    dmPaint.color=(s.number("danmaku_alpha",80).coerceIn(10,100)*255/100 shl 24) or (color and 0xffffff);val x=if(fixed)(size.width-width)/2 else DanmakuLayout056.xAt(size.width,width,travelPercent,duration,elapsed);val y=when(d.mode){4->(area-dmPaint.textSize*.3f).coerceAtLeast(dmPaint.textSize);5->dmPaint.textSize;else->dmPaint.textSize+lane*dmPaint.textSize*1.5f};native.drawText(d.text,x,y,dmPaint)
   }
  }
  val now=t-a.delayMs;val end=upper053(a.cues,now){it.start};val lines=a.cues.subList((end-12).coerceAtLeast(0),end).filter{now<it.end}.flatMap{it.text.lines()}.take(6)
  if(lines.isNotEmpty()){paint.typeface=Typeface.create(s.text("subtitle_font","sans-serif"),if(s.flag("subtitle_bold",true))Typeface.BOLD else Typeface.NORMAL);paint.textSize=s.number("subtitle_size",24).coerceIn(12,64).sp.toPx().coerceAtMost(size.width/12);paint.textAlign=Paint.Align.CENTER;paint.setShadowLayer(2.dp.toPx(),0f,1.dp.toPx(),android.graphics.Color.BLACK);val h=paint.textSize*1.25f;val base=size.height*.86f-(lines.size-1)*h
   lines.forEachIndexed{i,line->val width=paint.measureText(line);val y=base+i*h;paint.color=(s.number("subtitle_background_alpha",40).coerceIn(0,100)*255/100 shl 24) or (runCatching{android.graphics.Color.parseColor(s.text("subtitle_background_color","#000000"))}.getOrDefault(0) and 0xffffff);paint.clearShadowLayer();native.drawRoundRect((size.width-width)/2-8.dp.toPx(),y-paint.textSize,size.width/2+width/2+8.dp.toPx(),y+paint.textSize*.25f,4.dp.toPx(),4.dp.toPx(),paint);paint.color=s.subtitleColor();paint.setShadowLayer(2.dp.toPx(),0f,0f,android.graphics.Color.BLACK);native.drawText(line,size.width/2,y,paint)}
  }
 }
}
@Composable internal fun SubtitleBadge053(m:PtvModel,modifier:Modifier=Modifier){val a=m.addons;if(a.badge.isBlank())return;var bright by remember{mutableStateOf(true)};LaunchedEffect(a.searching){while(a.searching){kotlinx.coroutines.delay(1100);bright=!bright};bright=true};Text(a.badge,color=Color(0xffe0c9ff).copy(alpha=if(bright)1f else .55f),fontSize=12.sp,maxLines=2,modifier=modifier.widthIn(max=440.dp).background(Color(0x9a22152f),RoundedCornerShape(8.dp)).border(1.dp,Color(0xffb27af8),RoundedCornerShape(8.dp)).padding(9.dp))}
