package com.cbdog.ptvbox

object ResourceLabels040 {
    fun bytes(text:String):Long {
        val m=Regex("(?i)([0-9]+(?:\\.[0-9]+)?)\\s*(GiB|GB|MiB|MB|TiB|TB)\\b").find(text)?:return 0
        val scale=when(m.groupValues[2].lowercase()){ "mib","mb"->1024.0*1024;"tib","tb"->1024.0*1024*1024*1024;else->1024.0*1024*1024}
        return (m.groupValues[1].toDouble()*scale).takeIf{it>0&&it<Long.MAX_VALUE}?.toLong()?:0
    }
    fun height(text:String)=Regex("(?i)\\b(480|576|720|1080|1440|2160|4320)[pi]\\b").find(text)?.groupValues?.get(1)?.toInt()?:if(text.contains(Regex("(?i)\\b4k\\b")))2160 else 0
    fun tags(text:String)=buildList{if(text.contains(Regex("(?i)\\bHDR(?:10\\+?)?\\b")))add("HDR");if(text.contains(Regex("(?i)Dolby[ ._-]*Vision|\\bDV\\b")))add("DOLBY VISION");if(text.contains(Regex("(?i)Atmos")))add("ATMOS")}
    fun size(bytes:Long)=if(bytes<=0)""else if(bytes>=1024L*1024*1024)"%.2f GB".format(java.util.Locale.ROOT,bytes/(1024.0*1024*1024))else"%.0f MB".format(java.util.Locale.ROOT,bytes/(1024.0*1024))
    fun label(group:CatalogGroup)=group.road.name+" "+group.road.episodes.firstOrNull()?.name.orEmpty()
}
