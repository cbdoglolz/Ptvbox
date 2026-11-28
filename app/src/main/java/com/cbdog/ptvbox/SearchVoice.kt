package com.cbdog.ptvbox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberSearchVoice(fallback: () -> Unit, closed: () -> Unit = {}, submit: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latestSubmit by rememberUpdatedState(submit)
    val latestFallback by rememberUpdatedState(fallback)
    val latestClosed by rememberUpdatedState(closed)
    var listening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listening = true else latestFallback()
    }
    if (listening) {
        DisposableEffect(Unit) {
            status = "正在开启麦克风…"
            var recognizer: SpeechRecognizer? = null
            try {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { status = "正在收听，请说出片名" }
                    override fun onBeginningOfSpeech() { status = "正在收听…" }
                    override fun onEndOfSpeech() { status = "正在识别…" }
                    override fun onResults(results: Bundle?) {
                        val title = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (title.isNullOrBlank()) status = "没听清，请关闭后重试"
                        else { listening = false; latestSubmit(title) }
                    }
                    override fun onError(error: Int) { status = "语音识别暂不可用，请关闭后重试" }
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                recognizer.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN"))
            } catch (_: RuntimeException) {
                listening = false
                latestFallback()
            }
            onDispose { recognizer?.destroy() }
        }
        SearchVoiceDialog103(status) { listening = false; latestClosed() }
    }
    return {
        when {
            !SpeechRecognizer.isRecognitionAvailable(context) -> fallback()
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> listening = true
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}

@Composable internal fun SearchVoiceDialog103(status: String, close: () -> Unit) {
    SearchDialog103("语音搜索", status, close) {
        Canvas(Modifier.size(48.dp).background(PcCyan.copy(alpha=.1f),CircleShape).padding(12.dp)) {
            val stroke=Stroke(1.8.dp.toPx())
            drawRoundRect(PcCyan,Offset(size.width*.36f,0f),Size(size.width*.28f,size.height*.58f),CornerRadius(size.width*.14f))
            drawArc(PcCyan,0f,180f,false,Offset(size.width*.18f,size.height*.21f),Size(size.width*.64f,size.height*.55f),style=stroke)
            drawLine(PcCyan,Offset(size.width*.5f,size.height*.76f),Offset(size.width*.5f,size.height*.95f),stroke.width)
            drawLine(PcCyan,Offset(size.width*.32f,size.height*.95f),Offset(size.width*.68f,size.height*.95f),stroke.width)
        }
    }
}
