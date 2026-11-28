package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text
import kotlinx.coroutines.*
import org.json.*
import java.time.*

internal data class AnimeEntry056(val work:Work,val weekday:Int=0,val time:String="",val popularity:Double=0.0){
 fun json()=JSONObject().put("work",work.json()).put("weekday",weekday).put("time",time).put("popularity",popularity)
}
internal object AnimeSeason056 {
 val seasons=listOf("冬季","春季","夏季","秋季")
 fun range(year:Int,quarter:Int):Pair<LocalDate,LocalDate>{val from=LocalDate.of(year,quarter.coerceIn(0,3)*3+1,1);return from to from.plusMonths(3)}
 fun parse(raw:String):Pair<List<AnimeEntry056>,Boolean>{val page=JSONObject(raw).getJSONObject("data").getJSONObject("Page");return page.getJSONArray("media").objects().mapNotNull{j->val t=j.getJSONObject("title");val native=t.optString("native").ifBlank{t.optString("romaji")};if(native.isBlank())return@mapNotNull null
  val d=j.getJSONObject("startDate");val date=runCatching{LocalDate.of(d.getInt("year"),d.getInt("month"),d.optInt("day").coerceAtLeast(1)).toString()}.getOrDefault("")
  val times=j.optJSONObject("airingSchedule")?.optJSONArray("nodes")?.objects().orEmpty().mapNotNull{n->n.optLong("airingAt").takeIf{it>0}?.let{Instant.ofEpochSecond(it).atOffset(ZoneOffset.ofHours(8))}}
  val weekly=times.groupBy{it.dayOfWeek.value}.maxByOrNull{it.value.size};val clock=weekly?.value?.groupBy{"%02d:%02d".format(it.hour,it.minute)}?.maxByOrNull{it.value.size}?.key.orEmpty()
   val aliases=(listOf(t.optString("native"),t.optString("romaji"),t.optString("english"))+j.arrayStrings("synonyms")).filter{it.isNotBlank()&&it!="null"}.distinct()
   val chineseAlias=aliases.firstOrNull{InfoChinese056.isLikelyChinese(it)}
   val displayTitle=chineseAlias?:native
   AnimeEntry056(Work(j.optString("id"),displayTitle,if(j.optString("format")=="MOVIE")"movie"else"tv",d.optString("year"),j.optJSONObject("coverImage")?.optString("large").orEmpty(),provider="anilist",original=native,aliases=aliases,genres=listOf("animation"),season=AnimeMapping103.titleSeason(listOf(native)+aliases),episodeCount=j.optInt("episodes",0),releaseStatus=j.optString("status"),backdrop=j.optString("bannerImage").takeUnless{it=="null"}.orEmpty(),releaseDate=date),weekly?.key?:0,clock,j.optDouble("popularity",0.0))
 } to page.getJSONObject("pageInfo").optBoolean("hasNextPage")}
 fun supplement(rows:List<AnimeEntry056>,raw:String):List<AnimeEntry056>{val bg=JSONObject(raw).optJSONArray("data")?.objects().orEmpty();return rows.map{e->
  fun infoNames(b:JSONObject)=b.optJSONArray("infobox")?.objects().orEmpty().filter{it.optString("key") in listOf("中文名","别名","英文名","罗马字","简体中文名","繁体中文名")}.flatMap{row->val value=row.opt("value");when(value){is String->listOf(value);is JSONArray->(0 until value.length()).map{value.optJSONObject(it)?.optString("v")?:value.optString(it)};else->emptyList()}}.filter{it.isNotBlank()}
  val sourceForms=(e.work.aliases+listOf(e.work.title,e.work.original)).map{Identity.normalize(it)}.filter{it.isNotBlank()}.toSet()
  val ranked=bg.filter{it.optString("date").startsWith(e.work.year+"-")}.map{b->val candidateForms=(listOf(b.optString("name"),b.optString("name_cn"))+infoNames(b)).map{Identity.normalize(it)}.filter{it.isNotBlank()}.toSet()
   val sourceBases=(e.work.aliases+listOf(e.work.title,e.work.original)).map{Identity.base(it)}.filter{it.isNotBlank()}.toSet()
   val score=candidateForms.maxOfOrNull{name->when{name in sourceForms->120;Identity.base(name) in sourceBases->110;sourceForms.any{it.length>=4&&name.contains(it)}->80;else->0}}?:0
   b to score}.filter{it.second>=55}.sortedByDescending{it.second}
  val best=ranked.firstOrNull();val margin=if(best!=null)best.second-(ranked.getOrNull(1)?.second?:0)else 0
  if(best==null||(margin<10&&best.second<110))e else{
  val b=best.first;val infoAliases=infoNames(b)
  val tags=b.optJSONArray("tags")?.objects().orEmpty().map{it.optString("name")}.filter{it.isNotBlank()}.take(12)
  val rating=b.optJSONObject("rating");val score=rating?.optDouble("score",0.0)?.takeIf{it>0}?.let{String.format(java.util.Locale.US,"%.1f",it)}?:"-"
  val poster=b.optJSONObject("images")?.optString("large").orEmpty()
  val chinese=b.optString("name_cn").ifBlank{infoAliases.firstOrNull{InfoChinese056.hasHan(it)}?:e.work.title}
  e.copy(work=e.work.copy(title=chinese,overview=b.optString("summary",e.work.overview),aliases=(e.work.aliases+infoAliases+b.optString("name_cn")+b.optString("name")).filter{it.isNotBlank()}.distinct(),genres=(e.work.genres+tags).distinct(),poster=poster.ifBlank{e.work.poster},rating=score,votes=rating?.optInt("total",0)?:0,releaseDate=b.optString("date",e.work.releaseDate),facts=IdentityBridge060.bangumiFacts(e.work,b.optString("id"),b.optJSONArray("infobox"))))
 }}}
 suspend fun load(year:Int,quarter:Int,onRows:suspend(List<AnimeEntry056>)->Unit={}):List<AnimeEntry056>{val (from,to)=range(year,quarter);val rows=mutableListOf<AnimeEntry056>();val query="query(\$page:Int,\$from:FuzzyDateInt,\$to:FuzzyDateInt){Page(page:\$page,perPage:50){pageInfo{hasNextPage}media(type:ANIME,startDate_greater:\$from,startDate_lesser:\$to,isAdult:false,sort:POPULARITY_DESC){id title{native romaji english}synonyms episodes status format startDate{year month day}coverImage{large}bannerImage airingSchedule(perPage:50){nodes{airingAt episode}}}}}"
  for(page in 1..12){val body=JSONObject().put("query",query).put("variables",JSONObject().put("page",page).put("from",from.minusDays(1).toString().replace("-","").toInt()).put("to",to.toString().replace("-","").toInt()));val result=parse(Net.postJson("https://graphql.anilist.co",body.toString()));rows+=result.first;onRows(rows.distinctBy{it.work.key});if(!result.second)break}
  var result=rows.distinctBy{it.work.key}
  // Same exact-original-title + year enrichment as PCBOX. A failed supplement never removes the catalogue.
  try{for(page in 0..9){val body=JSONObject().put("keyword","").put("sort","heat").put("filter",JSONObject().put("type",JSONArray(listOf(2))).put("air_date",JSONArray(listOf(">=$from","<$to"))));val raw=Net.postJson("https://api.bgm.tv/v0/search/subjects?limit=50&offset=${page*50}",body.toString());result=supplement(result,raw);onRows(result);if((JSONObject(raw).optJSONArray("data")?.length()?:0)<50)break}}catch(e:CancellationException){throw e}catch(_:Exception){}
  return result
 }
 suspend fun search(value:String):List<AnimeEntry056>{val query="query(\$search:String){Page(page:1,perPage:50){pageInfo{hasNextPage}media(type:ANIME,search:\$search,isAdult:false,sort:POPULARITY_DESC){id title{native romaji english}synonyms episodes status format startDate{year month day}coverImage{large}bannerImage airingSchedule(perPage:50){nodes{airingAt episode}}}}}";val body=JSONObject().put("query",query).put("variables",JSONObject().put("search",value));return parse(Net.postJson("https://graphql.anilist.co",body.toString())).first}
}

