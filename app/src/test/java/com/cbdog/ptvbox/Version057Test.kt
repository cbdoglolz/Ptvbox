package com.cbdog.ptvbox

import org.junit.Assert.*
import org.junit.Test

class Version057Test {
 @Test fun seekAccelerationBoundariesAreStable(){
  assertEquals(5_000L,SeekPolicy057.INITIAL_STEP_MS)
  assertEquals(1_000L,SeekPolicy057.stepForHeld(0))
  assertEquals(1_000L,SeekPolicy057.stepForHeld(4_999))
  assertEquals(60_000L,SeekPolicy057.stepForHeld(5_000))
  assertEquals(60_000L,SeekPolicy057.stepForHeld(9_999))
  assertEquals(300_000L,SeekPolicy057.stepForHeld(10_000))
 }
 @Test fun seekRepeatSlowsForLargeSteps(){
  assertEquals(50L,SeekPolicy057.repeatDelayMs(1_000))
  assertEquals(150L,SeekPolicy057.repeatDelayMs(6_000))
  assertEquals(200L,SeekPolicy057.repeatDelayMs(11_000))
 }
 @Test fun historyTimestampsStayReadable(){val text=HistoryTime057.format(1_700_000_000_000L);assertTrue(text.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))}
 @Test fun animeSearchIsNotLimitedToYear(){val file=listOf(java.io.File("src/main/java/com/cbdog/ptvbox/AnimeSeason056.kt"),java.io.File("app/src/main/java/com/cbdog/ptvbox/AnimeSeason056.kt")).first{it.exists()};val source=file.readText();assertTrue(source.contains("suspend fun search(value:String)"));assertTrue(source.contains(".put(\"search\",value)"))}
 @Test fun pluginNoiseIsHiddenDuringScanning(){assertTrue(NoticePolicy057.suppressPluginNoise("已过滤广告视频 12 条",true,false,false));assertTrue(NoticePolicy057.suppressPluginNoise("扫码观看失败",false,true,false));assertTrue(NoticePolicy057.suppressPluginNoise("需要扫码授权",false,false,false));assertFalse(NoticePolicy057.suppressPluginNoise("播放失败：HTTP 404",true,false,true))}
}
