package com.cbdog.ptvbox

import java.math.BigDecimal
import java.util.UUID

enum class WatchState { PLANNED, WATCHING, COMPLETED }
data class MediaId(val value:String=UUID.randomUUID().toString())
data class EpisodeId(val value:String=UUID.randomUUID().toString())
data class ExternalId(val provider:String,val kind:String,val id:String)
object ProgressUnits {
    fun seconds(milliseconds:Long):String=BigDecimal.valueOf(milliseconds,3).toPlainString()
    fun milliseconds(seconds:String):Long=BigDecimal(seconds).movePointRight(3).longValueExact()
}
data class UserSnapshot(
    val favorites:Set<String> = emptySet(),val watches:List<Watch> = emptyList(),
    val states:Map<String,WatchState> = emptyMap(),val hidden:Set<String> = emptySet(),val episodeMarks:Map<String,EpisodeMark> = emptyMap()
){
    val lastByWork by lazy { watches.distinctBy{it.work.key}.associateBy{it.work.key} }
    val continues get()=watches.filter{!it.done&&it.position>0&&it.work.key !in hidden&&states[it.work.key]!=WatchState.COMPLETED}.distinctBy{it.work.key}
}
