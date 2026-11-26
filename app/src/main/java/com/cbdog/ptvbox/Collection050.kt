package com.cbdog.ptvbox
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object CollectionTabs050 {
 val labels=listOf("在看","已看","想看","收藏")
 fun rows(tab:String,snapshot:UserSnapshot,works:List<Work>,favorites:List<Work>):List<Work>{val state=when(tab){"在看"->WatchState.WATCHING;"已看"->WatchState.COMPLETED;"想看"->WatchState.PLANNED;else->null};return if(state==null)favorites else works.filter{snapshot.states[it.key]==state}.distinctBy{it.key}}
}
@Composable fun Collection050(m:PtvModel){var tab by rememberSaveable{mutableStateOf("收藏")};val rows=CollectionTabs050.rows(tab,m.user,m.markedWorks,m.favoriteWorks);val holders=rememberSaveableStateHolder050()
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)){
  PcSection("我的片库");Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){CollectionTabs050.labels.forEach{label->PcChip(label,tab==label,{tab=label})}}
  holders.SaveableStateProvider(tab){LazyVerticalGrid(GridCells.Fixed(6),Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(8.dp)){
   if(tab=="收藏"&&m.favoriteEpisodes.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){Column{PcSection("单集收藏");LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(m.favoriteEpisodes){(w,n)->Action("${w.title} · S${w.season}E$n",{m.open(w)},Modifier.width(230.dp))}}}}
   if(rows.isEmpty())item(span={GridItemSpan(maxLineSpan)}){PcEmpty("暂无${tab}作品","在详情页设置观看状态或收藏，作品会自动收录到这里。")}
   items(rows,key={it.key}){CinemaPoster(m,it,"collection-$tab")}
  }}
 }
}
@Composable private fun rememberSaveableStateHolder050()=androidx.compose.runtime.saveable.rememberSaveableStateHolder()