internal object InfoChinese056{
 fun hasHan(value:String)=value.any{Character.UnicodeScript.of(it.code)==Character.UnicodeScript.HAN}
 fun isLikelyChinese(value:String)=hasHan(value)&&value.none{it in '\u3040'..'\u30ff'}
}

@Composable private fun CompactSchedChip(label:String,selected:Boolean=false,click:()->Unit){
 val c=LocalCinema.current
 InputButton(click,Modifier.border(1.dp,if(selected)PcAccent.copy(alpha=.7f)else c.border.copy(alpha=.8f),RoundedCornerShape(6.dp)).selection051(selected),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(6.dp)),focusRadius=6.dp,colors=ptvButtonColors(if(selected)Color(0xff4d3a77)else c.raised.copy(alpha=.75f),if(selected)Color.White else c.text.copy(alpha=.88f)),contentPadding=PaddingValues(horizontal=9.dp,vertical=5.dp)){Text(label,fontSize=11.sp,maxLines=1)}
}
@Composable private fun YearStepCircle056(label:String,click:()->Unit){
    val c=LocalCinema.current
    InputButton(click,Modifier.size(26.dp).border(1.dp,PtvFocusBorder,CircleShape),shape=androidx.tv.material3.ButtonDefaults.shape(CircleShape),focusRadius=13.dp,colors=ptvButtonColors(Color.Transparent,c.text),contentPadding=PaddingValues(0.dp)){
        Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){Text(label,color=c.text,fontSize=14.sp)}
    }
}

