package com.example.inoconnect.data

// Update in data/User.kt
data class User(
    val userId: String = "",
    val email: String = "",
    val role: String = "",
    val username: String = "", // Used as Full Name
    val profileImageUrl: String = "",

    // --- Academic Fields ---
    val headline: String = "",
    val faculty: String = "",
    val course: String = "",
    val yearOfStudy: String = "",

    // --- Biodata ---
    val bio: String = "",
    val resumeUrl: String = "",

    // --- Stats  ---
    val connectionsCount: Int = 0,
    val followingCount: Int = 0,
    val projectsCompleted: Int = 0,

    // --- Contact ---
    val phoneNumber: String = "",
    val githubLink: String = "",
    val linkedinLink: String = "",
    val portfolioLink: String = "",

    val skills: List<String> = emptyList()
)
object UserRole {
    const val ORGANIZER = "ORGANIZER"
    const val PARTICIPANT = "PARTICIPANT"
}