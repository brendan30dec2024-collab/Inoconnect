package com.example.inoconnect.data

data class Event(
    val eventId: String = "",
    val organizerId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val imageUrl: String = "",
    val eventDate: String = "",
    val joiningDeadline: String = "",
    val participantIds: List<String> = emptyList()
)