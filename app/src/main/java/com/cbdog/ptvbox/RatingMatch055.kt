package com.cbdog.ptvbox
object RatingMatch055 {
 fun matches(w:Work,hit:Work,names:List<String>,scoped:Boolean=false):Boolean {
  val hitSeason=Identity.season(hit.title)?:if(w.kind=="tv")Identity.trailingSeason(hit.title)else null
  val workSeason=if(w.season>1)w.season else if(w.kind=="tv")(Identity.season(w.title)?:Identity.trailingSeason(w.title)?:1)else 1
  val nianfan=Identity.isNianfan(w)
  if(hit.kind!=w.kind)return false
  if(!nianfan){
   if(w.kind=="tv"&&hitSeason!=null&&hitSeason!=workSeason||scoped&&hitSeason!=workSeason)return false
   val yearDiff=if(w.year.length==4&&hit.year.length==4)kotlin.math.abs((w.year.toIntOrNull()?:0)-(hit.year.toIntOrNull()?:0))else 0
   val allowYearGap=(w.kind=="tv"&&workSeason>1)||(w.provider=="anilist"&&yearDiff<=1)
   if(w.year.isNotBlank()&&hit.year.isNotBlank()&&hit.year!=w.year&&!allowYearGap)return false
  }else if(w.kind=="tv"&&hitSeason!=null){
   val nf=Identity.nianfan(w.title)?:0
   if(nf>0&&hitSeason!=nf&&hitSeason!=w.season)return false
  }
  fun title(value:String)=if(w.kind=="movie")Identity.normalize(value)else Identity.base(Identity.stripTrailingSeason(value))
  return names.any{n->(listOf(hit.title,hit.original)+hit.aliases).filter{it.isNotBlank()}.any{title(n)==title(it)}}
 }
}
