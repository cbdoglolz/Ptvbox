package com.cbdog.ptvbox

internal object SeekPolicy057 {
 private const val SECOND=1_000L
 const val INITIAL_STEP_MS=5*SECOND
 const val DEBOUNCE_COMMIT_MS=650L
 fun stepForHeld(heldMs:Long):Long=when{
  heldMs<5*SECOND->1*SECOND
  heldMs<10*SECOND->60*SECOND
  else->5*60*SECOND
 }
 fun repeatDelayMs(heldMs:Long):Long=when{
  heldMs<5*SECOND->50L
  heldMs<10*SECOND->150L
  else->200L
 }
}
