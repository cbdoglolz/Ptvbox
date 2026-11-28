package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class SearchSeries103Test {
    @Test fun movieAndTvLinksAreBidirectionalAcrossFranchises(){
        for((movie,tv) in listOf("来自深渊：深魂的黎明" to "来自深渊","剧场版 鬼灭之刃 无限列车篇" to "鬼灭之刃","唐顿庄园：新纪元" to "唐顿庄园","The X-Files: I Want to Believe" to "The X-Files")){
            val a=Work("m",movie,"movie");val b=Work("t",tv,"tv")
            assertTrue(movie,SeriesLinks103.matches(a,b));assertTrue(tv,SeriesLinks103.matches(b,a))
        }
    }
    @Test fun seasonSuffixesAndAliasesDoNotMatchUnrelatedSubstrings(){
        val tv=Work("t","来自深渊 第二季","tv",original="Made in Abyss Season 2")
        assertTrue(SeriesLinks103.matches(tv,Work("m","Made in Abyss: Dawn of the Deep Soul")))
        assertFalse(SeriesLinks103.matches(tv,Work("u","来自深渊的你")))
        assertFalse(SeriesLinks103.matches(tv,tv))
        assertFalse(SeriesLinks103.matches(Work("a","Us"),Work("b","Us: Another")))
    }
    @Test fun relatedResultsPrecedeSimilarAndKeepMovieAndTvIdentities(){
        val w=Work("m","来自深渊：深魂的黎明")
        val tv=Work("t","来自深渊","tv")
        val similar=Work("s","千与千寻")
        val rows=SeriesLinks103.merge(w,listOf(tv,w),listOf(similar,tv))
        assertEquals(listOf(tv,similar),rows)
        assertEquals("tv",rows.first().kind)
    }
    @Test fun explicitAnimeRelationsIncludeTwoHopsButExcludeMangaAndCharacterCameos(){
        val raw=JSONObject("""{"data":{"Media":{"relations":{"edges":[
          {"relationType":"PREQUEL","node":{"id":2,"type":"ANIME","format":"MOVIE","title":{"native":"总集篇"},"startDate":{},"relations":{"edges":[{"relationType":"PARENT","node":{"id":3,"type":"ANIME","format":"TV","title":{"native":"TV本篇"},"startDate":{}}}]}}},
          {"relationType":"ADAPTATION","node":{"id":4,"type":"MANGA"}},
          {"relationType":"CHARACTER","node":{"id":5,"type":"ANIME","format":"MOVIE","title":{"native":"客串"},"startDate":{}}}
        ]}}}}""")
        val rows=SeriesLinks103.parseRelations(raw)
        assertEquals(listOf("2","3"),rows.map{it.id});assertEquals(listOf("movie","tv"),rows.map{it.kind})
    }
    @Test fun alternateProviderOfCurrentMovieIsExcludedButTvIsRetained(){
        val movie=Work("1","来自深渊：深魂的黎明","movie","2020",original="Made in Abyss: Dawn of the Deep Soul")
        val duplicate=movie.copy(id="2",provider="tmdb",title=movie.original)
        val tv=Work("3","来自深渊","tv","2017")
        assertEquals(listOf(tv),SeriesLinks103.merge(movie,listOf(duplicate,tv),emptyList()))
    }
    @Test fun fullSearchSubjectsRetainPosterAndSeriesAliases(){
        val root=JSONObject("""{"subjects":{"items":[{"target_type":"tv","target":{"id":"123","title":"来自深渊","year":"2017","cover_url":null,"pic":{"large":"https://example.org/poster.jpg"},"original_title":"メイドインアビス","aka":["Made in Abyss"],"genres":["动画"]}}]}}""")
        val w=DoubanSearch055.subjects(root).single()
        assertEquals("https://example.org/poster.jpg",w.poster);assertEquals("メイドインアビス",w.original)
        assertEquals(listOf("Made in Abyss"),w.aliases);assertEquals(listOf("动画"),w.genres)
    }
}
