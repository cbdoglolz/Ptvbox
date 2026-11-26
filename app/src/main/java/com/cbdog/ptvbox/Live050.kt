package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.*
import androidx.compose.ui.semantics.*
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import kotlinx.coroutines.*
import org.json.*

data class LiveEntry050(val site:Site,val vod:Vod){val work get()=Work(sha256((site.key+"|"+vod.id).toByteArray()),vod.title,kind="tv",poster=vod.poster,overview=vod.overview,provider="live");fun json()=JSONObject().put("site",site.json()).put("vod",VodWire.encode(vod));companion object{fun from(j:JSONObject)=LiveEntry050(Site.from(j.getJSONObject("site")),VodWire.decode(j.getJSONObject("vod")))}}
data class LiveSource050(val key:String,val name:String,val site:Site?=null,val url:String="",val groups:List<Vod> = emptyList(),val headers:Map<String,String> = emptyMap(),val platform:String="")
data class LivePage050(val rows:List<Vod>,val classes:List<Pair<String,String>>,val page:Int,val time:Long=System.currentTimeMillis())
object LiveParser050 {
 fun playlist(text:String,base:String=""):List<Vod>{val result=mutableListOf<Vod>();var title="";var logo="";var group="频道"
  fun add(name:String,url:String){val u=runCatching{java.net.URI(base).resolve(url).toString()}.getOrDefault(url);if(!u.startsWith("http://")&&!u.startsWith("https://"))return;result+=Vod(sha256((name+u).toByteArray()),name,"",logo,group,listOf(Road(group,listOf(Episode(name,u)))))}
  for(line in text.lineSequence().map{it.trim().trimStart('\uFEFF')}.filter{it.isNotBlank()})when{line.startsWith("#EXTINF:")->{title=line.substringAfterLast(',');logo=Regex("tvg-logo=\"([^\"]*)\"").find(line)?.groupValues?.get(1).orEmpty();group=Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.get(1).orEmpty().ifBlank{"频道"}}
   line.startsWith('#')->Unit
   line.contains(",#genre#")->group=line.substringBefore(',')
   line.startsWith("http")->add(title.ifBlank{"直播频道"},line)
   line.contains(',')->{val name=line.substringBefore(',');line.substringAfter(',').split('#').forEach{add(name,it.trim())}}
  };return result.distinctBy{it.id}
 }
 suspend fun load(url:String,headers:Map<String,String> = emptyMap(),depth:Int=0):List<Vod>{require(depth<=2){"直播订阅嵌套过深"};val body=Net.text(url,headers);if(!body.trimStart().startsWith('{'))return playlist(body,url);val sources=sources(body,"",emptyList());return sources.flatMap{s->s.groups.ifEmpty{if(s.url.startsWith("http"))load(s.url,headers+s.headers,depth+1)else emptyList()}}.distinctBy{it.id}}
 fun awaitingSignal(v:Vod)=Regex("未开始|未开播|未开赛|尚未开|暂无信号").containsMatchIn(v.overview)||Regex("(?:\\s|：)(?:暂无|未开始|未开赛|未开播)\\s*$").containsMatchIn(v.title)
 fun directoryLabel(v:Vod)=v.title.matches(Regex("20\\d{2}[-年/.]\\d{1,2}[-月/.]\\d{1,2}日?"))
 fun folder(s:Site?,v:Vod)=v.folder||s?.api?.contains("Alllive")==true&&v.id.matches(Regex("(?:huya|douyu|bili)_\\d+"))&&v.overview in listOf("网游","单机","手游","娱乐","其他","手游休闲")
 fun isPortal(s:Site)=listOf("体育","看球","直播","球赛","多多","抓饭").any{s.name.contains(it)}||listOf("Kanqiu","LiveGz","Alllive").any{s.api.contains(it,true)}
 fun sources(raw:String,extra:String,sites:List<Site>):List<LiveSource050>{val j=runCatching{JSONObject(raw)}.getOrDefault(JSONObject());val direct=j.optJSONArray("lives")?.objects().orEmpty().mapIndexed{n,l->
  val groups=l.optJSONArray("groups")?.objects().orEmpty().flatMap{g->(g.optJSONArray("channels")?:g.optJSONArray("channel"))?.objects().orEmpty().mapNotNull{c->val urls=c.optJSONArray("urls")?.let{a->(0 until a.length()).map{a.optString(it)}}?:listOf(c.optString("url"));val eps=urls.filter{it.startsWith("http")}.mapIndexed{k,u->Episode("${c.optString("name")} · 线路${k+1}",u)};if(eps.isEmpty())null else Vod(sha256((c.optString("name")+urls.joinToString()).toByteArray()),c.optString("name"),"",c.optString("logo"),g.optString("name"),listOf(Road(g.optString("name"),eps)))}}
  LiveSource050("playlist-$n",l.optString("name").ifBlank{"直播订阅${n+1}"},url=l.optString("url"),groups=groups,headers=l.optJSONObject("header")?.let{h->h.keys().asSequence().associateWith{h.optString(it)}}.orEmpty())}.filter{it.url.startsWith("http")||it.groups.isNotEmpty()}
  val aggregate=sites.firstOrNull{it.api.contains("Alllive")&&it.type==3}
  return sites.filter{isPortal(it)}.map{original->val platform=if(!original.api.startsWith("csp_")&&original.type==3)listOf("虎牙","斗鱼").firstOrNull{original.name.contains(it)}.orEmpty()else "";val active=if(platform.isNotBlank()&&aggregate!=null)aggregate.copy(key=original.key,name=original.name+" · 兼容线路")else original;LiveSource050(original.key,active.name,site=active,platform=platform.takeIf{aggregate!=null}.orEmpty())}+direct+if(extra.startsWith("http"))listOf(LiveSource050("custom-live","我的直播订阅",url=extra))else emptyList()
 }
}
@Composable fun LiveHub050(m:PtvModel){val scope=rememberCoroutineScope();var sources by remember{mutableStateOf<List<LiveSource050>>(emptyList())};var key by rememberSaveable{mutableStateOf("")};var category by rememberSaveable{mutableStateOf("")};var categoryTrail052 by rememberSaveable{mutableStateOf<List<String>>(emptyList())};var page by rememberSaveable{mutableIntStateOf(1)};var rows by remember{mutableStateOf<List<Vod>>(emptyList())};var classes by remember{mutableStateOf<List<Pair<String,String>>>(emptyList())};var busy by remember{mutableStateOf(false)};var status by remember{mutableStateOf("")};var refresh by remember{mutableIntStateOf(0)};var last by remember{mutableStateOf<LiveEntry050?>(null)};var more by rememberSaveable{mutableStateOf(true)};var focused by remember{mutableStateOf("")};val grid=rememberLazyGridState();var editing by remember{mutableStateOf(false)};var moving by remember{mutableStateOf("")}
 androidx.activity.compose.BackHandler(categoryTrail052.isNotEmpty()){category=categoryTrail052.last();categoryTrail052=categoryTrail052.dropLast(1);page=1;more=true}
 LaunchedEffect(m.availableSites,m.enabledSiteKeys,m.preferencesRevision){sources=withContext(Dispatchers.IO){val all=LiveParser050.sources(m.db.secrets.read("source_config"),m.db.secrets.read("live_subscription"),m.availableSites.filter{m.siteEnabled(it)});val saved=runCatching{JSONArray(m.db.prefs.getString("live_source_order","[]")).let{a->(0 until a.length()).map{a.getString(it)}}}.getOrDefault(emptyList());LiveOrder054.order(all.map{it.key},saved).mapNotNull{k->all.firstOrNull{it.key==k}}};last=withContext(Dispatchers.IO){runCatching{LiveEntry050.from(JSONObject(m.db.secrets.read("live_last")))}.getOrNull()};if(sources.none{it.key==key}){key=sources.firstOrNull()?.key.orEmpty();category="";categoryTrail052=emptyList();page=1}}
 LaunchedEffect(key,category,page,refresh,sources){val source=sources.firstOrNull{it.key==key}?:return@LaunchedEffect;val cacheKey=key+"|"+category;val cached=m.liveBrowseCache050[cacheKey];if(cached!=null&&page<=cached.page){rows=cached.rows;classes=cached.classes;if(refresh==0&&System.currentTimeMillis()-cached.time<60000){if(page<cached.page)page=cached.page;status="${rows.size}场 / 频道 · 第${cached.page}页";return@LaunchedEffect}}else if(page==1)rows=emptyList();busy=true;try{val root=source.site?.let{withContext(Dispatchers.IO){m.sites.browse(it,category,page)}};val loaded=if(root!=null){if(category.isBlank()){classes=root.optJSONArray("class")?.objects()?.map{it.optString("type_id") to it.optString("type_name")}.orEmpty();if(classes.isNotEmpty()){if(source.platform.isNotBlank())classes=classes.filter{it.second.contains(source.platform)};val initial=LiveOrder054.defaultClass(classes,source.platform);if(initial.isNotBlank()){category=initial;return@LaunchedEffect}}};root.optJSONArray("list")?.objects()?.map(VodParser::parse).orEmpty().filterNot(LiveParser050::directoryLabel)}else source.groups.ifEmpty{withContext(Dispatchers.IO){LiveParser050.load(source.url,source.headers)}};val freshCount=loaded.count{v->rows.none{it.id==v.id}};more=source.site!=null&&loaded.isNotEmpty()&&(page==1||freshCount>0);rows=(if(page>1)rows+loaded else loaded).distinctBy{it.id};m.liveBrowseCache050[cacheKey]=LivePage050(rows,classes,page);status="${rows.size}场 / 频道 · 第${page}页"}catch(e:CancellationException){throw e}catch(e:Exception){status="读取失败：${Regex("""HTTP \d{3}""").find(e.message.orEmpty())?.value?:e.message?.substringBefore(" @")?.take(80)?:"网络或规则不兼容"}；可刷新频道，当前播放保持"}finally{busy=false}}
  LazyVerticalGrid(GridCells.Adaptive(230.dp),Modifier.fillMaxSize().pagedDown051(grid,rows.map{it.id},{focused},busy,more){page=(m.liveBrowseCache050[key+"|"+category]?.page?:page)+1;refresh++},state=grid,horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(5.dp)){
   item(span={GridItemSpan(maxLineSpan)}){Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(10.dp)){
  PcSection("直播与体育",if(busy)"正在读取频道…"else status){Action("刷新频道",{refresh++})}
  last?.let{entry->Action("继续频道 · ${entry.vod.title}",{m.openLive050(entry)},Modifier.fillMaxWidth(),maxLines=Int.MAX_VALUE)}
  if(editing)Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Text("排序模式：选择分类，再点击两侧箭头移动",color=PcAccent,fontSize=12.sp);PcChip("完成排序",false,{editing=false;moving=""})}else Text("长按任意分类3秒调整顺序",color=LocalCinema.current.muted,fontSize=11.sp)
  LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(5.dp)){items(sources,key={it.key}){s->
   Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
    fun shift(d:Int){val keys=LiveOrder054.move(sources.map{it.key},s.key,d);sources=keys.mapNotNull{k->sources.firstOrNull{it.key==k}};m.db.prefs.edit().putString("live_source_order",JSONArray(keys).toString()).apply()}
    if(editing&&moving==s.key)SortArrow054("◀",{shift(-1)})
    InputButton({if(editing)moving=s.key else{key=s.key;category="";categoryTrail052=emptyList();page=1;more=true;classes=emptyList()}},Modifier.selection051(key==s.key),onLongClick={editing=true;moving=""},contentPadding=PaddingValues(13.dp,9.dp)){Text(s.name,color=if(key==s.key)PcAccent else LocalCinema.current.text,fontSize=13.sp)}
    if(editing&&moving==s.key)SortArrow054("▶",{shift(1)})
   }
  }}
  if(categoryTrail052.isNotEmpty())PcChip("← 返回上级分类",false,{category=categoryTrail052.last();categoryTrail052=categoryTrail052.dropLast(1);page=1;more=true})
  if(classes.isNotEmpty())LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(classes,key={it.first}){(id,name)->val active=LiveOrder054.activeClass(category,categoryTrail052,id);PcChip(name,active,{category=id;categoryTrail052=emptyList();page=1;more=true},Modifier.semantics{if(active)contentDescription="当前平台：$name"})}}
  }}
   if(busy&&rows.isEmpty()){
    item(span={GridItemSpan(maxLineSpan)}){Text("正在读取频道分类与直播内容…",color=PcAccent,fontSize=13.sp)}
    items(6,key={"live-loading-$it"}){Column(Modifier.fillMaxWidth().height(150.dp).background(LocalCinema.current.raised,RoundedCornerShape(10.dp)).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Box(Modifier.fillMaxWidth().height(75.dp).background(LocalCinema.current.surface,RoundedCornerShape(8.dp)));Text("频道信息加载中…",color=LocalCinema.current.muted,fontSize=12.sp)}}
   }
   if(rows.isEmpty()&&!busy)item(span={GridItemSpan(maxLineSpan)}){PcEmpty("暂无频道",if(classes.isNotEmpty())"请选择上方分类；未开赛与无信号的比赛需等开播后再刷新。"else"可在设置导入直播订阅，或选择已有TVBox体育/直播站点。")}
   items(rows,key={it.id}){v->val source=sources.firstOrNull{it.key==key};InputButton({if(source!=null&&LiveParser050.folder(source.site,v)){categoryTrail052=categoryTrail052+category;category=v.id;page=1;more=true}else if(source!=null){val site=source.site?:Site("live-${source.key}",source.name,"https://localhost",1,group="直播与体育",headers=source.headers,searchable=false);m.openLive050(LiveEntry050(site,v))}},Modifier.fillMaxWidth().then(workFocus(m,Work(v.id,v.title,provider="live"),"livegrid")).onFocusChanged{if(it.hasFocus)focused=v.id},shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(10.dp))){Column(verticalArrangement=Arrangement.spacedBy(8.dp)){if(v.poster.isNotBlank())AsyncImage(v.poster,v.title,Modifier.fillMaxWidth().height(100.dp),contentScale=ContentScale.Fit);Text(v.title,color=LocalCinema.current.text,fontSize=14.sp);if(v.folder)Text("分类 · 点击进入频道列表",color=PcAccent,fontSize=11.sp);if(LiveParser050.awaitingSignal(v))Text("未开播 / 暂无信号 · 开播后刷新",color=PcAccent,fontSize=11.sp);if(v.overview.isNotBlank())Text(v.overview,color=LocalCinema.current.muted,fontSize=11.sp,maxLines=2)}}}
   if(sources.firstOrNull{it.key==key}?.site!=null&&rows.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){MoreHint051(busy,more){page=(m.liveBrowseCache050[key+"|"+category]?.page?:page)+1;refresh++}}
  }
}
@Composable fun LiveDetail050(m:PtvModel){val entry=m.liveEntry?:return;val scope=rememberCoroutineScope();var value by remember(entry.work.key){mutableStateOf(entry)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")}
 LaunchedEffect(entry.work.key){busy=true;try{value=entry.copy(vod=withContext(Dispatchers.IO){m.sites.detail(entry.site,if(entry.site.type==3)entry.vod.copy(roads=emptyList())else entry.vod)});m.liveEntry=value}catch(e:CancellationException){throw e}catch(e:Exception){error="线路读取失败，可刷新重试"}finally{busy=false}}
 Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(14.dp)){PcSection(value.vod.title,if(busy)"正在读取直播线路…"else error);if(value.vod.poster.isNotBlank())AsyncImage(value.vod.poster,value.vod.title,Modifier.fillMaxWidth().height(140.dp),contentScale=ContentScale.Fit)
  Action(if(LiveParser050.awaitingSignal(value.vod))"尚未开播 · 刷新频道后再试"else"▶ 播放直播",{if(LiveParser050.awaitingSignal(value.vod))m.message("本场未开播或暂无信号，请开播后刷新频道")else m.playLive050(value)});PcSection("全部直播线路","${value.vod.roads.sumOf{it.episodes.size}}条 · 直播不会搜索点播资源站")
  LazyVerticalGrid(GridCells.Adaptive(260.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(value.vod.roads.flatMap{r->r.episodes.map{r to it}},key={it.first.name+"|"+it.second.name+"|"+it.second.url}){(r,e)->Action("${r.name} · ${e.name}",{m.playLive050(value,r.name+"|"+e.name)},Modifier.fillMaxWidth(),maxLines=Int.MAX_VALUE)}}
 }
}

@Composable private fun SortArrow054(text:String,click:()->Unit){InputButton(click,Modifier.background(Brush.horizontalGradient(listOf(Color(0x777c45d6),Color(0x77d09aff))),RoundedCornerShape(8.dp)),colors=ptvButtonColors(Color.Transparent,Color.White),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(8.dp)),contentPadding=PaddingValues(9.dp)){Text(text,color=Color.White)}}
