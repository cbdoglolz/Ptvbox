package com.cbdog.ptvbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Tracks
import androidx.tv.material3.Text

internal enum class MediaBadgeKind { RESOLUTION, DOLBY_VISION, HDR, ATMOS, AUDIO_CODEC, CHANNELS, LANGUAGE }

internal data class MediaBadge102(
    val label: String,
    val kind: MediaBadgeKind,
    val dolbyMark: Boolean = false
)

internal object SourceBadgeParser102 {
    private val langPatterns: List<Pair<String, Regex>> = listOf(
        "国语" to Regex("国语|国配|普通话|mandarin|(?i)(?:^|[^a-z])(?:chi|chs|cmn|zh[-_]?cn)(?:[^a-z]|$)"),
        "粤语" to Regex("粤语|粤配|cantonese|(?i)(?:^|[^a-z])(?:yue|zh[-_]?hk)(?:[^a-z]|$)"),
        "英语" to Regex("英语|英配|english|(?i)(?:^|[^a-z])(?:eng)(?:[^a-z]|$)"),
        "日语" to Regex("日语|日配|japanese|(?i)(?:^|[^a-z])(?:jpn)(?:[^a-z]|$)"),
        "韩语" to Regex("韩语|韩配|korean|(?i)(?:^|[^a-z])(?:kor)(?:[^a-z]|$)")
    )

    fun parse(text: String, heightHint: Int = 0): List<MediaBadge102> {
        val out = mutableListOf<MediaBadge102>()
        val h = heightHint.takeIf { it > 0 } ?: ResourceLabels040.height(text)
        when {
            h >= 2160 || Regex("(?i)\\b(?:4k|uhd|2160p)\\b|4K").containsMatchIn(text) ->
                out += MediaBadge102("4K", MediaBadgeKind.RESOLUTION)
            h >= 1080 || Regex("(?i)\\b1080[pi]\\b|蓝光").containsMatchIn(text) ->
                out += MediaBadge102("1080P", MediaBadgeKind.RESOLUTION)
            h >= 720 || Regex("(?i)\\b720[pi]\\b").containsMatchIn(text) ->
                out += MediaBadge102("720P", MediaBadgeKind.RESOLUTION)
        }

        val hasDv = Regex("(?i)dolby[ ._-]*vision|\\b(?:dv|dovi)\\b|杜比视界").containsMatchIn(text)
        val hasHdr10Plus = Regex("(?i)\\bhdr10\\+|hdr10plus").containsMatchIn(text)
        val hasHdr = Regex("(?i)\\bhdr(?:10)?\\b|\\bhlg\\b").containsMatchIn(text)
        if (hasDv) out += MediaBadge102("DV", MediaBadgeKind.DOLBY_VISION, dolbyMark = true)
        if (hasHdr10Plus) out += MediaBadge102("HDR10+", MediaBadgeKind.HDR)
        else if (hasHdr) out += MediaBadge102("HDR", MediaBadgeKind.HDR)

        val hasAtmos = Regex("(?i)atmos|杜比全景声|全景声").containsMatchIn(text)
        val hasTrueHd = Regex("(?i)truehd").containsMatchIn(text)
        val hasDts = Regex("(?i)\\bdts(?:[- .]?hd)?\\b").containsMatchIn(text)
        val hasDdp = Regex("(?i)\\b(?:ddp|dd\\+|e-?ac-?3|dolby[ ._-]*digital)\\b|杜比音效|杜比").containsMatchIn(text)
        if (hasAtmos) out += MediaBadge102("ATMOS", MediaBadgeKind.ATMOS, dolbyMark = true)
        else if (hasTrueHd) out += MediaBadge102("TrueHD", MediaBadgeKind.AUDIO_CODEC)
        else if (hasDts) out += MediaBadge102("DTS", MediaBadgeKind.AUDIO_CODEC)
        else if (hasDdp) out += MediaBadge102("DD+", MediaBadgeKind.AUDIO_CODEC, dolbyMark = true)

        when {
            Regex("(?i)(?:^|[^0-9])7\\.1(?:[^0-9]|$)|\\b8ch\\b").containsMatchIn(text) ->
                out += MediaBadge102("7.1", MediaBadgeKind.CHANNELS)
            Regex("(?i)(?:^|[^0-9])5\\.1(?:[^0-9]|$)|\\b6ch\\b").containsMatchIn(text) ->
                out += MediaBadge102("5.1", MediaBadgeKind.CHANNELS)
        }

        val explicitMulti = Regex("(?i)\\bmulti\\b|多语|多音轨").containsMatchIn(text)
        val explicitDual = Regex("(?i)\\bdual\\b|双语|国英|国粤|中英").containsMatchIn(text)
        val langs = langPatterns.filter { it.second.containsMatchIn(text) }.map { it.first }
        when {
            explicitMulti || langs.size >= 3 -> out += MediaBadge102("多语", MediaBadgeKind.LANGUAGE)
            explicitDual -> {
                out += MediaBadge102("双语", MediaBadgeKind.LANGUAGE)
                langs.take(2).forEach { out += MediaBadge102(it, MediaBadgeKind.LANGUAGE) }
            }
            else -> langs.take(2).forEach { out += MediaBadge102(it, MediaBadgeKind.LANGUAGE) }
        }
        if (Regex("中字|简繁|双字|特效字幕|中文字幕").containsMatchIn(text)) {
            out += MediaBadge102("中字", MediaBadgeKind.LANGUAGE)
        }
        return out.distinctBy { it.label }
    }
}

