package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test

class HomeCatalogTest {
    @Test fun optionalTmdbChannels(){
        assertEquals(5,HomeCatalog.forCredentials(false).size)
        assertEquals(5,HomeCatalog.forCredentials(true).size)
        assertTrue(HomeCatalog.channels.any{it.key=="continue-watching"&&it.kind=="continue"})
    }
    @Test fun stableDistinctCategories(){
        assertEquals(5,HomeCatalog.channels.map{it.key}.distinct().size)
        assertEquals(listOf("continue-watching","douban-tv","douban-movie","anime-hot","netflix-tv"),HomeCatalog.channels.map{it.key})
    }
}
