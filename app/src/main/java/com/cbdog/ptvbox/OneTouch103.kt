package com.cbdog.ptvbox

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Native HTTP implementation of the public OneTouchTV protocol used by PCBOX. */
internal object OneTouch103 {
    private const val ROOT="https://api3.devcorp.me"
    // Public protocol constants, not user credentials or a borrowed developer account.
    private const val KEY="696d37326368617250617373776f72646f6664496e6974566563746f7253746d"
    private const val IV="696d37326368617250617373776f7264"
    private fun hex(s:String)=s.chunked(2).map{it.toInt(16).toByte()}.toByteArray()
    internal fun decode(raw:String):Any {
        val text=raw.trim()
        val clear=if(text.startsWith("{")||text.startsWith("["))text else {
            val cipher=Cipher.getInstance("AES/CBC/PKCS5Padding")
            // The legacy protocol encodes the hexadecimal key/IV in Base64 in its JS adapter.
            cipher.init(Cipher.DECRYPT_MODE,SecretKeySpec(hex(KEY),"AES"),IvParameterSpec(hex(IV)))
            cipher.doFinal(java.util.Base64.getDecoder().decode(text.replace("-_.","/").replace("@","+").replace(Regex("\\s"),""))).toString(Charsets.UTF_8)
        }
        val json=JSONTokener(clear).nextValue()
        return (json as? JSONObject)?.opt("result")?:json
    }
    private suspend fun api(path:String)=decode(InternationalHttp103.bytes(ROOT+path,mapOf("Referer" to "$ROOT/")).toString(Charsets.UTF_8))
    private fun rows(data:Any):List<JSONObject> = when(data){is JSONArray->data.objects();is JSONObject->(data.optJSONArray("results")?:data.optJSONArray("items"))?.objects().orEmpty();else->emptyList()}
    private fun title(j:JSONObject)=j.optString("title").replace(Regex("\\s*\\(\\d{4}\\)\\s*$"),"").trim()
    internal fun episodeName(raw:String):String?=raw.toIntOrNull()?.takeIf{it>0}?.let{"第%02d集".format(it)}
    suspend fun search(q:String):List<Vod> = rows(api("/vod/search?page=1&keyword="+java.net.URLEncoder.encode(q,"UTF-8"))).map{j->
        Vod(j.optString("id"),title(j),j.optString("year"),j.optString("image"),"",emptyList())
    }
    suspend fun detail(vod:Vod):Vod {
        val j=api("/vod/${java.net.URLEncoder.encode(vod.id,"UTF-8")}/detail") as? JSONObject?:return vod
        val episodes=j.optJSONArray("episodes")?.objects().orEmpty()
        // Resolution is deferred until the chosen episode; catalogue browsing never starts playback.
        val eps=episodes.mapNotNull{ep->val play=ep.optString("playId");val name=episodeName(ep.optString("episode"));if(play.isBlank()||name==null)null else Episode(name,"onetouch:"+JSONObject().put("id",ep.optString("identifier").ifBlank{j.optString("identifier").ifBlank{vod.id}}).put("play",play).toString())}
        val direct=j.optJSONArray("sources")?.objects().orEmpty().mapNotNull{r->r.optString("url").takeIf{it.startsWith("http")}?.let{Episode("正片",it)}}
        return vod.copy(overview=j.optString("description").ifBlank{vod.overview},roads=if(eps.isNotEmpty())listOf(Road("OneTouchTV · 亚洲剧",eps.sortedBy{Identity.episode(it.name)?:Int.MAX_VALUE}))else direct.mapIndexed{n,ep->Road("OneTouchTV · 线路${n+1}",listOf(ep))})
    }
    suspend fun resolve(ep:Episode):Pair<Episode,Map<String,String>> {
        if(!ep.url.startsWith("onetouch:"))return ep to emptyMap()
        val request=JSONObject(ep.url.removePrefix("onetouch:"))
        val j=api("/vod/${java.net.URLEncoder.encode(request.getString("id"),"UTF-8")}/episode/${java.net.URLEncoder.encode(request.getString("play"),"UTF-8")}") as? JSONObject?:error("OneTouchTV未返回单集")
        val sources=j.optJSONArray("sources")?.objects().orEmpty().filter{it.optString("url").startsWith("http")}
        val row=sources.sortedByDescending{ResourceLabels040.height(it.optString("quality")+" "+it.optString("name"))}.firstOrNull()?:error("OneTouchTV未返回可播线路")
        val h=row.optJSONObject("headers")
        return ep.copy(url=row.getString("url")) to h?.keys()?.asSequence()?.filter{it.lowercase() !in listOf("host","content-length","connection") }?.associateWith{h.optString(it)}.orEmpty()
    }
}
