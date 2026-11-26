package com.cbdog.ptvbox

import androidx.compose.foundation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.*
import coil3.compose.AsyncImage

data class CinemaNav(val label:String,val id:String,val icon:Int)
private val navItems=listOf(CinemaNav("首页","home",R.drawable.pc_home),CinemaNav("动画","anime",R.drawable.pc_anime),CinemaNav("直播与体育","live",R.drawable.pc_live),CinemaNav("搜索筛选","search",R.drawable.pc_search),CinemaNav("收藏","favorites",R.drawable.pc_keep),CinemaNav("历史","history",R.drawable.pc_history))
private fun navigate(m:PtvModel,id:String){when(id){"anime"->m.screen="anime";"search"->m.enterSearch055();"movie","tv"->{m.changeFeed(k=id,c="热门");m.screen="library"};"documentary","variety"->{m.changeFeed(k="tv",c=when(id){"anime"->"动画";"documentary"->"纪录片";else->"综艺"});m.screen="library"};"sites","settings"->m.utility(id);else->m.screen=id}}
private fun navSelected(m:PtvModel,id:String):Boolean {
 if(m.screen!="library")return m.screen==id
 val special=mapOf("动画" to "anime","纪录片" to "documentary","综艺" to "variety")[m.category]
 return if(special!=null)id==special else id==m.kind
}
@Composable private fun NavIcon(n:CinemaNav,size:Int=25){Box(Modifier.size(size.dp)){Image(painterResource(n.icon),null,Modifier.fillMaxSize());if(n.id=="anime")Text("ANI",color=PcCyan,fontSize=6.sp,modifier=Modifier.align(Alignment.Center).padding(top=4.dp))}}
@Composable private fun RailTile050(label:String,selected:Boolean,pinned:Boolean,click:()->Unit,content:@Composable ()->Unit){
 val c=LocalCinema.current;var focused by remember{mutableStateOf(false)};val density=androidx.compose.ui.platform.LocalDensity.current;val gap=with(density){8.dp.roundToPx()}
 Box{InputButton(click,Modifier.fillMaxWidth().heightIn(min=56.dp).selection051(selected).onFocusChanged{focused=it.hasFocus}.semantics{contentDescription=label;this.selected=selected},shape=ButtonDefaults.shape(RoundedCornerShape(8.dp)),colors=ptvButtonColors(if(selected)c.raised else Color.Transparent,c.text),scale=ButtonDefaults.scale(1f,if(pinned)1.015f else 1f,.99f),contentPadding=PaddingValues(11.dp)){content();if(pinned){Spacer(Modifier.width(12.dp));Text(label,fontSize=14.sp,maxLines=2)}}
  if(focused&&!pinned)androidx.compose.ui.window.Popup(popupPositionProvider=remember(gap){object:androidx.compose.ui.window.PopupPositionProvider{override fun calculatePosition(anchorBounds:androidx.compose.ui.unit.IntRect,windowSize:IntSize,layoutDirection:LayoutDirection,popupContentSize:IntSize)=IntOffset((anchorBounds.right+gap).coerceAtMost(windowSize.width-popupContentSize.width), (anchorBounds.center.y-popupContentSize.height/2).coerceIn(0,(windowSize.height-popupContentSize.height).coerceAtLeast(0))) }},properties=androidx.compose.ui.window.PopupProperties(focusable=false)){Text(label,color=c.text,fontSize=15.sp,modifier=Modifier.background(c.raised,RoundedCornerShape(8.dp)).border(1.dp,PcAccent,RoundedCornerShape(8.dp)).padding(14.dp,9.dp).semantics{contentDescription="导航提示：$label"})}
 }
}
@Composable private fun NowPlayingTriangle(playing:Boolean){
 val transition=rememberInfiniteTransition(label="now-playing")
 val flow by transition.animateFloat(initialValue=0f,targetValue=1f,animationSpec=infiniteRepeatable(tween(1800,easing=LinearEasing),RepeatMode.Restart),label="now-playing-flow")
 val pulse by transition.animateFloat(initialValue=0.94f,targetValue=1.10f,animationSpec=infiniteRepeatable(tween(900,easing=LinearEasing),RepeatMode.Reverse),label="now-playing-pulse")
 val scale=if(playing)pulse else 1f
 Canvas(Modifier.size(28.dp).graphicsLayer{scaleX=scale;scaleY=scale}.semantics{contentDescription=if(playing)"正在播放水流渐变图标"else"已暂停播放图标"}){
  val w=size.width;val h=size.height
  val colors=if(playing)listOf(Color(0xff29e6ff),Color(0xff5b7cff),Color(0xffc86bff),Color(0xffff62b0),Color(0xff46f0b0),Color(0xff29e6ff))else listOf(Color(0xff8e8aa8),Color(0xff6b6782))
  val span=w*1.9f
  val offset=if(playing)flow*span else 0f
  val flowBrush=Brush.linearGradient(colors=colors,start=androidx.compose.ui.geometry.Offset(-span+offset,h+span-offset),end=androidx.compose.ui.geometry.Offset(offset,h-offset),tileMode=TileMode.Repeated)
  if(playing)drawCircle(brush=flowBrush,radius=w*.48f,alpha=.22f)
  val path=Path().apply{moveTo(w*.26f,h*.16f);lineTo(w*.86f,h*.5f);lineTo(w*.26f,h*.84f);close()}
  drawPath(path,brush=flowBrush)
  if(playing)drawCircle(brush=flowBrush,radius=w*.47f,style=androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
 }
}
@Composable internal fun NavigationRail(m:PtvModel){val c=LocalCinema.current;val pinned=remember(m.preferencesRevision){m.config.flag("drawer_pinned")}
 Column(Modifier.width(if(pinned)230.dp else 72.dp).fillMaxHeight().background(c.surface).padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)){
  Column(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)){
   Box(Modifier.size(if(pinned)42.dp else 46.dp).border(1.5.dp,Brush.horizontalGradient(listOf(Color(0xff55d8ff),Color(0xffb75cff))),RoundedCornerShape(10.dp)),contentAlignment=Alignment.Center){
    Image(painterResource(R.drawable.brand),"Ptvbox",Modifier.size(if(pinned)32.dp else 36.dp))
   }
   Text("Ptvbox",color=c.text,fontSize=if(pinned)15.sp else 10.sp,maxLines=1)
   Text("v${BuildConfig.VERSION_NAME}",color=c.muted,fontSize=if(pinned)11.sp else 8.sp,maxLines=1)
  }
  Spacer(Modifier.height(8.dp))
  LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(top=10.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
   if(m.current!=null)item(key="now-playing"){RailTile050("返回播放",false,pinned,{m.showPlayer()}){NowPlayingTriangle(m.playing)}}
   items(navItems,key={it.id}){n->RailTile050(n.label,navSelected(m,n.id),pinned,{navigate(m,n.id)}){NavIcon(n)}}
  }
  RailTile050("站点",m.screen=="sites",pinned,{m.utility("sites")}){Image(painterResource(R.drawable.pc_sites),null,Modifier.size(24.dp))}
  RailTile050("设置",m.screen=="settings",pinned,{m.utility("settings")}){Image(painterResource(R.drawable.pc_settings),null,Modifier.size(26.dp))}
 }
}
@Composable internal fun NavigationDrawer(m:PtvModel,close:()->Unit){val c=LocalCinema.current;val first=remember{FocusRequester()};LaunchedEffect(Unit){withFrameNanos{};first.requestFocus()}
 Dialog(close,properties=DialogProperties(usePlatformDefaultWidth=false)){Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.3f))){Column(Modifier.width(260.dp).fillMaxHeight().background(c.surface).padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
  Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){PcBrand();InputButton(close,Modifier.size(36.dp).focusRequester(first).semantics{contentDescription="收起导航"},contentPadding=PaddingValues(4.dp)){Text("‹",fontSize=24.sp)}}
   m.current?.let{w->Column(Modifier.fillMaxWidth().background(c.raised,RoundedCornerShape(10.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(w.title,color=c.text,fontSize=15.sp,maxLines=2,overflow=TextOverflow.Ellipsis);Text("S${w.season} · ${m.currentEpisode}",color=c.muted,fontSize=12.sp);Text("${clock(m.position)} / ${clock(m.duration)}",color=PcAccent,fontSize=12.sp)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){InputButton({m.showPlayer();close()},Modifier.size(48.dp).semantics{contentDescription="返回播放"},contentPadding=PaddingValues(8.dp)){Text("▶")};InputButton({if(m.playing)m.player.pause()else m.player.play()},Modifier.size(48.dp).semantics{contentDescription=if(m.playing)"暂停"else"播放"},contentPadding=PaddingValues(8.dp)){Text(if(m.playing)"Ⅱ"else"▶")};InputButton({m.stop();close()},Modifier.size(48.dp).semantics{contentDescription="停止"},contentPadding=PaddingValues(8.dp)){Text("■")}}
   }}
  LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){items(navItems,key={it.id}){n->InputButton({navigate(m,n.id);close()},Modifier.fillMaxWidth(),shape=ButtonDefaults.shape(shape=RoundedCornerShape(8.dp)),colors=ptvButtonColors(if(navSelected(m,n.id))c.raised else Color.Transparent,c.text),contentPadding=PaddingValues(12.dp,10.dp)){NavIcon(n);Spacer(Modifier.width(14.dp));Text(n.label,fontSize=15.sp)}}}
  Action("设置",{m.utility("settings");close()},Modifier.fillMaxWidth());Text("Ptvbox ${BuildConfig.VERSION_NAME}",color=c.muted,fontSize=11.sp)
 }}}
}
@Composable internal fun DiscoveryTabs(m:PtvModel){Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){listOf("豆瓣" to "douban","Netflix标签" to "netflix","TMDB目录" to "tmdb").forEach{(n,p)->InputButton({m.changeFeed(p=p)},shape=ButtonDefaults.shape(shape=RoundedCornerShape(8.dp)),colors=ptvButtonColors(if(m.provider==p)Color(0xff5b3f86)else Color.Transparent,LocalCinema.current.text),contentPadding=PaddingValues(10.dp)){Image(painterResource(when(p){"douban"->R.drawable.pc_douban;"netflix"->R.drawable.pc_netflix_wordmark;else->R.drawable.pc_tmdb}),n,Modifier.width(if(p=="netflix")100.dp else 85.dp).height(25.dp))}}};Text(if(m.provider=="netflix")"豆瓣 Netflix 标签专题 · 非Netflix官方榜单"else"按${if(m.provider=="tmdb")"TMDB"else"豆瓣"}推荐选片，在详情页查找可播放来源",color=LocalCinema.current.muted,fontSize=11.sp)}
@Composable private fun HomeCategories(m:PtvModel){FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){PcChip(if(m.includeUpcoming)"✓ 显示未上映"else"显示未上映",m.includeUpcoming,m::toggleUpcoming);buildList{add(Triple("热门剧集","tv","热门"));add(Triple("热门电影","movie","热门"));add(Triple("最新电影","movie","最新"));add(Triple("最新剧集","tv","最新"));add(Triple("动画作品","tv","动画"));if(m.provider!="netflix")add(Triple("TOP250","movie","TOP250"))}.forEach{(n,k,c)->PcChip(n,m.kind==k&&m.category==c,{m.changeFeed(k=k,c=c)})}}}
@Composable internal fun CategoryTabs(m:PtvModel){FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){buildList{addAll(listOf("热门","最新","高分","动画","纪录片","综艺"));if(m.provider!="netflix")add("TOP250")}.forEach{c->PcChip(c,m.category==c,{m.changeFeed(k=if(c=="TOP250")"movie"else m.kind,c=c)})}}}

