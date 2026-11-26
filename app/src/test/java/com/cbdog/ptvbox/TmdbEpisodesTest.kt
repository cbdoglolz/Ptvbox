package com.cbdog.ptvbox
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class TmdbEpisodesTest {
    @Test fun catalogCacheSeparatesSeasons(){val w=Work("1","Show","tv");assertNotEquals(w.catalogKey(),w.copy(season=2).catalogKey());assertEquals(w.key,w.copy(season=2).key)}
    @Test fun authoritativeTypeCorrectionPreservesIdentityAndRoundTrip(){val old=Work("1","第一季","movie");val corrected=old.copy(kind="tv",identityKey=old.key);assertEquals(old.key,corrected.key);assertEquals(old.key,Work.from(corrected.json()).key);assertEquals("tv",Work.from(corrected.json()).kind)}
    @Test fun queryKeepsForeignWordBoundaries(){assertEquals("The Last of Us",Identity.searchTitle("The Last of Us Season 2"));assertEquals("无职转生",Identity.searchTitle("无职转生 第三季"))}
    @Test fun individualScoreNeverUsesSeriesScore(){val j=JSONObject("""{"vote_average":9.9,"episodes":[{"id":1,"season_number":3,"episode_number":8,"name":"Episode","vote_average":7.3,"vote_count":12,"air_date":"2026-10-01"},{"id":2,"season_number":3,"episode_number":9,"vote_average":0,"vote_count":0}]}""");val rows=TmdbEpisodeParser.episodes(j);assertEquals("7.3",rows[0].rating);assertEquals(8,rows[0].number);assertEquals(3,rows[0].season);assertEquals("-",rows[1].rating)}
    @Test fun episodeFlagsSeparateSeasons(){val w=Work("1","测试","tv",season=1);assertNotEquals(episodeRef(w,1),episodeRef(w.copy(season=2),1));assertNotEquals(episodeRef(w,1),episodeRef(w,2))}
}
