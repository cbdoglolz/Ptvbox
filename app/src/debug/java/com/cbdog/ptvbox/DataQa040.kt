package com.cbdog.ptvbox

import android.content.Context
import android.content.ContextWrapper
import org.json.JSONObject
import java.util.UUID

object DataQa040 {
    fun run(context:Context):JSONObject {
        val id="qa040_${UUID.randomUUID()}";val names=listOf("${id}_a.db","${id}_b.db")
        fun isolated(prefix:String)=object:ContextWrapper(context){override fun getSharedPreferences(name:String,mode:Int)=context.getSharedPreferences("${id}_${prefix}_$name",mode)}
        val aContext=isolated("a");val bContext=isolated("b");val a=Library(aContext,names[0]);val b=Library(bContext,names[1])
        try{
            val w=Work("one","QA series","tv",season=2,identityKey="$id:one",facts=WorkFacts(runtime=40))
            val second=Work("two","QA movie",identityKey="$id:two")
            a.cache(w);a.cache(second);a.toggleFavorite(w);a.markEpisode(w,3,favorite=true,watched=true)
            a.save(Watch(w,"第03集","qa-source","中文",1080,123456,500000,updated=1000,sourceId="qa-source|one|中文"))
            a.secrets.save("opensub_key","qa-only-portable-secret");a.secrets.save("tmdb","qa-old-key")
            val archive=UserBackup.export(a);check(!archive.toString().contains("BEGIN PRIVATE KEY"));check(!archive.toString().contains("ptvbox.credentials"))
            val encrypted=BackupCipher.encrypt(archive.toString().toByteArray(),"qa040-password".toCharArray())
            val restored=JSONObject(BackupCipher.decrypt(encrypted,"qa040-password".toCharArray()).toString(Charsets.UTF_8))
            b.cache(w);val currentId=b.mediaId(w);val originalSecondId=a.mediaId(second)
            b.save(Watch(w,"第03集","qa-current","中文",1080,234567,500000,updated=2000))
            b.secrets.save("tmdb","qa-preserve-current")
            UserBackup.restore(b,restored)
            check(b.mediaId(w)==currentId);check(b.mediaId(second)==originalSecondId);check(b.last(w)?.position==234567L)
            check(b.favorite(w));check(b.favoriteEpisodes().single().first.season==2);check(b.favoriteEpisodes().single().second==3)
            check(b.secrets.read("tmdb")=="qa-preserve-current");check(b.secrets.read("opensub_key")=="qa-only-portable-secret")
            val raw=bContext.getSharedPreferences("encrypted_credentials",Context.MODE_PRIVATE).getString("opensub_key","").orEmpty();check(!raw.contains("qa-only-portable-secret"))
            val before=b.cached().size;val bad=JSONObject(restored.toString());bad.getJSONObject("tables").getJSONArray("state_v2").put(JSONObject().put("k",w.key).put("state","invalid"))
            check(runCatching{UserBackup.restore(b,bad)}.isFailure);check(b.cached().size==before&&b.last(w)?.position==234567L)
            val old="""{"sites":[{"key":"qa-one","name":"QA one","api":"https://example.invalid/a"}]}"""
            val next="""{"sites":[{"key":"qa-one","name":"QA one","api":"https://example.invalid/b"},{"key":"qa-two","name":"QA two","api":"https://example.invalid/c"}]}"""
            b.secrets.save("source_config",old);b.prefs.edit().putBoolean("site_qa-one",false).commit();val maintenance=SourceMaintenance(b);val preview=SubscriptionPreview.compare(old,next)
            check(b.secrets.read("source_config")==old);maintenance.activate(preview);check(b.secrets.read("source_config")==next);check(!b.prefs.getBoolean("site_qa-one",true));check(runCatching{maintenance.activate(preview)}.isFailure);maintenance.rollback();check(b.secrets.read("source_config")==old);check(b.favorite(w))
            return JSONObject().put("encryptedBackupRoundTrip",true).put("stableIdsPreserved",true).put("existingNewerProgressPreserved",true).put("favoriteAndEpisodeSeasonMerged",true).put("existingCredentialPreserved",true).put("credentialReencrypted",true).put("malformedRestoreDoesNotChangeData",true).put("noKeystorePrivateMaterial",true).put("subscriptionPreviewDoesNotActivate",true).put("subscriptionActivationPreservesSwitch",true).put("staleActivationRejected",true).put("subscriptionRollbackPreservesUserData",true)
        }finally{
            a.close();b.close();names.forEach{context.deleteDatabase(it)}
            for(prefix in listOf("a","b"))for(name in listOf("preferences","encrypted_credentials"))context.deleteSharedPreferences("${id}_${prefix}_$name")
        }
    }
}
