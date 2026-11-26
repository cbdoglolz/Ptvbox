package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

fun PtvModel.bulkEnable(rows:List<Site>,enabled:Boolean){
    db.prefs.edit().apply{rows.forEach{putBoolean("site_${it.key}",enabled)}}.apply()
    enabledSiteKeys=if(enabled)enabledSiteKeys+rows.map{it.key} else enabledSiteKeys-rows.map{it.key}.toSet()
}
@Composable internal fun Sites040(m:PtvModel){
    var group by rememberSaveable{mutableStateOf("")};var query by rememberSaveable{mutableStateOf("")};var updates by remember{mutableStateOf(false)};var routes by remember{mutableStateOf(false)};var routeType by rememberSaveable{mutableStateOf(ContentRouteType.CN_TV)}
    val all=m.availableSites;val groups=remember(all){(listOf("私人订阅","默认点播","国际高清","动画")+all.map{it.group}).distinct()}
    val rows=remember(all,group,query){all.filter{(group.isBlank()||it.group==group)&&(query.isBlank()||it.name.contains(query,true))}}
    val c=LocalCinema.current
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)){
        PcSection("站点 · ${all.size}个","${rows.size}个当前显示 · 并发${m.config.parallel} · 检测不自动更改启用状态")
        FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            PcChip("全部",group.isBlank(),{group=""});groups.forEach{g->PcChip("$g ${all.count{it.group==g}}",group==g,{group=g})}
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Entry(query,{query=it},"按站点名称筛选")};Action("分类选源",{routes=true});Action("检测当前组",{m.checkSites(rows)});Action("检测全部",{m.checkSites(all)});Action("更新 / 回退",{updates=true})}
        if(group.isNotBlank())Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Text("分组优先级 ${m.config.groupPriority(group)}（数字越小越先搜索）",color=PcAccent,fontSize=12.sp)
            PcChip("提高",false,{m.settingChanged("priority_$group",(m.config.groupPriority(group)-1).coerceAtLeast(0))});PcChip("降低",false,{m.settingChanged("priority_$group",(m.config.groupPriority(group)+1).coerceAtMost(9))});m.preferencesRevision
            Action("整组启用",{m.bulkEnable(rows,true)});Action("整组关闭",{m.bulkEnable(rows,false)})
        }
        if(m.stationChecking)Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){Text("检测 ${m.stationChecked}/${m.stationTotal}",color=PcAccent,fontSize=13.sp);Action("取消检测",{m.stationCheck?.cancel();m.stationChecking=false})}
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp),contentPadding=PaddingValues(2.dp,2.dp,8.dp,14.dp)){
            items(rows,key={it.key}){site->val enabled=m.siteEnabled(site);val status=m.health[site.key]
                Row(Modifier.fillMaxWidth().background(c.surface,RoundedCornerShape(7.dp)).padding(10.dp,7.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    PcChip(if(enabled)"已启用"else"已关闭",enabled,{m.toggleSite(site)},Modifier.width(86.dp).heightIn(min=48.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)){Text(site.name,color=if(enabled)c.text else c.muted,fontSize=15.sp,maxLines=2);if(status!=null)Text(status.stage,color=if(status.responding==true)PcGreen else PcAccent,fontSize=11.sp)}
                        Text(status?.description?:"${site.group} · ${if(!site.searchable)"不参与点播搜索"else if(site.type==3&&site.api.startsWith("csp_"))"TVBox Jar"else if(site.type==3)"JS待适配"else"MacCMS / HTTP"}",color=c.muted,fontSize=11.sp,maxLines=2)
                    }
                    PcChip("检测",false,{m.checkSites(listOf(site))},Modifier.heightIn(min=48.dp))
                }
            }
        }
    }
    if(updates)Dialog({updates=false},properties=DialogProperties(usePlatformDefaultWidth=false)){
        Column(Modifier.widthIn(max=860.dp).fillMaxWidth(.9f).heightIn(max=530.dp).background(c.surface,RoundedCornerShape(12.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            PcSection("更新源配置","先预览变更，再确认激活；失败与取消均保留现用源")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                Action("检查私人订阅更新",m::checkSourceUpdate);Action("检查云端发布信息",m::checkCloudSources)
                if(m.sourcePreview!=null){Action("确认激活订阅",m::activateSourceUpdate);Action("取消本次更新",{m.sourcePreview=null;m.sourceUpdateText="已取消，现用订阅保持"})}
                Action("回退上一份订阅",m::rollbackSources);Action("关闭",{updates=false})
            }
            Text("内置默认点播源随签名APK更新；PC动画规则已接入本机解析器。云端规则更新仍需先验签预览，再由用户确认激活。",color=c.muted,fontSize=12.sp)
            LazyColumn(verticalArrangement=Arrangement.spacedBy(7.dp)){items(m.sourceUpdateText.lines()){line->Text(line,color=PcAccent,fontSize=12.sp)}}
        }
    }
    if(routes)Dialog({routes=false},properties=DialogProperties(usePlatformDefaultWidth=false)){
        val availableGroups=remember(all){all.map{it.group}.filter{it.isNotBlank()}.distinct()}
        val order=remember(routeType,m.preferencesRevision,availableGroups){SourceRoutePolicy.groupOrder(routeType,m.config,availableGroups)}
        Column(Modifier.widthIn(max=760.dp).fillMaxWidth(.9f).heightIn(max=560.dp).background(c.surface,RoundedCornerShape(12.dp)).padding(16.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            PcSection("分类选源","不同类型按自己的分组顺序搜索；关闭的站点始终排除。")
            FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                ContentRouteType.entries.forEach{type->PcChip(type.label,routeType==type,{routeType=type})}
            }
            Text("${routeType.label} · 上一组没有可用候选才进入下一组。",color=c.muted,fontSize=11.sp)
            LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                itemsIndexed(order){index,groupName->
                    Row(Modifier.fillMaxWidth().background(c.raised,RoundedCornerShape(7.dp)).padding(9.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        Text("${index+1}. $groupName",color=c.text,fontSize=13.sp,modifier=Modifier.weight(1f))
                        Text("${all.count{it.group==groupName}}站",color=c.muted,fontSize=11.sp)
                        PcChip("上移",false,{SourceRoutePolicy.moveGroup(routeType,groupName,-1,m.config,availableGroups);m.preferencesRevision++})
                        PcChip("下移",false,{SourceRoutePolicy.moveGroup(routeType,groupName,1,m.config,availableGroups);m.preferencesRevision++})
                    }
                }
            }
            FlowRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                Action("恢复本类默认",{SourceRoutePolicy.reset(routeType,m.config,availableGroups);m.preferencesRevision++})
                Action("关闭",{routes=false})
            }
        }
    }
}
