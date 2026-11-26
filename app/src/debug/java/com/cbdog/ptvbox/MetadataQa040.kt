package com.cbdog.ptvbox
import android.content.Context
import kotlinx.coroutines.*
import org.json.JSONObject
object MetadataQa040 {
 fun run(context:Context):JSONObject=runBlocking {
  val db=Library(context);val out=JSONObject();val meta=Metadata(db);val probe=Work("qa040-link","刃牙道 第二季","tv",season=2,provider="qa")
  val keys=listOf(probe,probe.copy(season=3)).flatMap{w->listOf("douban","tmdb").map{meta.ratingStorageKey(w,it)}}.distinct();val prior=keys.associateWith{db.prefs.getString(it,null)}
  try {
   val id=withTimeout(30000){meta.providerCandidates(probe,"douban","38405325")}.single();check(id.id=="38405325");out.put("doubanNumericId",true).put("title",id.title)
   val names=withTimeout(40000){meta.providerCandidates(probe,"douban","刃牙道")};check(names.any{it.id=="38405325"}){"Douban title result missing expected ID"};out.put("doubanTitleFindsExpectedId",true)
   val alias=withTimeout(40000){meta.providerCandidates(probe,"douban","无敌武士")};check(alias.any{it.id=="38405325"}){"Douban alias result missing expected ID"};out.put("doubanAliasFindsExpectedId",true)
   meta.saveRatingLink(probe,"douban",id.id);meta.saveRatingLink(probe,"tmdb","1");meta.clearRatingLink(probe,"tmdb");check(meta.ratingWork(probe,"tmdb")==null);val linked=meta.ratingWork(probe,"douban")!!;check(linked.id==id.id&&linked.votes>0&&linked.rating!="-");out.put("ratingsAndVotesImmediatelyAvailable",true).put("clearOnePreservesOther",true)
   meta.saveRatingLink(probe.copy(season=3),"douban","123");check(db.prefs.getString(meta.ratingStorageKey(probe,"douban"),null)==id.id);out.put("crossSeasonDoubanLinksIndependent",true)
   out
  } finally { db.prefs.edit().apply{prior.forEach{(k,v)->if(v==null)remove(k)else putString(k,v)}}.commit();db.close() }
 }
}
