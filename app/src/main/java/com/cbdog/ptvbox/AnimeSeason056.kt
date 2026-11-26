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
   AnimeEntry056(Work(j.optString("id"),displayTitle,if(j.optString("format")=="MOVIE")"movie"else"tv",d.optString("year"),j.optJSONObject("coverImage")?.optString("large").orEmpty(),provider="anilist",original=native,aliases=aliases,genres=listOf("animation"),season=Identity.season(native)?:1,backdrop=j.optString("bannerImage").takeUnless{it=="null"}.orEmpty(),releaseDate=date),weekly?.key?:0,clock,j.optDouble("popularity",0.0))
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
 suspend fun load(year:Int,quarter:Int,onRows:suspend(List<AnimeEntry056>)->Unit={}):List<AnimeEntry056>{val (from,to)=range(year,quarter);val rows=mutableListOf<AnimeEntry056>();val query="query(\$page:Int,\$from:FuzzyDateInt,\$to:FuzzyDateInt){Page(page:\$page,perPage:50){pageInfo{hasNextPage}media(type:ANIME,startDate_greater:\$from,startDate_lesser:\$to,isAdult:false,sort:POPULARITY_DESC){id title{native romaji english}synonyms format startDate{year month day}coverImage{large}bannerImage airingSchedule(perPage:50){nodes{airingAt episode}}}}}"
  for(page in 1..12){val body=JSONObject().put("query",query).put("variables",JSONObject().put("page",page).put("from",from.minusDays(1).toString().replace("-","").toInt()).put("to",to.toString().replace("-","").toInt()));val result=parse(Net.postJson("https://graphql.anilist.co",body.toString()));rows+=result.first;onRows(rows.distinctBy{it.work.key});if(!result.second)break}
  var result=rows.distinctBy{it.work.key}
  // Same exact-original-title + year enrichment as PCBOX. A failed supplement never removes the catalogue.
  try{for(page in 0..9){val body=JSONObject().put("keyword","").put("sort","heat").put("filter",JSONObject().put("type",JSONArray(listOf(2))).put("air_date",JSONArray(listOf(">=$from","<$to"))));val raw=Net.postJson("https://api.bgm.tv/v0/search/subjects?limit=50&offset=${page*50}",body.toString());result=supplement(result,raw);onRows(result);if((JSONObject(raw).optJSONArray("data")?.length()?:0)<50)break}}catch(e:CancellationException){throw e}catch(_:Exception){}
  return result
 }
 suspend fun search(value:String):List<AnimeEntry056>{val query="query(\$search:String){Page(page:1,perPage:50){pageInfo{hasNextPage}media(type:ANIME,search:\$search,isAdult:false,sort:POPULARITY_DESC){id title{native romaji english}synonyms format startDate{year month day}coverImage{large}bannerImage airingSchedule(perPage:50){nodes{airingAt episode}}}}}";val body=JSONObject().put("query",query).put("variables",JSONObject().put("search",value));return parse(Net.postJson("https://graphql.anilist.co",body.toString())).first}
}

internal object InfoChinese056{
 fun hasHan(value:String)=value.any{Character.UnicodeScript.of(it.code)==Character.UnicodeScript.HAN}
 fun isLikelyChinese(value:String)=hasHan(value)&&value.none{it in '\u3040'..'\u30ff'}
}

@Composable private fun YearStepCircle056(label:String,click:()->Unit){
    val c=LocalCinema.current
    InputButton(click,Modifier.size(34.dp).border(1.dp,PtvFocusBorder,CircleShape),shape=androidx.tv.material3.ButtonDefaults.shape(CircleShape),colors=ptvButtonColors(Color.Transparent,c.text),contentPadding=PaddingValues(0.dp)){
        Box(Modifier.fillMaxSize(),contentAlignment=androidx.compose.ui.Alignment.Center){Text(label,color=c.text,fontSize=18.sp)}
    }
}

