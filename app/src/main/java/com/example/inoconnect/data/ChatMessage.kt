package com.example.inoconnect.data

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "", // Store name to avoid extra lookups
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now()
)