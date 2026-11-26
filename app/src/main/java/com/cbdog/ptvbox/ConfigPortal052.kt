package com.cbdog.ptvbox
import org.json.*

data class ConfigField052(val id:String,val section:String,val title:String,val kind:String="secret",val default:String="",val choices:List<String> = emptyList())
object ConfigPortal052 {
 val fields=listOf(
  ConfigField052("tmdb","metadata","TMDB Key / Read Token"),
  ConfigField052("subscription","configuration","私人TVBox点播订阅地址","link"),ConfigField052("live_subscription","configuration","直播订阅地址","link"),
  ConfigField052("subdl","subtitles","SubDL Key"),ConfigField052("assrt","subtitles","ASSRT Token"),ConfigField052("opensub_key","subtitles","OpenSubtitles API Key"),ConfigField052("opensub_user","subtitles","OpenSubtitles账号"),ConfigField052("opensub_password","subtitles","OpenSubtitles密码"),
  ConfigField052("danmaku_url","danmaku","自建弹幕服务地址","link"),ConfigField052("danmaku_appid","danmaku","弹弹play AppId"),ConfigField052("danmaku_secret","danmaku","弹弹play AppSecret"),
  ConfigField052("danmaku_size","danmaku","弹幕字号","int","20",listOf("12","16","20","24","28","32","40")),ConfigField052("danmaku_alpha","danmaku","弹幕不透明度","int","80",listOf("40","60","80","100")),ConfigField052("danmaku_density","danmaku","弹幕密度","int","12",listOf("4","8","12","16")),ConfigField052("danmaku_duration","danmaku","弹幕横穿秒数","int","8",listOf("6","8","10","12")),ConfigField052("danmaku_travel_percent","danmaku","弹幕横向飞行距离","int","100",listOf("33","50","67","75","100")),ConfigField052("danmaku_color","danmaku","弹幕颜色","choice","#FFFFFF",listOf("#FFFFFF","#FFF176","#80DEEA","#A5D6A7")),
  ConfigField052("source_update_token","sites","GitHub访问Token"),ConfigField052("source_update_repository","sites","源维护仓库","text","cbdoglolz/r-9b04e274ef5e41aa"),
  ConfigField052("proxy","network","网络代理地址","link"),ConfigField052("user_agent","network","User-Agent","text",NetworkPolicy.userAgent),
  ConfigField052("secondary_rating","metadata","同时显示其他评分","bool","true"),ConfigField052("metadata_hint_hidden","metadata","不再提示配置TMDB","bool","false"),
  ConfigField052("light_theme","interface","浅色主题","bool","false"),ConfigField052("drawer_pinned","interface","始终显示导航文字","bool","false"),ConfigField052("preload_catalog","interface","详情预加载普通线路","bool","false"),
  ConfigField052("search_display","interface","搜索结果样式","choice","sites",listOf("sites","groups")),
  ConfigField052("source_preference","playback","自动选源偏好","choice","speed",listOf("speed","quality")),
  ConfigField052("search_parallel","playback","搜索并发","int","12",listOf("4","6","10","12","20")),
  ConfigField052("startup_timeout","playback","起播超时（秒）","int","30",listOf("5","10","15","30","60")),
  ConfigField052("resize_choice","playback","画面比例（0自适应/1拉伸/2裁切）","int","0",listOf("0","1","2")),
  ConfigField052("auto_skip","playback","自动跳过片头片尾","bool","false"),ConfigField052("skip_intro","playback","默认片头（秒）","int","0",listOf("0","30","60","90","120")),ConfigField052("skip_outro","playback","默认片尾（秒）","int","0",listOf("0","30","60","90","120")),
  ConfigField052("background_playback","playback","允许系统后台播放","bool","false"),
  ConfigField052("subtitle_size","subtitles","内嵌字幕字号","int","24",listOf("16","20","24","28","32","40")),ConfigField052("subtitle_font","subtitles","内嵌字幕字体","choice","sans-serif",listOf("sans-serif","serif","monospace")),ConfigField052("subtitle_bold","subtitles","内嵌字幕粗体","bool","true"),ConfigField052("subtitle_style_override","subtitles","自定义内嵌字幕样式","bool","false"),ConfigField052("subtitle_color","subtitles","内嵌字幕颜色","choice","#FFFFFF",listOf("#FFFFFF","#FFF176","#80DEEA","#A5D6A7")),ConfigField052("subtitle_alpha","subtitles","字幕文字不透明度","int","100",listOf("40","60","80","100")),ConfigField052("subtitle_background_alpha","subtitles","字幕背景不透明度","int","40",listOf("0","20","40","60","80","100")),
  ConfigField052("image_cache_mb","network","图片缓存上限MiB","int","256",listOf("64","128","256","512","1024")),
  ConfigField052("site_timeout","network","站点超时（秒）","int","15",listOf("5","10","15","30","60")),ConfigField052("doh","network","安全DNS（DoH）","bool","false")
 )
 val secretStorage=(SettingsInventory.serviceSecrets+listOf("tmdb","subscription","live_subscription","proxy")).toSet()
 fun value(db:Library,f:ConfigField052):String=if(f.id in secretStorage)db.secrets.read(f.id).ifBlank{if(f.id=="tmdb")db.secrets.read("tmdb_pending")else ""}else when(f.kind){"bool"->db.prefs.getBoolean(f.id,f.default=="true").toString();"int"->db.prefs.getInt(f.id,f.default.toInt()).toString();else->db.prefs.getString(f.id,f.default).orEmpty()}
 fun snapshot(db:Library,budget:TvBudget?=null):JSONObject {val values=fields.map{f->val raw=if(!db.prefs.contains(f.id)&&budget!=null&&f.id=="search_parallel")budget.parallel.toString()else if(!db.prefs.contains(f.id)&&budget!=null&&f.id=="image_cache_mb")budget.imageMb.toString()else value(db,f);JSONObject().put("id",f.id).put("section",f.section).put("title",f.title).put("kind",f.kind).put("choices",JSONArray(f.choices)).put("saved",raw.isNotBlank()).put("value",raw)};return JSONObject().put("tmdbStatus",db.prefs.getString("tmdb_status","")).put("fields",JSONArray(values)).put("pages",JSONArray(SettingsInventory.portalSections.filter{it.id!="remote"}.map{JSONObject().put("id",it.id).put("title",it.title)}))}
 fun localValues(db:Library)=snapshot(db).getJSONArray("fields").objects().associate{it.getString("id") to it.getString("value")}
 fun validate(f:ConfigField052,v:String){require(v.length<=8192&&!v.contains('\r')&&!v.contains('\n'));if(f.kind=="bool")require(v in listOf("true","false"));if(f.choices.isNotEmpty())require(v in f.choices);if(f.id in listOf("subscription","live_subscription","danmaku_url")&&v.isNotBlank()){val u=java.net.URI(v);require(u.scheme in listOf("https","http")&&u.host!=null)};if(f.id=="proxy")NetworkPolicy.validateProxy(v);if(f.id=="user_agent")require(v.length in 1..512);if(f.id=="source_update_repository")require(v.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")))}
}
