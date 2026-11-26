package com.cbdog.ptvbox
import org.junit.Test
import org.junit.Assert.*
class Live054Test {
 @Test fun streamingHasNoTotalDeadlineButStillHasStallTimeouts(){val base=okhttp3.OkHttpClient.Builder().connectTimeout(8,java.util.concurrent.TimeUnit.SECONDS).readTimeout(12,java.util.concurrent.TimeUnit.SECONDS).callTimeout(26,java.util.concurrent.TimeUnit.SECONDS).build();val media=StreamHttp054.client(base);assertEquals(0,media.callTimeoutMillis);assertEquals(8000,media.connectTimeoutMillis);assertEquals(12000,media.readTimeoutMillis);assertEquals(26000,base.callTimeoutMillis)}
 @Test fun ordersKeepStableKeysAndAppendNewSources(){assertEquals(listOf("b","a","c"),LiveOrder054.order(listOf("a","b","c"),listOf("missing","b","a","b")));assertEquals(listOf("a","c","b"),LiveOrder054.move(listOf("a","b","c"),"c",-1));assertEquals(listOf("a","b"),LiveOrder054.move(listOf("a","b"),"a",-1))}
 @Test fun firstNetworkCategoryAndNestedHighlight(){val classes=listOf("b" to "B站","h" to "虎牙","d" to "斗鱼");assertEquals("h",LiveOrder054.defaultClass(classes));assertEquals("d",LiveOrder054.defaultClass(classes,"斗鱼"));assertTrue(LiveOrder054.activeClass("huya_lol",listOf("h"),"h"));assertFalse(LiveOrder054.activeClass("huya_lol",listOf("h"),"d"))}
 @Test fun liveRetryIsFiniteAndStartsNewWindow(){val r=LiveRetry054();r.reset(100);assertEquals(1500L,r.next(1000));assertEquals(3000L,r.next(3000));assertEquals(4500L,r.next(6000));assertNull(r.next(9000));assertEquals(1500L,r.next(100000))}
 @Test fun cloudAuthorizationDoesNotMisclassifyLiveOrInternational(){assertTrue(CloudPolicy054.needsAuthorization(Site("a","玩偶哥哥4K","csp_WoggGuard")));assertTrue(CloudPolicy054.needsAuthorization(Site("q","盘他|夸克","csp_Quark")));assertFalse(CloudPolicy054.needsAuthorization(Site("h","网络直播","csp_AllliveGuard")));assertFalse(CloudPolicy054.needsAuthorization(Site("b","B站","csp_BiliGuard")));assertFalse(CloudPolicy054.needsAuthorization(Site("i","HDHUB","https://example.invalid")))}
}
