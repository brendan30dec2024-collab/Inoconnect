package com.example.inoconnect.data

data class Project(
    val projectId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    // Project specific fields
    val tags: List<String> = emptyList(), // e.g., ["AI Engineer", "WIA2001"]
    val memberIds: List<String> = emptyList()
)