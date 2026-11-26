package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test

class Version102Test {
    @Test fun siteScoreFormattingAndColorRamp() {
        assertEquals("0", SiteScores102.format(0f))
        assertEquals("+1", SiteScores102.format(1f))
        assertEquals("+0.5", SiteScores102.format(0.5f))
        assertEquals("-2", SiteScores102.format(-2f))
        assertEquals("-1.5", SiteScores102.format(-1.5f))

        val highGood = SiteScores102.badgeStyle(6f)
        val zero = SiteScores102.badgeStyle(0f)
        val highBad = SiteScores102.badgeStyle(-6f)
        assertNotEquals(highGood.background, zero.background)
        assertNotEquals(highBad.background, zero.background)
        assertNotEquals(highGood.text, highBad.text)
    }

    @Test fun siteSortingOrdersHighestScoreFirst() {
        val a = Site("a", "Alpha", "https://a.test", 1, "默认点播")
        val b = Site("b", "Beta", "https://b.test", 1, "默认点播")
        val c = Site("c", "Gamma", "https://c.test", 1, "默认点播")
        val scores = mapOf("a" to -2f, "b" to 3.5f, "c" to 0.5f)
        val sorted = SiteScores102.sortSites(listOf(a, b, c), { scores[it.key] ?: 0f }, { true })
        assertEquals(listOf("b", "c", "a"), sorted.map { it.key })
    }

    @Test fun overviewAliasesAndOneYearToleranceMatchPossibleLove() {
        val work = Work(
            id = "36912345",
            title = "可能的爱",
            kind = "movie",
            year = "2026",
            original = "가능한 사랑",
            overview = "《假如爱有如果》讲述了两对已婚夫妇之间错综复杂的关系…"
        )
        val queries = Identity.searchQueries(work)
        assertTrue("Expected Chinese overview alias before Korean original: $queries", queries.indexOf("假如爱有如果") in 1 until queries.indexOf("가능한 사랑"))
        val vod2025 = Vod("1", "假如爱有如果", "2025", "", "", emptyList())
        val vod1999 = Vod("2", "假如爱有如果", "1999", "", "", emptyList())
        assertTrue(Identity.matches(work, vod2025))
        assertFalse(Identity.matches(work, vod1999))
    }

    @Test fun netflixJapaneseKoreanWorksPrioritizeInternationalHdhub() {
        val fromNetflixTab = Work("1", "末路兼职", "tv", "2026", provider = "netflix", facts = WorkFacts(languages = listOf("韩语"), countries = listOf("韩国")))
        val fromDoubanWithNetflixCompany = Work("2", "可能的爱", "movie", "2026", provider = "douban", facts = WorkFacts(languages = listOf("韩语"), countries = listOf("韩国"), companies = listOf(CompanyCredit("213", "Netflix", "播出平台"))))
        assertEquals(ContentRouteType.NETFLIX, SourceRoutePolicy.classify(fromNetflixTab))
        assertEquals(ContentRouteType.NETFLIX, SourceRoutePolicy.classify(fromDoubanWithNetflixCompany))
        assertEquals("国际高清", ContentRouteType.NETFLIX.defaults.first())
    }

    @Test fun cloudPolicyCatchesUcAndCloudRoadsWithoutFalsePositives() {
        assertTrue(CloudPolicy054.needsAuthorization(Site("u", "木偶4K", "csp_Muou")))
        assertTrue(CloudPolicy054.needsAuthorization(Site("u2", "优汐网盘", "csp_UC")))
        assertTrue(CloudPolicy054.isCloudRoad("UC网盘"))
        assertTrue(CloudPolicy054.isCloudRoad("UC"))
        assertTrue(CloudPolicy054.isCloudRoad("夸克4K"))
        assertFalse(CloudPolicy054.isCloudRoad("非凡云播"))
        assertFalse(CloudPolicy054.isCloudRoad("HDHUB 1080P"))
    }

    @Test fun sourceBadgeParserOrdersDolbyHdrChannelsAndLanguages() {
        val badges = SourceBadgeParser102.parse("HDHUB · 2160p WEB-DL Dolby Vision HDR10+ Atmos 5.1 Multi 国语 英语 中字", 2160)
        assertEquals(listOf("4K", "DV", "HDR10+", "ATMOS", "5.1", "多语", "中字"), badges.map { it.label })
        assertTrue(badges.first { it.label == "DV" }.dolbyMark)
        assertTrue(badges.first { it.label == "ATMOS" }.dolbyMark)

        val dual = SourceBadgeParser102.parse("1080P DDP 5.1 双语 国语 英语")
        assertEquals(listOf("1080P", "DD+", "5.1", "双语", "国语", "英语"), dual.map { it.label })
    }

    @Test fun trackLanguagePolicyClassifiesAndScoresChineseThenEnglish() {
        assertEquals("zh", TrackLanguagePolicy102.classify("zh-CN", ""))
        assertEquals("zh", TrackLanguagePolicy102.classify("und", "国语中字"))
        assertEquals("en", TrackLanguagePolicy102.classify("eng", "English"))
        assertEquals("ja", TrackLanguagePolicy102.classify("jpn", "日语"))
        assertTrue(
            TrackLanguagePolicy102.scoreOnlineSubtitle("简体中字", "", "zh", "en") >
                TrackLanguagePolicy102.scoreOnlineSubtitle("English", "", "zh", "en")
        )
    }

