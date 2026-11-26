package com.cbdog.ptvbox
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

class Presentation030Test {
    @Test fun subscriptionSearchFlagsDoNotDeleteSites(){for(flag in listOf("0","false")){val j=org.json.JSONObject("""{"key":"s","name":"Site","api":"csp_S","type":3,"searchable":$flag}""");val s=Site.from(j);assertFalse(s.searchable);assertEquals("s",s.key);assertFalse(Site.from(s.json()).searchable)};assertTrue(Site.from(org.json.JSONObject("""{"key":"s"}""")).searchable)}
    @Test fun fullDatesAndUnknownSort(){val a=Work("a","A",releaseDate="2020-09-01");val b=Work("b","B",releaseDate="2020-01-20");val c=Work("c","C",year="2020");assertEquals(listOf("b","a","c"),CatalogPresentation.filtered(listOf(a,c,b),sort=1).map{it.id});assertEquals(listOf("a","b","c"),CatalogPresentation.filtered(listOf(a,c,b),sort=2).map{it.id})}
    @Test fun upcomingUsesActualDate(){val w=Work("a","A",releaseDate="2026-11-21");assertTrue(w.isUpcoming(LocalDate.parse("2026-11-20")));assertFalse(w.isUpcoming(LocalDate.parse("2026-11-21")));assertFalse(w.copy(releaseDate="").isUpcoming(LocalDate.parse("2026-11-20")))}
    @Test fun additiveMetadataRoundTrips(){val w=Work("a","A",identityKey="legacy:a",releaseDate="2025-02-06",votes=35);assertEquals(w,Work.from(w.json()));assertEquals("上映于2025年2月6日",w.releaseLabel())}
    @Test fun backClosesPanelThenControlsThenDetails(){val first=PlayerOverlay().open("sources").back();assertFalse(first.second);assertEquals("",first.first.panel);val second=first.first.back();assertFalse(second.second);assertFalse(second.first.controls);assertTrue(second.first.back().second)}
    @Test fun backCancelsSeekBeforeExit(){val u=PlayerOverlay(controls=false,previewMs=10000);val result=u.back();assertFalse(result.second);assertNull(result.first.previewMs);assertTrue(result.first.back().second)}
}
