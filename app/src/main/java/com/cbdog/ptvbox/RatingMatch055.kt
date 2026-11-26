package com.cbdog.ptvbox
object RatingMatch055 {
 fun matches(w:Work,hit:Work,names:List<String>,scoped:Boolean=false):Boolean {
  val season=Identity.season(hit.title)
  if(hit.kind!=w.kind||w.kind=="tv"&&season!=null&&season!=w.season||scoped&&season!=w.season)return false
  if(w.year.isNotBlank()&&hit.year!=w.year&&!(w.kind=="tv"&&w.season>1))return false
  fun title(value:String)=if(w.kind=="movie")Identity.normalize(value)else Identity.base(value)
  return names.any{n->(listOf(hit.title,hit.original)+hit.aliases).filter{it.isNotBlank()}.any{title(n)==title(it)}}
 }
}
