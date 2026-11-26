package com.cbdog.ptvbox
import android.app.Instrumentation
import android.webkit.WebView
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
object PortalDomQa054 {
 fun run(i:Instrumentation,m:PtvModel):JSONObject {var view:WebView?=null;val schema=ConfigPortal052.snapshot(m.db)
  try{val fixture="window.calls=[];window.confirm=()=>true;HTMLAnchorElement.prototype.click=function(){window.downloaded=this.download};window.fetch=async(u,o)=>({ok:true,json:async()=>{if(u.includes('/api/backup')){const b=JSON.parse(o.body);window.calls.push(b);return b.action==='export'?{file:'qa.json',data:'e30=',message:'导出完成'}:b.action==='preview'?{ticket:'qa-ticket',message:'预览完成'}:{message:'导入完成'}}return "+schema.toString()+"}});"
   val html=i.targetContext.assets.open("config-portal.html").bufferedReader().readText().replace("<script>","<script>"+fixture)
   i.runOnMainSync{view=WebView(i.targetContext);view!!.settings.javaScriptEnabled=true;view!!.loadDataWithBaseURL("https://127.0.0.1/#fixture&page=configuration",html,"text/html","UTF-8",null)}
   fun js(code:String):String {val done=CompletableFuture<String>();i.runOnMainSync{view!!.evaluateJavascript(code){done.complete(it)}};return done.get(8,TimeUnit.SECONDS)}
   UiQa050.await{js("typeof document.getElementById('backupExport')?.onclick")=="\"function\""};check(js("document.getElementById('backupPassword')")=="null")
   js("document.getElementById('backupExport').dispatchEvent(new MouseEvent('click',{bubbles:true}))")
   try{UiQa050.await{js("window.downloaded")=="\"qa.json\""}}catch(e:Exception){error("export: "+js("JSON.stringify({downloaded:window.downloaded,status:document.getElementById('backupStatus').textContent,calls:window.calls.length})"))}
   check(js("!('password' in window.calls[0])")=="true")
   js("Object.defineProperty(document.getElementById('backupFile'),'files',{value:[new File(['{}'],'qa.json')]});document.getElementById('backupPreview').dispatchEvent(new MouseEvent('click',{bubbles:true}))")
   UiQa050.await{js("document.getElementById('backupImport').disabled")=="false"}
   check(js("window.calls.length")=="2"&&js("!('password' in window.calls[1])")=="true")
   js("document.getElementById('backupImport').dispatchEvent(new MouseEvent('click',{bubbles:true}))")
   UiQa050.await{js("window.calls.length")=="3"}
   check(js("window.calls[2].ticket")=="\"qa-ticket\""&&js("!('password' in window.calls[2])")=="true")
   UiQa050.await{js("document.getElementById('backupImport').disabled")=="true"}
   return JSONObject().put("mobileExportDownloadsPasswordlessTransfer",true).put("mobileHasNoPasswordField",true).put("mobilePreviewRequiresSeparateConfirmation",true).put("mobileImportUsesPreviewTicketOnce",true)
  }finally{i.runOnMainSync{view?.apply{stopLoading();destroy()}}}
 }
}
