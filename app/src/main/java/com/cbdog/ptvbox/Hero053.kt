package com.cbdog.ptvbox
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
private data class HeroSlide053(val work:Work,val logo:String)

@Composable internal fun Hero053(m:PtvModel,original:Work,page:Int,count:Int,visible:Boolean,select:(Int)->Unit){val (base,art)=presented050(m,original);val w=art?.ratings?.values?.filterNotNull()?.fold(base){v,r->v.enriched(r)}?:base;val colors=LocalCinema.current;val accent=PcAccent;val progress=remember{Animatable(0f)}
 LaunchedEffect(original.key,page,count,visible){progress.snapTo(0f);if(visible&&count>1){progress.animateTo(1f,tween(8000,easing=LinearEasing));select((page+1)%count)}}
 Box(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface).semantics{contentDescription="横幅 ${page+1}/$count · ${w.title}"}){
  AnimatedContent(HeroSlide053(w,art?.logo.orEmpty()),transitionSpec={(fadeIn(tween(420))+slideInHorizontally(tween(420)){it/16}) togetherWith (fadeOut(tween(280))+slideOutHorizontally(tween(420)){-it/16})},contentKey={it.work.key},label="横幅切换"){slide->val item=slide.work
   Box(Modifier.fillMaxSize()){
    AsyncImage(item.backdrop.ifBlank{item.poster},null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop,alpha=.6f)
    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(colors.background,colors.background.copy(alpha=.82f),Color.Transparent))))
    Row(Modifier.fillMaxSize().padding(20.dp).padding(bottom=90.dp),horizontalArrangement=Arrangement.spacedBy(22.dp),verticalAlignment=Alignment.CenterVertically){
     Column(Modifier.width(124.dp)){AsyncImage(item.poster,item.title,Modifier.fillMaxWidth().height(180.dp),contentScale=ContentScale.Fit);PcRatings(item,links=m.linkedRatings[item.ratingViewKey],tmdbConfigured=m.tmdbConfigured)}
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)){
      if(slide.logo.isNotBlank())AsyncImage(slide.logo,item.title,Modifier.widthIn(max=350.dp).height(58.dp),contentScale=ContentScale.Fit)else Text(item.title,color=colors.text,fontSize=28.sp,fontWeight=FontWeight.Bold,maxLines=2)
      Text(listOf(item.year,if(item.kind=="tv")"剧集"else"电影",item.genres.take(4).joinToString(" / ")).filter{it.isNotBlank()}.joinToString(" · "),color=PcAccent,fontSize=13.sp)
      Text(item.overview.ifBlank{"正在补充故事简介…"},color=colors.text,fontSize=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis)
      val cast=item.people.filter{it.role !in listOf("导演","Director")}.map{it.name}.distinct().take(4);if(cast.isNotEmpty())Text("主演："+cast.joinToString("、"),color=colors.muted,fontSize=12.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
     }
    }
   }
  }
  Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start=186.dp,end=24.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Action("查看资料与片源",{m.focusRow="hero";m.open(w)},Modifier.then(workFocus(m,w,"hero")))
   Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){repeat(count){i->PcChip("${i+1}",page==i,{select(i)})};Text("8秒自动切换",color=colors.muted,fontSize=11.sp)}
   Canvas(Modifier.fillMaxWidth().height(3.dp)){drawRoundRect(colors.border);drawRoundRect(accent,size=androidx.compose.ui.geometry.Size(size.width*(1-progress.value),size.height))}
  }
 }
}
