package com.cbdog.ptvbox

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Live preview driven by the same settings used by MediaOverlay053. */
@Composable internal fun DanmakuPreview060(m:PtvModel,modifier:Modifier=Modifier){
    m.preferencesRevision
    val c=LocalCinema.current
    val size=m.config.number("danmaku_size",20)
    val alpha=m.config.number("danmaku_alpha",80)
    val density=m.config.number("danmaku_density",12)
    val duration=m.config.number("danmaku_duration",8).coerceAtLeast(1)
    val travel=m.config.number("danmaku_travel_percent",100)
    val area=m.config.number("danmaku_area",50)
    val color=runCatching{android.graphics.Color.parseColor(m.config.text("danmaku_color","#FFFFFF"))}.getOrDefault(android.graphics.Color.WHITE)
    var now by remember{mutableLongStateOf(0L)}
    LaunchedEffect(Unit){while(true)withFrameNanos{now=it}}
    Column(modifier.background(Color(0xff11121f),RoundedCornerShape(12.dp)).border(1.dp,c.border,RoundedCornerShape(12.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text("实时预览",color=PcAccent,fontSize=12.sp)
            Text("$size sp · $alpha% · $travel%",color=c.muted,fontSize=10.sp)
        }
        Canvas(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xff080a13))){
            val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{
                textSize=size.sp.toPx()
                typeface=android.graphics.Typeface.DEFAULT_BOLD
                this.color=(alpha*255/100 shl 24) or (color and 0xffffff)
                setShadowLayer(2.dp.toPx(),0f,0f,android.graphics.Color.BLACK)
            }
            val samples=listOf("这个字幕同步了","画面很清晰","前方高能","终于更新了","这个镜头不错","弹幕实时预览","支持遥控器和触摸","继续追番")
            val visibleArea=DanmakuLayout056.area(this.size.height,area)
            val lanes=DanmakuLayout056.lanes(visibleArea,paint.textSize)
            samples.take(density.coerceIn(2,8)).forEachIndexed{index,text->
                val width=paint.measureText(text)
                val elapsed=((now/1_000_000_000.0+index*1.35)%duration.toDouble()).toFloat()
                val x=DanmakuLayout056.xAt(this.size.width,width,travel,duration.toFloat(),elapsed)
                val lane=index%lanes
                val y=paint.textSize+lane*paint.textSize*1.45f
                drawContext.canvas.nativeCanvas.drawText(text,x,y,paint)
            }
            drawContext.canvas.nativeCanvas.drawLine(
                0f,visibleArea.coerceAtMost(this.size.height),this.size.width,visibleArea.coerceAtMost(this.size.height),
                Paint().apply{strokeWidth=1.dp.toPx();this.color=android.graphics.Color.argb(100,187,156,245)}
            )
        }
    }
}