@Composable internal fun AnimePage056(m:PtvModel){
 val now=remember{LocalDate.now()};var year by rememberSaveable{mutableIntStateOf(now.year)};var quarter by rememberSaveable{mutableIntStateOf((now.monthValue-1)/3)};var weekday by rememberSaveable{mutableIntStateOf(0)};var refresh by remember{mutableIntStateOf(0)};var rows by remember{mutableStateOf<List<AnimeEntry056>>(emptyList())};var busy by remember{mutableStateOf(true)};var status by remember{mutableStateOf("")};var mode by rememberSaveable{mutableStateOf("quarter")};var query by rememberSaveable{mutableStateOf("")};var searchRows by remember{mutableStateOf<List<AnimeEntry056>>(emptyList())};var searchRevision by remember{mutableIntStateOf(0)};val context=LocalContext.current;val c=LocalCinema.current
 LaunchedEffect(year,quarter,refresh){rows=emptyList();busy=true;status="正在读取AniList季度目录，再补充Bangumi中文名与海报…";val file=java.io.File(context.cacheDir,"anime-season056-$year-$quarter.json");try{val cached=withContext(Dispatchers.IO){runCatching{JSONArray(file.readText()).objects().map{AnimeEntry056(Work.from(it.getJSONObject("work")),it.optInt("weekday"),it.optString("time"),it.optDouble("popularity",0.0))}}.getOrDefault(emptyList())};rows=cached;if(cached.isNotEmpty()&&refresh==0&&System.currentTimeMillis()-file.lastModified()<12*3600000L){status="已缓存季度目录 · 北京时间"}else{val fresh=withContext(Dispatchers.IO){AnimeSeason056.load(year,quarter){partial->withContext(Dispatchers.Main){rows=partial}}};rows=fresh;withContext(Dispatchers.IO){file.writeText(JSONArray(fresh.map{it.json()}).toString());fresh.forEach{m.db.cache(it.work)}};status="AniList主目录 · Bangumi中文名/海报补充 · 北京时间"}}catch(e:CancellationException){throw e}catch(_:Exception){status=if(rows.isEmpty())"目录暂不可用，可刷新重试"else"目录暂不可用，保留本地缓存"}finally{busy=false}}
 LaunchedEffect(searchRevision){if(mode!="search"||query.isBlank())return@LaunchedEffect;busy=true;status="正在搜索动画…";try{searchRows=withContext(Dispatchers.IO){AnimeSeason056.search(query.trim())};searchRows.forEach{m.db.cache(it.work)};status="动画搜索结果 ${searchRows.size}部"}catch(e:CancellationException){throw e}catch(_:Exception){status="动画搜索暂不可用，保留上次结果"}finally{busy=false}}
 val quarterRows=rows.filter{weekday==0||it.weekday==weekday}.sortedWith(compareBy<AnimeEntry056>{it.time.ifBlank{"99:99"}}.thenBy{it.work.title})
 val shown=when(mode){"hot"->rows.sortedByDescending{it.popularity};else->quarterRows}
 val animeSources=remember(m.availableSites){m.availableSites.filter{it.group=="动画"}}
 Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(12.dp)){
  Column(Modifier.widthIn(min=158.dp,max=210.dp).fillMaxHeight().background(c.surface,RoundedCornerShape(10.dp)).padding(10.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)){
   Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Text("动画季度",color=c.text,fontSize=18.sp,modifier=Modifier.weight(1f));PcChip("重置",false,{year=now.year;quarter=(now.monthValue-1)/3;weekday=0;mode="quarter";query="";searchRows=emptyList();refresh++})}
   Text(status,color=c.muted,fontSize=10.sp,maxLines=2)
   FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){listOf("quarter" to "季度新番","hot" to "当季热播").forEach{(id,label)->PcChip(label,mode==id,{mode=id;weekday=0})}}
   Text("$year",color=c.text,fontSize=18.sp,modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){PcChip("-5",false,{year=(year-5).coerceAtLeast(1970);weekday=0});YearStepCircle056("−"){year--;weekday=0};YearStepCircle056("+"){year++;weekday=0};PcChip("+5",false,{year=(year+5).coerceAtMost(now.year+2);weekday=0})}
   if(mode=="quarter"){FlowRow(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){AnimeSeason056.seasons.forEachIndexed{index,name->PcChip(name,quarter==index,{quarter=index;weekday=0})}};Text("每周更新",color=PcAccent,fontSize=11.sp);(0..7).forEach{day->val count=if(day==0)rows.size else rows.count{it.weekday==day};PcChip((if(day==0)"全部" else "周${"一二三四五六日"[day-1]}")+"  $count",weekday==day,{weekday=day},Modifier.fillMaxWidth())}}
   Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
   Text("动画专用源",color=c.text,fontSize=13.sp)
   Text("${animeSources.size}个站点已配置并参与作品搜索",color=if(animeSources.isEmpty())PcAccent else PcGreen,fontSize=10.sp,maxLines=2)
   Action("管理动画源",{m.utility("sites")},Modifier.fillMaxWidth())
   Action("动画推荐",{m.changeFeed(k="tv",c="动画");m.screen="library"},Modifier.fillMaxWidth())
   Text("季度目录只表示排期，不代表已存在可播放片源。",color=c.muted,fontSize=9.sp,maxLines=3)
  }
  LazyVerticalGrid(GridCells.Adaptive(138.dp),Modifier.weight(1f).fillMaxHeight(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(14.dp),contentPadding=PaddingValues(4.dp,4.dp,8.dp,18.dp)){
   item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(4.dp)){PcSection(when(mode){"hot"->"动画 · ${year}年${AnimeSeason056.seasons[quarter]}当季热播";else->"动画 · ${year}年${AnimeSeason056.seasons[quarter]}新番"},if(mode=="quarter"){if(weekday==0)"全部 ${shown.size}部" else "周${"一二三四五六日"[weekday-1]} · ${shown.size}部 · 北京时间"}else"${shown.size}部 · 按当前季热度排序");Text("季度目录来自AniList；Bangumi补充中文名与中文海报，匹配不到时会明确标记。",color=c.muted,fontSize=10.sp)}}
   if(busy&&rows.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Text("正在加载新番海报与排期…",color=PcAccent)}
   items(shown,key={it.work.key}){e->Column{CinemaPoster(m,e.work,"anime");if(e.weekday>0)Text("周${"一二三四五六日"[e.weekday-1]} ${e.time}",color=PcAccent,fontSize=11.sp);if(!InfoChinese056.hasHan(e.work.title))Text(if(busy)"中文名补充中…" else "中文名未匹配",color=if(busy)PcAccent else c.muted,fontSize=10.sp,maxLines=1)}}
   if(!busy&&shown.isEmpty())item(span={GridItemSpan(maxLineSpan)}){PcEmpty("当前星期暂无排期","可切回“全部”，排期未返回的作品仍会显示。")}
  }
 }
}
