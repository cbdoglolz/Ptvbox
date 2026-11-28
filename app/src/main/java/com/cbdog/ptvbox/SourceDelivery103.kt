package com.cbdog.ptvbox

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

internal object SourceDelivery103 {
    fun downloadOnly(label:String)=Regex("(?i)download[ ._-]*only|仅供下载|只供下载").containsMatchIn(label)
}

internal fun PtvModel.openDownload103(episode:Episode){
    val uri=Uri.parse(episode.url)
    if(uri.scheme !in listOf("https","http")){message("此下载入口不是有效网页地址");return}
    runCatching{getApplication<android.app.Application>().startActivity(Intent(Intent.ACTION_VIEW,uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}
        .onSuccess{message("已打开下载页面，请在浏览器中选择下载文件")}
        .onFailure{message("此设备未安装可打开下载页面的浏览器")}
}

@Composable internal fun SourceAction103(m:PtvModel,w:Work,g:CatalogGroup,ep:Episode,modifier:Modifier=Modifier){
    val download=SourceDelivery103.downloadOnly(g.road.name)
    val tint=if(download)Color(0xFFFFC765)else LocalCinema.current.text
    InputButton({m.playFromCatalog(w,g,ep)},modifier,colors=ptvButtonColors(if(download)Color(0xFF493717)else LocalCinema.current.raised,tint)){
        if(download)Canvas(Modifier.size(17.dp)){
            val stroke=2.dp.toPx()
            drawLine(tint,Offset(size.width*.5f,0f),Offset(size.width*.5f,size.height*.65f),stroke)
            drawLine(tint,Offset(size.width*.22f,size.height*.4f),Offset(size.width*.5f,size.height*.68f),stroke)
            drawLine(tint,Offset(size.width*.78f,size.height*.4f),Offset(size.width*.5f,size.height*.68f),stroke)
            drawLine(tint,Offset(0f,size.height*.95f),Offset(size.width,size.height*.95f),stroke)
        }else Text("▶",color=tint,fontSize=14.sp)
        Spacer(Modifier.width(7.dp))
        Text(if(download)"下载${if(w.kind=="tv")" · ${ep.name}"else""}"else if(w.kind=="movie")"播放"else ep.name,color=tint,fontSize=14.sp,maxLines=2)
    }
}
