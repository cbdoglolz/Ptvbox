package com.cbdog.ptvbox

import android.content.ContentValues
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable password-encrypted backup. Android Keystore material and system launcher IDs are never exported. */
object BackupCipher {
    private val magic="PTVBK1".toByteArray()
    private fun key(password:CharArray,salt:ByteArray):SecretKeySpec {
        require(password.size>=8){"备份密码至少8个字符"}
        val spec=PBEKeySpec(password,salt,210_000,256)
        return try{SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES")}finally{spec.clearPassword()}
    }
    fun encrypt(data:ByteArray,password:CharArray):ByteArray {
        require(data.size<=16_000_000);val random=SecureRandom();val salt=ByteArray(16).also(random::nextBytes);val nonce=ByteArray(12).also(random::nextBytes)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,nonce));cipher.updateAAD(magic)
        return magic+salt+nonce+cipher.doFinal(data)
    }
    fun decrypt(data:ByteArray,password:CharArray):ByteArray {
        require(data.size in 50..16_001_000&&data.copyOfRange(0,6).contentEquals(magic)){"不是Ptvbox加密备份"}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(password,data.copyOfRange(6,22)),GCMParameterSpec(128,data.copyOfRange(22,34)));cipher.updateAAD(magic)
        return cipher.doFinal(data,34,data.size-34)
    }
}
object UserBackup {
    private const val TRANSFER_FORMAT="PTVBOX-XFER"
    private const val TRANSFER_VERSION=1
    val tables=listOf("media_v2","aliases_v2","favorites_v2","state_v2","episodes_v2","progress_v2","hidden_v2","episode_flags_v3")
    private val secrets=(SettingsInventory.serviceSecrets+listOf("tmdb","tmdb_pending","source_config","source_config_previous","subscription","proxy","live_subscription")).distinct()
    private fun portable(key:String)=!key.startsWith("home_")&&!key.startsWith("qa_")&&key !in listOf("device_id","source_update_revision","source_update_previous")
    fun export(db:Library):JSONObject = synchronized(db){
        val data=JSONObject();for(table in tables){val rows=JSONArray();db.readableDatabase.rawQuery("SELECT * FROM $table",null).use{c->while(c.moveToNext()){val row=JSONObject();for(i in 0 until c.columnCount)row.put(c.getColumnName(i),c.getString(i)?:JSONObject.NULL);rows.put(row)}};data.put(table,rows)}
        val prefs=JSONObject();db.prefs.all.filterKeys(::portable).forEach{(k,v)->if(v is String||v is Boolean||v is Number)prefs.put(k,JSONObject().put("type",v.javaClass.simpleName).put("value",v))}
        val secure=JSONObject();for(name in (secrets+db.secrets.names()).distinct()){val value=db.secrets.read(name);if(value.isNotBlank())secure.put(name,value)}
        val files=JSONObject();var size=0L;var omitted=0;db.userMediaDir.listFiles()?.filter{it.isFile&&it.name.matches(Regex("[a-f0-9]{64}\\.json"))}?.sortedByDescending{it.lastModified()}?.forEach{f->val bytes=f.readBytes();val j=runCatching{JSONObject(bytes.toString(Charsets.UTF_8))}.getOrNull();if(j?.has("text")==true){if(size+bytes.size<=8_000_000){size+=bytes.size;files.put(f.name,java.util.Base64.getEncoder().encodeToString(bytes))}else omitted++}}
        JSONObject().put("schema",3).put("created",System.currentTimeMillis()).put("tables",data).put("preferences",prefs).put("credentials",secure).put("userMedia",files).put("omittedSubtitleCaches",omitted)
    }

    fun encodeTransfer(db:Library):ByteArray = JSONObject()
        .put("format",TRANSFER_FORMAT)
        .put("version",TRANSFER_VERSION)
        .put("exported_at",System.currentTimeMillis())
        .put("payload",export(db))
        .toString().toByteArray(Charsets.UTF_8)