@Composable
internal fun SourceMediaBadges102(badges: List<MediaBadge102>, modifier: Modifier = Modifier) {
    if (badges.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        badges.forEach { badge ->
            val (bg, border, fg) = when (badge.kind) {
                MediaBadgeKind.RESOLUTION -> if (badge.label == "4K") Triple(Color(0xD9422C08), Color(0xFFF59E0B), Color(0xFFFDE68A)) else Triple(Color(0xCC0D2E42), Color(0xFF38BDF8), Color(0xFFBAE6FD))
                MediaBadgeKind.DOLBY_VISION -> Triple(Color(0xD92D1554), Color(0xFFA855F7), Color(0xFFE9D5FF))
                MediaBadgeKind.HDR -> Triple(Color(0xD945230A), Color(0xFFFB923C), Color(0xFFFED7AA))
                MediaBadgeKind.ATMOS -> Triple(Color(0xD90E2754), Color(0xFF3B82F6), Color(0xFFBFDBFE))
                MediaBadgeKind.AUDIO_CODEC -> Triple(Color(0xCC1E2642), Color(0xFF818CF8), Color(0xFFE0E7FF))
                MediaBadgeKind.CHANNELS -> Triple(Color(0xCC0F362C), Color(0xFF10B981), Color(0xFFA7F3D0))
                MediaBadgeKind.LANGUAGE -> Triple(Color(0xCC24293D), Color(0xFF94A3B8), Color(0xFFE2E8F0))
            }
            Row(
                Modifier
                    .height(20.dp)
                    .background(bg, RoundedCornerShape(4.dp))
                    .border(1.dp, border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (badge.dolbyMark) {
                    Canvas(Modifier.size(11.dp, 9.dp)) {
                        val w = size.width
                        val h = size.height
                        // Unified geometric double-D emblem
                        val leftD = Path().apply {
                            moveTo(0f, 0f); lineTo(w * 0.22f, 0f)
                            cubicTo(w * 0.46f, 0f, w * 0.46f, h, w * 0.22f, h)
                            lineTo(0f, h); close()
                        }
                        val rightD = Path().apply {
                            moveTo(w, 0f); lineTo(w * 0.78f, 0f)
                            cubicTo(w * 0.54f, 0f, w * 0.54f, h, w * 0.78f, h)
                            lineTo(w, h); close()
                        }
                        drawPath(leftD, fg)
                        drawPath(rightD, fg)
                    }
                }
                Text(
                    badge.label,
                    color = fg,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

internal object TrackLanguagePolicy102 {
    val audioOptions: List<Pair<String, String>> = listOf(
        "zh" to "中文（国/粤）",
        "en" to "英语",
        "ja" to "日语",
        "ko" to "韩语",
        "default" to "片源默认"
    )
    val subtitleOptions: List<Pair<String, String>> = listOf(
        "zh" to "中文（简/繁）",
        "en" to "英语",
        "ja" to "日语",
        "ko" to "韩语",
        "default" to "片源默认",
        "off" to "默认关闭"
    )

    fun audioLabelOf(code: String): String = audioOptions.firstOrNull { it.first == code }?.second ?: code
    fun subtitleLabelOf(code: String): String = subtitleOptions.firstOrNull { it.first == code }?.second ?: code
    fun labelOf(code: String): String = subtitleLabelOf(code)

    fun classify(language: String?, label: String?): String {
        val rawLang = language.orEmpty().trim().lowercase()
        val rawLabel = label.orEmpty().trim().lowercase()
        val combined = "$rawLang $rawLabel"
        return when {
            rawLang.startsWith("zh") || rawLang in setOf("zho", "chi", "cmn", "yue") ||
                Regex("中文|国语|普通话|简体|繁体|中字|粤语|汉语|mandarin|cantonese|chinese|chs|cht|hans|hant").containsMatchIn(combined) -> "zh"
            rawLang.startsWith("en") || rawLang == "eng" ||
                Regex("英语|英文|english").containsMatchIn(combined) -> "en"
            rawLang.startsWith("ja") || rawLang in setOf("jpn", "jp") ||
                Regex("日语|日文|japanese").containsMatchIn(combined) -> "ja"
            rawLang.startsWith("ko") || rawLang in setOf("kor", "kr") ||
                Regex("韩语|韩文|korean").containsMatchIn(combined) -> "ko"
            else -> ""
        }
    }

    fun isSimplifiedChinese(language: String?, label: String?): Boolean {
        val s = "${language.orEmpty()} ${label.orEmpty()}".lowercase()
        return Regex("简体|简中|chs|hans|zh[-_]?cn|国语|普通话").containsMatchIn(s)
    }

    /** Returns Pair(group, trackIndex) for the best matching track, or null to keep stream default. */
    fun pickTrack(
        tracks: Tracks,
        trackType: Int,
        primary: String,
        secondary: String
    ): Pair<Tracks.Group, Int>? {
        val candidates = tracks.groups
            .filter { it.type == trackType }
            .flatMap { g -> (0 until g.length).map { i -> Triple(g, i, g.getTrackFormat(i)) } }
        if (candidates.isEmpty()) return null
        val prefs = listOf(primary, secondary).filter { it.isNotBlank() && it != "default" && it != "off" }.distinct()
        for (pref in prefs) {
            val matched = candidates.filter { (_, _, f) -> classify(f.language, f.label) == pref }
            if (matched.isNotEmpty()) {
                val chosen = if (pref == "zh") {
                    matched.firstOrNull { (_, _, f) -> isSimplifiedChinese(f.language, f.label) } ?: matched.first()
                } else matched.first()
                return chosen.first to chosen.second
            }
        }
        return null
    }

    fun scoreOnlineSubtitle(title: String, detail: String, primary: String, secondary: String): Int {
        val cls = classify("", "$title $detail")
        return when {
            cls == primary && primary == "zh" && isSimplifiedChinese("", "$title $detail") -> 300
            cls == primary -> 250
            cls == secondary -> 150
            cls == "zh" -> 100
            else -> 0
        }
    }

    fun exoLanguageHints(primary: String, secondary: String): Array<String> =
        listOf(primary, secondary).flatMap {
            when (it) {
                "zh" -> listOf("zh", "zho", "chi", "cmn", "yue")
                "en" -> listOf("en", "eng")
                "ja" -> listOf("ja", "jpn")
                "ko" -> listOf("ko", "kor")
                else -> emptyList()
            }
        }.distinct().toTypedArray()
}
