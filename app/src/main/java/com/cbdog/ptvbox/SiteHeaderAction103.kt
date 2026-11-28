package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

/** Foundation's common input path handles touchscreen, mouse, D-pad and accessibility together. */
@Composable internal fun SiteHeaderAction103(label:String,onClick:()->Unit){
    var focused by remember{mutableStateOf(false)}
    val c=LocalCinema.current;val shape=RoundedCornerShape(8.dp)
    Box(Modifier.heightIn(min=48.dp).onFocusChanged{focused=it.isFocused}
        .background(if(focused)PtvFocusFill else c.raised,shape)
        .border(if(focused)2.dp else 1.dp,if(focused)PtvFocusBorder else SolidColor(c.border),shape)
        .clickable(role=Role.Button,onClick=onClick).semantics{contentDescription=label}
        .padding(horizontal=13.dp),contentAlignment=androidx.compose.ui.Alignment.Center){Text(label,color=c.text,fontSize=13.sp)}
}
