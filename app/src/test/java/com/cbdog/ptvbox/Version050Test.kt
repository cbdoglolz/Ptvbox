package com.cbdog.ptvbox
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject

class Version050Test {
 @Test fun collectionStatusesAreExclusiveAndFavoritesIndependent(){val a=Work("a","甲");val b=Work("b","乙");val s=UserSnapshot(setOf(a.key),states=mapOf(a.key to WatchState.WATCHING,b.key to WatchState.COMPLETED));assertEquals(listOf(a),CollectionTabs050.rows("在看",s,listOf(a,b),listOf(a)));assertEquals(listOf(b),CollectionTabs050.rows("已看",s,listOf(a,b),listOf(a)));assertEquals(listOf(a),CollectionTabs050.rows("收藏",s,listOf(a,b),listOf(a)));assertTrue(CollectionTabs050.rows("想看",s,listOf(a,b),listOf(a)).isEmpty())}
 @Test fun chinesePosterWinsEvenWhenEnglishVoteHigher(){val j=JSONObject("""{"posters":[{"file_path":"/en.jpg","iso_639_1":"en","vote_average":9},{"file_path":"/zh.jpg","iso_639_1":"zh","vote_average":1}]}""");assertTrue(Presentation050.image(j,"posters",true).endsWith("/zh.jpg"));assertEquals("",Presentation050.image(JSONObject("""{"posters":[{"file_path":"/en.jpg","iso_639_1":"en"}]}"""),"posters",true))}
 @Test fun chineseLogoPreferredAndCacheRoundTripsBothScores(){val j=JSONObject("""{"logos":[{"file_path":"/en.svg","iso_639_1":"en","vote_average":9},{"file_path":"/zh.png","iso_639_1":"zh","vote_average":1}]}""");val p=Presentation050(logo=Presentation050.image(j,"logos",false),ratings=mapOf("douban" to Work("1","甲",rating="8.8"),"tmdb" to null));assertTrue(p.logo.endsWith("/zh.png"));assertEquals(p,Presentation050.from(p.json()))}
 @Test fun m3uAndTvboxTextKeepWholeNamesAndLogos(){val rows=LiveParser050.playlist("""#EXTM3U
#EXTINF:-1 tvg-logo="https://example.org/logo.png" group-title="体育",22:30 国际友谊 沙特 VS 韩国
https://example.org/live.m3u8
""");assertEquals("22:30 国际友谊 沙特 VS 韩国",rows.single().title);assertTrue(rows.single().poster.endsWith("logo.png"));assertEquals("体育",rows.single().roads.single().name);assertEquals(2,LiveParser050.playlist("体育,#genre#\n中文解说,https://example.org/a.m3u8#https://example.org/b.m3u8").size)}
 @Test fun livePortalsAreNotOrdinaryMusicOrCloudSites(){assertTrue(LiveParser050.isPortal(Site("sport","多多体育","csp_Sport")));assertTrue(LiveParser050.isPortal(Site("live","虎牙直播","https://example.org")));assertFalse(LiveParser050.isPortal(Site("music","音乐教学","csp_Music",searchable=false)))}
 @Test fun stremioKeepsAllHttpVariantsAndRejectsTorrentOnly(){val w=Work("1","甲");val j=JSONObject("""{"streams":[{"name":"720p","title":"2 GB","url":"https://example.org/720.mp4"},{"name":"1080p","title":"4 GB","url":"https://example.org/1080.mp4"},{"infoHash":"abc","name":"torrent"}]}""");val v=Stremio050.parse(Site("h","HDHUB","https://example.org",8),w,"正片",j);assertEquals(2,v.roads.size);assertTrue(v.roads.first().name.contains("2 GB"));assertEquals(w.title,v.title)}
 @Test fun liveIdentityAndLastChannelSnapshotAreStable(){val e=LiveEntry050(Site("sport","体育","csp_Live"),Vod("42","甲队 VS 乙队","","","",listOf(Road("中文",listOf(Episode("原生","https://example.org/a.m3u8"))))));assertEquals(e,LiveEntry050.from(e.json()));assertEquals("live",e.work.provider);assertFalse(e.work.json().toString().contains("example.org"))}
}
