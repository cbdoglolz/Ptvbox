package com.cbdog.ptvbox

import android.app.Application
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class SiteHealth040(val stage:String="未检测",val description:String="",val responding:Boolean?=null)

fun PtvModel.checkSites(rows:List<Site>){
    stationCheck?.cancel();stationChecked=0;stationTotal=rows.size
    stationCheck=viewModelScope.launch{
        stationChecking=true;val gate=Semaphore(config.parallel.coerceAtMost(6))
        try{rows.map{site->launch{gate.withPermit{
            health=health+(site.key to SiteHealth040("正在检测"))
            val status=when{
                site.type==8->try{withTimeout(config.timeout*1000L){withContext(Dispatchers.IO){Net.json(site.api.trimEnd('/')+"/manifest.json")}};SiteHealth040("接口可响应","Stremio清单可读；播放需作品ID与实际媒体检测",true)}catch(e:Exception){SiteHealth040("连接失败","Stremio清单暂不可读，现用配置保留",false)}
                !sites.supports(site)->SiteHealth040("待适配","配置已读取；当前Android引擎不支持此规则",null)
                !site.searchable->SiteHealth040("配置可读","订阅标记为不参与点播搜索；未执行播放检测",null)
                else->try{val result=withTimeout(config.timeout*1000L+4000){withContext(Dispatchers.IO){sites.search(site,"西游记")}};SiteHealth040("接口可响应","返回${result.size}项；解析与首帧尚未验证",true)}catch(e:CancellationException){if(e !is TimeoutCancellationException)throw e;SiteHealth040("超时","当前配置保留，可尝试检查订阅更新",false)}catch(e:Exception){SiteHealth040("连接/解析失败",Regex("HTTP \\d{3}").find(e.message.orEmpty())?.value?:"接口未返回可识别数据；现用配置保留",false)}
            }
            health=health+(site.key to status);stationChecked++
        }}}.joinAll()}finally{stationChecking=false}
    }
}
fun PtvModel.checkSourceUpdate(){
    if(sourceUpdating)return
    viewModelScope.launch{sourceUpdating=true;sourceUpdateText="正在读取订阅，尚未替换现用配置…";try{
        val preview=withContext(Dispatchers.IO){SourceMaintenance(db).checkSubscription()};sourcePreview=preview.takeIf{it.changedAnything};sourceUpdateText=if(preview.changedAnything)preview.description else"订阅内容没有变化，不需要更新"
    }catch(e:Exception){sourceUpdateText="检查失败：${Regex("HTTP \\d{3}").find(e.message.orEmpty())?.value?:"无法读取或验证订阅"}；现用站点保留"}finally{sourceUpdating=false}}
}
fun PtvModel.activateSourceUpdate(){val preview=sourcePreview?:return;viewModelScope.launch{try{withContext(Dispatchers.IO){SourceMaintenance(db).activate(preview)};configurationChanged();sourcePreview=null;sourceUpdateText="订阅更新已激活，开关与优先级保留；可回退上一份配置"}catch(_:Exception){sourceUpdateText="激活失败，现用配置保留；请重新检查"}}}
fun PtvModel.rollbackSources(){viewModelScope.launch{try{withContext(Dispatchers.IO){SourceMaintenance(db).rollback()};configurationChanged();sourcePreview=null;sourceUpdateText="已回退上一份订阅配置，用户开关与数据保留"}catch(_:Exception){sourceUpdateText="没有可用的上一份订阅，未更改现用配置"}}}
fun PtvModel.checkCloudSources(){if(sourceUpdating)return;viewModelScope.launch{sourceUpdating=true;sourceUpdateText="正在验证云端清单签名…";try{val cloud=withContext(Dispatchers.IO){SourceMaintenance(db).cloud(getApplication<Application>())};sourceUpdateText="签名已验证 · ${cloud.revision.take(12)}\n"+(cloud.components+cloud.health).joinToString("\n")+"\n此清单属于PC动画规则包，Android执行器尚未接入；可以预览更新详情，不会替换现用源。上游连接状态不等于本机播放实测。"}catch(e:Exception){sourceUpdateText="云端检查失败：${Regex("HTTP \\d{3}").find(e.message.orEmpty())?.value?:"权限、网络或签名验证失败"}；现用配置保留"}finally{sourceUpdating=false}}}
fun PtvModel.clearImageCache(){viewModelScope.launch{val loader=coil3.SingletonImageLoader.get(getApplication<Application>());withContext(Dispatchers.IO){loader.memoryCache?.clear();loader.diskCache?.clear()};message("图片与临时缓存已清理；收藏、进度、密钥和订阅保留")}}
