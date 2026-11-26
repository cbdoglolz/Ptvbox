package com.cbdog.ptvbox
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

val SelectionBrush051=Brush.horizontalGradient(listOf(Color(0xff53D7FF),Color(0xffB87CFF),Color(0xffF0A2FF)))
fun Modifier.selection051(selected:Boolean):Modifier=this.semantics{this.selected=selected}.then(if(selected)border(1.5.dp,SelectionBrush051,RoundedCornerShape(8.dp))else Modifier)
/** Status markers use outline-only emphasis so their label remains readable over the dark player UI. */
fun Modifier.outputline051(selected:Boolean,width:androidx.compose.ui.unit.Dp=1.dp):Modifier=
    semantics{this.selected=selected}.then(if(selected)border(width,SelectionBrush051,RoundedCornerShape(8.dp))else Modifier)
@Composable fun Modifier.focusOutline051(shape:RoundedCornerShape=RoundedCornerShape(8.dp),width:androidx.compose.ui.unit.Dp=1.dp):Modifier{
    var focused by remember{mutableStateOf(false)}
    return onFocusChanged{focused=it.isFocused}.then(if(focused)border(width,SelectionBrush051,shape)else Modifier)
}
fun lastRow051(focusKey:Any?,dataKeys:Set<Any>,rows:List<Triple<Any,Int,Int>>):Boolean {val last=rows.filter{it.first in dataKeys}.maxByOrNull{it.third}?:return false;return focusKey in dataKeys&&rows.any{it.first==focusKey&&it.second>=0&&it.second==last.second}&&last.first==dataKeys.lastOrNull()}
fun Modifier.pagedDown051(state:LazyGridState,keys:List<Any>,focused:()->Any?,busy:Boolean,more:Boolean,load:()->Unit)=onPreviewKeyEvent{e->if(e.key==Key.DirectionDown&&lastRow051(focused(),keys.toSet(),state.layoutInfo.visibleItemsInfo.map{Triple(it.key,it.row,it.index)})){if(e.type==KeyEventType.KeyDown&&e.nativeKeyEvent.repeatCount==0&&!busy&&more)load();true}else false}
@Composable fun MoreHint051(busy:Boolean,more:Boolean,load:()->Unit){Text(if(busy)"正在加载更多…"else if(more)"↓ 最后一行再按向下加载更多 · 触摸可点击这里"else"已显示当前全部内容",color=if(more)PcAccent else LocalCinema.current.muted,fontSize=13.sp,modifier=Modifier.fillMaxWidth().background(LocalCinema.current.surface,RoundedCornerShape(8.dp)).clickable(enabled=!busy&&more,onClick=load).padding(14.dp))}
object Episodes051 {
 fun names(kind:String,current:String,selected:Candidate?,candidates:List<Candidate>,roads:Map<String,List<Episode>>):List<String>{
  val entries=selected?.let{roads[it.sourceId]?:roads[it.site.key+"|"+it.road]}.orEmpty()
  if(entries.size>1)return entries.map{it.name}.distinct()
  if(kind=="movie")return listOf(current.ifBlank{"正片"})
  return (listOf(current)+candidates.filter{Identity.episode(it.episode.name)!=null}.map{it.episode.name}).filter{it.isNotBlank()}.distinct().sortedBy{Identity.episode(it)?:Int.MAX_VALUE}
 }
 fun fallbackPosition(current:Long,target:Long)=maxOf(current,target,0)
}
