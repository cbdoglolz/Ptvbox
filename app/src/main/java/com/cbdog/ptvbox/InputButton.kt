package com.cbdog.ptvbox

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.tv.material3.*

val PtvFocusFill=Color(0x73343B54)
val PtvFocusPressed=PtvFocusFill.copy(alpha=.92f)
val PtvFocusGlow=Color(0x664FBCE8)
val PtvFocusBorder=Brush.linearGradient(listOf(Color(0xff53D7FF),Color(0xffB87CFF),Color(0xffF0A2FF)))

@Composable
internal fun ptvButtonColors(container:Color,content:Color):ButtonColors=ButtonDefaults.colors(
    containerColor=container,
    contentColor=content,
    focusedContainerColor=PtvFocusFill,
    focusedContentColor=Color.White,
    pressedContainerColor=PtvFocusPressed,
    pressedContentColor=Color.White
)

/** TV Material 1.1.0 handles D-pad and accessibility clicks, but not pointer taps.
 * Add only pointer gestures; keep its existing key handler, semantics and focus target.
 * Parent scrolling cancels a tap, so dragging a poster does not open its details.
 */
@Composable
internal fun InputButton(
    onClick:()->Unit,
    modifier:Modifier=Modifier,
    enabled:Boolean=true,
    onLongClick:(()->Unit)?=null,
    longPressMillis:Long=3000,
    colors:ButtonColors?=null,
    shape:ButtonShape=ButtonDefaults.shape(),
    focusRadius:androidx.compose.ui.unit.Dp=10.dp,
    scale:ButtonScale?=null,
    contentPadding:PaddingValues=ButtonDefaults.ContentPadding,
    content:@Composable RowScope.()->Unit
){
    val latestClick by rememberUpdatedState(onClick)
    val latestLong by rememberUpdatedState(onLongClick);val scope=rememberCoroutineScope();var held by remember{mutableStateOf<Job?>(null)};var fired by remember{mutableStateOf(false)};val base=LocalViewConfiguration.current;val viewConfig=remember(base,onLongClick!=null,longPressMillis){object:androidx.compose.ui.platform.ViewConfiguration by base{override val longPressTimeoutMillis=if(onLongClick!=null)longPressMillis else base.longPressTimeoutMillis}}
    DisposableEffect(Unit){onDispose{held?.cancel()}}
    val keys=if(onLongClick!=null)Modifier.onFocusChanged{if(!it.hasFocus)held?.cancel()}.onPreviewKeyEvent{e->if(enabled&&e.nativeKeyEvent.keyCode in listOf(23,66,160)){if(e.type==KeyEventType.KeyDown&&e.nativeKeyEvent.repeatCount==0){fired=false;held?.cancel();held=scope.launch{delay(longPressMillis);fired=true;latestLong?.invoke()}};if(e.type==KeyEventType.KeyUp){held?.cancel();if(!fired)latestClick()};true}else false}else Modifier
    val interactions=remember{MutableInteractionSource()}
    val pointer=if(enabled)Modifier.pointerInput(Unit){
        detectTapGestures(
            onTap={latestClick()},onLongPress=if(onLongClick!=null){{latestLong?.invoke()}}else null,
            onPress={position->
                val press=PressInteraction.Press(position)
                var released=false
                interactions.emit(press)
                try{released=tryAwaitRelease()}
                finally{interactions.tryEmit(if(released)PressInteraction.Release(press)else PressInteraction.Cancel(press))}
            }
        )
    }else Modifier
    val actualColors=colors?:ptvButtonColors(Color.Transparent,LocalCinema.current.text)
    CompositionLocalProvider(LocalViewConfiguration provides viewConfig){Button(
        onClick=onClick,
        modifier=modifier.then(keys).then(pointer),
        enabled=enabled,
        scale=scale?:ButtonDefaults.scale(1f,1.015f,.99f),
        glow=ButtonDefaults.glow(focusedGlow=Glow(PtvFocusGlow,5.dp)),
        border=ButtonDefaults.border(focusedBorder=Border(BorderStroke(1.5.dp,PtvFocusBorder),0.dp,RoundedCornerShape(focusRadius))),
        colors=actualColors,
        shape=shape,
        contentPadding=contentPadding,
        interactionSource=interactions,
        content=content
    )}
}
