package com.cbdog.ptvbox
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text
data class BrowseReturn055(val screen:String,val work:Work?,val credit:CreditPage055?,val row:String,val focus:String)
class CreditPage055(val owner:Work,val person:PersonCredit?=null,val company:CompanyCredit?=null){
 val id get()=person?.let{"person:${it.provider}:${it.id}:${it.name}"}?:"company:${company?.role}:${company?.id}"
 val title get()=person?.name?:company?.name.orEmpty()
 var rows by mutableStateOf<List<Work>>(emptyList());var loading by mutableStateOf(false);var error by mutableStateOf("");var page=0;var more by mutableStateOf(true)
}
@Composable internal fun CreditWorksPage055(m:PtvModel){val page=m.creditPage055?:return;val grid=rememberLazyGridState();Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(12.dp)){
 PcSection(page.title,if(page.person!=null)"演职人员参与作品"else page.company?.role.orEmpty()){PcChip("← 返回原作品",false,m::back);PcChip("刷新作品",false,{m.loadCredit055(true)})}
 if(page.error.isNotBlank())Text(page.error,color=PcAccent)
 if(!m.tmdbConfigured&&page.person?.provider!="douban")PcChip("配置TMDB Key",false,{m.settingsSection="metadata";m.utility("settings")})
 Text(if(page.loading)"正在加载作品与评分…"else"${page.rows.size}部作品 · 海报和评分逐步补全",color=LocalCinema.current.muted)
 LazyVerticalGrid(GridCells.Fixed(6),state=grid,modifier=Modifier.weight(1f).pagedDown051(grid,page.rows.map{it.key},{m.focusWork.takeIf{m.focusRow=="credit-${page.id}"}},page.loading,page.more){m.loadCredit055()},horizontalArrangement=Arrangement.spacedBy(12.dp),verticalArrangement=Arrangement.spacedBy(16.dp),contentPadding=PaddingValues(5.dp)){
  items(page.rows,key={it.key}){CinemaPoster(m,it,"credit-${page.id}")}
  if(page.rows.isEmpty()&&!page.loading)item(span={GridItemSpan(maxLineSpan)}){PcEmpty("暂无作品",page.error.ifBlank{"提供方尚未返回作品，可刷新重试；原作品关联保留。"})}
  if(page.rows.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}){MoreHint051(page.loading,page.more){m.loadCredit055()}}
 }
}}
