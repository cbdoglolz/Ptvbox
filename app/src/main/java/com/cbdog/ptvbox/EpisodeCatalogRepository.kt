package com.cbdog.ptvbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

data class CatalogGroup(val key:String,val site:Site,val title:String,val road:Road)
data class CatalogState(val workKey:String="",val groups:List<CatalogGroup> = emptyList(),val checked:Int=0,val total:Int=0,val loading:Boolean=false,val failed:Int=0)
internal object DoubanEpisodeFallback055 {
    fun series(w:Work):SeriesMeta? {
        if(w.kind!="tv"||w.episodeCount<=0)return null
        val count=w.episodeCount.coerceIn(1,500)
        val season=SeasonMeta(w.season,if(w.season>1)"第${w.season}季" else w.title,w.year,count,w.poster,w.overview,w.releaseDate)
        val episodes=(1..count).map{n->EpisodeMeta("douban-season-${w.season}-episode-$n",w.season,n,"第%02d集".format(n),"","","-",0,"",runtime=0)}
        return SeriesMeta("douban:${w.id}:${w.season}",w.title,w.original,listOf(season),episodes)
    }
}

class EpisodeCatalogRepository(private val sites:Sites,private val db:Library){
    private val cache=mutableMapOf<String,List<CatalogGroup>>()
    fun observe(work:Work,refresh:Boolean=false,internationalOnly:Boolean=false):Flow<CatalogState> = channelFlow {
        if(work.releaseStatus=="NOT_YET_RELEASED"){send(CatalogState(work.key));return@channelFlow}
        val enabled=sites.ordered(work).filter{sites.enabled(it)&&it.searchable&&!LiveParser050.isPortal(it)&&sites.supports(it)&&(!internationalOnly||it.type in listOf(8,10))};val gate=Semaphore(sites.parallel.coerceAtMost(8))
        val previous=(if(refresh)db.loadCatalog(work,enabled)else cache[work.catalogKey()]?:db.loadCatalog(work,enabled)).filter{sites.enabled(it.site)}
        var groups=previous;var checked=0;var failed=0
        send(CatalogState(work.key,groups,0,enabled.size,true))
        enabled.map{site->launch{
            val found=gate.withPermit{
                try{withTimeout(12000){
                    var hits=if(site.type==8)listOf(Stremio050.lookup(site,work,if(work.kind=="tv")"第01集"else"正片",Metadata(db)))else sites.search(site,work.title)
                    if(site.type!=8&&hits.none{Identity.matches(work,it)})for(alias in Identity.searchQueries(work).filter{it!=work.title}.take(3))if(hits.none{Identity.matches(work,it)})hits=hits+sites.search(site,alias)
                    hits.filter{Identity.matches(work,it)}.distinctBy{it.id}.sortedByDescending{Identity.matchScore(work,it)}.take(3).flatMap{row->val vod=sites.detail(site,row);vod.roads.map{road->CatalogGroup("${site.key}|${vod.id}|${road.name}",site,vod.title,road)}}
                }}catch(e:TimeoutCancellationException){failed++;emptyList()}catch(e:CancellationException){throw e}catch(e:Exception){failed++;emptyList()}
            }
            // Aggregate on the flow's single dispatcher, never on a rendering callback.
            synchronized(cache){groups=((if(sites.enabled(site))found else emptyList())+groups).filter{sites.enabled(it.site)}.distinctBy{it.key};checked++;cache[work.catalogKey()]=groups
                trySend(CatalogState(work.key,groups,checked,enabled.size,checked<enabled.size,failed))}
        }}.joinAll()
        db.saveCatalog(work,groups)
        send(CatalogState(work.key,groups,checked,enabled.size,false,failed))
    }.flowOn(Dispatchers.IO.limitedParallelism(1))
}

