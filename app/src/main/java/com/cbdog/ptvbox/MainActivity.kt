package com.cbdog.ptvbox

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.*
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

private val ink=Color(0xff111223)
private val panel=Color(0xff22243b)
private val violet=Color(0xffa799ff)

class MainActivity:ComponentActivity(){
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode:Boolean,newConfig:Configuration){
        super.onPictureInPictureModeChanged(isInPictureInPictureMode,newConfig)
        androidx.lifecycle.ViewModelProvider(this)[PtvModel::class.java].pipMode=isInPictureInPictureMode
    }
    private val authReceiver054=object:android.content.BroadcastReceiver(){override fun onReceive(context:android.content.Context,intent:Intent){val m=androidx.lifecycle.ViewModelProvider(this@MainActivity)[PtvModel::class.java];val request=intent.getStringExtra("request").orEmpty();if(intent.action==BuildConfig.APPLICATION_ID+".AUTH_RESULT054")m.cloudResult054(request,intent.getStringExtra("workKey").orEmpty(),intent.getStringExtra("candidate").orEmpty())else m.cloudClosed054(request)}}
    override fun onDestroy(){runCatching{unregisterReceiver(authReceiver054)};super.onDestroy()}

    private var homeLink by mutableStateOf<android.net.Uri?>(null)
    override fun onStart(){super.onStart();androidx.lifecycle.ViewModelProvider(this)[PtvModel::class.java].onForeground()}
    override fun onStop(){if(!isChangingConfigurations)androidx.lifecycle.ViewModelProvider(this)[PtvModel::class.java].onExternalBackground();super.onStop()}
    override fun onNewIntent(intent:Intent){super.onNewIntent(intent);setIntent(intent);homeLink=intent.data}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);val filter=android.content.IntentFilter().apply{addAction(BuildConfig.APPLICATION_ID+".AUTH_RESULT054");addAction(BuildConfig.APPLICATION_ID+".AUTH_CLOSED054")};if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(authReceiver054,filter,android.content.Context.RECEIVER_NOT_EXPORTED)else registerReceiver(authReceiver054,filter);homeLink=intent.data;setContent{val m:PtvModel=viewModel();CinemaTheme(m.lightTheme){LaunchedEffect(homeLink){homeLink?.let(m::handleHomeLink);homeLink=null};val voice=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()){result->result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(m::search)};TvApp(m){try{voice.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_LANGUAGE,"zh-CN").putExtra(RecognizerIntent.EXTRA_PROMPT,"说出片名"))}catch(e:Exception){m.message("此设备未提供语音识别，请用键盘或拼音首字母搜索")}}}}}
}

@Composable internal fun Action(label:String,onClick:()->Unit,modifier:Modifier=Modifier,maxLines:Int=2,selected:Boolean=false){val c=LocalCinema.current;InputButton(onClick=onClick,modifier=modifier.selection051(selected),shape=ButtonDefaults.shape(shape=RoundedCornerShape(8.dp)),colors=ptvButtonColors(c.raised,c.text),contentPadding=PaddingValues(14.dp,10.dp)){Text(label,fontSize=14.sp,maxLines=maxLines)}}
@Composable internal fun Entry(value:String,changed:(String)->Unit,hint:String,secret:Boolean=false,modifier:Modifier=Modifier){val c=LocalCinema.current;val keyboard=LocalSoftwareKeyboardController.current;var focused by remember{mutableStateOf(false)};BasicTextField(value,changed,keyboardOptions=KeyboardOptions(showKeyboardOnFocus=false),cursorBrush=androidx.compose.ui.graphics.SolidColor(PcCyan),modifier=modifier.onFocusChanged{focused=it.isFocused}.onPreviewKeyEvent{if(it.type==KeyEventType.KeyUp&&it.key in listOf(Key.DirectionCenter,Key.Enter)){keyboard?.show();true}else false}.fillMaxWidth().background(if(focused)androidx.compose.ui.graphics.Color(0xFF2D3352)else c.raised,RoundedCornerShape(8.dp)).border(if(focused)2.dp else 1.dp,if(focused)PtvFocusBorder else androidx.compose.ui.graphics.SolidColor(c.border),RoundedCornerShape(8.dp)).padding(13.dp),singleLine=true,textStyle=TextStyle(color=c.text,fontSize=16.sp),visualTransformation=if(secret)PasswordVisualTransformation()else VisualTransformation.None,decorationBox={inner->Box{if(value.isEmpty())Text(if(focused)"$hint（按确认键输入）"else hint,color=if(focused)PcCyan else c.muted,fontSize=14.sp);inner()}})}

internal val LocalRailFocus=staticCompositionLocalOf<FocusRequester?>{null}
internal val LocalSearchRailFocus=staticCompositionLocalOf<FocusRequester?>{null}

