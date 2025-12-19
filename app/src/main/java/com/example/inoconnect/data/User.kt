package com.example.inoconnect.data

data class User(
    val userId: String = "",
    val email: String = "",
    val role: String = "",
    val username: String = "",
    val bio: String = "",
    val skills: List<String> = emptyList(),
    val githubLink: String = "",
    val profileImageUrl: String = ""
)
object UserRole {
    const val ORGANIZER = "ORGANIZER"
    const val PARTICIPANT = "PARTICIPANT"
}