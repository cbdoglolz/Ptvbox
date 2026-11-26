package com.cbdog.ptvbox

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.Base64

/** Paired HTTPS session only. Previews expire with this server; no clear backup is written to disk. */
class BackupPortal054(private val db:Library){
 private val gate=Mutex();private var preview:JSONObject?=null;private var ticket="";private var expires=0L
 suspend fun apply(j:JSONObject):JSONObject=gate.withLock {
  when(j.optString("action")){
   "export"->{val portable=UserBackup.encodeTransfer(db);JSONObject().put("file","Ptvbox-${BuildConfig.VERSION_NAME}-portable.json").put("data",Base64.getEncoder().encodeToString(portable)).put("message","无密码迁移文件已生成，请保存文件")}
   "preview"->{preview=null;ticket="";val encoded=j.getString("data");require(encoded.length<=21_336_000);val root=UserBackup.decodeTransfer(Base64.getDecoder().decode(encoded));val summary=UserBackup.summary(root);preview=root;ticket=java.util.UUID.randomUUID().toString();expires=android.os.SystemClock.elapsedRealtime()+120000;JSONObject().put("ticket",ticket).put("message",summary+"\n确认后才写入。预览2分钟内有效。")}
   "import"->{require(j.optBoolean("confirmed")&&j.optString("ticket")==ticket&&ticket.isNotBlank()&&android.os.SystemClock.elapsedRealtime()<expires){"预览已过期，请重新打开备份"};val root=preview?:error("缺少备份预览");preview=null;ticket="";val count=UserBackup.restore(db,root,j.optBoolean("replaceConfiguration"));JSONObject().put("message","已合并${count}条记录，较新进度与原有收藏保留")}
   else->error("未知备份操作")
  }
 }
 fun clear(){preview=null;ticket="";expires=0}
}
