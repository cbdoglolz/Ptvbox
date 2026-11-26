package com.cbdog.ptvbox

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import androidx.tv.material3.Text

internal object TrackLabels056 {
 fun label(language:String?,name:String?,codec:String?,mime:String?,channels:Int,index:Int):String {
  val lang=language.orEmpty().lowercase().replace('_','-');val base=lang.substringBefore('-')
  val translated=when(base){"ja","jpn"->"日语";"zh","zho","chi","cmn"->if(lang.contains("hant")||lang.contains("tw"))"中文（繁体）"else"中文";"yue"->"粤语";"en","eng"->"英语";"ko","kor"->"韩语";"fr","fra","fre"->"法语";"de","deu","ger"->"德语";"es","spa"->"西班牙语";"und",""->"语言未标注";else->runCatching{java.util.Locale.forLanguageTag(lang).getDisplayLanguage(java.util.Locale.SIMPLIFIED_CHINESE)}.getOrDefault(lang).ifBlank{lang}}
  val format=codec?.takeIf{it.isNotBlank()}?:mime.orEmpty().substringAfter('/')
  val pretty=when{format.contains("eac3",true)||format.contains("e-ac3",true)->"Dolby Digital Plus";format.contains("ac3",true)||format.contains("ac-3",true)->"Dolby Digital";format.contains("mp4a",true)->"AAC";format.contains("subrip",true)->"SRT";format.contains("ssa",true)->"ASS";format.contains("vtt",true)->"WebVTT";else->format.uppercase()}
  val channel=when(channels){1->"单声道";2->"双声道 2.0";6->"5.1声道";8->"7.1声道";in 3..32->"${channels}声道";else->""}
  return listOf(translated,name.orEmpty().takeUnless{it.equals(language,true)}.orEmpty(),channel,pretty).filter{it.isNotBlank()}.distinct().joinToString(" · ")
 }
}
internal object DanmakuLayout056 {
 fun area(height:Float,percent:Int)=height*(percent.coerceIn(10,100)/100f)
 fun lanes(area:Float,font:Float)=((area-font)/(font*1.5f)).toInt().plus(1).coerceIn(1,24)
 fun travel(width:Float,textWidth:Float,percent:Int)=if(percent.coerceIn(25,100)==100)width+textWidth else width*percent.coerceIn(25,100)/100f
 fun xAt(width:Float,textWidth:Float,percent:Int,duration:Float,elapsed:Float)=width-elapsed*(travel(width,textWidth,percent)/duration.coerceAtLeast(0.1f))
 fun visible(elapsed:Float,duration:Float)=elapsed in 0f..duration.coerceAtLeast(0.1f)
}
internal object WatchVisuals056 {
 fun argb(state:WatchState)=when(state){WatchState.COMPLETED->listOf(0xff25a86b.toInt(),0xff78e6a8.toInt());WatchState.WATCHING->listOf(0xff7650da.toInt(),0xffcb91ff.toInt());WatchState.PLANNED->listOf(0xffe34d9b.toInt(),0xffffa8cc.toInt())}
 fun colors(state:WatchState)=argb(state).map(::Color)
}
internal fun Modifier.watchTint056(state:WatchState?):Modifier=if(state==null)this else background(Brush.horizontalGradient(WatchVisuals056.colors(state)),RoundedCornerShape(8.dp))

@Composable internal fun WatchPicker056(title:String,selected:WatchState?,choose:(WatchState?)->Unit,close:()->Unit){
 val c=LocalCinema.current;val maxHeight=(LocalConfiguration.current.screenHeightDp-24).coerceAtLeast(170).dp
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){
  Column(Modifier.widthIn(max=410.dp).fillMaxWidth(.82f).heightIn(max=maxHeight).background(c.surface,RoundedCornerShape(10.dp)).padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
    Text(title,color=c.text,fontSize=15.sp,modifier=Modifier.weight(1f));Text("只改变标记 · 点外部或按返回关闭",color=c.muted,fontSize=10.sp)
   }
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
    listOf("已看" to WatchState.COMPLETED,"在看" to WatchState.WATCHING,"想看" to WatchState.PLANNED).forEach{(label,value)->CompactWatchChoice056(label,value,Modifier.weight(1f),selected==value){choose(value)}}
   }
   CompactWatchChoice056("未标记",null,Modifier.fillMaxWidth(),selected==null){choose(null)}
  }
 }
}

@Composable private fun CompactWatchChoice056(label:String,state:WatchState?,modifier:Modifier,selected:Boolean,click:()->Unit){
 val c=LocalCinema.current
 val tint=state?.let{WatchVisuals056.colors(it).first()}?:c.text
 InputButton(click,modifier.heightIn(min=40.dp).outputline051(selected),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(7.dp)),colors=ptvButtonColors(Color.Transparent,c.text),contentPadding=PaddingValues(6.dp,4.dp)){Text(label,color=if(state==null)c.text else tint,fontSize=13.sp,maxLines=1)}
}
