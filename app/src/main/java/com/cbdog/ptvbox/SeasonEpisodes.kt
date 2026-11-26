package com.cbdog.ptvbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable internal fun SeasonEpisodes(m:PtvModel,w:Work){
    val series by m.details.series.collectAsState();val status by m.details.seriesStatus.collectAsState();var id by remember(w.key){mutableStateOf("")}
    if(w.kind!="tv")return
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        PcSection("季与单集","优先显示集数占位，再加载剧照、评分和简介")
        Text(status,color=PcAccent,fontSize=13.sp)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Box(Modifier.weight(1f)){Entry(id,{id=it},"可选：手动关联TMDB剧集数字ID")};Action("关联",{if(id.isNotBlank())m.details.linkSeries(id.trim())});Action("资料设置",{m.settingsSection="metadata";m.screen="settings"})}
        series?.let{s->
            LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(s.seasons,key={it.number}){season->Action("${if(w.season==season.number)"● "else""}${season.name} · ${season.count}集",{m.details.selectSeason(season.number)})}}
            LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(end=8.dp)){items(s.episodes,key={it.id}){ep->EpisodeCard054(m,w,ep)}}
        }?:run{
            LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(4){EpisodeSkeleton055()}}
        }
    }
}
