package com.cbdog.ptvbox
object DecodeSupport052 {
 val main10:Boolean by lazy {runCatching{android.media.MediaCodecList(android.media.MediaCodecList.REGULAR_CODECS).codecInfos.filter{!it.isEncoder&&it.supportedTypes.any{t->t.equals("video/hevc",true)}}.any{info->info.getCapabilitiesForType("video/hevc").profileLevels.any{it.profile in listOf(2,4096,8192)}}}.getOrDefault(false)}
 fun compatible(label:String,hasMain10:Boolean):Boolean {val hevc=Regex("(?i)(hevc|h[ ._-]?265|x265)").containsMatchIn(label);val ten=Regex("(?i)(10[ ._-]?bit|hdr|dolby|\\bdv\\b)").containsMatchIn(label);return !hevc||!ten||hasMain10}
 fun compatible(c:Candidate)=compatible(c.road,main10)
}