@Composable internal fun AnimePage056(m:PtvModel){
 val now=remember{LocalDate.now()}
 var tab by rememberSaveable{mutableStateOf("hot")}
 var year by rememberSaveable{mutableIntStateOf(now.year)}
 var quarter by rememberSaveable{mutableIntStateOf((now.monthValue-1)/3)}
 var weekday by rememberSaveable{mutableIntStateOf(0)}
 var refresh by remember{mutableIntStateOf(0)}
 var rows by remember{mutableStateOf<List<AnimeEntry056>>(emptyList())}
 var busy by remember{mutableStateOf(true)}
 var status by remember{mutableStateOf("")}
 var doubanRows by remember{mutableStateOf<List<Work>>(emptyList())}
 var doubanPage by remember{mutableIntStateOf(0)}
 var doubanMore by remember{mutableStateOf(true)}
 var doubanBusy by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 val context=LocalContext.current
 val c=LocalCinema.current
 val gridState=rememberLazyGridState()
 val animeSources=remember(m.availableSites){m.availableSites.filter{it.group=="动画"}}
 fun loadDoubanTab(reset:Boolean){
  if(doubanBusy||(!reset&&!doubanMore))return
  val nextPage=if(reset)1 else doubanPage+1
  doubanBusy=true
  scope.launch{
   try{
    val kind=if(tab=="movie")"movie"else"tv"
    val cat=when(tab){"top"->"高分";"movie"->"动画";else->"动画"}
    val fetched=withContext(Dispatchers.IO){
     if(tab=="top")m.metadata.feed("douban","tv","筛选:动画",nextPage,m.includeUpcoming).sortedByDescending{it.rating.toDoubleOrNull()?:0.0}
     else m.metadata.feed("douban",kind,cat,nextPage,m.includeUpcoming)
    }
    doubanMore=fetched.any{r->reset||doubanRows.none{it.key==r.key}}
    doubanRows=(if(reset)fetched else doubanRows+fetched).distinctBy{it.key}
    doubanPage=nextPage
   }catch(e:CancellationException){throw e}catch(_:Exception){
    if(reset&&doubanRows.isEmpty())status="豆瓣动画目录暂不可用，可切换近期热门番剧或时间表"
   }finally{doubanBusy=false}
  }
 }
 LaunchedEffect(tab){
  if(tab in listOf("tv","movie","top")){doubanRows=emptyList();doubanPage=0;doubanMore=true;loadDoubanTab(true)}
 }
 LaunchedEffect(year,quarter,refresh){
  rows=emptyList();busy=true;status="正在读取AniList季度目录，再补充Bangumi中文名与海报…"
  val file=java.io.File(context.cacheDir,"anime-season056-$year-$quarter.json")
  try{
   val cached=withContext(Dispatchers.IO){runCatching{JSONArray(file.readText()).objects().map{AnimeEntry056(Work.from(it.getJSONObject("work")),it.optInt("weekday"),it.optString("time"),it.optDouble("popularity",0.0))}}.getOrDefault(emptyList())}
   rows=cached
   if(cached.isNotEmpty()&&refresh==0){CacheBudget101.touch(file);status="已缓存季度目录（空间上限LRU）· 北京时间"}
   else{
    val fresh=withContext(Dispatchers.IO){AnimeSeason056.load(year,quarter){partial->withContext(Dispatchers.Main){rows=partial}}}
    rows=fresh
    withContext(Dispatchers.IO){file.writeText(JSONArray(fresh.map{it.json()}).toString());CacheBudget101.trimFiles(context.cacheDir.listFiles()?.filter{it.name.startsWith("anime-season056-")}.orEmpty());fresh.forEach{m.db.cache(it.work)}}
    status="AniList主目录 · Bangumi中文名/海报补充 · 北京时间"
   }
  }catch(e:CancellationException){throw e}catch(_:Exception){status=if(rows.isEmpty())"目录暂不可用，可刷新重试"else"目录暂不可用，保留本地缓存"}finally{busy=false}
 }
 val scheduleRows=rows.filter{weekday==0||it.weekday==weekday}.sortedWith(compareBy<AnimeEntry056>{it.time.ifBlank{"99:99"}}.thenBy{it.work.title})
 val hotRows=rows.filter{m.includeUpcoming||!it.work.isUpcoming()}.sortedByDescending{it.popularity}
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)){
  FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
   listOf("hot" to "近期热门番剧","schedule" to "时间表","tv" to "国漫 / 动画剧集","movie" to "动画电影","top" to "高分动画").forEach{(id,label)->
    PcChip(label,tab==id,{tab=id;if(id=="hot"){year=now.year;quarter=(now.monthValue-1)/3;weekday=0}})
   }
   PcChip("搜索动画",false,{m.enterSearch055()})
   PcChip("管理动画源 (${animeSources.size})",false,{m.utility("sites")})
   if(tab in listOf("hot","schedule"))PcChip("刷新",false,{refresh++})
  }
  if(tab=="schedule"){
   Column(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(8.dp)).border(1.dp,c.border.copy(alpha=.6f),RoundedCornerShape(8.dp)).padding(horizontal=10.dp,vertical=7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
    Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
     Text("${year}年${AnimeSeason056.seasons[quarter]} · ${scheduleRows.size}部",color=c.text,fontSize=13.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.SemiBold,maxLines=1)
     Text("· $status",color=c.muted,fontSize=10.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,modifier=Modifier.weight(1f))
     CompactSchedChip("-5"){year=(year-5).coerceAtLeast(1970);weekday=0}
     YearStepCircle056("−"){year--;weekday=0}
     Text("$year",color=c.text,fontSize=13.sp)
     YearStepCircle056("+"){year++;weekday=0}
     CompactSchedChip("+5"){year=(year+5).coerceAtMost(now.year+2);weekday=0}
     listOf("冬","春","夏","秋").forEachIndexed{index,name->CompactSchedChip(name,quarter==index){quarter=index;weekday=0}}
     CompactSchedChip("本季"){year=now.year;quarter=(now.monthValue-1)/3;weekday=0;refresh++}
    }
    Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
     (0..7).forEach{day->val count=if(day==0)rows.size else rows.count{it.weekday==day};CompactSchedChip((if(day==0)"全部"else"周${"一二三四五六日"[day-1]}")+" $count",weekday==day){weekday=day}}
    }
   }
  }
  if(tab in listOf("tv","movie","top")){
   LazyVerticalGrid(GridCells.Adaptive(138.dp),state=gridState,modifier=Modifier.weight(1f).fillMaxWidth().pagedDown051(gridState,doubanRows.map{it.key},{m.focusWork.takeIf{m.focusRow=="anime"}},doubanBusy,doubanMore){loadDoubanTab(false)},horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(4.dp,4.dp,8.dp,18.dp)){
    item(span={GridItemSpan(maxLineSpan)}){PcSection(when(tab){"movie"->"动画电影";"top"->"高分动画";else->"国漫 / 动画剧集"},if(doubanBusy)"正在加载更多动画… · 已加载 ${doubanRows.size}部"else"已加载 ${doubanRows.size}部 · 支持国漫年番与多站自动匹配")}
    items(doubanRows,key={it.key}){w->CinemaPoster(m,w,"anime")}
    item(span={GridItemSpan(maxLineSpan)}){if(doubanRows.isEmpty()&&!doubanBusy)PcEmpty("暂未加载到动画条目","请检查网络或切换顶部其他分类");MoreHint051(doubanBusy,doubanMore){loadDoubanTab(false)}}
   }
  }else{
   val shown=if(tab=="hot")hotRows else scheduleRows
   var visibleLimit by rememberSaveable(tab,year,quarter,weekday){mutableIntStateOf(20)}
   val paged=shown.take(visibleLimit)
   val hasMore=paged.size<shown.size
   val animeGrid=rememberLazyGridState()
   LazyVerticalGrid(GridCells.Adaptive(138.dp),state=animeGrid,modifier=Modifier.weight(1f).fillMaxWidth().pagedDown051(animeGrid,paged.map{it.work.key},{m.focusWork.takeIf{m.focusRow=="anime"}},busy,hasMore){visibleLimit+=20},horizontalArrangement=Arrangement.spacedBy(14.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(4.dp,4.dp,8.dp,18.dp)){
    if(tab=="hot")item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
     PcSection("近期热门番剧 · ${year}年${AnimeSeason056.seasons[quarter]}","已显示 ${paged.size}/${shown.size}部 · 每次加载20部，向下滑加载更多")
     Text(status,color=c.muted,fontSize=10.sp)
    }}
    if(busy&&rows.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Text("正在加载新番海报与中文资料…",color=PcAccent)}
    items(paged,key={it.work.key}){e->val (shownWork,_)=presented050(m,e.work);Column{CinemaPoster(m,e.work,"anime");if(tab=="schedule"&&e.weekday>0)Text("周${"一二三四五六日"[e.weekday-1]} ${e.time}",color=PcAccent,fontSize=11.sp);if(busy&&!InfoChinese056.hasHan(shownWork.title))Text("中文名补充中…",color=PcAccent,fontSize=10.sp,maxLines=1)}}
    if(hasMore)item(span={GridItemSpan(maxLineSpan)}){MoreHint051(busy,true){visibleLimit+=20}}
    if(!busy&&shown.isEmpty())item(span={GridItemSpan(maxLineSpan)}){PcEmpty("当前星期暂无排期","可切回“全部”或切换其他年份与季度。")}
   }
  }
 }
}
