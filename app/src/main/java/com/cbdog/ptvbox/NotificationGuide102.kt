package com.cbdog.ptvbox

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text

object NotificationGuide102 {
    const val DISMISSED_KEY = "notif_guide_dismissed_102"

    fun isAndroid11Or12(): Boolean = Build.VERSION.SDK_INT in 31..32

    fun notificationsEnabled(context: Context): Boolean =
        runCatching { context.getSystemService(NotificationManager::class.java)?.areNotificationsEnabled() ?: true }
            .getOrDefault(true)

    fun shouldPrompt(context: Context, hasPrivateSubscription: Boolean, dismissed: Boolean): Boolean =
        isAndroid11Or12() && hasPrivateSubscription && !dismissed && notificationsEnabled(context)

    fun openSystemNotificationSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching { context.startActivity(intent); true }.getOrElse {
            val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallback); true }.getOrDefault(false)
        }
    }
}

@Composable
internal fun NotificationGuideDialog102(m: PtvModel) {
    val context = LocalContext.current
    val hasSub = m.configurationValues052["subscription"].orEmpty().isNotBlank()
    val dismissed = remember(m.preferencesRevision) { m.config.flag(NotificationGuide102.DISMISSED_KEY, false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(hasSub, dismissed) {
        visible = NotificationGuide102.shouldPrompt(context, hasSub, dismissed)
    }

    if (!visible) return
    val c = LocalCinema.current
    Dialog(
        onDismissRequest = {
            m.settingChanged(NotificationGuide102.DISMISSED_KEY, true)
            visible = false
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxWidth(.86f)
                .background(c.surface, RoundedCornerShape(14.dp))
                .border(1.5.dp, PcAccent.copy(alpha = .7f), RoundedCornerShape(14.dp))
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("建议关闭系统通知，彻底屏蔽私人源干扰弹窗", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "检测到当前为 Android ${Build.VERSION.RELEASE} 且已配置私人点播订阅。由于系统权限限制，App 无法自动关闭系统通知；若保持开启，扫描私人源时会频繁弹出第三方插件自带的系统气泡（如下图所示）。关闭系统通知不会影响 Ptvbox 应用内的任何正常提示。",
                color = c.muted, fontSize = 13.sp, lineHeight = 19.sp
            )
            // Visual mockup showing the noisy plugin toasts that disappear once disabled
            Column(
                Modifier.fillMaxWidth().background(Color(0xFF0D0F1B), RoundedCornerShape(10.dp))
                    .border(1.dp, c.border, RoundedCornerShape(10.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("画面示例 · 开启系统通知时会反复遮挡电视画面", color = Color(0xFFFCA5A5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.background(Color(0xCC2A2D3E), RoundedCornerShape(18.dp)).border(1.dp, Color(0x88EF4444), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text("✕  饭：已过滤视频中广告", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    }
                    Box(Modifier.background(Color(0xCC2A2D3E), RoundedCornerShape(18.dp)).border(1.dp, Color(0x88EF4444), RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text("✕  网盘源需使用……扫码登录", color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    }
                }
                Text("关闭 Ptvbox 系统通知后，以上第三方插件弹窗将彻底消失", color = PcGreen, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Action("前往关闭系统通知", {
                    m.settingChanged(NotificationGuide102.DISMISSED_KEY, true)
                    visible = false
                    if (!NotificationGuide102.openSystemNotificationSettings(context)) {
                        m.message("请手动前往系统设置 → 应用 → Ptvbox → 通知 → 关闭")
                    }
                }, selected = true)
                Action("忽略（稍后可在设置→界面打开）", {
                    m.settingChanged(NotificationGuide102.DISMISSED_KEY, true)
                    visible = false
                })
            }
        }
    }
}
