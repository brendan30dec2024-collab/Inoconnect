package com.example.inoconnect.data

data class User(
    val userId: String = "",
    val email: String = "",
    val role: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val backgroundImageUrl: String = "", // <--- THIS FIXES YOUR ERROR

    // --- Academic Fields ---
    val headline: String = "",
    val university: String = "", // <--- NEW FIELD
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

    val skills: List<String> = emptyList(),

    //--- Connection ---
    val connectionIds: List<String> = emptyList(), // List of UserIDs you are connected to
    val followingIds: List<String> = emptyList(),   // List of UserIDs you follow
)

object UserRole {
    const val ORGANIZER = "ORGANIZER"
    const val PARTICIPANT = "PARTICIPANT"
}