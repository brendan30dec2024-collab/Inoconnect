package com.example.inoconnect.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

enum class ChannelType {
    DIRECT,
    PROJECT_GROUP
}

// 1. Direct/Group Message Channel
data class ChatChannel(
    val channelId: String = "",
    val type: ChannelType = ChannelType.DIRECT, // Distinguish types
    val projectId: String? = null,              // Link to project (if group)
    val groupName: String? = null,              // For group chats
    val groupImageUrl: String? = null,          // For group chats
    val participantIds: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp = Timestamp.now(),
    val lastSenderId: String = ""
)

// 2. The Actual Message
data class DirectMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String? = null, // Useful for group chats to identify sender
    val content: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false,
    val attachmentUrl: String? = null,
    val attachmentType: String? = null, // "image", "video", "file"
    val attachmentName: String? = null,
    val attachmentSize: String? = null
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
    PROJECT_INVITE,
    PROJECT_DECLINE,
    NEW_FOLLOWER,
    NEW_DM,
    SYSTEM_ALERT,
    CONNECTION_ACCEPTED,
    PROJECT_JOIN_REQUEST,
    PROJECT_REMOVAL,
    PROJECT_ACCEPTED,
    NEW_EVENT,
    WELCOME_MESSAGE
}