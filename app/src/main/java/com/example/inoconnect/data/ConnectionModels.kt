package com.example.inoconnect.data

import com.google.firebase.Timestamp

data class ConnectionRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val timestamp: Timestamp = Timestamp.now()
)

// Helper for UI to show the user details + the request status
data class NetworkUser(
    val user: User,
    val connectionStatus: String // "not_connected", "pending_sent", "pending_received", "connected"
)