@Composable internal fun CinemaHome(m:PtvModel){val c=LocalCinema.current;val state=rememberLazyGridState();val rows=m.feed.filter{m.includeUpcoming||!it.isUpcoming()};var heroIndex by rememberSaveable(m.provider,m.kind,m.category){mutableIntStateOf(0)};val hero=rows.getOrNull(heroIndex.coerceAtMost((rows.size-1).coerceAtLeast(0)))
 LazyVerticalGrid(GridCells.Adaptive(138.dp),state=state,modifier=Modifier.pagedDown051(state,rows.map{it.key},{m.focusWork.takeIf{m.focusRow=="home"}},m.busy,m.feedHasMore){m.loadFeed(true)},horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(8.dp)){
  item(key="providers",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){DiscoveryTabs(m);HomeCategories(m)}}
  hero?.let{w->item(key="hero",span={GridItemSpan(maxLineSpan)}){Hero053(m,w,heroIndex,rows.take(5).size,state.layoutInfo.visibleItemsInfo.any{it.key=="hero"}){heroIndex=it}}}
  if(m.user.continues.isNotEmpty())item(key="continue",span={GridItemSpan(maxLineSpan)}){ContinueRow(m)}
  item(key="heading",span={GridItemSpan(maxLineSpan)}){PcSection("${if(m.kind=="movie")"电影"else"剧集"} · ${m.category}",if(m.busy)"正在更新海报…"else"${rows.size}部作品 · 选择海报查看资料与片源")}
  items(rows,key={it.key}){w->CinemaPoster(m,w,"home")}
  item(key="more",span={GridItemSpan(maxLineSpan)}){if(rows.isEmpty())PcEmpty("正在准备推荐","网络不可用时保留已缓存资料，可切换来源或搜索作品");MoreHint051(m.busy,m.feedHasMore){m.loadFeed(true)}}
 }
}
@Composable internal fun CinemaPoster(m:PtvModel,original:Work,row:String){val (w,art)=presented050(m,original);val c=LocalCinema.current
 var focused by remember{mutableStateOf(false)};val scale by animateFloatAsState(if(focused)1.045f else 1f,label="poster-scale")
 Column(Modifier.widthIn(min=110.dp,max=180.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  InputButton({m.focusRow=row;m.open(w)},Modifier.fillMaxWidth().graphicsLayer{scaleX=scale;scaleY=scale}.border(if(focused)1.5.dp else 1.dp,if(focused)Brush.horizontalGradient(listOf(Color(0xff58d8ff),Color(0xffb75cff)))else Brush.horizontalGradient(listOf(Color.Transparent,Color.Transparent)),RoundedCornerShape(9.dp)).onFocusChanged{focused=it.isFocused}.then(workFocus(m,w,row)),shape=ButtonDefaults.shape(shape=RoundedCornerShape(9.dp)),scale=ButtonDefaults.scale(1f,1f,1f),contentPadding=PaddingValues(2.dp)){
   Box(Modifier.fillMaxWidth().aspectRatio(158f/230f).background(c.surface)){
    AsyncImage(w.poster,w.title,Modifier.fillMaxSize(),contentScale=ContentScale.Fit)
    if(w.key in m.user.favorites)Text("★",color=Color.White,fontSize=18.sp,modifier=Modifier.align(Alignment.TopEnd).padding(6.dp).background(Color(0x99515066),RoundedCornerShape(5.dp)).padding(3.dp))
    m.user.states[w.key]?.let{s->Text(when(s){WatchState.PLANNED->"想看";WatchState.WATCHING->"在看";WatchState.COMPLETED->"✓ 已看"},color=PcAccent,fontSize=10.sp,modifier=Modifier.align(Alignment.TopStart).padding(5.dp).background(Color(0xdd151322),RoundedCornerShape(4.dp)).padding(4.dp))}
    if(w.isUpcoming())Box(Modifier.align(Alignment.BottomStart).padding(5.dp)){PendingBadge()}
   }
  }
  PcRatings(w,links=m.linkedRatings[w.ratingViewKey],tmdbConfigured=m.tmdbConfigured,primary=remember(m.preferencesRevision){m.config.text("primary_rating","douban")},secondary=remember(m.preferencesRevision){m.config.flag("secondary_rating",true)});Text(w.title,color=c.text,fontSize=14.sp,maxLines=2,overflow=TextOverflow.Ellipsis);Text(w.year,color=c.muted,fontSize=11.sp)
 }
}
@Composable internal fun ContinueRow(m:PtvModel){val c=LocalCinema.current;Column(verticalArrangement=Arrangement.spacedBy(11.dp)){
 PcSection("继续观看","从上次选择的作品和单集继续")
 LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(5.dp)){items(m.user.continues.take(30),key={it.work.key}){w->Column(Modifier.width(248.dp).background(c.surface,RoundedCornerShape(10.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  InputButton({m.focusRow="continue";m.open(w.work)},Modifier.fillMaxWidth().then(workFocus(m,w.work,"continue")),shape=ButtonDefaults.shape(shape=RoundedCornerShape(8.dp)),contentPadding=PaddingValues(0.dp)){Box(Modifier.fillMaxWidth().aspectRatio(16f/9f)){
   AsyncImage(w.work.backdrop.ifBlank{w.work.poster},w.work.title,Modifier.fillMaxSize(),contentScale=ContentScale.Crop);Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent,Color(0xdc10111d)))));Text(w.work.title,color=Color.White,maxLines=2,fontSize=16.sp,modifier=Modifier.align(Alignment.BottomStart).padding(10.dp))
  }};PcProgress(w.progress);Text("S${w.work.season} · ${w.episode} · ${clock(w.position)}",color=c.text,fontSize=12.sp);Text(if(w.duration>0)"剩余 ${clock((w.duration-w.position).coerceAtLeast(0))}"else"总时长待确认",color=c.muted,fontSize=11.sp)
  Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){PcChip("续播",false,{m.play(w.work,w.episode)});PcChip("移除",false,{m.hideContinue(w.work)});PcChip("已看",false,{m.setWatchState(w.work,WatchState.COMPLETED)})}
 }}}
}}

