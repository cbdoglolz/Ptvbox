package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

class Version103Test {
    @Test fun announcedAnimeKeepsUnknownDateAndNeverBecomesSeasonOne(){
        val raw="""{"data":{"Page":{"pageInfo":{"hasNextPage":false},"media":[{"id":198966,"title":{"native":"ダンダダン 第3期","romaji":"Dandadan 3rd Season"},"synonyms":[],"format":"TV","status":"NOT_YET_RELEASED","episodes":null,"startDate":{"year":2027,"month":null,"day":null}}]}}}"""
        val w=AnimeSeason056.parse(raw).first.single().work
        assertEquals(3,w.season);assertEquals("",w.releaseDate);assertEquals(0,w.episodeCount)
        assertTrue(w.isUpcoming(java.time.LocalDate.of(2026,11,27)))
        assertEquals("NOT_YET_RELEASED",Work.from(JSONObject(w.json().toString())).releaseStatus)
    }
    @Test fun episodePartsNeverTruncateIntoTheFullEpisode(){
        assertEquals("第01集",OneTouch103.episodeName("1"))
        assertEquals("第16集",OneTouch103.episodeName("16"))
        assertNull(OneTouch103.episodeName("16.1"));assertNull(OneTouch103.episodeName("16.2"))
        assertNull(OneTouch103.episodeName("0"))
    }
    @Test fun biliXmlRejectsExternalEntitiesBeforeParsing(){
        assertThrows(IllegalArgumentException::class.java){MediaText053.danmaku("<!DOCTYPE i [<!ENTITY x SYSTEM 'file:///private'>]><i><d p='1,1,1,1'>&x;</d></i>")}
    }
    @Test fun biliReleaseYearIsDistinctFromPlatformUploadYear(){
        assertTrue(BilibiliPolicy103.acceptsYear("1994",780249600))
        assertFalse(BilibiliPolicy103.acceptsYear("2019",780249600))
        assertTrue(BilibiliPolicy103.acceptsYear("1994",Long.MIN_VALUE))
    }
    @Test fun biliRawDeflateAndPlainXmlYieldTheSameComments(){
        val xml="<i><d p=\"1.5,1,25,16777215,0,0,0,0\">测试</d></i>"
        val buffer=ByteArrayOutputStream();val deflater=Deflater(6,true)
        DeflaterOutputStream(buffer,deflater).use{it.write(xml.toByteArray())};deflater.end()
        assertEquals(xml,BilibiliPolicy103.decode(buffer.toByteArray()))
        assertEquals(xml,BilibiliPolicy103.decode(xml.toByteArray()))
        assertEquals(1,MediaText053.danmaku(BilibiliPolicy103.decode(buffer.toByteArray())).size)
    }
    @Test fun downloadEntriesRemainInCatalogButAreIdentifiedBeforePlayback(){
        val site=Site("hd","HDHUB","https://example.org",8)
        val work=Work("w","Test","movie")
        val raw=JSONObject("""{"streams":[{"name":"1080p","description":"[Download Only] 2.6GB","url":"https://example.org/download"},{"name":"720p","url":"https://example.org/play.mp4"},{"name":"Donation needed","externalUrl":"https://example.org/donate"}]}""")
        val vod=Stremio050.parse(site,work,"正片",raw)
        assertEquals(2,vod.roads.size)
        assertEquals(1,vod.roads.count{SourceDelivery103.downloadOnly(it.name)})
        assertFalse(SourceDelivery103.downloadOnly("PixelDrain · 1080p"))
    }
    private fun episode(n:Int,season:Int=1,date:String="2026-01-01")=EpisodeMeta("e$n",season,n,"Title $n","overview $n","","8",100,date)
    @Test fun mappingProjectsLocalSeasonAndEpisodeWithoutLosingCanonicalIdentity(){
        val rows=AnimeMapping103.parse("""{"rows":[[1,10,1,0,1],[2,10,1,24,2],[3,10,1,48,3]]}""")
        val work=Work("2","某动画 第二季","tv","2025",provider="anilist",season=2,episodeCount=24)
        val map=AnimeMapping103.lookup(rows,work)!!
        assertEquals(48,map.endOffset)
        val series=SeriesMeta("10","某动画","",emptyList(),(1..60).map{episode(it)})
        val visible=AnimeMapping103.project(work,series,map)!!
        assertEquals(24,visible.episodes.size)
        assertEquals(1,visible.episodes.first().number)
        assertEquals("e25",visible.episodes.first().id)
        assertEquals(2,visible.episodes.first().season)
        assertEquals(24,visible.episodes.last().number)
        assertEquals(2,visible.seasons.single().number)
    }
    @Test fun missingThirdSeasonNeverFallsBackToFirstSeason(){
        val w=Work("third","动画 第三季","tv","2027",provider="anilist",season=3)
        val old=SeriesMeta("10","动画","",emptyList(),(1..12).map{episode(it,date="2024-01-01")})
        assertNull(AnimeMapping103.project(w,old,null))
        assertEquals(3,AnimeMapping103.titleSeason(listOf("Dandadan","Dandadan 3rd Season")))
    }
    @Test fun airDateFallbackFindsLaterCourAndNormalizesItsLocalEpisodeNumbers(){
        val eps=(1..12).map{episode(it,date=java.time.LocalDate.of(2024,1,1).plusWeeks((it-1).toLong()).toString())}+
            (13..24).map{episode(it,date=java.time.LocalDate.of(2025,1,1).plusWeeks((it-13).toLong()).toString())}
        val w=Work("second","动画 第二季","tv","2025",provider="douban",genres=listOf("动画"),season=2)
        val result=AnimeMapping103.project(w,SeriesMeta("10","动画","",emptyList(),eps),null)!!
        assertEquals("e13",result.episodes.first().id)
        assertEquals(1,result.episodes.first().number)
        assertEquals(12,result.episodes.size)
    }
    @Test fun tmdbWholeSeriesAndSelectedAnimeArtworkKeepExistingBehavior(){
        val full=SeriesMeta("10","Whole","",emptyList(),(1..60).map{episode(it)})
        assertEquals(full,AnimeMapping103.project(Work("10","Whole","tv",provider="tmdb"),full,null))
        val installment=Work("3","动画 第三季","tv","2026",poster="third.jpg",provider="anilist",original="Native 3",season=3,releaseDate="2026-04-01")
        val merged=installment.enriched(Work("10","动画","tv","2023",poster="first.jpg",provider="tmdb",original="Native",releaseDate="2023-01-01"))
        assertEquals("third.jpg",merged.poster);assertEquals("Native 3",merged.original);assertEquals("2026-04-01",merged.releaseDate)
    }
    @Test fun oneTouchAcceptsPlainAndEncryptedPublicApiResponses(){
        val plain="""{"result":[{"id":"fixture","title":"Fixture"}]}"""
        assertEquals("fixture",(OneTouch103.decode(plain) as org.json.JSONArray).getJSONObject(0).getString("id"))
        val cipher=javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding")
        fun hex(s:String)=s.chunked(2).map{it.toInt(16).toByte()}.toByteArray()
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE,javax.crypto.spec.SecretKeySpec(hex("696d37326368617250617373776f72646f6664496e6974566563746f7253746d"),"AES"),javax.crypto.spec.IvParameterSpec(hex("696d37326368617250617373776f7264")))
        val encoded=java.util.Base64.getEncoder().encodeToString(cipher.doFinal(plain.toByteArray())).replace("/","-_.").replace("+","@")
        assertEquals("fixture",(OneTouch103.decode(encoded) as org.json.JSONArray).getJSONObject(0).getString("id"))
    }
}
