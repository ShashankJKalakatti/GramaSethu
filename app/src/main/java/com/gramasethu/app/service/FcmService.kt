package com.gramasethu.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gramasethu.app.R
import com.gramasethu.app.ui.BridgeDetailActivity
import com.gramasethu.app.ui.MainActivity

/**
 * Firebase Cloud Messaging service.
 *
 * Project number (Sender ID): 177826766555
 * FCM topics used:
 *   - "all_bridges"      → every status change
 *   - "submerged_alerts" → only SUBMERGED events (high-priority alarm)
 *   - "karnataka_monsoon"→ monsoon / rainfall warnings
 *
 * Notification channels:
 *   - gs_alerts  (IMPORTANCE_HIGH  + alarm sound) → SUBMERGED warnings
 *   - gs_updates (IMPORTANCE_DEFAULT)             → OPEN / DAMAGED updates
 *   - gs_monsoon (IMPORTANCE_HIGH)                → monsoon alerts
 */
class FcmService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FcmService"

        // Sender ID from Firebase Console project 177826766555
        const val SENDER_ID = "177826766555"

        // Notification channel IDs
        const val CH_ALERTS  = "gs_alerts"
        const val CH_UPDATES = "gs_updates"
        const val CH_MONSOON = "gs_monsoon"

        // FCM topics – subscribe in MainActivity
        const val TOPIC_ALL       = "all_bridges"
        const val TOPIC_SUBMERGED = "submerged_alerts"
        const val TOPIC_MONSOON   = "karnataka_monsoon"

        /**
         * Subscribe this device to all relevant FCM topics.
         * Called once from MainActivity after sign-in.
         */
        fun subscribeToTopics() {
            val fcm = FirebaseMessaging.getInstance()
            fcm.subscribeToTopic(TOPIC_ALL)
                .addOnSuccessListener { Log.d(TAG, "Subscribed to $TOPIC_ALL") }
                .addOnFailureListener { Log.e(TAG, "Failed $TOPIC_ALL", it) }

            fcm.subscribeToTopic(TOPIC_SUBMERGED)
                .addOnSuccessListener { Log.d(TAG, "Subscribed to $TOPIC_SUBMERGED") }
                .addOnFailureListener { Log.e(TAG, "Failed $TOPIC_SUBMERGED", it) }

            fcm.subscribeToTopic(TOPIC_MONSOON)
                .addOnSuccessListener { Log.d(TAG, "Subscribed to $TOPIC_MONSOON") }
                .addOnFailureListener { Log.e(TAG, "Failed $TOPIC_MONSOON", it) }
        }
    }

    // ── Incoming message ───────────────────────────────────────────────────────

    override fun onMessageReceived(msg: RemoteMessage) {
        Log.d(TAG, "FCM from: ${msg.from}, data: ${msg.data}")

        val title    = msg.notification?.title ?: msg.data["title"] ?: "Grama-Sethu Alert"
        val body     = msg.notification?.body  ?: msg.data["body"]  ?: "A bridge status has changed."
        val type     = msg.data["type"]     ?: "update"   // "alert" | "monsoon" | "update"
        val bridgeId = msg.data["bridge_id"]              // optional – opens detail screen

        createChannels()

        val channelId = when (type) {
            "alert"   -> CH_ALERTS
            "monsoon" -> CH_MONSOON
            else      -> CH_UPDATES
        }

        showNotification(title, body, channelId, bridgeId, type)
    }

    // ── Token refresh ──────────────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: ${token.take(20)}…")
        // In production: send this token to your server so you can send
        // targeted (unicast) notifications to specific devices.
        // For now, topic-based (multicast) messaging is used instead.
    }

    // ── Notification builder ───────────────────────────────────────────────────

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        bridgeId: String?,
        type: String
    ) {
        // Tap action: open bridge detail if bridge_id provided, else open main
        val intent = if (!bridgeId.isNullOrBlank()) {
            Intent(this, BridgeDetailActivity::class.java)
                .putExtra(BridgeDetailActivity.EXTRA_ID, bridgeId)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } else {
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pi = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Icon and colour per type
        val smallIcon = when (type) {
            "alert"   -> R.drawable.ic_alert
            "monsoon" -> R.drawable.ic_alert
            else      -> R.drawable.ic_bridge
        }
        val color = when (type) {
            "alert"   -> getColor(R.color.status_submerged)
            "monsoon" -> getColor(R.color.primary_dark)
            else      -> getColor(R.color.primary)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(smallIcon)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CH_UPDATES) NotificationCompat.PRIORITY_DEFAULT
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .setContentIntent(pi)

        // Use alarm sound for SUBMERGED alerts
        if (channelId == CH_ALERTS) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
        }

        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mgr.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    // ── Notification channels ──────────────────────────────────────────────────

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // High-priority SUBMERGED alert channel with alarm sound
        if (mgr.getNotificationChannel(CH_ALERTS) == null) {
            NotificationChannel(CH_ALERTS, "🚨 Bridge Alerts",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Plays alarm when a nearby bridge is submerged"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                val audioAttr = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), audioAttr)
                mgr.createNotificationChannel(this)
            }
        }

        // Standard bridge status update channel
        if (mgr.getNotificationChannel(CH_UPDATES) == null) {
            NotificationChannel(CH_UPDATES, "Bridge Status Updates",
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifies when a bridge status changes to Open or Damaged"
                mgr.createNotificationChannel(this)
            }
        }

        // Monsoon / rainfall warning channel
        if (mgr.getNotificationChannel(CH_MONSOON) == null) {
            NotificationChannel(CH_MONSOON, "🌧 Monsoon Alerts",
                NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Heavy rainfall and flood warnings for Karnataka catchment areas"
                enableVibration(true)
                mgr.createNotificationChannel(this)
            }
        }
    }
}
