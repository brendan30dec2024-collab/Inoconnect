package com.example.inoconnect.data

import com.google.firebase.Timestamp

// 1. Direct Message Channel (Represents a conversation between 2 users)
data class ChatChannel(
    val channelId: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val lastSenderId: String = ""
)

// 2. The Actual Message
data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)

// 3. System Notification
data class AppNotification(
    val id: String = "",
    val userId: String = "", // Who receives this
    // FIXED: Changed .SYSTEM to .SYSTEM_ALERT to match the enum below
    val type: NotificationType = NotificationType.SYSTEM_ALERT,
    val title: String = "",
    val message: String = "",
    val relatedId: String = "", // Could be projectId, userId, etc.
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)

enum class NotificationType {
    PROJECT_INVITE,     // "You have been invited to join X"
    PROJECT_DECLINE,    // "Your request to join X was declined"
    NEW_FOLLOWER,       // "User X wants to follow/connect"
    NEW_DM,             // "New message from User X"
    SYSTEM_ALERT        // "Welcome to InnoConnect!"
}