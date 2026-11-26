package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable internal fun HistoryPage057(m:PtvModel){
 val c=LocalCinema.current;val rows=m.user.watches.distinctBy{it.work.key};var selected by remember{mutableStateOf<Set<String>>(emptySet())};var confirm by remember{mutableStateOf(false)};val grid=rememberLazyGridState()
 androidx.activity.compose.BackHandler(selected.isNotEmpty()){selected=emptySet()}
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)){
  PcSection("观看历史",if(selected.isEmpty())"${rows.size}部作品 · 长按进入多选" else "已选择 ${selected.size} 部"){
   InputButton({confirm=true},Modifier.size(50.dp).semantics{contentDescription=if(selected.isEmpty())"清空全部历史" else "删除选中的 ${selected.size} 部历史"},shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(9.dp)),contentPadding=PaddingValues(6.dp)){Text("🗑",fontSize=22.sp)}
  }
  LazyVerticalGrid(GridCells.Adaptive(118.dp),state=grid,modifier=Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(5.dp)){
   if(rows.isEmpty())item(span={GridItemSpan(maxLineSpan)}){PcEmpty("暂无观看历史","播放影片后会自动记录进度、单集和观看时间。")}
   items(rows,key={it.work.key}){watch->
    val chosen=watch.work.key in selected
    Column(Modifier.widthIn(max=118.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
     InputButton({if(selected.isEmpty())m.open(watch.work)else selected=if(chosen)selected-watch.work.key else selected+watch.work.key},Modifier.fillMaxWidth().then(workFocus(m,watch.work,"history")).semantics{contentDescription="观看历史 ${watch.work.title}"},onLongClick={selected=selected+watch.work.key},longPressMillis=700,shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(9.dp)),contentPadding=PaddingValues(0.dp)){
      Box(Modifier.fillMaxWidth().aspectRatio(158f/230f).background(c.surface,RoundedCornerShape(9.dp)).then(if(chosen)Modifier.border(3.dp,PcAccent,RoundedCornerShape(9.dp))else Modifier)){
       AsyncImage(watch.work.poster,watch.work.title,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
       Box(Modifier.fillMaxWidth().height(48.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xdd090b14)))))
       Text(HistoryTime057.format(watch.updated),color=Color.White,fontSize=10.sp,modifier=Modifier.align(Alignment.TopStart).background(Color(0xcc11121c),RoundedCornerShape(4.dp)).padding(4.dp).semantics{contentDescription="观看时间 ${HistoryTime057.format(watch.updated)}"})
       if(chosen)Text("✓",color=Color.White,fontSize=16.sp,modifier=Modifier.align(Alignment.TopEnd).padding(5.dp).background(PcAccent,RoundedCornerShape(12.dp)).padding(horizontal=7.dp,vertical=2.dp))
      }
     }
     Text(watch.work.title,color=c.text,fontSize=12.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
     Text("${watch.episode} · ${clock(watch.position)}",color=c.muted,fontSize=10.sp,maxLines=1)
    }
   }
  }
 }
 if(confirm)Dialog({confirm=false}){
  val clearAll=selected.isEmpty()
  Column(Modifier.widthIn(max=420.dp).background(c.surface,RoundedCornerShape(12.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   Text(if(clearAll)"清空全部观看历史？" else "删除选中的 ${selected.size} 部历史？",color=c.text,fontSize=18.sp)
   Text("此操作会删除对应作品的本地播放进度。收藏和观看状态不会自动删除。",color=c.muted,fontSize=12.sp)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
    Action(if(clearAll)"确认清空" else "确认删除",{
     val keys=selected
     m.users.write{if(clearAll)clearHistory()else rows.filter{it.work.key in keys}.forEach{forget(it.work)}}
     selected=emptySet();confirm=false;m.message(if(clearAll)"观看历史已清空" else "已删除 ${keys.size} 部观看历史")
    })
    Action("取消",{confirm=false})
   }
  }
 }
}

internal object HistoryTime057{
 private val formatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
 fun format(epochMs:Long)=runCatching{Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).format(formatter)}.getOrDefault("")
}
