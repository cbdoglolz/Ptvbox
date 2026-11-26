package com.cbdog.ptvbox

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.*

@Composable private fun DetailArtwork040(url:String,label:String?,revision:Int,modifier:Modifier,scale:ContentScale=ContentScale.Crop,alpha:Float=1f){
    val context=LocalContext.current;val request=remember(url,revision){ImageRequest.Builder(context).data(url).memoryCacheKey("$url|detail-$revision").diskCacheKey("$url|detail-$revision").build()}
    AsyncImage(request,label,modifier,contentScale=scale,alpha=alpha)
}
@Composable private fun AutoFitSingleLineTitle(text:String,color:Color,modifier:Modifier=Modifier){
    val initial=remember(text){when{text.length<=10->36f;text.length<=16->30f;text.length<=24->24f;text.length<=34->19f;else->16f}}
    var fontSize by remember(text){mutableFloatStateOf(initial)}
    Text(
        text=text,
        color=color,
        fontSize=fontSize.sp,
        fontWeight=FontWeight.Bold,
        maxLines=1,
        softWrap=false,
        overflow=TextOverflow.Ellipsis,
        onTextLayout={result->if(result.hasVisualOverflow&&fontSize>12f)fontSize=(fontSize*0.88f).coerceAtLeast(12f)},
        modifier=modifier.fillMaxWidth()
    )
}
@Composable private fun DetailFavoriteAction(selected:Boolean,click:()->Unit){
    val c=LocalCinema.current
    InputButton(click,Modifier.selection051(selected),shape=ButtonDefaults.shape(RoundedCornerShape(8.dp)),colors=ptvButtonColors(c.raised,c.text),contentPadding=PaddingValues(14.dp,10.dp)){
        Text(if(selected)"★ 已收藏" else "☆ 收藏",color=if(selected)Color(0xffffc857) else c.text,fontSize=if(selected)16.sp else 14.sp)
    }
}
@Composable private fun DetailWatchStateAction(state:WatchState?,click:()->Unit){
    val c=LocalCinema.current;val color=state?.let{WatchVisuals056.colors(it).first()}?:c.text
    InputButton(click,shape=ButtonDefaults.shape(RoundedCornerShape(8.dp)),colors=ptvButtonColors(c.raised,c.text),contentPadding=PaddingValues(14.dp,10.dp)){
        Text(when(state){WatchState.COMPLETED->"✓ 已看";WatchState.WATCHING->"● 在看";WatchState.PLANNED->"♡ 想看";null->"观看状态"},color=color,fontSize=14.sp)
    }
}
@Composable internal fun CinemaDetail040(m:PtvModel){
    val fresh by m.details.work.collectAsState();val raw=fresh?.takeIf{it.key==m.detail?.key}?:m.detail?:return;val (w,art)=presented050(m,raw)
    val catalog by m.details.catalog.collectAsState();val related by m.details.related.collectAsState();val collection by m.details.collection.collectAsState();val versions by m.details.versions.collectAsState();val artwork by m.details.artwork.collectAsState();val refreshing by m.details.refreshing.collectAsState()
    val relatedBusy by m.details.relatedLoading.collectAsState();val relatedMore by m.details.relatedMore.collectAsState();val c=LocalCinema.current;val actionScope=rememberCoroutineScope();val state=rememberLazyGridState();val actionFocus=remember{FocusRequester()}
    var sourceExpanded by rememberSaveable(w.key){mutableStateOf(false)};var groupKey by rememberSaveable(w.key,w.season){mutableStateOf("")};var resolution by rememberSaveable(w.key){mutableStateOf(listOf<Int>())};var statusMenu by remember{mutableStateOf(false)}
    LaunchedEffect(w.key){withFrameNanos{};runCatching{actionFocus.requestFocus()}}
    val groups=catalog.groups.filter{m.siteEnabled(it.site)}.sortedWith(compareBy<CatalogGroup>{ResourceLabels040.bytes(ResourceLabels040.label(it)).takeIf{size->size>0}?:Long.MAX_VALUE}.thenBy{it.key})
    val heights=groups.map{ResourceLabels040.height(ResourceLabels040.label(it))}.filter{it>0}.distinct().sorted()
    val visible=groups.filter{resolution.isEmpty()||ResourceLabels040.height(ResourceLabels040.label(it)) in resolution}
    Box(Modifier.fillMaxSize()){
        DetailArtwork040(m.metadata.doubanHdImage(w.backdrop.ifBlank{w.poster}),null,artwork,Modifier.fillMaxSize().blur(7.dp),ContentScale.Crop,.78f)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.background.copy(alpha=.08f),c.background.copy(alpha=.42f),c.background.copy(alpha=.84f)))))
        LazyVerticalGrid(GridCells.Adaptive(196.dp),state=state,modifier=Modifier.fillMaxSize().pagedDown051(state,related.map{"related|${it.key}"},{m.focusWork.takeIf{m.focusRow=="related"}?.let{"related|$it"}},relatedBusy,relatedMore,m.details::moreSimilar),horizontalArrangement=Arrangement.spacedBy(13.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(12.dp,8.dp,12.dp,24.dp)){
            item(key="hero",span={GridItemSpan(maxLineSpan)}){Row(Modifier.fillMaxWidth().background(Color(0x5210111b),RoundedCornerShape(12.dp)).padding(22.dp),horizontalArrangement=Arrangement.spacedBy(26.dp),verticalAlignment=Alignment.CenterVertically){
                DetailArtwork040(w.poster,w.title,artwork,Modifier.width(178.dp).height(252.dp).clip(RoundedCornerShape(10.dp)),ContentScale.Fit)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    ProviderRatingRows(m,w)
                    if(art?.logo?.isNotBlank()==true)AsyncImage(art.logo,w.title,Modifier.widthIn(max=500.dp).height(92.dp),contentScale=ContentScale.Fit)else AutoFitSingleLineTitle(w.title,c.text)
                    if(w.overview.isNotBlank())Text(w.overview,color=c.text,fontSize=14.sp,maxLines=4,overflow=TextOverflow.Ellipsis)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){w.detailFacts().forEach{fact->PcChip(DetailFact056.display(fact),false,{m.filterSearch056(w.kind,fact)})}}
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){if(w.releaseLabel().isNotBlank())Text(w.releaseLabel(),color=PcGreen,fontSize=14.sp);if(w.isUpcoming())PendingBadge()}
                    val progress=m.user.watches.firstOrNull{it.work.key==w.key&&it.work.season==w.season}
                    Text(progress?.let{"继续观看 · ${it.episode} · ${clock(it.position)}"}?:when(m.user.states[w.key]){WatchState.COMPLETED->"✓ 已看";WatchState.WATCHING->"● 在看";WatchState.PLANNED->"♡ 想看";null->"尚未观看"},color=PcAccent,fontSize=13.sp)
                }
            }}
            item(key="hero-actions",span={GridItemSpan(maxLineSpan)}){FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                Action("▶ 播放 / 续播",{m.play(w)},Modifier.focusRequester(actionFocus));DetailFavoriteAction(w.key in m.user.favorites){m.toggleFavorite(w)};DetailWatchStateAction(m.user.states[w.key]){statusMenu=true}
                Action("搜索高清源",{sourceExpanded=true;m.details.refresh(true)});Action(if(refreshing)"刷新资料中…"else"刷新全部资料",{if(!refreshing){m.details.refreshAll();actionScope.launch{m.artworkQueue.invalidate(w)}}});Action("校正季集关联",{m.seasonCorrectionRequest++})
            }}
            if(w.kind=="tv")item(key="seasons",span={GridItemSpan(maxLineSpan)}){SeasonEpisodes040(m,w)}
            item(key="sources-title",span={GridItemSpan(maxLineSpan)}){SettingsCard("作品资料与播放线路",if(catalog.loading)"正在查找 ${catalog.checked}/${catalog.total} · ${groups.size}组"else"${groups.size}组已知线路 · 按容量从小到大，未知容量末尾"){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){PcChip(if(sourceExpanded)"收起线路 ▴"else"展开线路 ▾",sourceExpanded,{sourceExpanded=!sourceExpanded});Action("查找普通线路与选集",{sourceExpanded=true;m.details.refresh()})}
                if(sourceExpanded){if(w.sourceFacts().isNotBlank())Text(w.sourceFacts(),color=c.text,fontSize=13.sp);Text("角色资料按提供方返回展示，未返回角色名时不猜测。标签画质等待播放器实际确认。",color=c.muted,fontSize=11.sp)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        PcChip("全部清晰度",resolution.isEmpty(),{resolution=emptyList()});heights.forEach{h->InputButton({resolution=if(h in resolution)resolution-h else resolution+h},colors=ptvButtonColors(if(h in resolution)Color(0xff206e50)else c.raised,c.text)){Text("${h}P",fontSize=13.sp)}}
                    }
                    if(groups.isEmpty())Text(if(catalog.loading)"正在读取来源，请稍候…"else"尚无目录，可查找普通线路；进入详情不会自动播放。",color=PcAccent,fontSize=13.sp)
                }
            }}
            if(sourceExpanded){items(visible,key={it.key}){g->val text=ResourceLabels040.label(g);val size=ResourceLabels040.bytes(text);val height=ResourceLabels040.height(text)
                Column(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(9.dp)).border(1.dp,c.border,RoundedCornerShape(9.dp)).padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Text(g.site.name,color=c.text,fontSize=14.sp,maxLines=2);Text(g.road.name,color=c.muted,fontSize=12.sp,maxLines=2)
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){if(height>0)PcTag("${height}P · 标签");if(size>0)PcTag(ResourceLabels040.size(size));ResourceLabels040.tags(text).forEach{PcTag(it)}}
                    PcChip(if(groupKey==g.key)"收起选集"else"${g.road.episodes.size}集 · 选择",groupKey==g.key,{groupKey=if(groupKey==g.key)""else g.key})
                    if(w.kind=="movie")g.road.episodes.firstOrNull()?.let{ep->Action("▶ 播放",{m.playFromCatalog(w,g,ep)})}
                }
            };groups.firstOrNull{it.key==groupKey}?.let{g->item(key="episodes-${g.key}",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(9.dp)){PcSection("${g.site.name} · ${g.road.name}");LazyRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){itemsIndexed(g.road.episodes){_,ep->Action(ep.name,{m.playFromCatalog(w,g,ep)},Modifier.width(160.dp).height(64.dp),maxLines=3)}}}}}}
            if(w.facts.companies.isNotEmpty())item(key="companies",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){PcSection("出品与播出");LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(w.facts.companies,key={it.role+it.id}){co->Column(Modifier.width(144.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){InputButton({m.openCredit055(w,company=co)},Modifier.fillMaxWidth(),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(10.dp)),contentPadding=PaddingValues(8.dp)){if(co.logo.isNotBlank())DetailArtwork040(co.logo,co.name,artwork,Modifier.width(118.dp).height(52.dp),ContentScale.Fit)else Text(co.name,color=c.text,maxLines=2)};Text(co.name,color=c.text,fontSize=13.sp,maxLines=2);Text(co.role,color=c.muted,fontSize=11.sp)}}}}}
            if(w.people.isNotEmpty())item(key="people",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){PcSection("导演与演员 · 点击头像查看参与作品");LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp)){items(w.people,key={it.id.ifBlank{it.name}}){p->Column(Modifier.width(116.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){InputButton({m.openCredit055(w,person=p)},Modifier.fillMaxWidth(),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(10.dp)),contentPadding=PaddingValues(0.dp)){DetailArtwork040(p.avatar,p.name,artwork,Modifier.fillMaxWidth().height(150.dp).background(c.surface))};Text(p.name,color=c.text,fontSize=13.sp,maxLines=2);Text(p.role,color=c.muted,fontSize=11.sp,maxLines=3)}}}}}
            if(collection.isNotEmpty())item(key="collection-row",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){PcSection(w.facts.collectionName.ifBlank{"同系列作品"});LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(4.dp)){items(collection,key={it.key}){item->Box(Modifier.width(138.dp)){CinemaPoster(m,item,"collection")}}}}}
            if(versions.isNotEmpty())item(key="versions-row",span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){PcSection("相关版本 · 不同年份为不同作品，请核对");LazyRow(horizontalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(4.dp)){items(versions,key={it.key}){item->Box(Modifier.width(138.dp)){CinemaPoster(m,item,"versions")}}}}}
            if(related.isNotEmpty())item(key="related-row",span={GridItemSpan(maxLineSpan)}){Column(Modifier.fillMaxWidth().onPreviewKeyEvent{e->val last=related.lastOrNull();if(e.key==Key.DirectionDown&&e.type==KeyEventType.KeyDown&&e.nativeKeyEvent.repeatCount==0&&m.focusRow=="related"&&m.focusWork==last?.key&&!relatedBusy&&relatedMore){m.details.moreSimilar();true}else false},verticalArrangement=Arrangement.spacedBy(10.dp)){PcSection("相似推荐","一行五部 · 最后一行继续向下加载");related.chunked(5).forEach{row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){row.forEach{work->Box(Modifier.weight(1f)){CinemaPoster(m,work,"related")}};repeat(5-row.size){Spacer(Modifier.weight(1f))}}};MoreHint051(relatedBusy,relatedMore,m.details::moreSimilar)}}
        }
    }
    if(statusMenu)WatchPicker056("观看状态",m.user.states[w.key],{state->if(state==null)m.clearWatchState(w)else m.setWatchState(w,state);statusMenu=false},{statusMenu=false})
}

