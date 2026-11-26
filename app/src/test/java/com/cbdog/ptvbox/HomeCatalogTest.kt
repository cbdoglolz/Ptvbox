package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test

class HomeCatalogTest {
    @Test fun optionalTmdbChannels(){assertEquals(6,HomeCatalog.forCredentials(false).size);assertEquals(8,HomeCatalog.forCredentials(true).size);assertFalse(HomeCatalog.forCredentials(false).any{it.provider=="tmdb"})}
    @Test fun stableDistinctCategories(){assertEquals(8,HomeCatalog.channels.map{it.key}.distinct().size);assertEquals(setOf("动画","动画最新"),HomeCatalog.channels.filter{it.key.startsWith("anime-")}.map{it.category}.toSet());assertEquals(setOf("movie","tv"),HomeCatalog.channels.filter{it.provider=="netflix"}.map{it.kind}.toSet())}
}
