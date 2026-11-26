package com.cbdog.ptvbox

import android.graphics.Typeface
import android.util.TypedValue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.tv.material3.Text

fun TvPreferences.subtitleColor():Int {val raw=runCatching{android.graphics.Color.parseColor(text("subtitle_color","#FFFFFF"))}.getOrDefault(android.graphics.Color.WHITE);return (number("subtitle_alpha",100).coerceIn(0,100)*255/100 shl 24) or (raw and 0xffffff)}
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun applySubtitleStyle040(view:SubtitleView?,settings:TvPreferences){
    view?:return;val custom=settings.flag("subtitle_style_override")
    view.setApplyEmbeddedStyles(!custom);view.setApplyEmbeddedFontSizes(!custom)
    if(!custom){view.setStyle(CaptionStyleCompat.DEFAULT);view.setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION)}
    if(custom){
        val back=(settings.number("subtitle_background_alpha",40).coerceIn(0,100)*255/100 shl 24) or (runCatching{android.graphics.Color.parseColor(settings.text("subtitle_background_color","#000000"))}.getOrDefault(0) and 0xffffff)
        view.setStyle(CaptionStyleCompat(settings.subtitleColor(),back,android.graphics.Color.TRANSPARENT,CaptionStyleCompat.EDGE_TYPE_OUTLINE,android.graphics.Color.BLACK,Typeface.create(settings.text("subtitle_font","sans-serif"),if(settings.flag("subtitle_bold",true))Typeface.BOLD else Typeface.NORMAL)))
        view.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP,settings.number("subtitle_size",24).coerceIn(12,64).toFloat())
    }
}
@Composable internal fun SubtitlePreview040(m:PtvModel){
    m.preferencesRevision
    val s=m.config;val family=when(s.text("subtitle_font")){"serif"->FontFamily.Serif;"monospace"->FontFamily.Monospace;else->FontFamily.SansSerif};val weight=if(s.flag("subtitle_bold",true))FontWeight.Bold else FontWeight.Normal
    Column(Modifier.fillMaxWidth().background(Color(runCatching{android.graphics.Color.parseColor(s.text("subtitle_background_color","#000000"))}.getOrDefault(0)).copy(alpha=s.number("subtitle_background_alpha",40).coerceIn(0,100)/100f)).padding(10.dp)){
        Text("星光照亮前行的路。",color=Color(s.subtitleColor()),fontSize=s.number("subtitle_size",24).sp,fontWeight=weight,fontFamily=family)
        Text("Stars light the way.",color=Color(s.subtitleColor()),fontSize=s.number("subtitle_size",24).sp,fontWeight=weight,fontFamily=family)
    }
}
