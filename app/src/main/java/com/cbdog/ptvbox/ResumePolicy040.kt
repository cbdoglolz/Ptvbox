package com.cbdog.ptvbox

object ResumePolicy040 {
    fun eligible(expected:Int,tagged:Int)=expected<=0||tagged<=0||tagged>=expected
    fun decodedAccepts(expected:Int,actual:Int,automatic:Boolean)=!automatic||expected<=0||actual<=0||actual>=expected
}
