package com.cbdog.ptvbox

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import coil3.compose.AsyncImage

private data class HeroSlide053(val work: Work, val logo: String)

@Composable
internal fun Hero053(
    m: PtvModel,
    original: Work,
    page: Int,
    count: Int,
    visible: Boolean,
    onEnter: () -> Unit = {},
    select: (Int) -> Unit
) {
    val (base, art) = presented050(m, original)
    val w = art?.ratings?.values?.filterNotNull()?.fold(base) { v, r -> v.enriched(r) } ?: base
    val colors = LocalCinema.current
    val links = m.linkedRatings[w.ratingViewKey]
    val doubanScore = (if (links?.containsKey("douban") == true) links["douban"] else w.takeIf { it.provider == "douban" })?.rating?.takeIf { it.isNotBlank() && it != "-" }
    val tmdbScore = (if (links?.containsKey("tmdb") == true) links["tmdb"] else w.takeIf { it.provider == "tmdb" })?.takeIf { m.tmdbConfigured }?.rating?.takeIf { it.isNotBlank() && it != "-" }
    var heroFocused by remember { mutableStateOf(false) }

    // Pause auto-advance while user's cursor is focused on Hero controls
    LaunchedEffect(original.key, page, count, visible, heroFocused) {
        if (visible && count > 1 && !heroFocused) {
            kotlinx.coroutines.delay(8000L)
            select((page + 1) % count)
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(282.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border.copy(alpha = .85f), RoundedCornerShape(16.dp))
            .semantics { contentDescription = "横幅 ${page + 1}/$count · ${w.title}" }
    ) {
        // Pure visual layer inside AnimatedContent (never destroys focusable buttons!)
        AnimatedContent(
            targetState = HeroSlide053(w, art?.logo.orEmpty()),
            transitionSpec = { fadeIn(tween(320)) togetherWith fadeOut(tween(220)) },
            contentKey = { it.work.key },
            label = "横幅切换"
        ) { slide ->
            val item = slide.work
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = m.metadata.doubanHdImage(item.backdrop.ifBlank { item.poster }),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = .72f
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.horizontalGradient(
                            0.0f to colors.background.copy(alpha = .97f),
                            0.45f to colors.background.copy(alpha = .86f),
                            0.76f to colors.background.copy(alpha = .30f),
                            1.0f to Color.Transparent
                        )
                    )
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1.0f to colors.background.copy(alpha = .90f)
                        )
                    )
                )
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left poster with dual-rating pill placed cleanly BELOW outside the poster image
                    Column(
                        Modifier.width(140.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .width(140.dp)
                                .height(210.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.raised)
                                .border(1.dp, Color(0x6694A3B8), RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = item.poster,
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(Color(0xD40B0D17), RoundedCornerShape(7.dp))
                                .border(1.dp, Color(0x55475569), RoundedCornerShape(7.dp))
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("豆瓣 ${doubanScore ?: "-"}", color = if (doubanScore != null) PcGreen else Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            if (m.config.flag("secondary_rating", true)) {
                                Text(" · ", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("TMDB ${tmdbScore ?: "-"}", color = if (tmdbScore != null) PcCyan else Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }

                    Column(
                        Modifier.weight(1f).padding(end = 110.dp, bottom = 52.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.background(Color(0xCC3B2769), RoundedCornerShape(6.dp)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                Text("精选推荐", color = Color(0xFFDDD6FE), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                            if (item.year.isNotBlank()) {
                                Box(Modifier.background(Color(0xB81E2238), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text(item.year, color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                }
                            }
                            Box(Modifier.background(Color(0xB81E2238), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                Text(if (item.kind == "tv") "剧集" else "电影", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                            val genres = item.genres.take(3).joinToString(" / ")
                            if (genres.isNotBlank()) {
                                Box(Modifier.background(Color(0xB81E2238), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                                    Text(genres, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        if (slide.logo.isNotBlank()) {
                            AsyncImage(
                                model = slide.logo,
                                contentDescription = item.title,
                                modifier = Modifier.widthIn(max = 320.dp).height(46.dp),
                                contentScale = ContentScale.Fit,
                                alignment = Alignment.CenterStart
                            )
                        } else {
                            Text(item.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Text(
                            item.overview.ifBlank { "正在补充故事简介…" },
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        val cast = item.people.filter { it.role !in listOf("导演", "Director") }.map { it.name }.distinct().take(4)
                        if (cast.isNotEmpty()) {
                            Text("主演：${cast.joinToString(" · ")}", color = Color(0xFF94A3B8), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Stable focusable CTA row OUTSIDE AnimatedContent so focus NEVER jumps on slide switch!
        val heroBtnColors = ButtonDefaults.colors(
            containerColor = Color(0xCC161A2C),
            contentColor = Color(0xFFD6DFEE),
            focusedContainerColor = Color(0xFF32406E),
            focusedContentColor = Color.White,
            pressedContainerColor = Color(0xFF3E4E84),
            pressedContentColor = Color.White
        )
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 184.dp, bottom = 16.dp)
                .onFocusChanged {
                    val was = heroFocused
                    heroFocused = it.hasFocus
                    if (!was && it.hasFocus) onEnter()
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InputButton(
                onClick = { m.focusRow = "hero"; m.open(w) },
                modifier = Modifier.then(workFocus(m, w, "hero")).border(1.dp, Color(0x70586580), RoundedCornerShape(10.dp)),
                shape = ButtonDefaults.shape(RoundedCornerShape(10.dp)),
                focusRadius = 10.dp,
                scale = ButtonDefaults.scale(1f, 1.045f, .98f),
                colors = heroBtnColors,
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 9.dp)
            ) {
                Text("详情", fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            if (count > 1) {
                InputButton(
                    onClick = { select((page + 1) % count) },
                    modifier = Modifier.border(1.dp, Color(0x70586580), RoundedCornerShape(10.dp)),
                    shape = ButtonDefaults.shape(RoundedCornerShape(10.dp)),
                    focusRadius = 10.dp,
                    scale = ButtonDefaults.scale(1f, 1.045f, .98f),
                    colors = heroBtnColors,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text("下一部 ›", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }

        if (count > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 18.dp)
                    .background(Color(0xA60B0D17), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(count) { i ->
                    Box(
                        Modifier
                            .width(if (page == i) 22.dp else 7.dp)
                            .height(6.dp)
                            .background(
                                if (page == i) PcAccent else Color(0x8864748B),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}
