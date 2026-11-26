package com.cbdog.ptvbox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Version0510Test {
    private fun source(path:String)=listOf(java.io.File(path),java.io.File("app/$path")).first{it.exists()}.readText()

    @Test fun detailKeepsRequestedBannerAndActionOrder(){
        val detail=source("src/main/java/com/cbdog/ptvbox/CinemaDetail040.kt")
        assertTrue(detail.indexOf("ProviderRatingRows(m,w)")<detail.indexOf("art?.logo"))
        assertTrue(detail.indexOf("\"刷新全部资料\"")<detail.indexOf("\"校正季集关联\""))
        assertFalse(detail.contains("item(key=\"overview\""))
    }

    @Test fun episodeCardsUseSixteenByNineMediaAndExternalSynopsis(){
        val episode=source("src/main/java/com/cbdog/ptvbox/EpisodeCard054.kt")
        val season=source("src/main/java/com/cbdog/ptvbox/SeasonEpisodes040.kt")
        assertTrue(episode.contains("aspectRatio(16f/9f)"))
        assertFalse(episode.contains("Text(ep.overview"))
        assertTrue(season.contains("focusedEpisode?.let{episode->EpisodeSynopsis060"))
    }
}