    @Test fun splitSeasonDetectsApothecaryDiariesS1E29AsS2E05() {
        val s1Eps = (1..24).map { n ->
            EpisodeMeta("e$n", 1, n, "第${n}集", "", "", "8.5", 100, java.time.LocalDate.of(2023, 10, 22).plusWeeks((n - 1).toLong()).toString())
        }
        val s2Eps = (25..48).map { n ->
            EpisodeMeta("e$n", 1, n, "第${n}集", "", "", "8.8", 120, java.time.LocalDate.of(2025, 1, 10).plusWeeks((n - 25).toLong()).toString())
        }
        val s3Eps = (49..60).map { n ->
            EpisodeMeta("e$n", 1, n, "第${n}集", "", "", "8.9", 90, java.time.LocalDate.of(2026, 4, 10).plusWeeks((n - 49).toLong()).toString())
        }
        val series = SeriesMeta("210879", "药屋少女的呢喃", "薬屋のひとりごと", emptyList(), s1Eps + s2Eps + s3Eps)

        val ep29 = SplitSeason102.fromSeries(series, 1, 29)
        assertNotNull(ep29)
        assertEquals(2, ep29!!.splitSeason)
        assertEquals(5, ep29.localEpisode)
        assertEquals("2025", ep29.expectedYear)

        val ep50 = SplitSeason102.fromSeries(series, 1, 50)
        assertNotNull(ep50)
        assertEquals(3, ep50!!.splitSeason)
        assertEquals(2, ep50.localEpisode)

        assertNull(SplitSeason102.fromSeries(series, 1, 18))

        val work = Work("210879", "药屋少女的呢喃", "tv", "2023")
        val v1 = Vod("101", "药屋少女的呢喃", "2023", "", "", listOf(Road("线路1", (1..24).map { Episode("第%02d集".format(it), "u1/$it") })))
        val v2 = Vod("102", "药屋少女的呢喃 第二季", "2025", "", "", listOf(Road("线路1", (1..24).map { Episode("第%02d集".format(it), "u2/$it") })))
        val v3 = Vod("103", "药屋少女的呢喃 第三季", "2026", "", "", listOf(Road("线路1", (1..12).map { Episode("第%02d集".format(it), "u3/$it") })))
        val matched = SplitSeason102.seriesVods(work, listOf(v3, v1, v2))
        assertEquals(listOf(1, 2, 3), matched.map { it.first })

        val resolved29 = SplitSeason102.resolveByCumulative(matched, 29)
        assertNotNull(resolved29)
        assertEquals("102", resolved29!!.first.id)
        assertEquals(2, resolved29.second)
        assertEquals(5, resolved29.third)
    }

    @Test fun episodesFallbackPreservesFullSeasonOnResumeAndSplitSeason() {
        val site = Site("hd", "HDHUB", "https://example.org", type = 8)
        val cand = Candidate(site, "1080P", Episode("第05集", "https://example.org/5"), sourceId = "hd|1|1080P")
        val fullSeason = (1..24).map { "第%02d集".format(it) }
        val names = Episodes051.names("tv", "第05集", cand, listOf(cand), mapOf("hd|1|1080P" to listOf(Episode("第05集", "u"))), fullSeason)
        assertEquals(24, names.size)
        assertEquals("第01集", names.first())
        assertEquals("第24集", names.last())

        val splitRoad = (1..24).map { Episode("第%02d集".format(it), "u/$it") }
        val full60 = (1..60).map { "第%02d集".format(it) }
        val cand29 = Candidate(site, "线路1 · S2E05", Episode("第29集", "u/5"), sourceId = "hd|2|线路1")
        val names29 = Episodes051.names("tv", "第29集", cand29, listOf(cand29), mapOf("hd|2|线路1" to splitRoad), full60)
        assertEquals(60, names29.size)
        assertTrue(names29.contains("第29集"))
    }

    @Test fun playerTopBarSubtitleAndDanmakuLabelsDistinguishExternalAndUnit() {
        assertEquals("字幕 ON", subtitleStatusLabel102(hasExternal = false, externalEnabled = false, embeddedDisabled = false, hasSelectedEmbedded = true))
        assertEquals("字幕 OFF", subtitleStatusLabel102(hasExternal = false, externalEnabled = false, embeddedDisabled = true, hasSelectedEmbedded = false))
        assertEquals("字幕未报告", subtitleStatusLabel102(hasExternal = false, externalEnabled = false, embeddedDisabled = false, hasSelectedEmbedded = false))
        assertEquals("外挂字幕 ON", subtitleStatusLabel102(hasExternal = true, externalEnabled = true, embeddedDisabled = true, hasSelectedEmbedded = false))
        assertEquals("外挂字幕 OFF", subtitleStatusLabel102(hasExternal = true, externalEnabled = false, embeddedDisabled = true, hasSelectedEmbedded = false))

        assertEquals("弹幕 ON 326条", danmakuStatusLabel102(enabled = true, count = 326))
        assertEquals("弹幕 ON 0", danmakuStatusLabel102(enabled = true, count = 0))
        assertEquals("弹幕 OFF", danmakuStatusLabel102(enabled = false, count = 326))
    }
}
