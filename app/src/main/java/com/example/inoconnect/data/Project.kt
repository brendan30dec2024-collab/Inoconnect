package com.example.inoconnect.data

data class Project(
    val projectId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(), // This now represents "Looking For (Roles)"
    val memberIds: List<String> = emptyList(),
    val recruitmentDeadline: String = "",
    val targetTeamSize: Int = 1
)