@Composable private fun TvApp(m:PtvModel,voice:()->Unit){
    val pages=rememberSaveableStateHolder()
    val contentFocus=remember{FocusRequester()}
    val railFocus=remember{FocusRequester()}
    val searchRailFocus=remember{FocusRequester()}
    BackHandler{m.back()}
    CompositionLocalProvider(LocalRailFocus provides railFocus, LocalSearchRailFocus provides searchRailFocus){
    Box(Modifier.fillMaxSize().background(LocalCinema.current.background).onPreviewKeyEvent{if(it.type==KeyEventType.KeyDown&&it.key in listOf(Key.DirectionUp,Key.DirectionDown,Key.DirectionLeft,Key.DirectionRight))m.restoreFocus="";false}){
        Row(Modifier.fillMaxSize()){
            if(m.screen!="player")NavigationRail(m,Modifier.focusRequester(railFocus),searchRailFocus)
            Column(Modifier.weight(1f).fillMaxHeight().padding(if(m.screen in listOf("player","detail"))0.dp else 18.dp).focusRequester(contentFocus).focusRestorer()){
                Box(Modifier.weight(1f)){
                pages.SaveableStateProvider("${m.screen}|${if(m.screen=="detail")m.detail?.key else if(m.screen=="credits")m.creditPage055?.id else ""}"){
                    when(m.screen){
                        "player"->CinemaPlayer(m)
                        "detail"->if(m.detail?.provider=="live")LiveDetail050(m)else CinemaDetail040(m)
                        "search"->TvSearch055(m,voice)
                        "credits"->CreditWorksPage055(m)
                        "sites"->Sites040(m)
                        "live","sports"->LiveHub050(m)
                        "settings"->Settings040(m)
                        "favorites"->Collection050(m)
                        "history"->HistoryPage057(m)
                        "continue"->LazyColumn(Modifier.fillMaxSize()){item{if(m.user.continues.isEmpty())Text("暂无继续观看记录",color=LocalCinema.current.text)else ContinueRow(m)}}
                        "anime"->AnimePage056(m)
                        "library"->LibraryPage(m)
                        else->CinemaHome(m)
                    }
                }
            }
        }}

        if(m.notice.isNotEmpty()&&m.screen!="player")Text(m.notice,color=LocalCinema.current.text,fontSize=14.sp,modifier=Modifier.align(Alignment.TopEnd).padding(20.dp).widthIn(max=400.dp).background(LocalCinema.current.raised).padding(12.dp))
        if(m.screen!="player")NotificationGuideDialog102(m)
    }}
}

@Composable internal fun Poster(m:PtvModel,w:Work){CinemaPoster(m,w,if(m.screen=="home")"home"else m.screen)}
@Composable private fun Wall(m:PtvModel,rows:List<Work>,title:String="",header:@Composable ()->Unit={}){val state=rememberLazyGridState();LazyVerticalGrid(GridCells.Adaptive(138.dp),Modifier.fillMaxSize().pagedDown051(state,rows.map{it.key},{m.focusWork.takeIf{m.focusRow==m.screen}},m.busy,m.feedHasMore&&m.screen=="library"){m.loadFeed(true)},state=state,horizontalArrangement=Arrangement.spacedBy(16.dp),verticalArrangement=Arrangement.spacedBy(18.dp),contentPadding=PaddingValues(8.dp)){
    item(span={GridItemSpan(maxLineSpan)}){Column(verticalArrangement=Arrangement.spacedBy(12.dp)){if(title.isNotBlank())PcSection(title);header()}}
    items(rows,key={it.key}){Poster(m,it)}
    if(m.screen=="library")item(span={GridItemSpan(maxLineSpan)}){MoreHint051(m.busy,m.feedHasMore){m.loadFeed(true)}}
}}
@Composable private fun LibraryPage(m:PtvModel){var sort by rememberSaveable{mutableIntStateOf(0)};var year by rememberSaveable{mutableStateOf("")};var filter by remember{mutableStateOf(false)};val rows=CatalogPresentation.filtered(m.feed,year,sort,m.includeUpcoming);Wall(m,rows,if(m.kind=="movie")"电影"else"电视剧 / 专题"){
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)){DiscoveryTabs(m);CategoryTabs(m);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){PcChip("更多筛选${if(year.isNotBlank())" · $year"else""}",filter,{filter=!filter});PcChip(listOf("默认排序","日期正序 ↑","日期逆序 ↓")[sort],sort!=0,{sort=(sort+1)%3});PcChip("清除筛选",false,{year="";sort=0})};if(filter)FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){PcChip("全部年份",year.isBlank(),{year=""});m.feed.map{it.year}.filter{it.isNotBlank()}.distinct().sortedDescending().forEach{y->PcChip(y,year==y,{year=y})}};if(m.error.isNotBlank())Text(m.error,color=PcAccent,fontSize=12.sp);Text("${rows.size}部已加载作品",color=LocalCinema.current.muted,fontSize=12.sp)}
}}
@Composable private fun SearchPage(m:PtvModel,voice:()->Unit){var provider by rememberSaveable{mutableStateOf("")};Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
    PcSection("搜索影视","片名、原名、别名或已缓存片名的拼音首字母");Entry(m.query,{m.query=it},"片名 / 已缓存片名的拼音首字母")
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Action("搜索",{m.search()});Action("语音搜索",voice);Action("按资源站搜索",{m.searchSites(m.query)});PcChip("全部",provider.isBlank(),{provider=""});PcChip("豆瓣",provider=="douban",{provider="douban"});PcChip("TMDB",provider=="tmdb",{provider="tmdb"})}
    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){items(m.searchHistory){q->PcChip(q,false,{m.search(q)})}}
    Text(if(m.busy)"正在搜索…"else"${m.searchResults.size}个候选 · 请核对年份和季数",color=LocalCinema.current.muted,fontSize=12.sp)
    Box(Modifier.weight(1f)){SearchResults040(m,provider)}
}}
