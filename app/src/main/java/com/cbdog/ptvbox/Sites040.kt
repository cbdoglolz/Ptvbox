package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text

fun PtvModel.bulkEnable(rows:List<Site>,enabled:Boolean){
    db.prefs.edit().apply{rows.forEach{putBoolean("site_${it.key}",enabled)}}.apply()
    enabledSiteKeys=if(enabled)enabledSiteKeys+rows.map{it.key} else enabledSiteKeys-rows.map{it.key}.toSet()
}

@Composable internal fun Sites040(m:PtvModel){
    var filter by rememberSaveable{mutableStateOf("")} // "", "good", "bad", or group name
    var updates by remember{mutableStateOf(false)}
    var routes by remember{mutableStateOf(false)}
    var routeType by rememberSaveable{mutableStateOf(ContentRouteType.CN_TV)}
    val all=m.availableSites
    val scoreRev=m.siteScoresRevision
    val enabledSet=m.enabledSiteKeys
    val groups=remember(all){(listOf("私人订阅","默认点播","国际高清","动画")+all.map{it.group}).distinct()}
    val scoreMap=remember(all,scoreRev){all.associate{it.key to SiteScores102.score(m.db,it.key)}}
    val goodCount=remember(scoreMap){scoreMap.values.count{it>0.01f}}
    val badCount=remember(scoreMap){scoreMap.values.count{it<-0.01f}}
    val enabledCount=remember(all,enabledSet){all.count{it.key in enabledSet}}
    val rows=remember(all,filter,scoreMap,enabledSet){
        val filtered=all.filter{site->
            val score=scoreMap[site.key]?:0f
            when(filter){
                ""->true
                "good"->score>0.01f
                "bad"->score<-0.01f
                else->site.group==filter
            }
        }
        SiteScores102.sortSites(filtered,{scoreMap[it.key]?:0f},{it.key in enabledSet})
    }
    val c=LocalCinema.current
    Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Column(Modifier.weight(1f)){
                Text("站点 · ${all.size}个",color=c.text,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                Text("启用 $enabledCount · 好站 $goodCount · 差站 $badCount · 按评分降序（有源+1 / 待扫+0.5 / 无源-1）",color=c.muted,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            PcChip("分类选源",false,{routes=true})
            PcChip("检测当前",false,{m.checkSites(rows)})
            PcChip("更新 / 回退",false,{updates=true})
            if(goodCount+badCount>0)PcChip("清零计分",false,{m.resetSiteScores()})
        }
        FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            PcChip("全部 ${all.size}",filter.isBlank(),{filter=""})
            PcChip("好站 $goodCount",filter=="good",{filter="good"})
            PcChip("差站 $badCount",filter=="bad",{filter="bad"})
            groups.forEach{g->PcChip("$g ${all.count{it.group==g}}",filter==g,{filter=g})}
        }
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
            m.preferencesRevision
            val stopCount=m.config.sourceStopCount
            Text("自动停扫：",color=c.muted,fontSize=11.sp)
            listOf(1 to "1条",2 to "2条",3 to "3条",5 to "5条",8 to "8条",0 to "不限制").forEach{(n,label)->
                PcChip(label,stopCount==n,{m.settingChanged("source_stop_count",n)})
            }
            Text("（找到可用源即停止同波扫描）",color=c.muted.copy(alpha=.75f),fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        if(filter.isNotBlank()&&filter!="good"&&filter!="bad")Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(7.dp)){
            Text("分组优先级 ${m.config.groupPriority(filter)}（越小越先搜）",color=PcAccent,fontSize=11.sp)
            PcChip("提高",false,{m.settingChanged("priority_$filter",(m.config.groupPriority(filter)-1).coerceAtLeast(0))})
            PcChip("降低",false,{m.settingChanged("priority_$filter",(m.config.groupPriority(filter)+1).coerceAtMost(9))});m.preferencesRevision
            PcChip("整组启用",false,{m.bulkEnable(rows,true)})
            PcChip("整组关闭",false,{m.bulkEnable(rows,false)})
        }
        if(filter=="bad"&&rows.isNotEmpty())Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Text("共 ${rows.size} 个负分站点，可一键批量关闭以加快搜源速度",color=Color(0xFFFCA5A5),fontSize=11.sp)
            PcChip("关闭全部负分站",false,{m.bulkEnable(rows,false);m.message("已关闭 ${rows.size} 个负分站点")})
        }
        if(m.stationChecking)Row(horizontalArrangement=Arrangement.spacedBy(10.dp),verticalAlignment=Alignment.CenterVertically){
            Text("正在检测 ${m.stationChecked}/${m.stationTotal}",color=PcAccent,fontSize=12.sp)
            PcChip("取消检测",false,{m.stationCheck?.cancel();m.stationChecking=false})
        }
        LazyVerticalGrid(
            columns=GridCells.Fixed(3),
            modifier=Modifier.weight(1f),
            horizontalArrangement=Arrangement.spacedBy(8.dp),
            verticalArrangement=Arrangement.spacedBy(7.dp),
            contentPadding=PaddingValues(2.dp,2.dp,6.dp,12.dp)
        ){
            items(rows,key={it.key}){site->
                val enabled=site.key in enabledSet
                val status=m.health[site.key]
                val score=scoreMap[site.key]?:0f
                val badge=SiteScores102.badgeStyle(score)
                val engineLabel=when{
                    status!=null->status.stage
                    !site.searchable->"非点播"
                    site.type==3&&site.api.startsWith("csp_")->"Jar"
                    site.type==3->"JS"
                    site.type==9->"规则"
                    else->"HTTP"
                }
                Column(
                    Modifier.fillMaxWidth()
                        .background(if(enabled)c.surface else c.surface.copy(alpha=.52f),RoundedCornerShape(8.dp))
                        .border(1.dp,if(enabled)c.border.copy(alpha=.85f)else c.border.copy(alpha=.32f),RoundedCornerShape(8.dp))
                        .padding(horizontal=10.dp,vertical=7.dp),
                    verticalArrangement=Arrangement.spacedBy(5.dp)
                ){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        Text(
                            site.name,
                            color=if(enabled)c.text else c.muted.copy(alpha=.65f),
                            fontSize=13.sp,
                            fontWeight=FontWeight.Medium,
                            maxLines=1,
                            overflow=TextOverflow.Ellipsis,
                            modifier=Modifier.weight(1f)
                        )
                        Box(
                            Modifier.background(badge.background,RoundedCornerShape(5.dp))
                                .border(1.dp,badge.border,RoundedCornerShape(5.dp))
                                .padding(horizontal=6.dp,vertical=1.dp)
                        ){
                            Text(SiteScores102.format(score),color=badge.text,fontSize=10.sp,fontWeight=FontWeight.Bold,maxLines=1)
                        }
                    }
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)){
                        Text(
                            "${site.group} · $engineLabel",
                            color=if(status?.responding==true)PcGreen else c.muted,
                            fontSize=10.sp,
                            maxLines=1,
                            overflow=TextOverflow.Ellipsis,
                            modifier=Modifier.weight(1f)
                        )
                        InputButton(
                            {m.checkSites(listOf(site))},
                            Modifier.border(1.dp,c.border.copy(alpha=.7f),RoundedCornerShape(5.dp)),
                            shape=ButtonDefaults.shape(RoundedCornerShape(5.dp)),
                            focusRadius=5.dp,
                            colors=ptvButtonColors(c.raised.copy(alpha=.75f),c.text.copy(alpha=.85f)),
                            contentPadding=PaddingValues(horizontal=7.dp,vertical=3.dp)
                        ){Text("测",fontSize=10.sp,maxLines=1)}
                        InputButton(
                            {m.toggleSite(site)},
                            Modifier.border(1.dp,if(enabled)PcGreen.copy(alpha=.7f)else c.border,RoundedCornerShape(14.dp)),
                            shape=ButtonDefaults.shape(RoundedCornerShape(14.dp)),
                            focusRadius=14.dp,
                            colors=ptvButtonColors(if(enabled)Color(0x332EE08C)else c.raised,if(enabled)PcGreen else c.muted),
                            contentPadding=PaddingValues(horizontal=7.dp,vertical=3.dp)
                        ){
                            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
                                Box(Modifier.width(20.dp).height(11.dp).background(if(enabled)PcGreen.copy(alpha=.45f)else c.border,RoundedCornerShape(6.dp)).padding(1.5.dp)){
                                    Box(Modifier.size(8.dp).align(if(enabled)Alignment.CenterEnd else Alignment.CenterStart).background(if(enabled)PcGreen else c.muted,CircleShape))
                                }
                                Text(if(enabled)"开"else"关",color=if(enabled)PcGreen else c.muted,fontSize=10.sp)
                            }
                        }
                    }
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
