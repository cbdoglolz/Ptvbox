package com.cbdog.ptvbox

import java.time.LocalDate

internal data class SplitSeasonTarget102(
    val splitSeason: Int,
    val localEpisode: Int,
    val seasonStartOffset: Int,
    val expectedYear: String = ""
)

internal object SplitSeason102 {
    private val cnNums = mapOf(
        1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五",
        6 to "六", 7 to "七", 8 to "八", 9 to "九", 10 to "十"
    )
    private val excludeRegex = Regex("解说|預告|预告|花絮|幕后|抢先看|剧场版|电影版|总集篇|総集編|\\bOVA\\b|\\bOAD\\b|\\bSP\\b", RegexOption.IGNORE_CASE)
    private val partRegex = Regex("(?i)(?:part|cour)\\s*(\\d{1,2})")

    /**
     * Detects domestic split-season boundaries from TMDB Season 1 air-date gaps (>= 60 days hiatus
     * after at least 6 aired episodes in the previous cour/season).
     * Example: Apothecary Diaries TMDB S1 (1..24 in 2023-2024, 25..48 in 2025, 49..60 in 2026)
     * maps S1E29 -> SplitSeasonTarget102(splitSeason=2, localEpisode=5, seasonStartOffset=24, expectedYear="2025").
     */
    fun fromSeries(series: SeriesMeta?, workSeason: Int, absEpisode: Int): SplitSeasonTarget102? {
        if (workSeason != 1 || absEpisode <= 12) return null
        val eps = series?.episodes
            ?.filter { it.season == workSeason && it.number > 0 }
            ?.sortedBy { it.number }
            .orEmpty()
        if (eps.size < 13) return null

        var currentSplit = 1
        var offset = 0
        var blockCount = 0
        var prevDay: Long? = null
        var prevNumber = 0

        for (ep in eps) {
            val day = parseEpochDay(ep.airDate)
            if (day != null && prevDay != null && day - prevDay >= 60L && blockCount >= 6) {
                currentSplit++
                offset = prevNumber
                blockCount = 0
            }
            if (ep.number == absEpisode) {
                if (currentSplit <= 1) return null
                val local = absEpisode - offset
                if (local <= 0) return null
                return SplitSeasonTarget102(
                    splitSeason = currentSplit,
                    localEpisode = local,
                    seasonStartOffset = offset,
                    expectedYear = ep.airDate.take(4)
                )
            }
            blockCount++
            if (day != null) prevDay = day
            prevNumber = ep.number
        }
        return null
    }

    fun detectVodSeason(title: String): Int {
        Identity.season(title)?.let { return it }
        partRegex.find(title)?.groupValues?.get(1)?.toIntOrNull()?.let { if (it in 2..12) return it }
        Identity.trailingSeason(title)?.let { return it }
        if (title.contains("续篇") || title.contains("續篇") || title.contains("下篇") || title.contains("后篇") || title.contains("後篇")) return 2
        return 1
    }

    private fun coreBase(title: String): String {
        val stripped = Identity.stripTrailingSeason(Identity.searchTitle(title))
            .replace(Regex("(?:续篇|續篇|下篇|后篇|後篇|完结篇|完結篇)$"), "")
            .trim()
        return Identity.normalize(stripped)
    }

    /**
     * Collects VODs from search hits belonging to the same franchise across split seasons (S1, S2, S3...),
     * allowing multi-year gaps for sequel seasons (vodYear in workYear-1 .. workYear+8).
     */
    fun seriesVods(work: Work, hits: List<Vod>): List<Pair<Int, Vod>> {
        if (work.kind != "tv") return emptyList()
        val workNames = (listOf(work.title, work.original) + work.aliases + Identity.overviewAliases(work.overview))
            .map(::coreBase)
            .filter { it.length >= 2 }
            .distinct()
        if (workNames.isEmpty()) return emptyList()
        val wy = work.year.take(4).toIntOrNull()
        return hits
            .distinctBy { it.id }
            .filter { vod ->
                if (excludeRegex.containsMatchIn(vod.title)) return@filter false
                val vb = coreBase(vod.title)
                if (vb !in workNames) return@filter false
                val vy = vod.year.take(4).toIntOrNull()
                if (wy != null && vy != null && (vy < wy - 1 || vy > wy + 8)) return@filter false
                true
            }
            .map { detectVodSeason(it.title) to it }
            .sortedBy { it.first }
    }

    fun splitQueries(work: Work, target: SplitSeasonTarget102?, absEpisode: Int): List<String> {
        val core = Identity.searchTitle(work.title).ifBlank { work.title }.trim()
        if (core.length < 2) return emptyList()
        val seasonNum = target?.splitSeason ?: when {
            absEpisode > 48 -> 3
            else -> 2
        }
        val cn = cnNums[seasonNum]
        return buildList {
            add(core)
            if (cn != null) {
                add("${core}第${cn}季")
                add("$core 第${cn}季")
            }
            add("${core}第${seasonNum}季")
        }.distinct()
    }

    /**
     * Fallback when TMDB air-date metadata is unavailable: sums real episode counts from site S1, S2, S3...
     * VOD details to determine which season VOD and local episode number correspond to absEpisode.
     */
    fun resolveByCumulative(orderedDetails: List<Pair<Int, Vod>>, absEpisode: Int): Triple<Vod, Int, Int>? {
        if (absEpisode <= 0 || orderedDetails.isEmpty()) return null
        val sorted = orderedDetails.sortedBy { it.first }
        // First check if any season VOD directly uses absolute episode numbering (e.g. S2 labeled 第25集..第48集).
        for ((seasonNum, vod) in sorted) {
            if (vod.roads.any { r -> Identity.pick(r, "第%02d集".format(absEpisode), "tv") != null }) {
                return Triple(vod, seasonNum, absEpisode)
            }
        }
        var offset = 0
        for ((seasonNum, vod) in sorted) {
            val count = maxEpisodeCount(vod)
            if (count <= 0) continue
            if (absEpisode > offset && absEpisode <= offset + count) {
                return Triple(vod, seasonNum, absEpisode - offset)
            }
            offset += count
        }
        return null
    }

    fun maxEpisodeCount(vod: Vod): Int = vod.roads.maxOfOrNull { road ->
        val maxNumber = road.episodes.mapNotNull { Identity.episode(it.name) }.maxOrNull() ?: 0
        maxOf(road.episodes.size, maxNumber)
    } ?: 0

    private fun parseEpochDay(raw: String): Long? {
        val clean = raw.trim().take(10)
        if (clean.length < 10) return null
        return runCatching { LocalDate.parse(clean).toEpochDay() }.getOrNull()
    }
}
