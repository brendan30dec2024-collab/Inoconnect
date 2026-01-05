package com.example.inoconnect.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

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
    val isRead: Boolean = false,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null // "image", "video", "file"
)

// 3. System Notification
data class AppNotification(
    val id: String = "",
    val userId: String = "", // Who receives this
    val type: NotificationType = NotificationType.SYSTEM_ALERT,
    val title: String = "",
    val message: String = "",
    val relatedId: String = "", // Could be projectId, userId, etc.
    val senderId: String = "",
    val timestamp: Timestamp = Timestamp.now(),

    @get:PropertyName("isRead")
    val isRead: Boolean = false
)

enum class NotificationType {
    PROJECT_INVITE,       // "You have been invited to join X"
    PROJECT_DECLINE,      // "Your request to join X was declined"
    NEW_FOLLOWER,         // "User X wants to follow/connect"
    NEW_DM,               // "New message from User X"
    SYSTEM_ALERT,         // System messages
    CONNECTION_ACCEPTED,  // "You are now connected with X"

    // --- NEW TYPES ---
    PROJECT_JOIN_REQUEST, // "User X wants to join your project"
    PROJECT_REMOVAL,      // "You were removed from project X"
    PROJECT_ACCEPTED,     // "You were accepted into project X"
    NEW_EVENT,            // "New event published by X"
    WELCOME_MESSAGE       // "Welcome to InnoConnect"
}