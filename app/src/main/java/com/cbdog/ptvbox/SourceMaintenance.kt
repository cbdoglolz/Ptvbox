package com.cbdog.ptvbox

import org.json.JSONObject
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.security.MessageDigest

fun sha256(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
data class SubscriptionPreview(val body:String,val previous:String,val added:List<String>,val changed:List<String>,val removed:List<String>){
    val changedAnything get()=added.isNotEmpty()||changed.isNotEmpty()||removed.isNotEmpty()
    val description get()=buildList{add("新增 ${added.size} · 修改 ${changed.size} · 移除 ${removed.size}");if(added.isNotEmpty())add("新增：${added.joinToString("、")}");if(changed.isNotEmpty())add("修改：${changed.joinToString("、")}");if(removed.isNotEmpty())add("移除：${removed.joinToString("、")}");add("激活只替换订阅配置；已有站点开关/优先级、收藏和进度保留。新规则/插件仍须本机检测。")} .joinToString("\n")
    companion object{
        fun compare(old:String,next:String):SubscriptionPreview {
            SubscriptionConfig.validate(next)
            fun rows(raw:String)=runCatching{JSONObject(raw).getJSONArray("sites").objects().associateBy{it.getString("key")}}.getOrDefault(emptyMap())
            val before=rows(old);val after=rows(next)
            // JSON member order is not a content change.
            fun canonical(v:Any?):String=when(v){is JSONObject->v.keys().asSequence().sorted().joinToString(prefix="{",postfix="}"){it+":"+canonical(v.opt(it))};is org.json.JSONArray->(0 until v.length()).joinToString(prefix="[",postfix="]"){canonical(v.opt(it))};is String->JSONObject.quote(v);else->v?.toString()?:"null"}
            val added=(after.keys-before.keys).map{after.getValue(it).optString("name")}
            val changed=(after.keys intersect before.keys).filter{canonical(after[it])!=canonical(before[it])}.map{after.getValue(it).optString("name")}.toMutableList()
            val oldRoot=runCatching{JSONObject(old).apply{remove("sites")}}.getOrDefault(JSONObject());val newRoot=JSONObject(next).apply{remove("sites")}
            if(canonical(oldRoot)!=canonical(newRoot))changed+="订阅公共模块/解析配置"
            return SubscriptionPreview(next,old,added,changed,(before.keys-after.keys).map{before.getValue(it).optString("name")})
        }
    }
}
data class SignedMaintenance(val revision:String,val minClient:String,val components:List<String>,val health:List<String>,val files:List<String>) {
    companion object {
        fun verify(raw:ByteArray,signature:String,pem:String):SignedMaintenance {
            require(raw.size<=2_000_000)
            val publicBytes=Base64.getMimeDecoder().decode(pem.replace(Regex("-----[A-Z ]+-----"),""))
            val key=KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(publicBytes))
            require(Signature.getInstance("SHA256withRSA").run{initVerify(key);update(raw);verify(Base64.getMimeDecoder().decode(signature.trim()))}){"云端清单签名无效，现用源保留"}
            val j=JSONObject(raw.toString(Charsets.UTF_8))
            fun JSONObject.field(key:String)=keys().asSequence().firstOrNull{it.equals(key,true)}?.let{get(it)}
            fun JSONObject.str(key:String)=field(key)?.toString().orEmpty()
            fun JSONObject.rows(key:String)=(field(key) as? org.json.JSONArray)?.objects().orEmpty()
            require(j.str("schema")=="1"&&j.str("revision").matches(Regex("[0-9a-f]{64}")))
            val files=j.rows("files");require(files.size in 1..150)
            require(files.map{it.str("path").lowercase()}.distinct().size==files.size)
            for(f in files)require((f.str("path")=="animeko-sources.json"||f.str("path").matches(Regex("anime-rules/[A-Za-z0-9_-]{1,64}\\.json")))&&f.str("sha256").matches(Regex("[a-f0-9]{64}"))&&(f.str("size").toIntOrNull()?:0) in 1..2_000_000)
            val components=j.rows("components");require(components.size<=30)
            return SignedMaintenance(j.str("revision"),j.str("minClient"),components.map{c->listOf(c.str("label"),c.str("status"),c.str("summary"),c.str("upstreamRevision").take(12)).filter{it.isNotBlank()}.joinToString(" · ")},components.flatMap{c->c.rows("health").map{h->"${h.str("name")} · 上游连接 ${h.str("connection")} / 搜索 ${h.str("search")} / 播放 ${h.str("playback")}"}},files.map{it.str("path")})
        }
    }
}
class SourceMaintenance(private val db:Library){
    suspend fun checkSubscription():SubscriptionPreview {
        val url=db.secrets.read("subscription");require(url.isNotBlank()){"未保存私人订阅地址"}
        return SubscriptionPreview.compare(db.secrets.read("source_config"),Net.text(url))
    }
    fun activate(preview:SubscriptionPreview){
        require(db.secrets.read("source_config")==preview.previous){"现用配置已改变，请重新检查"}
        SubscriptionConfig.validate(preview.body)
        db.secrets.save("source_config_previous",preview.previous);db.secrets.save("source_config",preview.body)
    }
    fun rollback(){val old=db.secrets.read("source_config_previous");require(old.isNotBlank()){"尚无上一份订阅快照"};SubscriptionConfig.validate(old);val current=db.secrets.read("source_config");db.secrets.save("source_config",old);db.secrets.save("source_config_previous",current)}
    suspend fun cloud(context:android.content.Context):SignedMaintenance {
        val repo=db.prefs.getString("source_update_repository","cbdoglolz/r-9b04e274ef5e41aa").orEmpty();require(repo.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")))
        val token=db.secrets.read("source_update_token");val headers=if(token.isNotBlank())mapOf("Authorization" to "Bearer $token")else emptyMap()
        val commit=Net.json("https://api.github.com/repos/$repo/commits/main",headers).getString("sha");require(commit.matches(Regex("[a-f0-9]{40}")))
        suspend fun content(name:String):ByteArray {val j=Net.json("https://api.github.com/repos/$repo/contents/$name?ref=$commit",headers);require(j.optString("encoding")=="base64");return Base64.getMimeDecoder().decode(j.getString("content"))}
        return SignedMaintenance.verify(content("manifest.json"),content("manifest.sig").toString(Charsets.UTF_8),context.assets.open("maintenance-public.pem").bufferedReader().use{it.readText()})
    }
}
