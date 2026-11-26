package com.cbdog.ptvbox

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.tv.material3.*
import java.time.LocalDate

@Immutable data class CinemaColors(val background:Color,val surface:Color,val raised:Color,val text:Color,val muted:Color,val border:Color)
private val NightCinema=CinemaColors(Color(0xff10111d),Color(0xff1d1e2d),Color(0xff28293b),Color(0xfff4f3fa),Color(0xffa9a9bb),Color(0xff39394c))
private val DayCinema=CinemaColors(Color(0xfff4f5f8),Color.White,Color(0xffe9e9f1),Color(0xff202032),Color(0xff777382),Color(0xffdad9e4))
val LocalCinema=staticCompositionLocalOf{NightCinema}
val PcAccent:Color @Composable get()=if(LocalCinema.current.background==DayCinema.background)Color(0xff7750b8)else Color(0xffae92f7)
val PcGreen=Color(0xff56c99d)
val PcCyan=Color(0xff55c5e1)
val PcGradient=Brush.horizontalGradient(listOf(Color(0xff695de8),Color(0xff9842d9)))
@Composable fun CinemaTheme(light:Boolean,content:@Composable ()->Unit){CompositionLocalProvider(LocalCinema provides if(light)DayCinema else NightCinema){MaterialTheme(colorScheme=if(light)lightColorScheme()else darkColorScheme(),content=content)}}
@Composable fun PcBrand(compact:Boolean=false){Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(9.dp)){
    Image(painterResource(R.drawable.brand),"Ptvbox Logo",Modifier.size(if(compact)30.dp else 38.dp))
    if(!compact){Text("Ptvbox",color=LocalCinema.current.text,fontSize=24.sp,fontWeight=FontWeight.ExtraBold);Text("TV",color=PcAccent,fontSize=10.sp)}
}}
@Composable fun PcSection(title:String,subtitle:String="",trailing:@Composable ()->Unit={}){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text(title,color=LocalCinema.current.text,fontSize=22.sp,fontWeight=FontWeight.SemiBold);if(subtitle.isNotBlank())Text(subtitle,color=LocalCinema.current.muted,fontSize=12.sp)};trailing()}}
@Composable fun PcChip(label:String,selected:Boolean=false,onClick:()->Unit,modifier:Modifier=Modifier){InputButton(onClick,modifier.selection051(selected).semantics{contentDescription=label},shape=ButtonDefaults.shape(shape=RoundedCornerShape(8.dp)),colors=ptvButtonColors(if(selected)Color(0xff4d3a77)else LocalCinema.current.surface,if(selected)Color.White else LocalCinema.current.muted),contentPadding=PaddingValues(14.dp,9.dp)){Text(label,fontSize=13.sp,maxLines=1)}}
@Composable fun PcTag(label:String,color:Color=PcAccent){Text(label,color=color,fontSize=11.sp,modifier=Modifier.background(color.copy(alpha=.1f),RoundedCornerShape(5.dp)).border(1.dp,color.copy(alpha=.35f),RoundedCornerShape(5.dp)).padding(7.dp,4.dp))}
@Composable fun PendingBadge(){Text("未上映",fontSize=11.sp,color=Color(0xfff1b4e8),modifier=Modifier.background(Brush.horizontalGradient(listOf(Color(0xdd713151),Color(0xdd4b2877))),RoundedCornerShape(5.dp)).border(1.dp,Color(0xffba6fd8),RoundedCornerShape(5.dp)).padding(6.dp,3.dp))}
fun Work.isUpcoming(today:LocalDate=LocalDate.now()):Boolean=runCatching{LocalDate.parse(releaseDate.take(10))>today}.getOrDefault(false)
fun Work.releaseLabel():String=runCatching{val d=LocalDate.parse(releaseDate.take(10));"上映于${d.year}年${d.monthValue}月${d.dayOfMonth}日"}.getOrDefault("")
@Composable fun PcRatings(w:Work,compact:Boolean=true,links:Map<String,Work?>?=null,tmdbConfigured:Boolean=true,primary:String="douban",secondary:Boolean=true){val c=LocalCinema.current;Row(Modifier.fillMaxWidth().background(c.surface.copy(alpha=.94f),RoundedCornerShape(4.dp)).padding(if(compact)6.dp else 12.dp),horizontalArrangement=Arrangement.SpaceBetween){
    for(p in if(secondary)listOf(primary,if(primary=="douban")"tmdb"else"douban")else listOf(primary)){val known=links?.containsKey(p)==true;val data=if(known)links?.get(p)else w.takeIf{it.provider==p&&(p!="douban"||w.kind!="tv"||w.season==w.catalogSeason)};val matched=data!=null&&(p!="tmdb"||tmdbConfigured);val color=if(matched){if(p=="douban")PcGreen else PcCyan}else Color(0xff948c89).copy(alpha=.55f)
        Box(Modifier.weight(1f).height(if(compact)20.dp else 26.dp),contentAlignment=Alignment.Center){
            Text("${if(p=="douban")"豆瓣"else"TMDB"} ${if(matched)data?.rating?:"-" else "-"}",color=color,maxLines=1,softWrap=false,lineHeight=if(compact)18.sp else 24.sp,fontSize=if(compact)11.sp else 15.sp,fontWeight=if(matched)FontWeight.Medium else FontWeight.Normal)
        }
    }
}}
@Composable fun PcProgress(fraction:Float,modifier:Modifier=Modifier){Box(modifier.fillMaxWidth().height(3.dp).background(LocalCinema.current.border)){Box(Modifier.fillMaxWidth(fraction.coerceIn(0f,1f)).fillMaxHeight().background(PcAccent))}}
@Composable fun PcEmpty(title:String,description:String){Column(Modifier.fillMaxWidth().background(LocalCinema.current.surface,RoundedCornerShape(10.dp)).padding(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(title,color=LocalCinema.current.text,fontSize=18.sp);Text(description,color=LocalCinema.current.muted,fontSize=13.sp)}}

/** Restore only when the stable card is attached. A new user direction cancels a pending restore. */
@Composable fun workFocus(m:PtvModel,w:Work,row:String):Modifier{
    val requester=remember(w.key,row){FocusRequester()};var attached by remember{mutableStateOf(false)}
    LaunchedEffect(m.restoreFocus,attached){if(attached&&m.restoreFocus=="$row|${w.key}"){withFrameNanos{};if(m.restoreFocus=="$row|${w.key}")runCatching{requester.requestFocus()}}}
    return Modifier.focusRequester(requester).onGloballyPositioned{attached=true}.onFocusChanged{if(it.isFocused){m.focusRow=row;m.focusWork=w.key;if(m.restoreFocus=="$row|${w.key}")m.restoreFocus=""}}
}

