package com.cbdog.ptvbox

import org.json.JSONObject

data class SeasonMeta(val number:Int,val name:String,val year:String,val count:Int,val poster:String="",val overview:String="",val airDate:String="")
data class EpisodeMeta(val id:String,val season:Int,val number:Int,val title:String,val overview:String,val still:String,val rating:String,val votes:Int,val airDate:String,val pending:Boolean=false,val runtime:Int=0)
data class SeriesMeta(val id:String,val name:String,val original:String,val seasons:List<SeasonMeta>,val episodes:List<EpisodeMeta>)
data class EpisodeMark(val favorite:Boolean=false,val watched:Boolean=false,val watchState:WatchState?=if(watched)WatchState.COMPLETED else null)
fun episodeRef(w:Work,number:Int)="${w.key}@${w.season}@regular:$number"
object TmdbEpisodeParser {
    fun episodes(json:JSONObject):List<EpisodeMeta> = json.optJSONArray("episodes")?.objects()?.map{j->val votes=j.optInt("vote_count");EpisodeMeta(j.optString("id"),j.optInt("season_number"),j.optInt("episode_number"),j.optString("name"),j.optString("overview"),j.optString("still_path").takeIf{it.startsWith('/')}?.let{"https://image.tmdb.org/t/p/w500$it"}.orEmpty(),if(votes>0)j.optDouble("vote_average").let{"%.1f".format(java.util.Locale.ROOT,it)}else"-",votes,j.optString("air_date").takeUnless{it=="null"}.orEmpty(),runtime=j.optInt("runtime",0))}.orEmpty()
}
internal object TmdbSeasonResolver101 {
    fun pickSeason(work:Work,requested:Int,seasons:List<SeasonMeta>):Int {
        val regular=seasons.filter{it.number>0}
        if(regular.isEmpty())return requested
        val nf=Identity.nianfan(work.title)?:work.aliases.firstNotNullOfOrNull(Identity::nianfan)
        if(nf==null&&regular.any{it.number==requested})return requested
        if(work.year.length==4){
            regular.firstOrNull{it.year==work.year}?.let{return it.number}
            val targetYear=work.year.toIntOrNull()
            if(targetYear!=null){
                regular.filter{it.year.toIntOrNull()!=null}.minByOrNull{kotlin.math.abs(it.year.toInt()-targetYear)}?.takeIf{kotlin.math.abs(it.year.toInt()-targetYear)<=1}?.let{return it.number}
            }
        }
        if(nf!=null&&nf>0)regular.firstOrNull{it.number==nf}?.let{return it.number}
        return regular.firstOrNull{it.number==requested}?.number?:regular.last().number
    }
}
