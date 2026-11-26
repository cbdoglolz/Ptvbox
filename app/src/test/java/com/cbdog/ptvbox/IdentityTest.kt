package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class IdentityTest {
    private fun vod(title:String,year:String="2026")=Vod("1",title,year,"","",emptyList())
    @Test fun episodeFormats(){listOf("S1EP03","S01E03","第03集","EP03","03").forEach{assertEquals(it,3,Identity.episode(it))}}
    @Test fun rejectAnotherYear(){assertFalse(Identity.matches(Work("x","奥德赛",year="2026"),vod("奥德赛","1997")))}
    @Test fun seasonMustBeExplicit(){val w=Work("x","无职转生 第三季","tv","2026",season=3);assertTrue(Identity.matches(w,vod("无职转生 第3季")));assertFalse(Identity.matches(w,vod("无职转生 第二季")));assertFalse(Identity.matches(w,vod("无职转生")))}
    @Test fun aliasesLinkNames(){val w=Work("x","刃牙道 第二季","tv",aliases=listOf("刃牙道：无敌武士 第2部"),season=2);assertTrue(Identity.matches(w,vod("刃牙道：无敌武士 第2部","")))}
    @Test fun rejectExtras(){assertFalse(Identity.matches(Work("x","奥德赛"),vod("奥德赛幕后花絮")))}
    @Test fun pinyin(){assertEquals("rx",Identity.initials("热血"));assertTrue(Identity.keyword(Work("x","攻壳机动队"),"gkjdd"))}
    @Test fun parseMultipleRoads(){val j=JSONObject().put("vod_name","测试").put("vod_play_from","中文\$\$\$英文").put("vod_play_url","第01集\$https://example.test/a#第02集\$https://example.test/b\$\$\$S1E1\$https://example.test/c");val v=VodParser.parse(j);assertEquals(2,v.roads.size);assertEquals("https://example.test/b",Identity.pick(v.roads[0],"S1EP02","tv")?.url);assertEquals("英文",v.roads[1].name)}
    @Test fun absentEpisodeNeverFallsToFirst(){assertNull(Identity.pick(Road("a",listOf(Episode("第01集","a"))),"第02集","tv"))}
    @Test fun progressRoundTrip(){val w=Watch(Work("x","测试","tv",season=2),"第3集","s","1080P",1080,12000,90000);assertEquals(w,Watch.from(w.json()))}
}
