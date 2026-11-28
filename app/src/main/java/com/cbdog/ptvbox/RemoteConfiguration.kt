package com.cbdog.ptvbox

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable internal fun RemoteConfiguration(m:PtvModel,section:String="remote"){
    val context=LocalContext.current
    val c=LocalCinema.current
    val closeFocus=remember{FocusRequester()}
    val qrFocus=remember{FocusRequester()}
    var qrFocused by remember{mutableStateOf(false)}
    var started by remember{mutableStateOf(m.remoteUrl.isNotBlank())}
    var autoFocusClose by remember{mutableStateOf(false)}
    LaunchedEffect(m.remoteUrl){
        if(m.remoteUrl.isNotBlank()){
            started=true
            if(autoFocusClose){
                autoFocusClose=false
                androidx.compose.runtime.withFrameNanos{}
                runCatching{closeFocus.requestFocus()}
            }
        }else{
            started=false
            autoFocusClose=false
        }
    }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(m.remoteUrl.isBlank()){
            Text("同一WiFi下扫码配置；会话10分钟有效，仅扫码设备可写入。",color=c.muted,fontSize=13.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Action(if(started)"正在生成二维码…"else"点击扫码",{if(!started){started=true;autoFocusClose=true;m.startRemoteConfig()}})
            }
        }else{
            val paired=m.remoteUrl+"&page="+if(section=="remote")"configuration"else section
            val qr=remember(paired){remoteQrBitmap(paired)}
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(18.dp),verticalAlignment=androidx.compose.ui.Alignment.Top){
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                    Action("关闭二维码",m::stopRemoteConfig,Modifier.focusRequester(closeFocus))
                    Action(
                        m.remoteUrl.substringBefore('#'),
                        {runCatching{context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(paired)))}.onFailure{m.message("此设备未安装可打开链接的浏览器")}},
                        maxLines=2
                    )
                    Text("手机与电视需处于同一局域网。",color=c.muted,fontSize=12.sp)
                    Text("本地自签 HTTPS 加密，手机浏览器提示「不安全」属正常，点「继续访问」即可。",color=c.muted,fontSize=11.5.sp)
                    Text("证书指纹：${m.remoteFingerprint.take(23)}…",color=c.muted,fontSize=11.sp)
                }
                InputButton(
                    {runCatching{context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(paired)))}.onFailure{m.message("此设备未安装可打开链接的浏览器")}},
                    Modifier.focusRequester(qrFocus).onFocusChanged{qrFocused=it.isFocused}.semantics{contentDescription="二维码"+(if(qrFocused)" · 当前焦点" else "")},
                    contentPadding=PaddingValues(0.dp)
                ){Image(qr,"手机配置配对二维码 · 点击打开浏览器",Modifier.size(180.dp).background(Color.White))}
            }
        }
    }
}

@Composable internal fun RemoteFold052(m:PtvModel,section:String){
    var expanded by remember(section){mutableStateOf(false)}
    SettingsCard("手机扫码局域网"){
        PcChip(if(expanded)"收起手机配置 ▴"else"展开手机配置 ▾",expanded,{expanded=!expanded})
        if(expanded)RemoteConfiguration(m,section)
    }
}

internal fun remoteQrBitmap(url:String):androidx.compose.ui.graphics.ImageBitmap {
    val size=340
    val matrix=QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,size,size)
    val pixels=IntArray(size*size){index->if(matrix[index%size,index/size])android.graphics.Color.BLACK else android.graphics.Color.WHITE}
    return Bitmap.createBitmap(pixels,size,size,Bitmap.Config.ARGB_8888).asImageBitmap()
}
