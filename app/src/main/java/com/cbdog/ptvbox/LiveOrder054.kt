package com.cbdog.ptvbox
object LiveOrder054 {
 fun order(keys:List<String>,saved:List<String>):List<String> = (saved.filter{it in keys}+keys).distinct()
 fun move(keys:List<String>,key:String,direction:Int):List<String>{val out=keys.toMutableList();val from=out.indexOf(key);val to=from+direction;if(from>=0&&to in out.indices){val value=out.removeAt(from);out.add(to,value)};return out}
 fun activeClass(category:String,ancestors:List<String>,id:String)=category==id||id in ancestors
 fun defaultClass(classes:List<Pair<String,String>>,platform:String="")=classes.firstOrNull{it.second.contains(platform.ifBlank{"虎牙"})}?.first?:classes.firstOrNull()?.first.orEmpty()
}
