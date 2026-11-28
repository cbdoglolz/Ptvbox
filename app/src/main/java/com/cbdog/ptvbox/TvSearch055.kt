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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.tv.material3.Text
import kotlinx.coroutines.*
@Composable internal fun TvSearch055(m:PtvModel,voice:()->Unit){val c=LocalCinema.current;var source by rememberSaveable{mutableStateOf("catalog")};var choose by remember{mutableStateOf(false)};var results by rememberSaveable{mutableStateOf(false)};var hot by remember{mutableStateOf<Map<String,List<Work>>>(emptyMap())};var hintState by remember{mutableStateOf(SearchHints055("",emptyList(),false))};val keyFocus=remember{FocusRequester()};val entryFocus=remember{FocusRequester()};var remote by remember{mutableStateOf(false)};var enterRight by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();val rightFocus=remember{FocusRequester()};val keyRequests=remember{SearchText055.keys.associateWith{if(it=="A")keyFocus else FocusRequester()}};val sourceFocus=remember{FocusRequester()};val submitFocus=remember{FocusRequester()};val clearFocus=remember{FocusRequester()};val remoteFocus=remember{FocusRequester()};val voiceFocus=remember{FocusRequester()};var lastInput by remember{mutableStateOf(keyFocus)}
 LaunchedEffect(Unit){if(m.query.isBlank()){withFrameNanos{};keyFocus.requestFocus()}}
 LaunchedEffect(m.searchRevision055){results=m.searchRevision055>0;remote=false;if(m.searchRevision055>0)enterRight=true}
 LaunchedEffect(Unit){hot=m.searchAssist055.hot()}
 LaunchedEffect(m.query,results){val q=m.query;if(results)return@LaunchedEffect;m.searchAssist055.observe(q,hot).collect{loaded->if(m.query==q)hintState=loaded}}
 val hints=hintState.takeIf{it.query==m.query.trim()}?:SearchHints055(m.query.trim(),emptyList(),false,pending=m.query.isNotBlank())
 val hintLoading=hints.pending
 val canEnterRight=results||if(m.query.isBlank())hot.values.any{it.isNotEmpty()}else hints.names.isNotEmpty()
 val rightTarget=if(canEnterRight)rightFocus else FocusRequester.Default
 LaunchedEffect(enterRight,canEnterRight,results){if(enterRight&&canEnterRight){withFrameNanos{};rightFocus.requestFocus();enterRight=false}}
 fun returnToHints(){results=false;enterRight=true}
 fun closeRemote(){remote=false;scope.launch{withFrameNanos{};remoteFocus.requestFocus()}}

 fun change(s:String){enterRight=false;m.editQuery055(s);results=false}
 fun submit(value:String=m.query){if(value.isBlank())return;m.query=value;if(source!="catalog"&&!source.startsWith("provider:")&&SearchText055.initialQuery(value)&&hints.query.trim()==value.trim()&&hints.names.isNotEmpty()&&hints.names.none{it.equals(value,true)}){results=false;m.message("请从右侧联想选择完整片名，再搜索资源站");return};results=true;enterRight=true;if(source=="catalog"||source.startsWith("provider:"))m.search(value)else m.searchSites(value,source.takeUnless{it=="all"})}
 val startVoice=rememberSearchVoice(voice,closed={scope.launch{withFrameNanos{};voiceFocus.requestFocus()}}){submit(it)}
 BoxWithConstraints(Modifier.fillMaxSize()){
  val historyWidth=(maxWidth*.16f).coerceIn(105.dp,170.dp);val inputWidth=(maxWidth*.30f).coerceIn(252.dp,330.dp)
  val rows=SearchText055.keys.chunked(8)
  val compact=maxHeight<440.dp
  Row(Modifier.fillMaxSize(),horizontalArrangement=Arrangement.spacedBy(14.dp)){
   Column(Modifier.width(historyWidth).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp)){Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text("搜索历史",color=c.text,fontSize=14.sp);SearchControl055("清空",Modifier.width(42.dp)){m.users.write{clearSearch()};m.searchHistory=emptyList()}};LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){items(m.searchHistory,key={it}){q->InputButton({submit(q)},Modifier.fillMaxWidth(),contentPadding=PaddingValues(8.dp,6.dp)){Text(q,color=c.muted,fontSize=11.sp,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}}}}
   Column(Modifier.width(inputWidth).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(if(compact)4.dp else 7.dp)){
    Action("指定搜索源 · "+when(source){"catalog"->"影视资料";"all"->"全部站点";"provider:douban"->"豆瓣资料";"provider:tmdb"->"TMDB资料";else->m.availableSites.firstOrNull{it.key==source}?.name?:"全部站点"},{choose=true},Modifier.fillMaxWidth().focusRequester(sourceFocus).onFocusChanged{if(it.isFocused)lastInput=sourceFocus}.focusProperties{right=rightTarget;down=entryFocus},maxLines=1)
    Entry(m.query,{change(it.take(120))},"中文 / 英文 / 拼音首字母",modifier=Modifier.focusRequester(entryFocus).focusProperties{right=rightTarget;up=sourceFocus;down=submitFocus}.onFocusChanged{if(it.isFocused)lastInput=entryFocus}.onPreviewKeyEvent{if(it.key==Key.DirectionRight&&it.type==KeyEventType.KeyDown&&canEnterRight){rightFocus.requestFocus();true}else false})
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){Action("搜索",{submit()},Modifier.weight(1f).focusRequester(submitFocus).onFocusChanged{if(it.isFocused)lastInput=submitFocus}.focusProperties{up=entryFocus;down=remoteFocus;right=clearFocus});Action("清空输入",{change("")},Modifier.weight(1f).focusRequester(clearFocus).onFocusChanged{if(it.isFocused)lastInput=clearFocus}.focusProperties{up=entryFocus;down=voiceFocus;left=submitFocus;right=rightTarget})}
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){
     SearchControl055("远程搜索",Modifier.weight(1f).focusRequester(remoteFocus).onFocusChanged{if(it.isFocused)lastInput=remoteFocus}.focusProperties{up=submitFocus;right=voiceFocus;down=keyFocus}){remote=true;m.startRemoteConfig()}
     SearchControl055("语音搜索",Modifier.weight(1f).focusRequester(voiceFocus).onFocusChanged{if(it.isFocused)lastInput=voiceFocus}.focusProperties{up=clearFocus;left=remoteFocus;down=keyFocus;right=rightTarget},startVoice)
    }
    Column(Modifier.weight(1f).heightIn(max=34.dp*rows.size+2.dp*(rows.size-1)),verticalArrangement=Arrangement.spacedBy(2.dp)){
     rows.forEachIndexed{rowIndex,row->Row(Modifier.fillMaxWidth().weight(1f),horizontalArrangement=Arrangement.spacedBy(3.dp)){
      row.forEachIndexed{columnIndex,k->InputButton({change(SearchText055.edit(m.query,k))},Modifier.weight(1f).fillMaxHeight().focusRequester(keyRequests.getValue(k)).onFocusChanged{if(it.isFocused)lastInput=keyRequests.getValue(k)}.focusProperties{
       right=if(columnIndex<row.lastIndex)keyRequests.getValue(row[columnIndex+1])else rightTarget
       if(columnIndex>0)left=keyRequests.getValue(row[columnIndex-1])
       up=if(rowIndex==0){if(columnIndex<4)remoteFocus else voiceFocus}else keyRequests.getValue(rows[rowIndex-1][columnIndex.coerceAtMost(rows[rowIndex-1].lastIndex)])
       down=if(rowIndex<rows.lastIndex)keyRequests.getValue(rows[rowIndex+1][columnIndex.coerceAtMost(rows[rowIndex+1].lastIndex)])else FocusRequester.Cancel
      },shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(5.dp)),contentPadding=PaddingValues(0.dp)){Text(k,color=c.text,fontSize=if(k.length>1)10.sp else 14.sp,maxLines=1)}}
      repeat(8-row.size){Spacer(Modifier.weight(1f))}
     }}
    }
    if(!compact)Text("首字母先选联想词；也可用语音或系统中文输入",color=c.muted,fontSize=10.sp)
   }
   Column(Modifier.weight(1f).fillMaxHeight(),verticalArrangement=Arrangement.spacedBy(10.dp)){
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(if(results)"搜索结果 · ${m.searchResults.size}"else if(m.query.isBlank())"热门搜索"else"联想搜索",color=c.text,fontSize=20.sp);if(results)PcChip("返回联想",false,::returnToHints,Modifier.focusRequester(rightFocus).focusProperties{left=lastInput})}
    if(results){Text(if(m.busy)"正在搜索…"else if(m.searchFilterGenre056.isNotBlank())"分类筛选：${m.searchFilterGenre056} · 可继续多选交叉筛选和排序"else"核对年份和季数，再打开作品",color=PcAccent,fontSize=12.sp);Box(Modifier.weight(1f)){SearchResults040(m,source.takeIf{it.startsWith("provider:")}?.substringAfter(":").orEmpty(),m.searchFilterGenre056)}}
    else if(m.query.isBlank()){Text("豆瓣近期热门推荐 · 非搜索量排名",color=c.muted,fontSize=10.sp);LazyColumn(verticalArrangement=Arrangement.spacedBy(18.dp)){items(listOf("电视榜","电影榜","综艺榜")){label->Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(label,color=PcGreen,fontSize=19.sp);FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){hot[label].orEmpty().forEach{w->PcChip(w.title,false,{submit(w.title)},Modifier.then(if(w==hot.values.flatten().firstOrNull())Modifier.focusRequester(rightFocus)else Modifier).focusProperties{left=lastInput})}};if(hot[label].isNullOrEmpty())Text("正在读取热门片名…",color=c.muted,fontSize=12.sp)}}}}
    else {
     Text(if(hintLoading)"正在补充在线联想…"else if(hints.online)"爱奇艺联想词 + 本机片库"else"本机片库联想 · 在线结果暂不可用",color=c.muted,fontSize=11.sp)
     BoxWithConstraints(Modifier.weight(1f)){
      val columns=(maxWidth.value/150f).toInt().coerceIn(1,4)
      LazyVerticalGrid(GridCells.Fixed(columns),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
       itemsIndexed(hints.names,key={_,name->name}){index,name->Action(name,{submit(name)},Modifier.fillMaxWidth().then(if(index==0)Modifier.focusRequester(rightFocus)else Modifier).focusProperties{if(index%columns==0)left=lastInput},maxLines=2)}
      }
     }
     if(!hintLoading&&hints.names.isEmpty())Text("暂无联想，可输入完整片名或英文原名搜索",color=c.muted,fontSize=12.sp)
    }
   }
  }
 }
 if(remote)SearchRemoteDialog103(m,::closeRemote)

 if(choose)Dialog({choose=false}){SettingsCard("指定搜索源","联想不会触发全部站点扫描，确认搜索后才查询所选来源。"){LazyColumn(Modifier.heightIn(max=400.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){item{Action("影视资料 · 豆瓣 / TMDB",{source="catalog";choose=false},Modifier.fillMaxWidth(),selected=source=="catalog")};items(listOf("douban" to "豆瓣资料","tmdb" to "TMDB资料")){(id,label)->Action(label,{source="provider:$id";choose=false},Modifier.fillMaxWidth(),selected=source=="provider:$id")};item{Action("全部已启用资源站",{source="all";choose=false},Modifier.fillMaxWidth(),selected=source=="all")};items(m.availableSites.filter{m.siteEnabled(it)&&it.searchable&&!LiveParser050.isPortal(it)},key={it.key}){s->Action(s.name,{source=s.key;choose=false},Modifier.fillMaxWidth(),selected=source==s.key)}};Action("关闭",{choose=false})}}
}

@Composable private fun SearchControl055(label:String,modifier:Modifier,click:()->Unit){InputButton(click,modifier.height(38.dp),shape=androidx.tv.material3.ButtonDefaults.shape(RoundedCornerShape(7.dp)),contentPadding=PaddingValues(4.dp)){Text(label,color=LocalCinema.current.text,fontSize=12.sp,maxLines=1)}}

@Composable internal fun SearchRemoteDialog103(m:PtvModel,close:()->Unit){
 SearchDialog103("远程搜索","手机与电视连接同一网络，扫码搜索。",close){
  val url=m.remoteUrl
  if(url.isBlank()){
   Text("正在等待配对服务",color=LocalCinema.current.muted,fontSize=12.sp)
   Action("重试",{m.startRemoteConfig()})
  }else Image(remember(url){remoteQrBitmap(url+"&page=search")},"远程搜索二维码",Modifier.size(156.dp))
 }
}
