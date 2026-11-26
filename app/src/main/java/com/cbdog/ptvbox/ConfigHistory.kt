package com.cbdog.ptvbox

import org.json.JSONArray

/** Encrypted, bounded key history. Values never enter preferences or diagnostics. */
internal object ConfigHistory {
    fun items(db:Library,name:String):List<String>{
        val raw=db.secrets.read("history_$name")
        return runCatching{JSONArray(raw).let{a->(0 until a.length()).map{a.optString(it)}.filter{it.isNotBlank()}}}.getOrDefault(emptyList())
    }
    fun remember(db:Library,name:String,value:String){
        val next=(listOf(value.trim())+items(db,name).filter{it!=value.trim()}).filter{it.isNotBlank()}.distinct().take(8)
        db.secrets.save("history_$name",JSONArray(next).toString())
    }
    fun remove(db:Library,name:String,value:String){
        val next=items(db,name).filter{it!=value}
        db.secrets.save("history_$name",JSONArray(next).toString())
    }
}
