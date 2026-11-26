package com.cbdog.ptvbox

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerTouch060Test {
    @Test fun tapZonesUseLeftAndRightThirds(){
        assertEquals("left",PlayerTouch060.zone(100f,1000f))
        assertEquals("center",PlayerTouch060.zone(500f,1000f))
        assertEquals("right",PlayerTouch060.zone(900f,1000f))
    }

    @Test fun horizontalDragMapsAcrossMediaDuration(){
        assertEquals(50_000L,PlayerTouch060.target(40_000,100f,1000f,100_000))
        assertEquals(0L,PlayerTouch060.target(40_000,-900f,1000f,100_000))
        assertEquals(100_000L,PlayerTouch060.target(99_000,1000f,1000f,100_000))
    }

    @Test fun doubleTapSeekUsesFiveSeconds(){
        assertEquals(5_000L,PlayerTouch060.SEEK_STEP_MS)
    }
}
