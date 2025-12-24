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
import kotlinx.coroutines.flow.combine // --- ADDED THIS IMPORT
import kotlinx.coroutines.flow.flowOf   // --- ADDED THIS IMPORT
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

    // --- NEW: CONNECTION STATUS FLOW (Fixes Sync & Pending Bugs) ---
    fun getConnectionStatusFlow(otherUserId: String): Flow<String> {
        val uid = currentUserId ?: return flowOf("none")

        // 1. Check if already connected (Real-time listener on User document)
        val connectedFlow = callbackFlow {
            val listener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val ids = snapshot.get("connectionIds") as? List<String> ?: emptyList()
                        trySend(ids.contains(otherUserId))
                    } else {
                        trySend(false)
                    }
                }
            awaitClose { listener.remove() }
        }

        // 2. Check if I sent a pending request (Real-time listener on Requests)
        val pendingFlow = callbackFlow {
            val listener = db.collection("connection_requests")
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("toUserId", otherUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    // If any document exists, it means a request is pending
                    trySend(snapshot != null && !snapshot.isEmpty)
                }
            awaitClose { listener.remove() }
        }

        // 3. Combine both flows to determine the final UI state
        return combine(connectedFlow, pendingFlow) { isConnected, isPending ->
            when {
                isConnected -> "connected"
                isPending -> "pending"
                else -> "none"
            }
        }
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
        val milestone = Milestone(title = title, isCompleted = false)
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

    // ==========================================
    //       NETWORK / CONNECTION BACKEND
    // ==========================================

    // 1. Send a Connection Request
    suspend fun sendConnectionRequest(toUserId: String): Boolean {
        val uid = currentUserId ?: return false

        // Check if request already exists
        val existing = db.collection("connection_requests")
            .whereEqualTo("fromUserId", uid)
            .whereEqualTo("toUserId", toUserId)
            .get().await()

        if (!existing.isEmpty) return false // Already sent

        val newRef = db.collection("connection_requests").document()
        val request = ConnectionRequest(
            id = newRef.id,
            fromUserId = uid,
            toUserId = toUserId,
            status = "pending"
        )
        newRef.set(request).await()

        // Auto-follow the person you connect with? (Optional, LinkedIn does this)
        followUser(toUserId)
        return true
    }

    // 2. Accept a Request
    suspend fun acceptConnectionRequest(requestId: String, fromUserId: String) {
        val currentUid = currentUserId ?: return

        db.runTransaction { transaction ->
            // A. Update Request Status to 'accepted'
            val requestRef = db.collection("connection_requests").document(requestId)
            transaction.update(requestRef, "status", "accepted")

            // B. Add each other to 'connectionIds' list in Users collection
            val myRef = db.collection("users").document(currentUid)
            val otherRef = db.collection("users").document(fromUserId)

            transaction.update(myRef, "connectionIds", FieldValue.arrayUnion(fromUserId))
            transaction.update(myRef, "connectionsCount", FieldValue.increment(1))

            transaction.update(otherRef, "connectionIds", FieldValue.arrayUnion(currentUid))
            transaction.update(otherRef, "connectionsCount", FieldValue.increment(1))
        }.await()
    }

    // 3. Follow a User
    suspend fun followUser(targetUserId: String) {
        val uid = currentUserId ?: return
        val myRef = db.collection("users").document(uid)

        // Add to local 'followingIds'
        myRef.update("followingIds", FieldValue.arrayUnion(targetUserId)).await()
        myRef.update("followingCount", FieldValue.increment(1)).await()
    }

    // 4. REAL-TIME: Get Suggested Users (Users I am NOT connected to)
    // --- REAL-TIME SUGGESTIONS FIX ---
    fun getSuggestedUsersFlow(): Flow<List<NetworkUser>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        // 1. Listen to ALL Users (So we see name/photo/university changes INSTANTLY)
        val usersListener = db.collection("users")
            // .limit(50) // Removed limit to ensure you see your test accounts
            .addSnapshotListener { allUsersSnap, error ->
                if (error != null) return@addSnapshotListener
                val allUsers = allUsersSnap?.toObjects(User::class.java) ?: emptyList()

                // 2. Fetch MY User Data (To check who I am already connected with)
                // We use a simple get() here to avoid complex nested listeners,
                // assuming my connections don't change by magic in the background.
                db.collection("users").document(uid).get().addOnSuccessListener { mySnap ->
                    val myUser = mySnap.toObject(User::class.java)
                    val myConnections = myUser?.connectionIds?.toSet() ?: emptySet()

                    // 3. Fetch Pending Requests (To show 'Pending' button correctly)
                    db.collection("connection_requests")
                        .whereEqualTo("fromUserId", uid)
                        .whereEqualTo("status", "pending")
                        .get()
                        .addOnSuccessListener { reqSnap ->
                            val pendingSentIds = reqSnap.documents
                                .mapNotNull { it.getString("toUserId") }
                                .toSet()

                            // 4. Filter & Map Results
                            val suggestions = allUsers.mapNotNull { user ->
                                // Don't suggest myself
                                if (user.userId == uid) return@mapNotNull null
                                // Don't suggest people I'm already connected with
                                if (myConnections.contains(user.userId)) return@mapNotNull null

                                // Check if I already sent a request
                                val status = if (pendingSentIds.contains(user.userId)) "pending_sent" else "not_connected"

                                NetworkUser(user, status)
                            }

                            // Emit the fresh list to the UI
                            trySend(suggestions)
                        }
                }
            }

        // Clean up listener when screen is closed
        awaitClose { usersListener.remove() }
    }

    // 5. REAL-TIME: Get Network Stats (Invites, Connections, Following)
    fun getNetworkStatsFlow(): Flow<Map<String, Int>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { close(); return@callbackFlow }

        val sub = db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            if (user != null) {
                // We also need to count "Invites Sent" manually or store it
                // For now, let's just return what we have in the User object
                val stats = mapOf(
                    "connections" to user.connectionsCount,
                    "following" to user.followingCount
                    // "invites" requires a separate query count, effectively
                )
                trySend(stats)
            }
        }
        awaitClose { sub.remove() }
    }

    // ==========================================
    //       MESSAGING & NOTIFICATIONS
    // ==========================================

    // --- 1. NOTIFICATIONS ---
    fun getNotificationsFlow(): Flow<List<AppNotification>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        val sub = db.collection("users").document(uid).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    trySend(snap.toObjects(AppNotification::class.java))
                }
            }
        awaitClose { sub.remove() }
    }

    suspend fun sendNotification(toUserId: String, type: NotificationType, title: String, message: String, relatedId: String = "") {
        val ref = db.collection("users").document(toUserId).collection("notifications").document()
        val notif = AppNotification(
            id = ref.id,
            userId = toUserId,
            type = type,
            title = title,
            message = message,
            relatedId = relatedId
        )
        ref.set(notif).await()
    }

    // --- 2. DIRECT MESSAGES ---

    // Get list of active conversations
    fun getChatChannelsFlow(): Flow<List<ChatChannel>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        val sub = db.collection("chat_channels")
            .whereArrayContains("participantIds", uid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) trySend(snap.toObjects(ChatChannel::class.java))
            }
        awaitClose { sub.remove() }
    }

    // Get messages inside a conversation
    fun getDirectMessagesFlow(channelId: String): Flow<List<DirectMessage>> = callbackFlow {
        val sub = db.collection("chat_channels").document(channelId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) trySend(snap.toObjects(DirectMessage::class.java))
            }
        awaitClose { sub.remove() }
    }

    // Send a Direct Message
    suspend fun sendDirectMessage(toUserId: String, content: String) {
        val uid = currentUserId ?: return

        // Generate a consistent Channel ID (Sort UIDs alphabetically so UserA_UserB is same as UserB_UserA)
        val channelId = if (uid < toUserId) "${uid}_$toUserId" else "${toUserId}_$uid"

        val channelRef = db.collection("chat_channels").document(channelId)

        // 1. Ensure Channel Exists or Update Last Message
        val channelUpdate = mapOf(
            "channelId" to channelId,
            "participantIds" to listOf(uid, toUserId), // Ensure both are in
            "lastMessage" to content,
            "lastMessageTimestamp" to Timestamp.now(),
            "lastSenderId" to uid
        )
        channelRef.set(channelUpdate, com.google.firebase.firestore.SetOptions.merge()).await()

        // 2. Add Message
        val msgRef = channelRef.collection("messages").document()
        val msg = DirectMessage(
            id = msgRef.id,
            senderId = uid,
            content = content
        )
        msgRef.set(msg).await()
    }

    // Helper to get the "Other User" details from a chat channel
    suspend fun getOtherUserInChannel(channel: ChatChannel): User? {
        val uid = currentUserId ?: return null
        val otherId = channel.participantIds.find { it != uid } ?: return null
        return getUserById(otherId)
    }

    // --- FOR "FOLLOWERS" TAB (Incoming Connection Requests) ---
    fun getIncomingConnectionRequestsFlow(): Flow<List<ConnectionRequest>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        val sub = db.collection("connection_requests")
            .whereEqualTo("toUserId", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    trySend(snap.toObjects(ConnectionRequest::class.java))
                }
            }
        awaitClose { sub.remove() }
    }

    suspend fun rejectConnectionRequest(requestId: String) {
        db.collection("connection_requests").document(requestId).update("status", "rejected").await()
    }

    // --- FOR "INVITATIONS" TAB (Project Invites) ---
    // Simulating accepting a project invite notification
    suspend fun acceptProjectInvite(notificationId: String, projectId: String) {
        val uid = currentUserId ?: return

        // 1. Add user to project
        db.collection("projects").document(projectId)
            .update("memberIds", FieldValue.arrayUnion(uid))
            .await()

        // 2. Mark notification as read/handled
        db.collection("users").document(uid)
            .collection("notifications").document(notificationId)
            .delete() // Or update to isRead = true
            .await()
    }

    // --- FULL PROFILE UPDATE ---
    suspend fun updateUserComplete(
        username: String,
        headline: String,
        bio: String,
        university: String,
        faculty: String,
        course: String,
        yearOfStudy: String,
        skills: List<String>,       // merged from updateUserProfile
        imageUri: Uri?,
        backgroundUri: Uri?
    ) {
        val uid = currentUserId ?: return

        // 1. Upload Images if they exist
        val finalImageUrl = if (imageUri != null) uploadImage(imageUri) else null
        val finalBackgroundUrl = if (backgroundUri != null) uploadImage(backgroundUri) else null

        // 2. Prepare Updates Map
        val updates = mutableMapOf<String, Any>(
            "username" to username,
            "headline" to headline,
            "bio" to bio,
            "university" to university,
            "faculty" to faculty,
            "course" to course,
            "yearOfStudy" to yearOfStudy,
            "skills" to skills,           // Added
        )

        // 3. Only add URLs if they are not null (to avoid overwriting with null)
        if (finalImageUrl != null) updates["profileImageUrl"] = finalImageUrl
        if (finalBackgroundUrl != null) updates["backgroundImageUrl"] = finalBackgroundUrl

        // 4. Update Firestore
        db.collection("users").document(uid).update(updates).await()
    }

    // --- NEW: Update Skills Only ---
    suspend fun updateUserSkills(skills: List<String>) {
        val uid = currentUserId ?: return
        db.collection("users").document(uid).update("skills", skills).await()
    }

}