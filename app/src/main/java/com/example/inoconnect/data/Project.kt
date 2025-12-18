package com.example.inoconnect.data

data class Project(
    val projectId: String = "",
    val creatorId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val memberIds: List<String> = emptyList(),
    val recruitmentDeadline: String = "",
    val targetTeamSize: Int = 1,

    val status: String = "Active",
    val milestones: List<Milestone> = emptyList(),
    val pendingApplicantIds: List<String> = emptyList()
)

data class Milestone(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val isCompleted: Boolean = false
)