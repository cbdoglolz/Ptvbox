package com.cbdog.ptvbox
import android.app.Instrumentation
import android.webkit.WebView
import org.json.JSONObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
object PortalDomQa052 {
 fun run(i:Instrumentation,m:PtvModel):JSONObject {val schema=ConfigPortal052.snapshot(m.db);schema.getJSONArray("fields").objects().first{it.getString("id")=="assrt"}.put("saved",true).put("value","saved-secret");schema.put("tmdbHistory",org.json.JSONArray().put("old-key-1"));var view:WebView?=null
  try{val fixture="window.qaState="+schema.toString()+";window.qaBody=null;window.fetch=async(u,o)=>({ok:true,json:async()=>{if(u.includes('/api/config')){window.qaBody=JSON.parse(o.body);const b=window.qaBody;if(b.fields)for(const [k,v] of Object.entries(b.fields)){const f=window.qaState.fields.find(x=>x.id===k);if(f){f.value=v;f.saved=true}}return {message:'已保存'}}return window.qaState}});";val html=i.targetContext.assets.open("config-portal.html").bufferedReader().readText().replace("<script>","<script>"+fixture)
   i.runOnMainSync{view=WebView(i.targetContext);view!!.settings.javaScriptEnabled=true;view!!.loadDataWithBaseURL("https://127.0.0.1/#fixture&page=subtitles",html,"text/html","UTF-8",null)}
   fun js(code:String):String{val done=CompletableFuture<String>();i.runOnMainSync{view!!.evaluateJavascript(code){done.complete(it)}};return done.get(8,TimeUnit.SECONDS)}
   UiQa050.await{js("document.querySelectorAll('.card').length")!="0"};if(js("document.getElementById('page').value")!="\"subtitles\"")js("document.getElementById('page').value='subtitles';document.getElementById('page').onchange();")
   UiQa050.await{js("document.body.textContent.includes('免费公共字幕')")=="true"}
   check(js("document.querySelectorAll('[data-id]').length")=="0")
   js("Array.from(document.querySelectorAll('.card')).find(c=>c.querySelector('h3').textContent==='ASSRT Token').querySelector('button').click()")
   UiQa050.await{js("document.getElementById('editor').open")=="true"}
   check(js("document.getElementById('editValue').value")=="\"saved-secret\"")
   js("document.getElementById('editValue').value='qa-dom-only';document.getElementById('save').click()")
   UiQa050.await{js("window.qaBody&&window.qaBody.fields.assrt")=="\"qa-dom-only\""}
   UiQa050.await{js("Array.from(document.querySelectorAll('.card')).some(c=>c.textContent.includes('qa-dom-only'))")=="true"}
   js("document.getElementById('page').value='metadata';document.getElementById('page').onchange()")
   UiQa050.await{js("Array.from(document.querySelectorAll('.card')).some(c=>c.querySelector('h3').textContent.includes('TMDB'))")=="true"}
   check(js("Array.from(document.getElementById('page').options).some(o=>o.value==='sites')")=="true")
   return JSONObject().put("mobileCardsOpenOnlyOnDemand",true).put("savedValuePlaintextOutside",true).put("saveUsesDialog",true).put("freeSubtitlePinned",true).put("sitesOnlyInPhonePortal",true)
  }finally{i.runOnMainSync{view?.apply{stopLoading();destroy()}}}
 }
}
