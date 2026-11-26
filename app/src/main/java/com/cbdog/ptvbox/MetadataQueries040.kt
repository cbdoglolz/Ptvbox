package com.cbdog.ptvbox

import org.json.JSONObject
import org.json.JSONArray

object MetadataQueries040 {
    fun names(w:Work):List<String> = (listOf(w.title,w.original)+w.aliases).flatMap{listOf(it,Identity.searchTitle(it),it.substringBefore('：').substringBefore(':'))}.map{it.trim()}.filter{it.length>=2}.distinct().take(6)
    fun date(value:Any?):String {
        val items=when(value){is JSONArray->(0 until value.length()).map{value.optString(it)};is String->listOf(value);else->emptyList()}
        fun parsed(s:String):String?{val m=Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(s)?:return null;return runCatching{java.time.LocalDate.of(m.groupValues[1].toInt(),m.groupValues[2].toInt(),m.groupValues[3].toInt()).toString()}.getOrNull()}
        val preferred=items.filter{it.contains("中国大陆")}.ifEmpty{items}
        return preferred.mapNotNull(::parsed).minOrNull().orEmpty()
    }
}
suspend fun Metadata.doubanFullText(query:String,kind:String?=null):List<Work> = DoubanSearch055.search(query).filter{kind==null||it.kind==kind}
