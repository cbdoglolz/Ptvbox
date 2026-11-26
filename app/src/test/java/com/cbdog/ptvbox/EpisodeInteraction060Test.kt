package com.cbdog.ptvbox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeInteraction060Test {
    private fun source(path:String)=listOf(java.io.File(path),java.io.File("app/$path")).first{it.exists()}.readText()

    @Test fun compactEpisodeCardReservesSpaceForRoundWatchMarker(){
        val card=source("src/main/java/com/cbdog/ptvbox/EpisodeCard054.kt")
        assertTrue(card.contains("width(248.dp).height(244.dp)"))
        assertTrue(card.contains("Row(Modifier.fillMaxWidth().height(34.dp)"))
        assertTrue(card.contains("?:\"未播放\""))
        assertTrue(card.contains("CircleShape"))
        assertTrue(card.contains("size(34.dp)"))
        assertFalse(card.contains("收藏第${'$'}{ep.number}集"))
    }

    @Test fun selectedEpisodeAlwaysShowsReachableSynopsisPanel(){
        val seasons=source("src/main/java/com/cbdog/ptvbox/SeasonEpisodes040.kt")
        assertTrue(seasons.contains("focusedEpisode?.let{episode->EpisodeSynopsis060"))
        assertTrue(seasons.contains("LaunchedEffect(show.id,show.episodes)"))
        assertTrue(seasons.contains("show.episodes.firstOrNull{it.number==number}"))
        assertTrue(seasons.contains("synopsisBring.bringIntoView()"))
        assertTrue(seasons.contains("本集暂未提供简介"))
        assertTrue(seasons.contains("contentDescription=\"第${'$'}{episode.number}集简介\""))
        assertTrue(seasons.contains("点按展开简介"))
    }

    @Test fun watchPickerUsesOutsideOrBackToClose(){
        val picker=source("src/main/java/com/cbdog/ptvbox/Presentation056.kt")
        assertTrue(picker.contains("点外部或按返回关闭"))
        assertFalse(picker.contains("Text(\"关闭\""))
    }
}
