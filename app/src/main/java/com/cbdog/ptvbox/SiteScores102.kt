package com.cbdog.ptvbox

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Persistent site quality score tracker.
 * +1.0 when a site returns a playable resource;
 * +0.5 when a cloud/auth site has matching entries awaiting scan/login;
 * -1.0 when a site returns no playable result or fails during scan.
 */
object SiteScores102 {
    private const val PREFIX = "site_score_v1_"
    private const val DEDUPE_KEY = "site_score_dedupe_v1"

    data class BadgeStyle(val background: Color, val border: Color, val text: Color)

    fun score(db: Library, siteKey: String): Float = db.prefs.getFloat(PREFIX + siteKey, 0f)

    fun record(db: Library, siteKey: String, delta: Float): Float {
        val next = ((score(db, siteKey) + delta) * 2f).roundToInt() / 2f
        val clamped = next.coerceIn(-99f, 99f)
        db.prefs.edit().putFloat(PREFIX + siteKey, clamped).apply()
        return clamped
    }

    /** Record score at most once per (work, season, episode, site) so replaying/re-searching never double-penalizes or double-rewards. */
    @Synchronized
    fun recordOnce(db: Library, workKey: String, season: Int, episode: String, siteKey: String, delta: Float): Boolean {
        val epToken = Identity.episode(episode)?.toString() ?: episode.trim().ifBlank { "0" }
        val entry = "$workKey|S$season|E$epToken|$siteKey"
        val seen = db.prefs.getStringSet(DEDUPE_KEY, emptySet()).orEmpty().toMutableSet()
        if (entry in seen) return false
        if (seen.size >= 4000) seen.clear()
        seen.add(entry)
        val next = ((score(db, siteKey) + delta) * 2f).roundToInt() / 2f
        db.prefs.edit()
            .putFloat(PREFIX + siteKey, next.coerceIn(-99f, 99f))
            .putStringSet(DEDUPE_KEY, seen)
            .apply()
        return true
    }

    fun resetAll(db: Library, keys: Collection<String>) {
        val edit = db.prefs.edit()
        keys.forEach { edit.remove(PREFIX + it) }
        edit.remove(DEDUPE_KEY)
        edit.apply()
    }

    fun format(score: Float): String {
        val snapped = (score * 2f).roundToInt() / 2f
        if (abs(snapped) < 0.01f) return "0"
        val sign = if (snapped > 0f) "+" else "-"
        val mag = abs(snapped)
        return if (abs(mag - mag.toInt()) < 0.01f) "$sign${mag.toInt()}" else "$sign$mag"
    }

    /** Distinct multi-tier color ramp: deep emerald for positive, neutral slate for zero, vivid crimson for negative. */
    fun badgeStyle(score: Float): BadgeStyle = when {
        score >= 5.0f -> BadgeStyle(Color(0xFF0E4429), Color(0xFF22C55E), Color(0xFF4ADE80))
        score >= 2.0f -> BadgeStyle(Color(0xFF113825), Color(0xFF16A34A), Color(0xFF34D399))
        score > 0.01f -> BadgeStyle(Color(0xFF0F2D22), Color(0xFF10B981), Color(0xFF6EE7B7))
        score < -4.99f -> BadgeStyle(Color(0xFF8C0F1A), Color(0xFFFF3B30), Color(0xFFFFFFFF))
        score < -1.99f -> BadgeStyle(Color(0xFF6B161F), Color(0xFFEF4444), Color(0xFFFFCDD2))
        score < -0.01f -> BadgeStyle(Color(0xFF451920), Color(0xFFF87171), Color(0xFFFCA5A5))
        else -> BadgeStyle(Color(0xFF222536), Color(0xFF475569), Color(0xFF94A3B8))
    }

    fun sortSites(sites: List<Site>, scoreOf: (Site) -> Float, enabledOf: (Site) -> Boolean): List<Site> =
        sites.sortedWith(
            compareByDescending<Site> { scoreOf(it) }
                .thenByDescending { enabledOf(it) }
                .thenBy { it.name.lowercase() }
        )
}
