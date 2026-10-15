package io.github.mangi.eta.ui.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.github.mangi.eta.R
import io.github.mangi.eta.core.AndroidAgentLogger
import io.github.mangi.eta.core.safeLogType
import io.github.mangi.eta.ui.MainActivity

/**
 * Agent run 结束时在手机屏幕弹出一条结果通知：高优先级渠道会出现横幅提示，点击回到对应会话。
 * 只报告一次运行结果，不承载进行中的状态。
 */
internal object AgentRunResultNotification {
    private const val CHANNEL = "eta_run_result"
    private const val ID = 1109
    private const val TAG = "eta-run-result"
    private const val MAX_CHARS = 400

    fun show(context: Context, ok: Boolean, content: String, error: String?, conversationId: String?) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    context.getString(R.string.run_result_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
            val text = content.trim().ifBlank { error?.trim().orEmpty() }
                .ifBlank { context.getString(R.string.run_result_empty) }
                .take(MAX_CHARS)
            val open = Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_CONVERSATION_ID)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            conversationId?.takeIf(String::isNotBlank)?.let { open.putExtra(MainActivity.EXTRA_CONVERSATION_ID, it) }
            val pending = PendingIntent.getActivity(
                context,
                0,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(
                    if (ok) R.string.run_result_title_ok else R.string.run_result_title_failed
                ))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(pending)
                .setCategory(Notification.CATEGORY_STATUS)
                .setAutoCancel(true)
                .build()
            manager.notify(TAG, ID, notification)
        }.onFailure { AndroidAgentLogger.warn("Run result notification failed: type=${it.safeLogType()}") }
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(TAG, ID)
    }
}