class DetailViewModel(private val metadata:Metadata,private val catalogs:EpisodeCatalogRepository):ViewModel(){
    private val mutableWork=MutableStateFlow<Work?>(null);val work=mutableWork.asStateFlow()
    private val mutableCatalog=MutableStateFlow(CatalogState());val catalog=mutableCatalog.asStateFlow()
    private val mutableRelated=MutableStateFlow<List<Work>>(emptyList());val related=mutableRelated.asStateFlow()
    private val mutableCollection=MutableStateFlow<List<Work>>(emptyList());val collection=mutableCollection.asStateFlow()
    private val mutableVersions=MutableStateFlow<List<Work>>(emptyList());val versions=mutableVersions.asStateFlow()
    private val mutableArtwork=MutableStateFlow(0);val artwork=mutableArtwork.asStateFlow()
    private val mutableBusy=MutableStateFlow(false);val refreshing=mutableBusy.asStateFlow()
    private var relatedPage=1;private var relatedBusy=false
    private val relatedLoadingState=MutableStateFlow(false);val relatedLoading=relatedLoadingState.asStateFlow()
    private val relatedMoreState=MutableStateFlow(true);val relatedMore=relatedMoreState.asStateFlow()
    private val mutableSeries=MutableStateFlow<SeriesMeta?>(null);val series=mutableSeries.asStateFlow()
    private val mutableSeriesStatus=MutableStateFlow("");val seriesStatus=mutableSeriesStatus.asStateFlow()
    private val mutableSeriesLoading=MutableStateFlow(false);val seriesLoading=mutableSeriesLoading.asStateFlow()
    private var seriesTask:Job?=null
    private var manualSeriesId:String?=null
    private var task:Job?=null;private var catalogTask:Job?=null;private var request=0
    private fun seasonal(w:Work):Work{if(w.kind!="tv"||w.season==w.catalogSeason)return w;val season=mutableSeries.value?.seasons?.firstOrNull{it.number==w.season}?:return w;return w.copy(year=season.year.ifBlank{w.year},releaseDate=season.airDate.ifBlank{w.releaseDate},poster=season.poster.ifBlank{w.poster})}
    fun load(w:Work){cancelLoad();manualSeriesId=null;val token=request;mutableWork.value=w;mutableRelated.value=emptyList();mutableCollection.value=emptyList();mutableVersions.value=emptyList();relatedPage=1;relatedBusy=false;relatedLoadingState.value=false;relatedMoreState.value=true;mutableSeries.value=null;mutableCatalog.value=CatalogState(w.key,loading=true);loadSeries(w)
        if(metadata.preloadCatalog)catalogTask=viewModelScope.launch{catalogs.observe(w).collect{if(token==request)mutableCatalog.value=it}}else mutableCatalog.value=CatalogState(w.key)
        task=viewModelScope.launch{
            mutableBusy.value=true
            try{val fresh=withContext(Dispatchers.IO){metadata.details(w)};if(token==request){mutableWork.value=seasonal(fresh);for(p in listOf("douban","tmdb")){if(p!=fresh.provider){val extra=withContext(Dispatchers.IO){runCatching{metadata.ratingWork(fresh,p)}.getOrNull()};if(token==request&&extra!=null)mutableWork.value=mutableWork.value?.enriched(extra)?.let(::seasonal)}};if(fresh.kind!=w.kind||fresh.releaseStatus!=w.releaseStatus||fresh.episodeCount!=w.episodeCount||(fresh.aliases-w.aliases.toSet()).isNotEmpty()||fresh.original!=w.original){if(metadata.preloadCatalog||mutableCatalog.value.total>0)refresh();loadSeries(fresh,manualSeriesId)}}}catch(e:CancellationException){throw e}catch(e:Exception){/* Existing metadata stays visible. */}
            try{val rows=withContext(Dispatchers.IO){metadata.moreRelated(mutableWork.value?:w,1)};if(token==request)mutableRelated.value=rows}catch(e:CancellationException){throw e}catch(e:Exception){/* Retain existing recommendations on a failed refresh. */}
            val current=mutableWork.value?:w
            runCatching{withContext(Dispatchers.IO){metadata.collection(current)}}.onSuccess{if(token==request)mutableCollection.value=it}
            runCatching{withContext(Dispatchers.IO){metadata.versions(current)}}.onSuccess{if(token==request)mutableVersions.value=it}
            if(token==request)mutableBusy.value=false
        }
    }
    fun refreshAll(){val w=mutableWork.value?:return;metadata.invalidateDetail(w);mutableArtwork.value++;load(w);refresh()}
    fun enrich(linked:Work){val w=mutableWork.value?:return;val token=request;mutableWork.value=seasonal(w.enriched(linked));viewModelScope.launch{runCatching{withContext(Dispatchers.IO){metadata.collection(w.enriched(linked))}}.onSuccess{if(token==request&&mutableWork.value?.key==w.key)mutableCollection.value=it}}}
    fun moreSimilar(){val w=mutableWork.value?:return;if(relatedBusy||!relatedMoreState.value)return;relatedBusy=true;relatedLoadingState.value=true;val token=request;viewModelScope.launch{try{val rows=withContext(Dispatchers.IO){metadata.moreRelated(w,relatedPage+1)};if(token==request){relatedMoreState.value=rows.any{r->mutableRelated.value.none{it.key==r.key}};relatedPage++;mutableRelated.value=(mutableRelated.value+rows).distinctBy{row->row.key}}}catch(e:CancellationException){throw e}catch(_:Exception){}finally{if(token==request){relatedBusy=false;relatedLoadingState.value=false}}}}
    fun refresh(internationalOnly:Boolean=false){val w=mutableWork.value?:return;catalogTask?.cancel();val token=request;catalogTask=viewModelScope.launch{catalogs.observe(w,true,internationalOnly).collect{if(token==request)mutableCatalog.value=it}}}
    internal fun doubanSeries(w:Work)=DoubanEpisodeFallback055.series(w)
    private fun loadSeries(w:Work,id:String?=null){seriesTask?.cancel();val token=request;mutableSeriesLoading.value=w.kind=="tv";mutableSeriesStatus.value=if(w.kind=="tv")"优先加载中文片名与海报，单集资料随后加载…"else"";seriesTask=viewModelScope.launch{
        try{if(w.kind!="tv")return@launch
            if(id?.startsWith("douban:")==true){val fallback=doubanSeries(w);if(token==request&&fallback!=null){mutableSeries.value=fallback;mutableSeriesStatus.value="豆瓣目录 · 第${w.season}季 · ${fallback.episodes.size}集 · 无分集剧照时显示集数占位"};return@launch}
            val cached=withContext(Dispatchers.IO){runCatching{val f=metadata.episodeCache055(w);if(f.isFile&&f.length()<2_000_000)EpisodeRail055.from(org.json.JSONObject(f.readText()))else null}.getOrNull()}
            if(token!=request)return@launch
            if(cached!=null&&(id==null||id==cached.id))mutableSeries.value=cached
            if(cached==null)delay(800)
            val tmdbResult=withContext(Dispatchers.IO){metadata.episodeSeries(w,w.season,id){outline->if(token==request){val old=mutableSeries.value?.takeIf{it.id==outline.id}?.episodes.orEmpty().associateBy{it.number};mutableSeries.value=outline.copy(episodes=outline.episodes.map{old[it.number]?:it})}}}
            val fallback=if(tmdbResult==null&&id==null)doubanSeries(w)else null
            val result=tmdbResult?:fallback
            if(token==request){if(result!=null){mutableSeries.value=result;withContext(Dispatchers.IO){runCatching{metadata.episodeCache055(w).writeText(EpisodeRail055.json(result).toString())}}};mutableWork.value=mutableWork.value?.let(::seasonal);mutableSeriesStatus.value=when{result==null&&w.isUpcoming()->"尚未开播 · 正片集数未公布，不使用其他季目录";result==null->"豆瓣与TMDB均未返回可用集数；仍可从普通来源目录选择";tmdbResult!=null->"TMDB · 第${w.season}季 · ${result.episodes.size}集 · 点击卡片播放";else->"豆瓣目录 · 第${w.season}季 · ${result.episodes.size}集 · 无分集剧照，使用集数占位"}}
        }catch(e:CancellationException){throw e}catch(_:Exception){if(token==request)mutableSeriesStatus.value="季集资料暂不可用，保留已有目录，可刷新重试"}finally{if(token==request)mutableSeriesLoading.value=false}
    }}
    fun clearSeries(){seriesTask?.cancel();manualSeriesId=null;mutableSeries.value=null;mutableSeriesStatus.value="已清除TMDB关联，可随时手动匹配"}
    fun linkSeries(id:String){if(!id.matches(Regex("[0-9]+"))){mutableSeriesStatus.value="请输入数字TMDB剧集ID";return};manualSeriesId=id;mutableWork.value?.let{loadSeries(it,id)}}
    fun selectSeason(number:Int){val w=mutableWork.value?:return;val s=mutableSeries.value?:return;val season=s.seasons.firstOrNull{it.number==number}?:return;cancelLoad();val target=w.copy(season=number,title=s.name+" 第${number}季",original=s.original,year=season.year,releaseDate=season.airDate,aliases=(w.aliases+listOf(w.title,s.name)).distinct());mutableWork.value=target;mutableSeries.value=EpisodeRail055.outline(s.id,s.name,s.original,s.seasons,number);loadSeries(target,s.id);refresh()}
    fun cancelLoad(){request++;task?.cancel();catalogTask?.cancel();seriesTask?.cancel()}
    fun dispose(){cancelLoad();viewModelScope.cancel()}
}
