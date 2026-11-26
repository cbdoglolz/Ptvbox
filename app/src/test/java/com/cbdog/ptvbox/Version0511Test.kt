package com.cbdog.ptvbox

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Version0511Test {
    private fun source(path:String)=listOf(java.io.File(path),java.io.File("app/$path")).first{it.exists()}.readText()

    @Test fun doubanEpisodeCountBuildsSelectableFallback(){
        val work=Work("38405325","刃牙道 第二期","tv","2026",provider="douban",season=2,episodeCount=13)
        val restored=Work.from(JSONObject(work.json().toString()))
        assertEquals(13,restored.episodeCount)
        val fallback=DoubanEpisodeFallback055.series(restored)!!
        assertEquals(13,fallback.episodes.size)
        assertEquals("第03集",fallback.episodes[2].title)
        assertEquals(2,fallback.seasons.single().number)
    }

    @Test fun relatedAndCreditPagesKeepSixColumnRowsAndPaging(){
        val detail=source("src/main/java/com/cbdog/ptvbox/CinemaDetail040.kt")
        val credit=source("src/main/java/com/cbdog/ptvbox/CreditPage055.kt")
        assertTrue(detail.contains("related.chunked(5)"))
        assertTrue(detail.contains("m.details.moreSimilar()"))
        assertTrue(credit.contains("GridCells.Fixed(6)"))
        assertTrue(credit.contains("pagedDown051"))
    }

    @Test fun playerEpisodeRowsExposeNameRatingAndDuration(){
        val player=source("src/main/java/com/cbdog/ptvbox/CinemaPlayer.kt")
        assertTrue(player.contains("评分未公开"))
        assertTrue(player.contains("时长未公开"))
        assertTrue(player.contains("episodeSeries?.episodes"))
    }

    @Test fun compactEpisodeCardUsesOnlyRoundWatchMarker(){
        val episode=source("src/main/java/com/cbdog/ptvbox/EpisodeCard054.kt")
        assertFalse(episode.contains("收藏第${'$'}{ep.number}集"))
        assertTrue(episode.contains("CircleShape"))
        assertTrue(episode.contains("size(34.dp)"))
    }
}