@Composable internal fun CinemaDetail(m:PtvModel){val fresh by m.details.work.collectAsState();val w=fresh?.takeIf{it.key==m.detail?.key}?:m.detail?:return;val catalog by m.details.catalog.collectAsState();val related by m.details.related.collectAsState();val c=LocalCinema.current;val heroFocus=remember(w.key){FocusRequester()};LaunchedEffect(w.key){withFrameNanos{};runCatching{heroFocus.requestFocus()}};var groupKey by rememberSaveable(w.key,w.season){mutableStateOf("")};var statusMenu by remember{mutableStateOf(false)};val state=rememberLazyGridState();LaunchedEffect(w.key,w.season){if(m.details.work.value?.let{it.key==w.key&&it.season==w.season}!=true)m.details.load(w)}
 LazyVerticalGrid(GridCells.Adaptive(200.dp),state=state,verticalArrangement=Arrangement.spacedBy(15.dp),horizontalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(8.dp)){
  item(key="hero",span={GridItemSpan(maxLineSpan)}){Box(Modifier.fillMaxWidth().heightIn(min=330.dp).clip(RoundedCornerShape(12.dp)).background(c.surface).focusRequester(heroFocus).focusable()){
   AsyncImage(w.backdrop.ifBlank{w.poster},null,Modifier.matchParentSize(),contentScale=ContentScale.Crop,alpha=.38f);Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(c.background.copy(alpha=.96f),c.background.copy(alpha=.76f),c.background.copy(alpha=.35f)))))
   Row(Modifier.fillMaxSize().padding(22.dp),horizontalArrangement=Arrangement.spacedBy(24.dp)){
     Column(Modifier.width(155.dp)){AsyncImage(w.poster,w.title,Modifier.fillMaxWidth().height(232.dp).clip(RoundedCornerShape(9.dp)),contentScale=ContentScale.Fit)}
     Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(11.dp)){
      Text(w.title,color=c.text,fontSize=32.sp,fontWeight=FontWeight.Bold,maxLines=2,overflow=TextOverflow.Ellipsis)
      if(w.overview.isNotBlank())Text(w.overview,color=c.text.copy(alpha=.86f),fontSize=13.sp,maxLines=3,overflow=TextOverflow.Ellipsis)
      FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){w.detailFacts().forEach{fact->val shown=DetailFact056.display(fact);PcChip(shown,false,{m.filterSearch056(w.kind,fact)})}}
      ProviderRatingRows(m,w)
      if(w.releaseLabel().isNotBlank())Text(w.releaseLabel(),color=PcGreen,fontSize=14.sp);if(w.isUpcoming())PendingBadge()
      m.user.states[w.key]?.let{s->Text(when(s){WatchState.COMPLETED->"✓ 已看";WatchState.WATCHING->"● 在看";WatchState.PLANNED->"♡ 想看"},color=s.let{WatchVisuals056.colors(it).first()},fontSize=13.sp,fontWeight=FontWeight.SemiBold)}
      m.user.watches.firstOrNull{it.work.key==w.key&&it.work.season==w.season}?.let{Text("继续观看 · ${it.episode} · ${clock(it.position)}",color=PcAccent,fontSize=13.sp)}
     }
    }
   }
   }
  item(key="hero-actions",span={GridItemSpan(maxLineSpan)}){FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){Action("▶ 播放 / 续播",{m.play(w)});Action(if(w.key in m.user.favorites)"★ 已收藏"else"☆ 收藏",{m.toggleFavorite(w)});Action("观看状态",{statusMenu=true});Action("搜索高清源",{m.changePreference("quality");m.details.refresh()});Action("刷新全部资料",{m.details.load(w);m.details.refresh()})}}
  if(w.kind=="tv")item(key="episodes",span={GridItemSpan(maxLineSpan)}){SeasonEpisodes(m,w)}
  item(key="source-heading",span={GridItemSpan(maxLineSpan)}){PcSection("作品资料与播放线路",if(catalog.loading)"正在读取目录 ${catalog.checked}/${catalog.total} · 已到${catalog.groups.size}组"else"${catalog.groups.size}组目录 · ${catalog.failed}站暂不可用"){PcChip("刷新目录",false,m.details::refresh)}}
  if(catalog.groups.isEmpty())item(span={GridItemSpan(maxLineSpan)}){PcEmpty(if(catalog.loading)"正在读取片源目录"else"暂未找到可靠目录","无需先播放，也不强制要求TMDB密钥。可刷新或通过播放按钮继续搜源。")}
  items(catalog.groups,key={it.key}){g->Column(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(10.dp)).border(1.dp,if(groupKey==g.key)PcAccent else c.border,RoundedCornerShape(10.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(g.site.name,color=c.text,fontSize=14.sp,maxLines=2);Text("${g.road.name} · ${g.road.episodes.size}集",color=c.muted,fontSize=12.sp);PcChip(if(groupKey==g.key)"收起选集"else"查看选集",groupKey==g.key,{groupKey=if(groupKey==g.key)""else g.key});if(w.kind=="movie")g.road.episodes.firstOrNull()?.let{ep->Action("播放此线路",{m.playFromCatalog(w,g,ep)})}
  }}
  catalog.groups.firstOrNull{it.key==groupKey}?.let{g->item(key="source-episodes",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("${g.site.name} · ${g.road.name}",color=c.text);LazyVerticalGrid(GridCells.Adaptive(100.dp),Modifier.fillMaxWidth().height((((g.road.episodes.size+4)/5)*48).coerceIn(50,220).dp),horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){itemsIndexed(g.road.episodes){_,ep->PcChip(ep.name,false,{m.playFromCatalog(w,g,ep)})}}}}}
  if(w.overview.isNotBlank())item(key="overview",span={GridItemSpan(maxLineSpan)}){Column(Modifier.background(c.surface,RoundedCornerShape(10.dp)).padding(18.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){PcSection("剧情简介");Text(w.overview,color=c.text,fontSize=14.sp)}}
  if(w.people.isNotEmpty())item(key="people",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){PcSection("导演与演员");LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(w.people,key={it.id.ifBlank{it.name}}){p->Column(Modifier.width(94.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){AsyncImage(p.avatar,p.name,Modifier.fillMaxWidth().height(126.dp).clip(RoundedCornerShape(8.dp)).background(c.surface),contentScale=ContentScale.Crop);Text(p.name,color=c.text,fontSize=12.sp,maxLines=2);Text(p.role,color=c.muted,fontSize=11.sp,maxLines=2)}}}}}
  if(related.isNotEmpty())item(key="related-row",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(10.dp)){PcSection("相似推荐","一行五部");Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){related.take(5).forEach{w2->Box(Modifier.weight(1f)){CinemaPoster(m,w2,"related")}}}}}
 }
 if(statusMenu)Dialog({statusMenu=false}){Column(Modifier.background(c.surface,RoundedCornerShape(12.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){PcSection("观看状态");listOf("想看" to WatchState.PLANNED,"在看" to WatchState.WATCHING,"已看" to WatchState.COMPLETED).forEach{(n,s)->Action(n,{m.setWatchState(w,s);statusMenu=false},Modifier.fillMaxWidth())};Action("清除状态",{m.clearWatchState(w);statusMenu=false})}}
}


