package com.cbdog.ptvbox

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPriority060Test {
    private fun source(path:String)=listOf(java.io.File(path),java.io.File("app/$path")).first{it.exists()}.readText()

    @Test fun desktopSettingsKeepSitesOnlyInPortalAndRemoveLegacyCards(){
        assertTrue(SettingsInventory.sections.none{it.id=="sites"})
        assertTrue(SettingsInventory.portalSections.any{it.id=="sites"})
        assertFalse(SettingsInventory.sections.any{it.id=="appearance"})
        val settings=source("src/main/java/com/cbdog/ptvbox/Settings040.kt")
        assertFalse(settings.contains("当前站点 / 默认解析器"))
        assertFalse(settings.contains("ATV系统首页"))
        assertFalse(settings.contains("海报主评分"))
        assertFalse(settings.contains("默认倍速"))
        assertTrue(settings.contains("网络与缓存"))
        assertTrue(settings.contains("AppUpdate040(m)"))
        assertFalse(SettingsInventory.sections.any{it.id in setOf("cache","privacy","updates")})
    }

    @Test fun settingsUseDialogsFreeProvidersAndVisiblePrivacyRule(){
        val settings=source("src/main/java/com/cbdog/ptvbox/Settings040.kt")
        assertTrue(settings.contains("ConfigValueCard"))
        assertTrue(settings.contains("免费公共字幕 · 无需Key"))
        assertTrue(settings.contains("免费公共弹幕 · 无需配置"))
        assertTrue(settings.contains("密码与密钥始终明文显示"))
        assertFalse(settings.contains("隐藏密码与密钥"))
        assertTrue(settings.contains("横向飞行距离"))
        assertTrue(settings.contains("\"danmaku_color\""))
    }

    @Test fun publicSubtitleIsFirstAndCredentialFree(){
        val subtitles=source("src/main/java/com/cbdog/ptvbox/OnlineSubtitles053.kt")
        assertTrue(subtitles.contains("listOf(\"OpenSubtitles V3\")+"))
        assertTrue(subtitles.contains("opensubtitles-v3.strem.io"))
        val configured=subtitles.substring(subtitles.indexOf("fun configured()"))
        assertTrue(configured.indexOf("OpenSubtitles V3")<configured.indexOf("SubDL"))
    }

    @Test fun artworkPrecedesEpisodeDetailsAndUsesShortCache(){
        val presentation=source("src/main/java/com/cbdog/ptvbox/Presentation050.kt")
        assertTrue(presentation.contains("else\"w1280\""))
        assertTrue(presentation.contains("12*60*60*1000L"))
        assertTrue(presentation.indexOf("artwork(tm)")<presentation.indexOf("for(p in listOf(\"douban\"))"))
        val detail=source("src/main/java/com/cbdog/ptvbox/CinemaDetail040.kt")
        assertTrue(detail.contains("blur(7.dp)"))
        val episodes=source("src/main/java/com/cbdog/ptvbox/EpisodeCatalogRepository.kt")
        assertTrue(episodes.contains("优先加载中文片名与海报"))
        assertTrue(episodes.contains("if(cached==null)delay(800)"))
        val anime=source("src/main/java/com/cbdog/ptvbox/AnimeSeason056.kt")
        assertTrue(anime.contains("中文名补充中"))
        assertTrue(anime.contains("12*3600000L"))
    }
}
