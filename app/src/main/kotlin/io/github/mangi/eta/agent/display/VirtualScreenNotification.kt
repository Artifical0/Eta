package io.github.mangi.eta.agent.display

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import io.github.mangi.eta.R
import io.github.mangi.eta.core.AndroidAgentLogger
import io.github.mangi.eta.core.safeLogType

/**
 * The only entry to a running virtual screen: tapping opens the viewer on the phone screen, and the
 * extra action brings the floating preview back after the user dismissed it. Nothing is shown
 * automatically.
 */
internal object VirtualScreenNotification {
    private const val CHANNEL = "eta_virtual_screen"
    private const val ID = 1110

    fun show(context: Context, sessionId: String) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return
        runCatching {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL, context.getString(R.string.virtual_screen_notification_channel), NotificationManager.IMPORTANCE_LOW,
            ))
            val open = PendingIntent.getActivity(context, 0,
                Intent(context, VirtualScreenViewerActivity::class.java)
                    .setData(Uri.parse("eta-virtual-screen://view/$sessionId"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val preview = PendingIntent.getBroadcast(context, 1,
                Intent(context, VirtualScreenPreviewReceiver::class.java)
                    .setAction(VirtualScreenPreviewReceiver.ACTION_SHOW_PREVIEW)
                    .putExtra(VirtualScreenPreviewReceiver.EXTRA_SESSION, sessionId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val text = context.getString(R.string.virtual_screen_notification_content)
            val builder = Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.virtual_screen_notification_title))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(Notification.Action.Builder(null,
                    context.getString(R.string.virtual_screen_notification_open), open).build())
                .addAction(Notification.Action.Builder(null,
                    context.getString(R.string.virtual_screen_notification_preview), preview).build())
            // 状态栏胶囊与锁屏流体云由平台的提升常驻通知接管；用户关闭该功能时只是一条普通常驻通知。
            if (Build.VERSION.SDK_INT >= 36) {
                builder.setRequestPromotedOngoing(true)
                if (!manager.canPostPromotedNotifications()) {
                    AndroidAgentLogger.warn("Virtual screen live update disabled by user settings")
                }
            }
            manager.notify(sessionId, ID, builder.build())
        }.onFailure { AndroidAgentLogger.warn("Virtual screen notification failed: type=${it.safeLogType()}") }
    }

    fun cancel(context: Context, sessionId: String) {
        context.getSystemService(NotificationManager::class.java)?.cancel(sessionId, ID)
    }
}
