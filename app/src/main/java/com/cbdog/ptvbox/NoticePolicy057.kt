package com.cbdog.ptvbox

internal object NoticePolicy057 {
 private val pluginNoise=Regex("过滤广告|广告视频|扫码观看|请扫码|需要扫码授权|免费资源|请勿上当|跳过.{0,12}广告|扫描.{0,8}网盘|网盘.{0,8}二维码",RegexOption.IGNORE_CASE)
 private val privateSourceNoise=Regex("已过滤视频中广告|网盘源需使用.{0,20}扫码|百度网盘APP扫码|此?资源失效|饭\\s*[：:]",RegexOption.IGNORE_CASE)
 fun suppressPluginNoise(value:String,searching:Boolean,loading:Boolean,hasCandidate:Boolean)=privateSourceNoise.containsMatchIn(value)||pluginNoise.containsMatchIn(value)&&(searching||loading||!hasCandidate)
}
