package com.example.inoconnect.data

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // --- REAL-TIME DASHBOARD FLOW (For MyProjectScreen) ---
    fun getUserProjectsFlow(): Flow<List<Project>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val subscription = db.collection("projects")
            .whereArrayContains("memberIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val projects = snapshot.toObjects(Project::class.java)
                    trySend(projects)
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- AUTH ---
    suspend fun registerUser(email: String, pass: String, role: String, username: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return false
            val newUser = User(userId = uid, email = email, role = role, username = username)
            db.collection("users").document(uid).set(newUser).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginUser(email: String, pass: String): String? {
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return null
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.getString("role")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun logout() {
        auth.signOut()
    }

    // --- EVENT LOGIC (Restored for CreateEventScreen) ---
    suspend fun createEvent(
        title: String,
        description: String,
        location: String,
        eventDate: String,
        joiningDeadline: String,
        imageUrl: String
    ) {
        val uid = currentUserId ?: return
        val newDoc = db.collection("events").document()
        val event = Event(
            eventId = newDoc.id,
            organizerId = uid,
            title = title,
            description = description,
            location = location,
            eventDate = eventDate,
            joiningDeadline = joiningDeadline,
            imageUrl = imageUrl
        )
        newDoc.set(event).await()
    }

    suspend fun getOrganizerEvents(): List<Event> {
        val uid = currentUserId ?: return emptyList()
        val snapshot = db.collection("events").whereEqualTo("organizerId", uid).get().await()
        return snapshot.toObjects(Event::class.java)
    }

    suspend fun getAllEvents(): List<Event> {
        val snapshot = db.collection("events").get().await()
        return snapshot.toObjects(Event::class.java)
    }

    suspend fun getEventById(eventId: String): Event? {
        return try {
            val snapshot = db.collection("events").document(eventId).get().await()
            snapshot.toObject(Event::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteEvent(eventId: String) {
        try {
            val snapshot = db.collection("events").document(eventId).get().await()
            val imageUrl = snapshot.getString("imageUrl")
            if (!imageUrl.isNullOrEmpty()) {
                try { storage.getReferenceFromUrl(imageUrl).delete().await() } catch (e: Exception) {}
            }
            db.collection("events").document(eventId).delete().await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun joinEvent(eventId: String) {
        val uid = currentUserId ?: return
        try {
            db.collection("events").document(eventId)
                .update("participantIds", FieldValue.arrayUnion(uid))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- PROJECT LOGIC ---
    suspend fun createProject(
        title: String,
        description: String,
        imageUrl: String,
        tags: List<String>,
        recruitmentDeadline: String,
        targetTeamSize: Int
    ) {
        val uid = currentUserId ?: return
        val newDoc = db.collection("projects").document()
        val initialMembers = listOf(uid)

        val project = Project(
            projectId = newDoc.id,
            creatorId = uid,
            title = title,
            description = description,
            imageUrl = imageUrl,
            tags = tags,
            memberIds = initialMembers,
            recruitmentDeadline = recruitmentDeadline,
            targetTeamSize = targetTeamSize
        )
        newDoc.set(project).await()
    }

    suspend fun getProjectById(projectId: String): Project? {
        return try {
            val snapshot = db.collection("projects").document(projectId).get().await()
            snapshot.toObject(Project::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllProjects(): List<Project> {
        val snapshot = db.collection("projects").get().await()
        return snapshot.toObjects(Project::class.java)
    }

    // --- STORAGE ---
    suspend fun uploadImage(imageUri: Uri): String? {
        return try {
            val filename = UUID.randomUUID().toString()
            val ref = storage.reference.child("images/$filename")
            ref.putFile(imageUri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            null
        }
    }

    // --- MILESTONE LOGIC ---
    suspend fun addMilestone(projectId: String, title: String) {
        val milestone = com.example.inoconnect.data.Milestone(title = title, isCompleted = false)
        db.collection("projects").document(projectId)
            .update("milestones", FieldValue.arrayUnion(milestone))
            .await()
    }

    suspend fun toggleMilestone(projectId: String, milestone: com.example.inoconnect.data.Milestone) {
        db.runTransaction { transaction ->
            val ref = db.collection("projects").document(projectId)
            val snapshot = transaction.get(ref)
            val project = snapshot.toObject(Project::class.java) ?: return@runTransaction

            val updatedMilestones = project.milestones.map {
                if (it.id == milestone.id) it.copy(isCompleted = !it.isCompleted) else it
            }
            transaction.update(ref, "milestones", updatedMilestones)
        }.await()
    }

    suspend fun deleteMilestone(projectId: String, milestone: com.example.inoconnect.data.Milestone) {
        db.runTransaction { transaction ->
            val ref = db.collection("projects").document(projectId)
            val snapshot = transaction.get(ref)
            val project = snapshot.toObject(Project::class.java) ?: return@runTransaction

            val updatedMilestones = project.milestones.filter { it.id != milestone.id }
            transaction.update(ref, "milestones", updatedMilestones)
        }.await()
    }

    // --- JOIN REQUEST LOGIC ---
    suspend fun requestToJoinProject(projectId: String): Boolean {
        val uid = currentUserId ?: return false
        return try {
            db.runTransaction { transaction ->
                val ref = db.collection("projects").document(projectId)
                val snapshot = transaction.get(ref)
                val project = snapshot.toObject(Project::class.java) ?: return@runTransaction false

                if (project.memberIds.size >= project.targetTeamSize) throw Exception("Full")
                if (project.pendingApplicantIds.contains(uid) || project.memberIds.contains(uid)) return@runTransaction true

                transaction.update(ref, "pendingApplicantIds", FieldValue.arrayUnion(uid))
                true
            }.await()
        } catch (e: Exception) { false }
    }

    suspend fun acceptJoinRequest(projectId: String, applicantId: String) {
        val ref = db.collection("projects").document(projectId)
        db.runTransaction { transaction ->
            transaction.update(ref, "pendingApplicantIds", FieldValue.arrayRemove(applicantId))
            transaction.update(ref, "memberIds", FieldValue.arrayUnion(applicantId))
        }.await()
    }

    suspend fun rejectJoinRequest(projectId: String, applicantId: String) {
        db.collection("projects").document(projectId)
            .update("pendingApplicantIds", FieldValue.arrayRemove(applicantId))
            .await()
    }

    // --- ADMIN LOGIC ---
    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) return emptyList()
        return userIds.mapNotNull { uid ->
            try {
                db.collection("users").document(uid).get().await().toObject(User::class.java)
            } catch (e: Exception) { null }
        }
    }

    suspend fun removeMember(projectId: String, memberId: String) {
        db.collection("projects").document(projectId)
            .update("memberIds", FieldValue.arrayRemove(memberId))
            .await()
    }

    suspend fun updateProjectStatus(projectId: String, status: String) {
        db.collection("projects").document(projectId).update("status", status).await()
    }

    suspend fun deleteProject(projectId: String) {
        db.collection("projects").document(projectId).delete().await()
    }

    // --- CHAT ---
    suspend fun sendMessage(projectId: String, messageText: String, senderName: String) {
        val uid = currentUserId ?: return
        val newMsgRef = db.collection("projects").document(projectId).collection("messages").document()
        val message = ChatMessage(id = newMsgRef.id, senderId = uid, senderName = senderName, message = messageText, timestamp = Timestamp.now())
        newMsgRef.set(message).await()
    }

    fun getProjectMessages(projectId: String): Flow<List<ChatMessage>> = callbackFlow {
        val subscription = db.collection("projects").document(projectId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) trySend(snapshot.toObjects(ChatMessage::class.java))
            }
        awaitClose { subscription.remove() }
    }

    // --- PROFILE ---
    suspend fun updateUserProfile(bio: String, skills: List<String>, githubLink: String, imageUri: Uri?) {
        val uid = currentUserId ?: return
        var finalImageUrl: String? = null
        if (imageUri != null) finalImageUrl = uploadImage(imageUri)

        val updates = mutableMapOf<String, Any>("bio" to bio, "skills" to skills, "githubLink" to githubLink)
        if (finalImageUrl != null) updates["profileImageUrl"] = finalImageUrl

        db.collection("users").document(uid).update(updates).await()
    }
}