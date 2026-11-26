package com.cbdog.ptvbox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Version101Test {
    private fun source(path:String)=listOf(java.io.File(path),java.io.File("app/$path")).first{it.exists()}.readText()

    @Test fun episodeCardRequiresSelectionBeforeTouchPlayback(){
        val card=source("src/main/java/com/cbdog/ptvbox/EpisodeCard054.kt")
        val seasons=source("src/main/java/com/cbdog/ptvbox/SeasonEpisodes040.kt")
        assertTrue(card.contains("selected:Boolean=false"))
        assertTrue(card.contains("if(active)m.play(w,\"第%02d集\".format(ep.number))else onFocused(ep)"))
        assertTrue(seasons.contains("selected=episodeInteracted&&focusedEpisode?.number==episode.number"))
    }

    @Test fun detailSimilarRecommendationsUseFiveColumns(){
        val detail=source("src/main/java/com/cbdog/ptvbox/CinemaDetail040.kt")
        assertTrue(detail.contains("related.chunked(5)"))
        assertTrue(detail.contains("repeat(5-row.size)"))
        assertTrue(detail.contains("一行五部"))
        assertFalse(detail.contains("related.chunked(6)"))
    }

    @Test fun netflixHidesTop250AndFallsBackFromTop250(){
        val screens=source("src/main/java/com/cbdog/ptvbox/TvScreens.kt")
        val model=source("src/main/java/com/cbdog/ptvbox/PtvModel.kt")
        assertTrue(screens.contains("if(m.provider!=\"netflix\")add(Triple(\"TOP250\",\"movie\",\"TOP250\"))"))
        assertTrue(screens.contains("if(m.provider!=\"netflix\")add(\"TOP250\")"))
        assertTrue(model.contains("if(p==\"netflix\"&&c==\"TOP250\")\"热门\"else c"))
    }

    @Test fun nowPlayingTriangleIsAnimatedAndAboveHome(){
        val screens=source("src/main/java/com/cbdog/ptvbox/TvScreens.kt")
        assertTrue(screens.contains("private fun NowPlayingTriangle(playing:Boolean)"))
        assertTrue(screens.contains("rememberInfiniteTransition(label=\"now-playing\")"))
        val nowIdx=screens.indexOf("item(key=\"now-playing\")")
        val navIdx=screens.indexOf("items(navItems,key={it.id})")
        assertTrue(nowIdx>0&&navIdx>nowIdx)
    }

    @Test fun detailGenreClickFetchesCategoryFeedWithoutDroppingNonGenreFacts(){
        val model=source("src/main/java/com/cbdog/ptvbox/PtvModel.kt")
        val results=source("src/main/java/com/cbdog/ptvbox/SearchResults040.kt")
        assertTrue(model.contains("metadata.feed(\"douban\",kind,\"筛选:\$normalized\""))
        assertTrue(model.contains("w.facts.countries.any"))
        assertTrue(results.contains("var genres by remember(initialGenre){mutableStateOf<Set<String>>(emptySet())}"))
    }

    @Test fun nianfanAnimeAutoMatchesSeriesCoreAndRanksCandidates(){
        val work=Work("37000001","凡人修仙传 年番4","tv","2025",provider="douban",season=1)
        org.junit.Assert.assertEquals(4,Identity.nianfan(work.title))
        org.junit.Assert.assertEquals("凡人修仙传",Identity.searchTitle(work.title))
        assertTrue(Identity.searchQueries(work).contains("凡人修仙传"))

        val exact=Vod("1","凡人修仙传 年番4","","","",emptyList())
        val season4=Vod("2","凡人修仙传 第四季","","","",emptyList())
        val genericNf=Vod("3","凡人修仙传 年番","","","",emptyList())
        val combined=Vod("4","凡人修仙传","","","",emptyList())
        val arc=Vod("5","凡人修仙传：外海风云","","","",emptyList())
        val wrongSeason=Vod("6","凡人修仙传 第一季","","","",emptyList())
        val wrongNf=Vod("7","凡人修仙传 年番2","","","",emptyList())

        assertTrue(Identity.matches(work,exact))
        assertTrue(Identity.matches(work,season4))
        assertTrue(Identity.matches(work,genericNf))
        assertTrue(Identity.matches(work,combined))
        assertTrue(Identity.matches(work,arc))
        assertFalse(Identity.matches(work,wrongSeason))
        assertFalse(Identity.matches(work,wrongNf))
        assertTrue(Identity.matchScore(work,exact)>Identity.matchScore(work,genericNf))
        assertTrue(Identity.matchScore(work,genericNf)>Identity.matchScore(work,combined))

        // RatingMatch055 allows matching TMDB parent series (2020) for nianfan (2025)
        val tmdbParent=Work("105971","凡人修仙传","tv","2020",provider="tmdb")
        assertTrue(RatingMatch055.matches(work,tmdbParent,MetadataQueries040.names(work)))

        // TmdbSeasonResolver101 aligns 2025 nianfan 4 to Season 4
        val seasons=listOf(
            SeasonMeta(1,"第 1 季","2020",21),
            SeasonMeta(2,"第 2 季","2021",52),
            SeasonMeta(3,"第 3 季","2023",52),
            SeasonMeta(4,"第 4 季","2025",52)
        )
        org.junit.Assert.assertEquals(4,TmdbSeasonResolver101.pickSeason(work,1,seasons))
    }

    @Test fun animePageUsesTopTabsWithHotFirstAndScheduleSecond(){
        val anime=source("src/main/java/com/cbdog/ptvbox/AnimeSeason056.kt")
        val hotIdx=anime.indexOf("\"hot\" to \"近期热门番剧\"")
        val scheduleIdx=anime.indexOf("\"schedule\" to \"时间表\"")
        assertTrue(hotIdx>0&&scheduleIdx>hotIdx)
        assertTrue(anime.contains("var tab by rememberSaveable{mutableStateOf(\"hot\")}"))
        assertTrue(anime.contains("if(tab==\"schedule\")"))
    }

    @Test fun detailTitleAutoFitsSingleLineAndPosterPrefersChinese(){
        val detail=source("src/main/java/com/cbdog/ptvbox/CinemaDetail040.kt")
        val presentation=source("src/main/java/com/cbdog/ptvbox/Presentation050.kt")
        assertTrue(detail.contains("AutoFitSingleLineTitle(w.title,c.text)"))
        assertTrue(detail.contains("maxLines=1"))
        assertTrue(detail.contains("onTextLayout={result->if(result.hasVisualOverflow"))
        assertTrue(presentation.contains("InfoChinese056.isLikelyChinese(w.title)"))
    }

    @Test fun tvSeekUsesDebouncedPreviewAndClearsOldMediaBeforeSelect(){
        val player=source("src/main/java/com/cbdog/ptvbox/CinemaPlayer.kt")
        val chrome=source("src/main/java/com/cbdog/ptvbox/PlaybackChrome.kt")
        val model=source("src/main/java/com/cbdog/ptvbox/PtvModel.kt")
        assertTrue(player.contains("SeekPolicy057.DEBOUNCE_COMMIT_MS"))
        assertTrue(player.contains("queueSeekDelta("))
        assertTrue(chrome.contains("var scrubPreview by remember"))
        assertTrue(model.contains("SeekParameters.CLOSEST_SYNC"))
        assertTrue(model.contains("player.stop();player.clearMediaItems()"))
    }

    @Test fun animePosterChineseTitleAndSequelMatchDoubanAndTmdb(){
        // Black Clover: Japanese title enriched by TMDB Chinese title puts Chinese name first for Douban lookup
        val jp=Work("97940","ブラッククローバー","tv","2017",provider="anilist",original="ブラッククローバー",aliases=listOf("Black Clover"))
        val tmdbZh=Work("73223","黑色五叶草","tv","2017",provider="tmdb",original="ブラッククローバー")
        val enriched=jp.enriched(tmdbZh)
        val queries=MetadataQueries040.names(enriched)
        assertTrue(queries.first()=="黑色五叶草")
        val doubanHit=Work("26935251","黑色五叶草","tv","2017",provider="douban")
        assertTrue(RatingMatch055.matches(enriched,doubanHit,queries))

        // Cyberpunk: Edgerunners 2 matches TMDB parent series (2022) and Douban Season 2
        val edgerunners2=Work("190001","赛博朋克：边缘行者2","tv","2026",provider="anilist",original="Cyberpunk: Edgerunners 2",aliases=listOf("Cyberpunk: Edgerunners 2"))
        val tmdbEdge=Work("105248","赛博朋克：边缘行者","tv","2022",provider="tmdb",original="Cyberpunk: Edgerunners")
        val doubanEdge2=Work("36999999","赛博朋克：边缘行者 第二季","tv","2026",provider="douban")
        val edgeQueries=MetadataQueries040.names(edgerunners2)
        assertTrue(RatingMatch055.matches(edgerunners2,tmdbEdge,edgeQueries))
        assertTrue(RatingMatch055.matches(edgerunners2,doubanEdge2,edgeQueries))
    }

    @Test fun romanNumeralSeasonAndSubtitleMatchMushokuTensei3(){
        org.junit.Assert.assertEquals(3,Identity.season("無職転生III ～異世界行ったら本気だす～"))
        org.junit.Assert.assertEquals(3,Identity.season("無職転生Ⅲ ～異世界行ったら本気だす～"))
        org.junit.Assert.assertEquals(3,Identity.season("无职转生III"))
        org.junit.Assert.assertEquals("無職転生",Identity.searchTitle("無職転生III ～異世界行ったら本気だす～"))
        org.junit.Assert.assertEquals("无职转生",Identity.searchTitle("无职转生III"))
        val w=Work("180001","無職転生III ～異世界行ったら本気だす～","tv","2026",provider="anilist",original="無職転生III ～異世界行ったら本気だす～",aliases=listOf("无职转生III"))
        val tmdbShow=Work("94664","无职转生～到了异世界就拿出真本事～","tv","2021",provider="tmdb",original="無職転生 ～異世界行ったら本気だす～")
        assertTrue(RatingMatch055.matches(w,tmdbShow,MetadataQueries040.names(w)))
    }

    @Test fun pagingTwentyAndDoubanHdBackdropAndFlowingTriangle(){
        val anime=source("src/main/java/com/cbdog/ptvbox/AnimeSeason056.kt")
        val search=source("src/main/java/com/cbdog/ptvbox/SearchResults040.kt")
        val net=source("src/main/java/com/cbdog/ptvbox/Network.kt")
        val screens=source("src/main/java/com/cbdog/ptvbox/TvScreens.kt")
        assertTrue(anime.contains("mutableIntStateOf(20)"))
        assertTrue(anime.contains("visibleLimit+=20"))
        assertTrue(search.contains("mutableIntStateOf(20)"))
        assertTrue(search.contains("loadMoreFilterSearch056()"))
        assertTrue(net.contains("/view/photo/l/"))
        assertTrue(screens.contains("TileMode.Repeated"))
    }

    @Test fun ordinalJapaneseSeasonPatternsAndPlayerChromeRedesign(){
        org.junit.Assert.assertEquals(5,Identity.season("ようこそ実力至上主義の教室へ 5th Season"))
        org.junit.Assert.assertEquals(2,Identity.season("佐々木とピーちゃん シーズン2"))
        org.junit.Assert.assertEquals(2,Identity.season("千歳くんはラムネ瓶のなか 2クール"))
        org.junit.Assert.assertEquals("ようこそ実力至上主義の教室へ",Identity.searchTitle("ようこそ実力至上主義の教室へ 5th Season"))
        org.junit.Assert.assertEquals("佐々木とピーちゃん",Identity.searchTitle("佐々木とピーちゃん シーズン2"))
        org.junit.Assert.assertEquals("千歳くんはラムネ瓶のなか",Identity.searchTitle("千歳くんはラムネ瓶のなか 2クール"))

        val chrome=source("src/main/java/com/cbdog/ptvbox/PlaybackChrome.kt")
        val player=source("src/main/java/com/cbdog/ptvbox/CinemaPlayer.kt")
        val live=source("src/main/java/com/cbdog/ptvbox/Live050.kt")
        assertTrue(chrome.contains("colors=ptvButtonColors(btnBg,Color.White)"))
        assertFalse(chrome.contains("RoundedCornerShape(20.dp)"))
        assertFalse(chrome.contains("\$audio / \$subtitle / \$danmaku"))
        assertFalse(player.contains("m::toggleFavorite"))
        assertTrue(player.contains("compactSummary"))
        assertTrue(live.contains("m.sites.liveDetail054(entry)"))
    }

    @Test fun lruSpaceCacheAndTmdbEpisodeFallbackAndLiveLoading(){
        val dir=java.nio.file.Files.createTempDirectory("ptvbox-lru-test").toFile()
        try{
            val old=java.io.File(dir,"old.json").apply{writeBytes(ByteArray(600));setLastModified(1000L)}
            val recent=java.io.File(dir,"recent.json").apply{writeBytes(ByteArray(600));setLastModified(2000L)}
            CacheBudget101.trimDirectory(dir,maxBytes=1000L)
            assertFalse(old.exists())
            assertTrue(recent.exists())
        }finally{dir.deleteRecursively()}

        val app=source("src/main/java/com/cbdog/ptvbox/PtvApplication.kt")
        val net=source("src/main/java/com/cbdog/ptvbox/Network.kt")
        val live=source("src/main/java/com/cbdog/ptvbox/Live050.kt")
        assertTrue(app.contains("max-age=31536000"))
        assertTrue(net.contains("ratingStorageKey(work,\"tmdb\")"))
        assertTrue(net.contains("val fallbackNum=regularSeasons.lastOrNull"))
        assertTrue(live.contains("LiveLogoImage101"))
        assertTrue(live.contains("体育/直播源解析较慢，请稍候"))
    }
}
