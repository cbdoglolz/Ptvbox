package com.cbdog.ptvbox

import org.json.JSONObject

enum class ContentRouteType(val id:String,val label:String,val defaults:List<String>){
    NETFLIX("netflix","Netflix 作品",listOf("国际高清","私人订阅","默认点播","动画")),
    CN_TV("cn_tv","中文剧集",listOf("私人订阅","默认点播","国际高清","动画")),
    CN_MOVIE("cn_movie","中文电影",listOf("私人订阅","默认点播","国际高清","动画")),
    JP_KR("jp_kr","日韩影视",listOf("私人订阅","国际高清","默认点播","动画")),
    WESTERN("western","欧美影视",listOf("国际高清","私人订阅","默认点播","动画")),
    ANIME("anime","动画",listOf("动画","私人订阅","国际高清","默认点播")),
    DOC_VARIETY("doc_variety","纪录片/综艺",listOf("私人订阅","默认点播","国际高清","动画")),
    UNKNOWN("unknown","未识别",listOf("私人订阅","默认点播","国际高清","动画"))
}

internal object SourceRoutePolicy {
    private const val PREF="source_routes_v1"

    fun isNetflix(work:Work):Boolean=work.provider.equals("netflix",true)||work.facts.companies.any{it.name.contains("netflix",true)}

    fun classify(work:Work):ContentRouteType{
        val genres=work.genres.joinToString("|")
        if(work.provider=="anilist"||Regex("动画|动漫|anime|animation",RegexOption.IGNORE_CASE).containsMatchIn(genres))return ContentRouteType.ANIME
        if(isNetflix(work))return ContentRouteType.NETFLIX
        if(Regex("纪录片|综艺|documentary|reality|variety",RegexOption.IGNORE_CASE).containsMatchIn(genres))return ContentRouteType.DOC_VARIETY
        val languages=(work.facts.languages+work.facts.countries).joinToString("|").lowercase()
        if(Regex("日本|日语|japan|japanese|jp|韩国|韩语|korea|korean|kr").containsMatchIn(languages))return ContentRouteType.JP_KR
        if(Regex("中国|大陆|台湾|香港|中文|汉语|普通话|粤语|china|chinese|mandarin|cantonese").containsMatchIn(languages))return if(work.kind=="tv")ContentRouteType.CN_TV else ContentRouteType.CN_MOVIE
        if(languages.isNotBlank())return ContentRouteType.WESTERN
        return ContentRouteType.UNKNOWN
    }

    fun groupOrder(type:ContentRouteType,settings:TvPreferences,availableGroups:List<String> = emptyList()):List<String>{
        val saved=runCatching{JSONObject(settings.text(PREF,"{}")).optJSONArray(type.id)?.let{a->(0 until a.length()).map{a.optString(it)}.filter{it.isNotBlank()}}}.getOrNull().orEmpty()
        return (saved+type.defaults+availableGroups).filter{it.isNotBlank()}.distinct()
    }

    fun saveGroupOrder(type:ContentRouteType,order:List<String>,settings:TvPreferences){
        val root=runCatching{JSONObject(settings.text(PREF,"{}"))}.getOrDefault(JSONObject())
        root.put(type.id,org.json.JSONArray(order.distinct()))
        settings.set(PREF,root.toString())
    }

    fun moveGroup(type:ContentRouteType,group:String,delta:Int,settings:TvPreferences,availableGroups:List<String>):List<String>{
        val order=groupOrder(type,settings,availableGroups).toMutableList()
        val from=order.indexOf(group)
        if(from>=0){
            val to=(from+delta).coerceIn(0,order.lastIndex)
            if(to!=from){val value=order.removeAt(from);order.add(to,value);saveGroupOrder(type,order,settings)}
        }
        return order
    }

    fun reset(type:ContentRouteType,settings:TvPreferences,availableGroups:List<String>):List<String>{
        val root=runCatching{JSONObject(settings.text(PREF,"{}"))}.getOrDefault(JSONObject())
        root.remove(type.id);settings.set(PREF,root.toString())
        return groupOrder(type,settings,availableGroups)
    }

    fun order(work:Work?,sites:List<Site>,settings:TvPreferences):List<Site>{
        val groups=sites.map{it.group}.distinct()
        val type=work?.let(::classify)?:ContentRouteType.UNKNOWN
        val order=groupOrder(type,settings,groups)
        val ranks=order.withIndex().associate{it.value to it.index}
        return sites.sortedWith(compareBy<Site>{ranks[it.group]?:999}.thenBy{it.name})
    }

    fun rankMap(work:Work?,sites:List<Site>,settings:TvPreferences):Map<String,Int>{
        val groups=sites.map{it.group}.distinct()
        val type=work?.let(::classify)?:ContentRouteType.UNKNOWN
        val order=groupOrder(type,settings,groups)
        val ranks=order.withIndex().associate{it.value to it.index}
        return sites.associate{it.key to (ranks[it.group]?:999)}
    }
}
