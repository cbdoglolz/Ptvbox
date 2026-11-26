package com.cbdog.ptvbox

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences

/** Android policy behind the PC-style settings; no credentials live in these preferences. */
data class TvBudget(val name:String,val parallel:Int,val imageMb:Int,val bufferMb:Int,val imageFraction:Double){
    companion object {
        fun resolve(choice:String,lowRam:Boolean,memoryClass:Int,totalRamMb:Long=4096L):TvBudget = when {
            choice=="economy" || choice=="auto"&&(lowRam||memoryClass<=192||totalRamMb in 1..2300)->TvBudget("轻量",4,128,24,.06)
            choice=="performance"->TvBudget("高性能",16,512,64,.12)
            choice=="auto"&&totalRamMb in 2301..4200->TvBudget("电视均衡",6,256,32,.08)
            else->TvBudget("均衡",10,256,48,.10)
        }
    }
}
class TvPreferences(val context:Context){
    val prefs:SharedPreferences=context.getSharedPreferences("preferences",Context.MODE_PRIVATE)
    val budget:TvBudget get(){val am=context.getSystemService(ActivityManager::class.java);val mi=ActivityManager.MemoryInfo();runCatching{am.getMemoryInfo(mi)};val totalMb=if(mi.totalMem>0)mi.totalMem/(1024L*1024L)else 4096L;return TvBudget.resolve(text("device_profile","auto"),am.isLowRamDevice,am.memoryClass,totalMb)}
    fun text(key:String,default:String="")=runCatching{prefs.getString(key,default)?:default}.getOrDefault(default)
    fun flag(key:String,default:Boolean=false)=runCatching{prefs.getBoolean(key,default)}.getOrDefault(default)
    fun number(key:String,default:Int)=runCatching{prefs.getInt(key,default)}.getOrDefault(default)
    fun set(key:String,value:String){prefs.edit().putString(key,value).apply()}
    fun set(key:String,value:Boolean){prefs.edit().putBoolean(key,value).apply()}
    fun set(key:String,value:Int){prefs.edit().putInt(key,value).apply()}
    val parallel get()=number("search_parallel",budget.parallel).coerceIn(1,20)
    val sourceStopCount get()=number("source_stop_count",3).coerceIn(0,20)
    val audioPrimaryLang get()=text("audio_lang_primary","zh")
    val audioSecondaryLang get()=text("audio_lang_secondary","en")
    val subtitlePrimaryLang get()=text("subtitle_lang_primary","zh")
    val subtitleSecondaryLang get()=text("subtitle_lang_secondary","en")
    val imageMb get()=number("image_cache_mb",budget.imageMb).coerceIn(64,1024)
    val timeout get()=number("site_timeout",15).coerceIn(5,60)
    val startupSeconds get()=number("startup_timeout",30).coerceIn(5,60)
    val seekStepMs get()=number("seek_step_seconds",5).coerceIn(5,30)*1000L
    val speed get()=1f
    val background get()=flag("background_playback")
    val incognito get()=flag("incognito")
    fun groupPriority(group:String)=number("priority_$group",if(group=="私人订阅")0 else if(group=="默认点播")1 else 2)
    fun sorted(sites:List<Site>)=sites.sortedBy{groupPriority(it.group)}
}

data class SettingSection(val id:String,val title:String)
object SettingsInventory {
    val sections=listOf(SettingSection("remote","手机扫码局域网"),
        SettingSection("metadata","影视资料"),
        SettingSection("interface","界面"),SettingSection("configuration","点播与直播配置"),
        SettingSection("playback","播放与自动选源"),SettingSection("subtitles","字幕服务"),
        SettingSection("danmaku","弹幕"),
        SettingSection("network","网络与缓存"),SettingSection("about","关于 Ptvbox"))
    val portalSections=sections+SettingSection("sites","站点与云端源维护")
    val serviceSecrets=listOf("subdl","assrt","opensub_key","opensub_user","opensub_password","danmaku_appid","danmaku_secret","danmaku_url","source_update_token")
}
