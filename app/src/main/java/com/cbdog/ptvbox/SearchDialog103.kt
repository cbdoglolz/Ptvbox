package com.cbdog.ptvbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

/** Bounded content instead of the full-width settings panel, including on wide TVs. */
@Composable internal fun SearchDialog103(title:String,description:String,onClose:()->Unit,content:@Composable ColumnScope.()->Unit={}) {
    val c=LocalCinema.current
    val close=remember{FocusRequester()}
    Dialog(onClose,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Column(Modifier.widthIn(max=304.dp).fillMaxWidth(.9f)
            .background(c.surface,RoundedCornerShape(18.dp))
            .border(1.dp,c.border,RoundedCornerShape(18.dp)).padding(16.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(title,color=c.text,fontSize=18.sp,fontWeight=FontWeight.SemiBold)
            Text(description,color=c.muted,fontSize=12.sp)
            content()
            InputButton(onClose,Modifier.focusRequester(close).fillMaxWidth().height(34.dp),contentPadding=PaddingValues(4.dp)){Text("关闭",color=c.text,fontSize=14.sp)}
        }
        LaunchedEffect(Unit){withFrameNanos{};close.requestFocus()}
    }
}
