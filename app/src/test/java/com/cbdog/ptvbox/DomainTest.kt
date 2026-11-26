package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    @Test fun secondsKeepEveryMillisecond(){listOf(0L,1L,999L,1234567L,Long.MAX_VALUE).forEach{assertEquals(it,ProgressUnits.milliseconds(ProgressUnits.seconds(it)))}}
    @Test fun typesHaveDistinctIdentity(){assertNotEquals(Work("123","a",kind="movie",provider="tmdb").key,Work("123","b",kind="tv",provider="tmdb").key)}
    @Test fun specialsDoNotMatchByNullNumber(){assertFalse(Identity.sameEpisode("OVA","特别篇"));assertTrue(Identity.sameEpisode("第03集","S01E03"))}
    @Test fun favoriteAndWatchStateAreIndependent(){val w=Watch(Work("x","测试"),"正片","s","r",1080,1000,90000);assertEquals(1,UserSnapshot(setOf(w.work.key),listOf(w),mapOf(w.work.key to WatchState.PLANNED)).continues.size);assertTrue(UserSnapshot(setOf(w.work.key),listOf(w),mapOf(w.work.key to WatchState.COMPLETED)).continues.isEmpty())}
    @Test fun hiddenContinueKeepsHistory(){val w=Watch(Work("x","测试"),"正片","s","r",1080,1000,90000);val s=UserSnapshot(watches=listOf(w),hidden=setOf(w.work.key));assertTrue(s.continues.isEmpty());assertEquals(w,s.lastByWork[w.work.key])}
}
