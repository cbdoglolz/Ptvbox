package com.cbdog.ptvbox

import org.junit.Assert.assertTrue
import org.junit.Test

class Version059Test {
    private fun source(path:String)=listOf(java.io.File(path),java.io.File("app/$path")).first{it.exists()}.readText()

    @Test fun detailRouteUsesTheImplementedDetailPage(){
        val activity=source("src/main/java/com/cbdog/ptvbox/MainActivity.kt")
        assertTrue(activity.contains("\"detail\"->if(m.detail?.provider==\"live\")LiveDetail050(m)else CinemaDetail040(m)"))
        val detail=source("src/main/java/com/cbdog/ptvbox/CinemaDetail040.kt")
        assertTrue(detail.indexOf("item(key=\"hero-actions\"")<detail.indexOf("item(key=\"sources-title\""))
        assertTrue(detail.contains("SeasonEpisodes040(m,w)"))
    }

    @Test fun episodeFocusOutlineScalesWithWholeCard(){
        val episode=source("src/main/java/com/cbdog/ptvbox/EpisodeCard054.kt")
        assertTrue(episode.contains("animateFloatAsState(if(focused)1.035f else 1f"))
        assertTrue(episode.contains("drawWithContent"))
        assertTrue(episode.contains("drawRoundRect(brush=SelectionBrush051"))
    }
}