    fun decodeTransfer(data:ByteArray):JSONObject {
        require(data.size<=21_000_000){"迁移文件超过21MB"}
        val root=JSONObject(data.toString(Charsets.UTF_8))
        if(root.optString("format")==TRANSFER_FORMAT){
            require(root.optInt("version",0)==TRANSFER_VERSION){"迁移文件版本不受支持"}
            return root.getJSONObject("payload")
        }
        require(root.has("schema")&&root.has("tables")){"文件不是PTVBOX-XFER迁移文件，或为旧版加密备份"}
        return root
    }
    fun summary(root:JSONObject):String {
        require(root.optInt("schema")==3);val data=root.getJSONObject("tables")
        return "作品 ${data.getJSONArray("media_v2").length()} · 收藏 ${data.getJSONArray("favorites_v2").length()} · 进度 ${data.getJSONArray("progress_v2").length()}\n合并缺少记录，进度仅采用较新的记录；已有配置和凭据默认保留。\n字幕缓存 ${root.optJSONObject("userMedia")?.length()?:0} 份；超容量未包含 ${root.optInt("omittedSubtitleCaches")} 份（可重新下载）；弹幕下载缓存不迁移。"
    }
    /** Validate all rows before starting the transaction; restore is an additive merge, not a clear/rebuild. */
    fun restore(db:Library,root:JSONObject,replaceConfiguration:Boolean=false):Int = synchronized(db){
        summary(root);val data=root.getJSONObject("tables");var total=0
        val columns=tables.associateWith{table->db.readableDatabase.rawQuery("PRAGMA table_info($table)",null).use{c->buildSet{while(c.moveToNext())add(c.getString(c.getColumnIndexOrThrow("name")))}}}
        for(table in tables){val rows=data.getJSONArray(table);total+=rows.length();require(rows.objects().size==rows.length());require(total<=20000){"备份记录过多"};for(row in rows.objects()){require(row.keys().asSequence().toSet()==columns.getValue(table)){"备份字段不兼容"};for(k in columns.getValue(table))require(row.optString(k).length<=500_000);if(table=="media_v2")require(Work.from(JSONObject(row.getString("j"))).key==row.getString("k"));if(table=="progress_v2"){val watch=Watch.from(JSONObject(row.getString("j")));require(watch.position==row.getString("position_ms").toLong()&&watch.duration==row.getString("duration_ms").toLong());require(row.getString("position_ms").toLong()>=0&&row.getString("duration_ms").toLong()>=0)};if(table=="state_v2")WatchState.valueOf(row.getString("state"))}}
        val secure=root.optJSONObject("credentials")?:JSONObject();require(secure.length()<=2000);for(k in secure.keys()){require(k.length<=300&&secure.getString(k).length<=4_000_000)};secure.optString("source_config").takeIf{it.isNotBlank()}?.let(SubscriptionConfig::validate)
        val checkedPreferences=root.optJSONObject("preferences")?:JSONObject();require(checkedPreferences.length()<=10000);for(k in checkedPreferences.keys()){require(k.length<=500);val v=checkedPreferences.getJSONObject(k);when(v.getString("type")){"Boolean"->v.getBoolean("value");"Integer"->v.getInt("value");"Long"->v.getLong("value");"Float"->require(v.getDouble("value").isFinite());"String"->require(v.getString("value").length<=4_000_000);else->error("不兼容的设置格式")}}
        val files=root.optJSONObject("userMedia")?:JSONObject();var fileBytes=0;val decoded=files.keys().asSequence().associateWith{k->require(k.matches(Regex("[a-f0-9]{64}\\.json")));java.util.Base64.getDecoder().decode(files.getString(k)).also{fileBytes+=it.size;require(fileBytes<=8_000_000);JSONObject(it.toString(Charsets.UTF_8))}}
        val sql=db.writableDatabase;var inserted=0;sql.beginTransaction()
        try{
            for(table in tables)for(row in data.getJSONArray(table).objects()){
                val values=ContentValues();for(key in columns.getValue(table))if(row.isNull(key))values.putNull(key)else values.put(key,row.getString(key))
                if(table=="progress_v2"){
                    val old=sql.rawQuery("SELECT updated FROM progress_v2 WHERE k=?",arrayOf(row.getString("k"))).use{if(it.moveToFirst())it.getLong(0)else Long.MIN_VALUE}
                    if(row.getString("updated").toLong()<=old)continue
                    sql.insertWithOnConflict(table,null,values,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);inserted++
                }else if(sql.insertWithOnConflict(table,null,values,android.database.sqlite.SQLiteDatabase.CONFLICT_IGNORE)!=-1L)inserted++
            }
            sql.setTransactionSuccessful()
        }finally{sql.endTransaction()}
        val editor=db.prefs.edit();val prefs=root.optJSONObject("preferences")?:JSONObject()
        for(k in prefs.keys())if(portable(k)&&(replaceConfiguration||!db.prefs.contains(k))){
            val v=prefs.getJSONObject(k);when(v.optString("type")){"Boolean"->editor.putBoolean(k,v.getBoolean("value"));"Integer"->editor.putInt(k,v.getInt("value"));"Long"->editor.putLong(k,v.getLong("value"));"Float"->editor.putFloat(k,v.getDouble("value").toFloat());"String"->editor.putString(k,v.getString("value"))}
        };editor.commit()
        for(name in secure.keys())if((replaceConfiguration||db.secrets.read(name).isBlank())&&secure.optString(name).isNotBlank())db.secrets.save(name,secure.getString(name))
        for((name,bytes)in decoded){val file=java.io.File(db.userMediaDir,name);if(!file.exists()){val temp=java.io.File(db.userMediaDir,"$name.tmp");temp.writeBytes(bytes);require(temp.renameTo(file)){"缓存还原失败，原数据保留"}}}
        db.changes.value++;inserted
    }
}
