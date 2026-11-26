package com.cbdog.ptvbox
import org.json.JSONObject
import org.json.JSONArray
object EpisodeRail055 {
 fun outline(id:String,name:String,original:String,seasons:List<SeasonMeta>,number:Int)=SeriesMeta(id,name,original,seasons,(1..(seasons.firstOrNull{it.number==number}?.count?:0).coerceIn(0,2000)).map{EpisodeMeta("outline-$number-$it",number,it,"第${it}集","","","-",0,"",true)})
 fun json(s:SeriesMeta)=JSONObject().put("id",s.id).put("name",s.name).put("original",s.original).put("seasons",JSONArray(s.seasons.map{JSONObject().put("number",it.number).put("name",it.name).put("year",it.year).put("count",it.count).put("poster",it.poster).put("overview",it.overview).put("airDate",it.airDate)})).put("episodes",JSONArray(s.episodes.filterNot{it.pending}.map{JSONObject().put("id",it.id).put("season",it.season).put("number",it.number).put("title",it.title).put("overview",it.overview).put("still",it.still).put("rating",it.rating).put("votes",it.votes).put("airDate",it.airDate).put("runtime",it.runtime)}))
 fun from(j:JSONObject)=SeriesMeta(j.getString("id"),j.getString("name"),j.optString("original"),j.getJSONArray("seasons").objects().map{SeasonMeta(it.getInt("number"),it.getString("name"),it.optString("year"),it.getInt("count"),it.optString("poster"),it.optString("overview"),it.optString("airDate"))},j.getJSONArray("episodes").objects().map{EpisodeMeta(it.getString("id"),it.getInt("season"),it.getInt("number"),it.getString("title"),it.optString("overview"),it.optString("still"),it.optString("rating","-"),it.optInt("votes"),it.optString("airDate"),runtime=it.optInt("runtime",0))})
}
