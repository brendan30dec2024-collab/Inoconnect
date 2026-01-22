package com.example.inoconnect

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        var title = "InnoConnect"
        var body = "New Message"

        // 1. Check Data Payload (Preferred for custom attachment text)
        if (remoteMessage.data.isNotEmpty()) {
            val data = remoteMessage.data

            // Extract Sender Name if available
            title = data["senderName"] ?: data["title"] ?: title

            // Extract Message Content or generate based on Attachment Type
            val content = data["content"] ?: data["message"] ?: ""
            val attachmentType = data["attachmentType"]
            val attachmentName = data["attachmentName"]

            body = when (attachmentType) {
                "image" -> "📷 Sent a photo"
                "video" -> "🎥 Sent a video"
                "file" -> {
                    if (!attachmentName.isNullOrEmpty()) "📎 Sent a file: $attachmentName"
                    else "📎 Sent a file"
                }
                else -> content.ifEmpty { body }
            }
        }

        // 2. Fallback to Notification Payload (if Data didn't provide info)
        remoteMessage.notification?.let {
            title = it.title ?: title
            // Only overwrite body if we didn't generate a specific attachment message above
            if (remoteMessage.data.isEmpty()) {
                body = it.body ?: body
            }
        }

        showNotification(title, body)
    }

    private fun showNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "inoconnect_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody)) // Allows long file names to wrap

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "InnoConnect Messages", // User visible channel name
                NotificationManager.IMPORTANCE_HIGH 
            )
            channel.description = "Notifications for new messages and alerts"
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .update("fcmToken", token)
            .addOnFailureListener { e -> Log.e("FCM", "Failed to save token", e) }
    }
}
