package com.cbdog.ptvbox
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import kotlinx.coroutines.*
@Composable internal fun TvSearch055(m:PtvModel,voice:()->Unit){val c=LocalCinema.current;var source by rememberSaveable{mutableStateOf("catalog")};var choose by remember{mutableStateOf(false)};var results by rememberSaveable{mutableStateOf(false)};var hot by remember{mutableStateOf<Map<String,List<Work>>>(emptyMap())};var hints by remember{mutableStateOf(SearchHints055("",emptyList(),false))};var hintLoading by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();val keyFocus=remember{FocusRequester()};val entryFocus=remember{FocusRequester()};val keyboard=LocalSoftwareKeyboardController.current
 LaunchedEffect(Unit){if(m.query.isBlank()){withFrameNanos{};keyFocus.requestFocus()}}
 LaunchedEffect(m.searchRevision055){results=m.searchRevision055>0}
 LaunchedEffect(Unit){hot=m.searchAssist055.hot()}
 LaunchedEffect(m.query){val q=m.query;hints=SearchHints055(q,emptyList(),false);if(q.isBlank()){hintLoading=false;return@LaunchedEffect};hintLoading=true;delay(350);try{val loaded=m.searchAssist055.suggest(q,hot);if(m.query==q)hints=loaded}finally{if(m.query==q)hintLoading=false}}
 fun change(s:String){m.query=s;m.searchFilterGenre056="";results=false}
 fun submit(value:String=m.query){if(value.isBlank())return;m.query=value;if(source!="catalog"&&!source.startsWith("provider:")&&SearchText055.initialQuery(value)&&hints.names.isNotEmpty()&&hints.names.none{it.equals(value,true)}){results=false;m.message("请从右侧联想选择完整片名，再搜索资源站");return};results=true;if(source=="catalog"||source.startsWith("provider:"))m.search(value)else m.searchSites(value,source.takeUnless{it=="all"})}
 BoxWithConstraints(Modifier.fillMaxSize()){
  val historyWidth=(maxWidth*.16f).coerceIn(105.dp,170.dp);val inputWidth=(maxWidth*.30f).coerceIn(252.dp,330.dp);val keyHeight=((maxHeight-180.dp)/7-6.dp).coerceIn(28.dp,44.dp)
  Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
   Column(Modifier.width(historyWidth).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("搜索历史",color=c.text,fontSize=17.sp);PcChip("清空",false,{m.users.write{clearSearch()};m.searchHistory=emptyList()})};LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){items(m.searchHistory,key={it}){q->InputButton({submit(q)},Modifier.fillMaxWidth(),contentPadding=PaddingValues(8.dp,6.dp)){Text(q,color=c.muted,fontSize=11.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}}}}
   Column(Modifier.width(inputWidth).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(7.dp)){
    Action("指定搜索源 · "+when(source){"catalog"->"影视资料";"all"->"全部站点";"provider:douban"->"豆瓣资料";"provider:tmdb"->"TMDB资料";else->m.availableSites.firstOrNull{it.key==source}?.name?:"全部站点"},{choose=true},Modifier.fillMaxWidth(),maxLines=1)
    Entry(m.query,{change(it.take(120))},"中文 / 英文 / 拼音首字母",modifier=Modifier.focusRequester(entryFocus))
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Action("搜索",{submit()},Modifier.weight(1f));Action("清空输入",{change("")},Modifier.weight(1f))}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){SearchControl055("远程搜索",Modifier.weight(1f)){m.settingsSection="remote";m.utility("settings")};SearchControl055("中文输入",Modifier.weight(1f)){entryFocus.requestFocus();keyboard?.show()};SearchControl055("语音搜索",Modifier.weight(1f),voice)}
    LazyVerticalGrid(GridCells.Fixed(6),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(5.dp),verticalArrangement=Arrangement.spacedBy(5.dp),contentPadding=PaddingValues(3.dp)){items(SearchText055.keys,key={it}){k->InputButton({change(SearchText055.edit(m.query,k))},Modifier.fillMaxWidth().height(keyHeight).then(if(k=="A")Modifier.focusRequester(keyFocus)else Modifier),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(7.dp)),contentPadding=PaddingValues(2.dp)){Text(k,color=c.text,fontSize=if(k.length>1)12.sp else 18.sp)}}}
    Text("首字母先选联想词；也可用语音或系统中文输入",color=c.muted,fontSize=10.sp)
   }
   Column(Modifier.weight(1f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(if(results)"搜索结果 · ${m.searchResults.size}"else if(m.query.isBlank())"热门搜索"else"联想搜索",color=c.text,fontSize=20.sp);if(results)PcChip("返回联想",false,{results=false})}
    if(results){Text(if(m.busy)"正在搜索…"else if(m.searchFilterGenre056.isNotBlank())"分类筛选：${m.searchFilterGenre056} · 可继续多选交叉筛选和排序"else"核对年份和季数，再打开作品",color=PcAccent,fontSize=12.sp);Box(Modifier.weight(1f)){SearchResults040(m,source.takeIf{it.startsWith("provider:")}?.substringAfter(":").orEmpty(),m.searchFilterGenre056)}}
    else if(m.query.isBlank()){Text("豆瓣近期热门推荐 · 非搜索量排名",color=c.muted,fontSize=10.sp);LazyColumn(verticalArrangement=Arrangement.spacedBy(18.dp)){items(listOf("电视榜","电影榜","综艺榜")){label->Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(label,color=PcGreen,fontSize=19.sp);FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){hot[label].orEmpty().forEach{w->PcChip(w.title,false,{submit(w.title)})}};if(hot[label].isNullOrEmpty())Text("正在读取热门片名…",color=c.muted,fontSize=12.sp)}}}}
    else {Text(if(hintLoading)"正在联想…"else if(hints.online)"爱奇艺联想词 + 本机片库"else"本机片库联想 · 在线结果暂不可用",color=c.muted,fontSize=11.sp);LazyVerticalGrid(GridCells.Adaptive(142.dp),Modifier.weight(1f),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){items(hints.names,key={it}){name->Action(name,{submit(name)},Modifier.fillMaxWidth(),maxLines=2)}};if(!hintLoading&&hints.names.isEmpty())Text("暂无联想，可输入完整片名或英文原名搜索",color=c.muted,fontSize=12.sp)}
   }
  }
 }
 if(choose)Dialog({choose=false}){SettingsCard("指定搜索源","联想不会触发全部站点扫描，确认搜索后才查询所选来源。"){LazyColumn(Modifier.heightIn(max=400.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){item{Action("影视资料 · 豆瓣 / TMDB",{source="catalog";choose=false},Modifier.fillMaxWidth(),selected=source=="catalog")};items(listOf("douban" to "豆瓣资料","tmdb" to "TMDB资料")){(id,label)->Action(label,{source="provider:$id";choose=false},Modifier.fillMaxWidth(),selected=source=="provider:$id")};item{Action("全部已启用资源站",{source="all";choose=false},Modifier.fillMaxWidth(),selected=source=="all")};items(m.availableSites.filter{m.siteEnabled(it)&&it.searchable&&!LiveParser050.isPortal(it)},key={it.key}){s->Action(s.name,{source=s.key;choose=false},Modifier.fillMaxWidth(),selected=source==s.key)}};Action("关闭",{choose=false})}}
}

@Composable private fun SearchControl055(label:String,modifier:Modifier,click:()->Unit){InputButton(click,modifier.height(38.dp),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(7.dp)),contentPadding=PaddingValues(4.dp)){Text(label,color=LocalCinema.current.text,fontSize=12.sp,maxLines=1)}}
