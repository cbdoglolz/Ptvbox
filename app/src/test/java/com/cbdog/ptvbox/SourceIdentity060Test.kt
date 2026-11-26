package com.cbdog.ptvbox

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceIdentity060Test {
    @Test fun contentRoutingClassifiesChineseForeignAndAnime(){
        assertEquals(ContentRouteType.CN_TV,SourceRoutePolicy.classify(Work("1","中文剧","tv",facts=WorkFacts(languages=listOf("汉语"),countries=listOf("中国大陆")))))
        assertEquals(ContentRouteType.WESTERN,SourceRoutePolicy.classify(Work("2","Western","movie",facts=WorkFacts(languages=listOf("English"),countries=listOf("United States")))))
        assertEquals(ContentRouteType.JP_KR,SourceRoutePolicy.classify(Work("3","日剧","tv",facts=WorkFacts(languages=listOf("日语"),countries=listOf("日本")))))
        assertEquals(ContentRouteType.ANIME,SourceRoutePolicy.classify(Work("4","Animation","tv",genres=listOf("动画"))))
    }

    @Test fun titleNormalizationHandlesWidthCaseChineseAndRomanNumbers(){
        assertEquals(Identity.normalize("ROCKY III"),Identity.normalize("Rocky 3"))
        assertEquals(Identity.normalize("第３季"),Identity.normalize("第三季"))
        assertEquals(Identity.normalize("Season Two"),Identity.normalize("Season 2"))
        assertEquals(Identity.base("Show Season 2"),Identity.base("Show 第二季"))
    }

    @Test fun externalIdsRoundTripAndBangumiInfoboxAreRead(){
        val work=Work("1","动画","tv",facts=WorkFacts(imdbId="tt1234567",tmdbId="22",bangumiId="33",doubanId="44"))
        val restored=Work.from(work.json())
        assertEquals("tt1234567",restored.facts.imdbId)
        assertEquals("22",restored.facts.tmdbId)
        assertEquals("33",restored.facts.bangumiId)
        assertEquals("44",restored.facts.doubanId)
        val info=JSONArray().put(JSONObject().put("key","IMDb").put("value","tt7654321")).put(JSONObject().put("key","TMDB").put("value","55")).put(JSONObject().put("key","豆瓣").put("value","66"))
        val facts=IdentityBridge060.bangumiFacts(restored,"77",info)
        assertEquals("tt1234567",facts.imdbId)
        assertEquals("22",facts.tmdbId)
        assertEquals("44",facts.doubanId)
        assertEquals("77",facts.bangumiId)
    }

    @Test fun settingUiHasPreviewAndCategoryRouting(){
        val settings=java.io.File("src/main/java/com/cbdog/ptvbox/Settings040.kt").takeIf{it.exists()}?.readText()?:java.io.File("app/src/main/java/com/cbdog/ptvbox/Settings040.kt").readText()
        assertTrue(settings.contains("DanmakuPreview060"))
        val sites=java.io.File("src/main/java/com/cbdog/ptvbox/Sites040.kt").takeIf{it.exists()}?.readText()?:java.io.File("app/src/main/java/com/cbdog/ptvbox/Sites040.kt").readText()
        assertTrue(sites.contains("分类选源"))
        assertTrue(sites.contains("SourceRoutePolicy.moveGroup"))
        val model=java.io.File("src/main/java/com/cbdog/ptvbox/PtvModel.kt").takeIf{it.exists()}?.readText()?:java.io.File("app/src/main/java/com/cbdog/ptvbox/PtvModel.kt").readText()
        assertTrue(model.contains("SourceRoutePolicy.rankMap(work,active,config)"))
        assertTrue(model.contains("for((_,wave) in waves)"))
        assertTrue(model.contains("if(!background&&candidates.isNotEmpty())break"))
    }

    @Test fun aniListChineseSynonymIsPreferredBeforeBangumiEnrichment(){
        val raw="""{"data":{"Page":{"pageInfo":{"hasNextPage":false},"media":[{"id":1,"title":{"native":"日本語タイトル","romaji":"Japanese Title","english":"English Title"},"synonyms":["中文片名"],"format":"TV","startDate":{"year":2026,"month":4,"day":1},"coverImage":{"large":"https://example.org/a.jpg"},"bannerImage":"","airingSchedule":{"nodes":[]}}]}}}"""
        assertEquals("中文片名",AnimeSeason056.parse(raw).first.single().work.title)
    }
}
