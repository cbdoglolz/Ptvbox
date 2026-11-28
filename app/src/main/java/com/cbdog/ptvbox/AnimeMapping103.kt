package com.cbdog.ptvbox

import org.json.JSONObject
import java.time.LocalDate

internal data class AnimeCoordinate103(val anilist:String,val tmdb:String,val season:Int,val offset:Int,val logicalSeason:Int=0,val endOffset:Int?=null)

/** Provider identity, display season and source episode number are separate coordinates. */
internal object AnimeMapping103 {
    fun parse(raw:String):List<AnimeCoordinate103> {
        val rows=JSONObject(raw).getJSONArray("rows")
        return (0 until rows.length()).mapNotNull{i->val a=rows.getJSONArray(i);val season=a.optInt(2);val offset=a.optInt(3)
            if(season<=0||offset<0)null else AnimeCoordinate103(a.optString(0),a.optString(1),season,offset,a.optInt(4))
        }
    }
    fun lookup(rows:List<AnimeCoordinate103>,w:Work):AnimeCoordinate103? {
        if(w.provider!="anilist"||w.kind!="tv")return null
        val mapped=rows.filter{it.anilist==w.id}.singleOrNull()?:return null
        val end=rows.filter{it.tmdb==mapped.tmdb&&it.season==mapped.season&&it.offset>mapped.offset}.minOfOrNull{it.offset}
        return mapped.copy(endOffset=end)
    }
    fun installment(w:Work)=w.kind=="tv"&&(w.provider=="anilist"||w.genres.any{Regex("动画|动漫|anime|animation",RegexOption.IGNORE_CASE).containsMatchIn(it)})&&w.provider!="tmdb"
    fun titleSeason(names:List<String>):Int=names.firstNotNullOfOrNull{Identity.season(it)?:Identity.trailingSeason(it)}?:1
    fun project(w:Work,series:SeriesMeta,map:AnimeCoordinate103?):SeriesMeta? {
        if(!installment(w))return series
        val all=series.episodes.sortedBy{it.number}
        val chosen:List<EpisodeMeta>
        var offset=map?.offset?:0
        if(map!=null){
            chosen=all.filter{it.season==map.season&&it.number>offset&&(map.endOffset==null||it.number<=map.endOffset)}.let{if(w.episodeCount>0)it.take(w.episodeCount)else it}
        }else{
            // Air-date blocks are a fallback for entries not covered by the ID map; never use S1 as S3.
            val blocks=mutableListOf<MutableList<EpisodeMeta>>()
            for(ep in all){val current=blocks.lastOrNull();val previous=current?.lastOrNull();val gap=runCatching{LocalDate.parse(ep.airDate).toEpochDay()-LocalDate.parse(previous?.airDate).toEpochDay()}.getOrDefault(0)
                if(current==null||gap>=60&&current.size>=6)blocks+=mutableListOf(ep)else current+=ep
            }
            val byYear=blocks.filter{b->w.year.length==4&&b.firstOrNull()?.airDate?.startsWith(w.year)==true}
            val block=if(byYear.size==1)byYear.single()else if(w.season==1)blocks.firstOrNull()else blocks.getOrNull(w.season-1)
            if(block==null)return null
            if(w.year.length==4&&block.firstOrNull()?.airDate?.take(4)?.takeIf{it.isNotBlank()}?.let{it!=w.year}==true)return null
            if(w.season>1&&blocks.size==1&&block.firstOrNull()?.season!=w.season)return null
            chosen=block.let{if(w.episodeCount>0)it.take(w.episodeCount)else it}
            offset=(chosen.firstOrNull()?.number?:1)-1
        }
        if(chosen.isEmpty())return if(map!=null)project(w,series,null)else null
        val visible=chosen.map{it.copy(season=w.season,number=it.number-offset)}
        val season=SeasonMeta(w.season,"第${w.season}季",w.year,visible.size,w.poster,w.overview,w.releaseDate)
        return series.copy(name=w.title,original=w.original,seasons=listOf(season),episodes=visible)
    }
}
