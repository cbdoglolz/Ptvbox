package com.cbdog.ptvbox

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.*
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text

internal object SearchFilter060 {
    val kindLabels=mapOf("movie" to "电影","tv" to "剧集/动画")
    fun resolution(w:Work):String {val text=(w.title+" "+w.overview).lowercase();return when{Regex("2160p|\\b4k\\b").containsMatchIn(text)->"2160P";Regex("1080p").containsMatchIn(text)->"1080P";Regex("720p").containsMatchIn(text)->"720P";else->""}}
    fun bytes(w:Work):Long {val text=w.title+" "+w.overview;val m=Regex("(?i)([0-9]+(?:\\.[0-9]+)?)\\s*(GB|MB)").find(text)?:return Long.MAX_VALUE;val n=m.groupValues[1].toDouble();return (n*(if(m.groupValues[2].equals("GB",true))1024.0*1024*1024 else 1024.0*1024)).toLong()}
    fun rating(w:Work)=w.rating.toDoubleOrNull()?:-1.0
    fun year(w:Work)=w.releaseDate.take(10).ifBlank{w.year}.padEnd(10,'0')
}

@Composable private fun FilterToggle060(label:String,selected:Boolean,click:()->Unit){val green=Color(0xff50c58a);InputButton(click,Modifier.border(1.dp,if(selected)green else Color(0xff69657a),RoundedCornerShape(7.dp)).background(if(selected)green.copy(alpha=.18f)else Color.Transparent,RoundedCornerShape(7.dp)),shape=ButtonDefaults.shape(RoundedCornerShape(7.dp)),colors=ptvButtonColors(Color.Transparent,if(selected)green else Color.White),contentPadding=PaddingValues(10.dp,6.dp)){Text(label,color=if(selected)green else Color.White,fontSize=12.sp,maxLines=1)}}

@Composable internal fun SearchResults040(m:PtvModel,provider:String,initialGenre:String=""){
    val grid=rememberLazyGridState()
    var selected by rememberSaveable{mutableStateOf("")}
    var kinds by remember(initialGenre){mutableStateOf<Set<String>>(emptySet())}
    var genres by remember(initialGenre){mutableStateOf<Set<String>>(if(initialGenre.isBlank())emptySet()else setOf(initialGenre))}
    var resolutions by remember(initialGenre){mutableStateOf<Set<String>>(emptySet())}
    var sort by rememberSaveable{mutableStateOf("default")}
    val mode=remember(m.preferencesRevision){m.config.text("search_display","sites")}
    val base=m.searchResults.filter{provider.isBlank()||it.provider==provider}
    val allGenres=base.flatMap{it.genres}.distinct().sorted().take(18)
    val rows=base.filter{w->
        (kinds.isEmpty()||w.kind in kinds)&&(genres.isEmpty()||w.genres.any{it in genres})&&(resolutions.isEmpty()||SearchFilter060.resolution(w) in resolutions)
    }.let{w->when(sort){"yearAsc"->w.sortedWith(compareBy<Work>{SearchFilter060.year(it)}.thenBy{it.title});"yearDesc"->w.sortedWith(compareByDescending<Work>{SearchFilter060.year(it)}.thenBy{it.title});"rating"->w.sortedByDescending{SearchFilter060.rating(it)};"size"->w.sortedBy{SearchFilter060.bytes(it)};else->w}}
    val groups=rows.groupBy{it.provider}
    fun label(key:String)=m.availableSites.firstOrNull{key=="site_${it.key}"}?.name?:when(key){"douban"->"豆瓣";"tmdb"->"TMDB";else->key}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            SearchFilter060.kindLabels.forEach{(id,name)->FilterToggle060(name,id in kinds){kinds=if(id in kinds)kinds-id else kinds+id}}
            listOf("720P","1080P","2160P").filter{p->base.any{SearchFilter060.resolution(it)==p}}.forEach{p->FilterToggle060(p,p in resolutions){resolutions=if(p in resolutions)resolutions-p else resolutions+p}}
            allGenres.forEach{g->FilterToggle060(g,g in genres){genres=if(g in genres)genres-g else genres+g}}
            listOf("default" to "默认紧凑","size" to "容量小到大","yearAsc" to "年份升序","yearDesc" to "年份降序","rating" to "评分优先").forEach{(id,name)->FilterToggle060(name,sort==id){sort=id}}
        }
        if(groups.keys.any{it.startsWith("site_")}&&mode=="sites")LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){item{PcChip("全部站点",selected.isBlank(),{selected=""})};items(groups.keys.toList(),key={it}){key->PcChip("${label(key)} ${groups[key]?.size}",selected==key,{selected=key})}}
        if(mode=="groups"&&groups.keys.any{it.startsWith("site_")}){
            LazyColumn(verticalArrangement=Arrangement.spacedBy(18.dp)){items(groups.keys.toList(),key={it}){key->Column(verticalArrangement=Arrangement.spacedBy(9.dp)){PcSection(label(key));LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(groups[key].orEmpty(),key={it.key}){CinemaPoster(m,it,"search-group-$key")}}}}}
        }else LazyVerticalGrid(GridCells.Adaptive(112.dp),state=grid,horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){items(rows.filter{selected.isBlank()||selected !in groups||it.provider==selected},key={it.key}){CinemaPoster(m,it,"search")}}
    }
}
