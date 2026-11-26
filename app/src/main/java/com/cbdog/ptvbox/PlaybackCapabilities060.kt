package com.cbdog.ptvbox

import android.app.Application
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks

/**
 * Reports only values exposed by the selected track and the active display.
 * It never changes the picture to imitate HDR and never claims unsupported output.
 */
internal data class PlaybackCapability060(
    val resolution:String="未报告",
    val dynamicRange:String="SDR/未报告",
    val audio:String="音轨未报告",
    val display:String="显示能力未报告",
    val warning:String=""
) {
    val summary:String get()=listOf(resolution,dynamicRange,audio,display).filter{it.isNotBlank()}.joinToString(" / ")
    val compactSummary:String get()=listOf(
        resolution.takeUnless{it=="未报告"||it=="分辨率未报告"},
        dynamicRange.replace("/未报告","").takeUnless{it.isBlank()},
        audio.replace("语言未标注 · ","").takeUnless{it=="音轨未报告"}
    ).filterNotNull().distinct().joinToString(" · ")
}

internal object PlaybackCapabilities060 {
    fun display(application:Application):String = runCatching {
        val display=application.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)
        val types=(display?.hdrCapabilities?.supportedHdrTypes?:intArrayOf()).asSequence().mapNotNull{when(it){
            Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION->"Dolby Vision"
            Display.HdrCapabilities.HDR_TYPE_HDR10->"HDR10"
            Display.HdrCapabilities.HDR_TYPE_HLG->"HLG"
            Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS->"HDR10+"
            else->null
        }}.distinct().toList()
        if(types.isEmpty())"显示支持 SDR" else "显示支持 "+types.joinToString("/")
    }.getOrDefault("显示能力未报告")

    fun inspect(tracks:Tracks,application:Application):PlaybackCapability060 {
        val video=tracks.groups.asSequence().filter{it.type==C.TRACK_TYPE_VIDEO}.flatMap{g->(0 until g.length).asSequence().map{g.getTrackFormat(it)}}.firstOrNull()
        val audioGroup=tracks.groups.firstOrNull{g->g.type==C.TRACK_TYPE_AUDIO&&(0 until g.length).any{g.isTrackSelected(it)}}
        val audioFormat=audioGroup?.let{g->val i=(0 until g.length).first{g.isTrackSelected(it)};g.getTrackFormat(i)}
        val resolution=video?.let{f->if(f.width>0&&f.height>0)"${f.width}×${f.height}" else f.label.orEmpty().takeIf{it.isNotBlank()}?:f.codecs.orEmpty()}.orEmpty().ifBlank{"分辨率未报告"}
        val range=video?.let(::dynamicRange)?.ifBlank{"SDR/未报告"}?:"SDR/未报告"
        val audio=audioFormat?.let{f->TrackLabels056.label(f.language,f.label,f.codecs,f.sampleMimeType,f.channelCount,1)}?:"音轨未报告"
        val device=display(application)
        val warning=when{
            range.contains("Dolby Vision")&&!device.contains("Dolby Vision")->"设备未声明Dolby Vision"
            range.contains("HDR")&&!device.contains("HDR")&&!device.contains("HLG")->"设备将按SDR映射"
            else->""
        }
        return PlaybackCapability060(resolution,range,audio,device,warning)
    }

    private fun dynamicRange(f:Format):String {
        val codecs=f.codecs.orEmpty().lowercase();val mime=f.sampleMimeType.orEmpty().lowercase()
        if(mime==MimeTypes.VIDEO_DOLBY_VISION||codecs.startsWith("dvh")||codecs.startsWith("dav1"))return "Dolby Vision"
        return when(f.colorInfo?.colorTransfer){
            C.COLOR_TRANSFER_ST2084->"HDR10 / PQ"
            C.COLOR_TRANSFER_HLG->"HLG"
            else->if(codecs.contains("hdr"))"HDR" else "SDR/未报告"
        }
    }
}
