package com.cbdog.ptvbox
object Variant052 {
 fun key(road:String)=Identity.normalize(road.replace(Regex("\\s*[·|]\\s*#\\d+\\s*$"),""))
 fun same(a:String,b:String)=key(a)==key(b)
 fun previousId(w:Watch?):String?=w?.sourceId?.split('|',limit=3)?.takeIf{it.size==3&&it[0]==w.siteKey}?.get(1)?.takeIf{it.isNotBlank()}
 fun order(sites:List<Site>,watch:Watch?,priority:(String)->Int)=sites.sortedWith(compareBy<Site>{if(it.key==watch?.siteKey)0 else 1}.thenBy{priority(it.group)})
}
