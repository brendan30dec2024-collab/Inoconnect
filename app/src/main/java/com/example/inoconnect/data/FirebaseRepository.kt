package com.example.inoconnect.data

import android.net.Uri
import android.util.Log
import com.facebook.AccessToken
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val currentUserId: String?
        get() = auth.currentUser?.uid

    // ============================================================================================
    // AUTHENTICATION
    // ============================================================================================

    // Checks if the currently signed-in user has verified their email
    suspend fun checkEmailVerification(): Boolean {
        return try {
            val user = auth.currentUser ?: return false
            user.reload().await() // Fetches latest data from server
            user.isEmailVerified
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun registerUser(email: String, pass: String, role: String, username: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = result.user ?: return false

            // Send Verification Email
            user.sendEmailVerification().await()

            val newUser = User(userId = user.uid, email = email, role = role, username = username)
            db.collection("users").document(user.uid).set(newUser).await()

            // Note: We do NOT sign out here. We keep them logged in to wait for verification.
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loginUser(email: String, pass: String): String {
        try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val user = result.user ?: throw Exception("Authentication failed")

            //Comment out this block temporary for testing purpose
            //if (!user.isEmailVerified) {
            //    auth.signOut()
            //    throw Exception("Email not verified. Please check your inbox.")
            //}

            val uid = user.uid
            val snapshot = db.collection("users").document(uid).get().await()
            return snapshot.getString("role") ?: "participant"
        } catch (e: Exception) {
            throw e
        }
    }

    fun logout() {
        auth.signOut()

    }

    suspend fun signInWithGoogle(idToken: String): String? {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return null
            val docRef = db.collection("users").document(user.uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.getString("role")
            } else {
                val newUser = User(
                    userId = user.uid,
                    email = user.email ?: "",
                    role = "participant",
                    username = user.displayName ?: "User"
                )
                docRef.set(newUser).await()
                "participant"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun onSignInWithGithubSuccess(): String? {
        val user = auth.currentUser ?: return null
        return try {
            val docRef = db.collection("users").document(user.uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.getString("role")
            } else {
                val newUser = User(
                    userId = user.uid,
                    email = user.email ?: "",
                    role = "participant",
                    username = user.displayName ?: "GitHub User"
                )
                docRef.set(newUser).await()
                "participant"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun signInWithFacebook(token: AccessToken): String? {
        return try {
            val credential = FacebookAuthProvider.getCredential(token.token)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: return null
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                val newUser = User(
                    userId = uid,
                    email = result.user?.email ?: "",
                    role = "Participant",
                    username = result.user?.displayName ?: "New User"
                )
                db.collection("users").document(uid).set(newUser).await()
                return "Participant"
            }
            doc.getString("role")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun sendPasswordReset(email: String): Boolean {
        return try {
            auth.sendPasswordResetEmail(email).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun reauthenticate(password: String): Boolean {
        val user = auth.currentUser ?: return false
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        return try {
            user.reauthenticate(credential).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updatePassword(newPass: String) {
        auth.currentUser?.updatePassword(newPass)?.await()
    }

    suspend fun deleteAccount() {
        val uid = currentUserId ?: return
        db.collection("users").document(uid).delete().await()
        auth.currentUser?.delete()?.await()
    }

    // --- UPDATED: Added extension parameter to ensure files have proper types ---
    suspend fun uploadFile(uri: Uri, folder: String = "uploads", extension: String? = null): String? {
        return try {
            val extSuffix = if (extension != null) ".$extension" else ""
            val filename = "${UUID.randomUUID()}$extSuffix"
            val ref = storage.reference.child("$folder/$filename")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- UPDATED: Pass "jpg" extension for images ---
    suspend fun uploadImage(imageUri: Uri): String? {
        return uploadFile(imageUri, "images", "jpg")
    }

    suspend fun getUserById(userId: String): User? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            snapshot.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUsersByIds(userIds: List<String>): List<User> {
        if (userIds.isEmpty()) return emptyList()
        return userIds.mapNotNull { uid ->
            try {
                db.collection("users").document(uid).get().await().toObject(User::class.java)
            } catch (e: Exception) { null }
        }
    }

    suspend fun updateUserComplete(
        username: String,
        headline: String,
        bio: String,
        university: String,
        faculty: String,
        course: String,
        yearOfStudy: String,
        skills: List<String>,
        imageUri: Uri?,
        backgroundUri: Uri?
    ) {
        val uid = currentUserId ?: return
        val finalImageUrl = if (imageUri != null) uploadImage(imageUri) else null
        val finalBackgroundUrl = if (backgroundUri != null) uploadImage(backgroundUri) else null
        val updates = mutableMapOf<String, Any>(
            "username" to username,
            "headline" to headline,
            "bio" to bio,
            "university" to university,
            "faculty" to faculty,
            "course" to course,
            "yearOfStudy" to yearOfStudy,
            "skills" to skills,
        )
        if (finalImageUrl != null) updates["profileImageUrl"] = finalImageUrl
        if (finalBackgroundUrl != null) updates["backgroundImageUrl"] = finalBackgroundUrl
        db.collection("users").document(uid).update(updates).await()
    }

    suspend fun updateUserSkills(skills: List<String>) {
        val uid = currentUserId ?: return
        db.collection("users").document(uid).update("skills", skills).await()
    }

    fun searchUsers(query: String): Flow<List<User>> = callbackFlow {
        if (query.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = db.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThan("username", query + "\uf8ff")
            .limit(20) // Added limit for safety
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    val filtered = users.filter { it.userId != currentUserId }
                    trySend(filtered)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getConnectionStatusFlow(otherUserId: String): Flow<String> {
        val uid = currentUserId ?: return flowOf("none")
        val connectedFlow = callbackFlow {
            val listener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        val rawList = snapshot.get("connectionIds") as? List<*>
                        val ids = rawList?.filterIsInstance<String>() ?: emptyList()
                        trySend(ids.contains(otherUserId))
                    } else {
                        trySend(false)
                    }
                }
            awaitClose { listener.remove() }
        }
        val pendingFlow = callbackFlow {
            val listener = db.collection("connection_requests")
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("toUserId", otherUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    trySend(snapshot != null && !snapshot.isEmpty)
                }
            awaitClose { listener.remove() }
        }
        return combine(connectedFlow, pendingFlow) { isConnected, isPending ->
            when {
                isConnected -> "connected"
                isPending -> "pending"
                else -> "none"
            }
        }
    }

    suspend fun sendConnectionRequest(toUserId: String): Boolean {
        val uid = currentUserId ?: return false
        val querySnapshot = db.collection("connection_requests")
            .whereEqualTo("fromUserId", uid)
            .whereEqualTo("toUserId", toUserId)
            .get().await()
        if (!querySnapshot.isEmpty) {
            val existingDoc = querySnapshot.documents.first()
            val status = existingDoc.getString("status")
            if (status == "pending" || status == "accepted") {
                return false
            }
            if (status == "rejected") {
                existingDoc.reference.update(
                    mapOf("status" to "pending", "timestamp" to Timestamp.now())
                ).await()
                return true
            }
        }
        val newRef = db.collection("connection_requests").document()
        val request = ConnectionRequest(
            id = newRef.id,
            fromUserId = uid,
            toUserId = toUserId,
            status = "pending"
        )
        newRef.set(request).await()
        return true
    }

    suspend fun acceptConnectionRequest(requestId: String, fromUserId: String) {
        val currentUid = currentUserId ?: return
        val myUser = getUserById(currentUid)
        val otherUser = getUserById(fromUserId)
        val myName = myUser?.username ?: "User"
        val otherName = otherUser?.username ?: "User"
        try {
            db.runTransaction { transaction ->
                val requestRef = db.collection("connection_requests").document(requestId)
                transaction.update(requestRef, "status", "accepted")
                val myRef = db.collection("users").document(currentUid)
                val senderRef = db.collection("users").document(fromUserId)
                transaction.update(myRef, "connectionIds", FieldValue.arrayUnion(fromUserId))
                transaction.update(myRef, "connectionsCount", FieldValue.increment(1))
                transaction.update(myRef, "followingIds", FieldValue.arrayUnion(fromUserId))
                transaction.update(myRef, "followingCount", FieldValue.increment(1))
                transaction.update(senderRef, "connectionIds", FieldValue.arrayUnion(currentUid))
                transaction.update(senderRef, "connectionsCount", FieldValue.increment(1))
                transaction.update(senderRef, "followingIds", FieldValue.arrayUnion(currentUid))
                transaction.update(senderRef, "followingCount", FieldValue.increment(1))
            }.await()
            sendNotification(
                toUserId = fromUserId,
                type = NotificationType.CONNECTION_ACCEPTED,
                title = "Connection Accepted",
                message = "You are now connected with $myName",
                relatedId = currentUid
            )
            sendNotification(
                toUserId = currentUid,
                type = NotificationType.CONNECTION_ACCEPTED,
                title = "Connection Successful",
                message = "You are now connected with $otherName",
                relatedId = fromUserId
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
        try {
            db.collection("connection_requests")
                .document(requestId)
                .update("status", "rejected")
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun followUser(targetUserId: String) {
        val uid = currentUserId ?: return
        val myRef = db.collection("users").document(uid)
        myRef.update("followingIds", FieldValue.arrayUnion(targetUserId)).await()
        myRef.update("followingCount", FieldValue.increment(1)).await()
    }

    // --- UPDATED: Added limit(50) to prevent reading the entire database ---
    fun getSuggestedUsersFlow(): Flow<List<NetworkUser>> {
        val uid = currentUserId ?: return flowOf(emptyList())
        val usersFlow = callbackFlow {
            val listener = db.collection("users")
                .limit(50) // Fix: Limit read operations
                .addSnapshotListener { snapshot, _ ->
                    val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                    trySend(users)
                }
            awaitClose { listener.remove() }
        }
        val requestsFlow = callbackFlow {
            val listener = db.collection("connection_requests")
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    val ids = snapshot?.documents?.mapNotNull { it.getString("toUserId") }?.toSet()
                        ?: emptySet()
                    trySend(ids)
                }
            awaitClose { listener.remove() }
        }
        val myProfileFlow = callbackFlow {
            val listener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    val user = snapshot?.toObject(User::class.java)
                    val connectionIds = user?.connectionIds?.toSet() ?: emptySet()
                    trySend(connectionIds)
                }
            awaitClose { listener.remove() }
        }
        return combine(usersFlow, requestsFlow, myProfileFlow) { allUsers, pendingSentIds, myConnectionIds ->
            allUsers.mapNotNull { user ->
                if (user.userId == uid) return@mapNotNull null
                val isConnected = myConnectionIds.contains(user.userId)
                if (allUsers.size >= 20 && isConnected) {
                    return@mapNotNull null
                }
                val status = when {
                    isConnected -> "connected"
                    pendingSentIds.contains(user.userId) -> "pending_sent"
                    else -> "not_connected"
                }
                NetworkUser(user, status)
            }
        }
    }

    fun getNetworkStatsFlow(): Flow<Map<String, Int>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { close(); return@callbackFlow }
        val sub = db.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            val user = snapshot?.toObject(User::class.java)
            if (user != null) {
                val stats = mapOf(
                    "connections" to user.connectionsCount,
                    "following" to user.followingCount
                )
                trySend(stats)
            }
        }
        awaitClose { sub.remove() }
    }

    suspend fun createEvent(
        title: String, description: String, location: String,
        eventDate: String, joiningDeadline: String, imageUrl: String
    ) {
        val uid = currentUserId ?: return
        val newDoc = db.collection("events").document()
        val event = Event(
            eventId = newDoc.id, organizerId = uid, title = title,
            description = description, location = location, eventDate = eventDate,
            joiningDeadline = joiningDeadline, imageUrl = imageUrl
        )
        newDoc.set(event).await()
        try {
            val allUsersSnapshot = db.collection("users").get().await()
            val organizerName = getUserById(uid)?.username ?: "An organizer"
            for (doc in allUsersSnapshot.documents) {
                if (doc.id == uid) continue
                sendNotification(
                    toUserId = doc.id,
                    type = NotificationType.NEW_EVENT,
                    title = "New Event: $title",
                    message = "$organizerName has posted a new event.",
                    relatedId = newDoc.id,
                    senderId = uid
                )
            }
        } catch (e: Exception) { e.printStackTrace() }
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
        val projectId = newDoc.id
        val initialMembers = listOf(uid)

        val project = Project(
            projectId = projectId,
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

        val channelId = "project_$projectId"
        val channel = ChatChannel(
            channelId = channelId,
            type = ChannelType.PROJECT_GROUP,
            projectId = projectId,
            groupName = title,
            groupImageUrl = imageUrl,
            participantIds = initialMembers,
            lastMessage = "Project created",
            lastMessageTimestamp = Timestamp.now()
        )
        db.collection("chat_channels").document(channelId).set(channel).await()
    }

    suspend fun getProjectById(projectId: String): Project? {
        return try {
            val snapshot = db.collection("projects").document(projectId).get().await()
            snapshot.toObject(Project::class.java)
        } catch (e: Exception) {
            null
        }
    }

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

    suspend fun getAllProjects(): List<Project> {
        val snapshot = db.collection("projects").get().await()
        return snapshot.toObjects(Project::class.java)
    }

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

    suspend fun requestToJoinProject(projectId: String): Boolean {
        val uid = currentUserId ?: return false
        val myUser = getUserById(uid)
        val myName = myUser?.username ?: "A user"
        return try {
            val projectCreatorId = db.runTransaction { transaction ->
                val ref = db.collection("projects").document(projectId)
                val snapshot = transaction.get(ref)
                val project = snapshot.toObject(Project::class.java) ?: throw Exception("Project not found")
                if (project.memberIds.size >= project.targetTeamSize) throw Exception("Full")
                if (project.pendingApplicantIds.contains(uid) || project.memberIds.contains(uid)) return@runTransaction null
                transaction.update(ref, "pendingApplicantIds", FieldValue.arrayUnion(uid))
                project.creatorId
            }.await()
            if (projectCreatorId != null) {
                val project = getProjectById(projectId)
                val projectTitle = project?.title ?: "your project"
                sendNotification(
                    toUserId = projectCreatorId,
                    type = NotificationType.PROJECT_JOIN_REQUEST,
                    title = "Join Request",
                    message = "$myName wants to join '$projectTitle'",
                    relatedId = projectId,
                    senderId = uid
                )
                true
            } else {
                false
            }
        } catch (e: Exception) { false }
    }

    suspend fun acceptJoinRequest(projectId: String, applicantId: String) {
        val ref = db.collection("projects").document(projectId)
        db.runTransaction { transaction ->
            transaction.update(ref, "pendingApplicantIds", FieldValue.arrayRemove(applicantId))
            transaction.update(ref, "memberIds", FieldValue.arrayUnion(applicantId))
        }.await()

        db.collection("chat_channels").document("project_$projectId")
            .update("participantIds", FieldValue.arrayUnion(applicantId))
            .await()

        val project = getProjectById(projectId)
        val projectTitle = project?.title ?: "a project"
        sendNotification(
            toUserId = applicantId,
            type = NotificationType.PROJECT_ACCEPTED,
            title = "Request Accepted",
            message = "You have been accepted into '$projectTitle'",
            relatedId = projectId
        )
    }

    suspend fun rejectJoinRequest(projectId: String, applicantId: String) {
        db.collection("projects").document(projectId)
            .update("pendingApplicantIds", FieldValue.arrayRemove(applicantId))
            .await()
        val project = getProjectById(projectId)
        val projectTitle = project?.title ?: "a project"
        sendNotification(
            toUserId = applicantId,
            type = NotificationType.PROJECT_DECLINE,
            title = "Application Declined",
            message = "Your request to join '$projectTitle' was declined.",
            relatedId = projectId
        )
    }

    suspend fun removeMember(projectId: String, memberId: String) {
        db.collection("projects").document(projectId)
            .update("memberIds", FieldValue.arrayRemove(memberId))
            .await()

        db.collection("chat_channels").document("project_$projectId")
            .update("participantIds", FieldValue.arrayRemove(memberId))
            .await()

        val project = getProjectById(projectId)
        val projectTitle = project?.title ?: "a project"
        sendNotification(
            toUserId = memberId,
            type = NotificationType.PROJECT_REMOVAL,
            title = "Removed from Project",
            message = "You have been removed from '$projectTitle'",
            relatedId = projectId
        )
    }

    suspend fun updateProjectStatus(projectId: String, status: String) {
        db.collection("projects").document(projectId).update("status", status).await()
    }

    suspend fun deleteProject(projectId: String) {
        db.collection("projects").document(projectId).delete().await()
    }

    suspend fun updateGroupChatName(channelId: String, newName: String) {
        db.collection("chat_channels").document(channelId)
            .update("groupName", newName)
            .await()
    }

    // --- UPDATED: Pass extension based on type ---
    suspend fun sendMessage(
        channelId: String,
        content: String,
        attachmentUri: Uri? = null,
        attachmentType: String? = null,
        attachmentName: String? = null,
        attachmentSize: String? = null
    ) {
        val uid = currentUserId ?: return
        var attachmentUrl: String? = null
        if (attachmentUri != null && attachmentType != null) {
            val folder = when(attachmentType) {
                "image" -> "chat_images"
                "video" -> "chat_videos"
                else -> "chat_files"
            }
            // Simple extension mapping
            val ext = when(attachmentType) {
                "image" -> "jpg"
                "video" -> "mp4"
                else -> null // Let uploadFile handle no extension or caller should have passed it
            }
            attachmentUrl = uploadFile(attachmentUri, folder, ext)
        }

        val myUser = getUserById(uid)
        val myName = myUser?.username ?: "User"

        val channelRef = db.collection("chat_channels").document(channelId)

        if (channelId.startsWith("project_")) {
            val docSnap = channelRef.get().await()
            val isMissingData = !docSnap.exists() || docSnap.get("participantIds") == null

            if (isMissingData) {
                val projectId = channelId.removePrefix("project_")
                val pSnap = db.collection("projects").document(projectId).get().await()
                val project = pSnap.toObject(Project::class.java)

                if (project != null) {
                    val baseData = mapOf(
                        "channelId" to channelId,
                        "type" to ChannelType.PROJECT_GROUP,
                        "projectId" to projectId,
                        "groupName" to project.title,
                        "groupImageUrl" to project.imageUrl,
                        "participantIds" to project.memberIds
                    )
                    channelRef.set(baseData, SetOptions.merge()).await()
                }
            }
        }

        val previewText = if (attachmentUrl != null) {
            "[$attachmentType] ${if(content.isNotBlank()) content else ""}"
        } else {
            content
        }

        val lastMessageText = if(channelId.startsWith("project_")) "$myName: $previewText" else previewText

        val channelUpdate = mutableMapOf<String, Any>(
            "lastMessage" to lastMessageText,
            "lastMessageTimestamp" to Timestamp.now(),
            "lastSenderId" to uid
        )
        channelRef.set(channelUpdate, SetOptions.merge()).await()

        val msgRef = channelRef.collection("messages").document()
        val msgData = hashMapOf(
            "id" to msgRef.id,
            "senderId" to uid,
            "senderName" to myName,
            "content" to content,
            "attachmentUrl" to attachmentUrl,
            "attachmentType" to attachmentType,
            "attachmentName" to attachmentName,
            "attachmentSize" to attachmentSize,
            "timestamp" to Timestamp.now()
        )
        msgRef.set(msgData).await()
    }

    suspend fun sendDirectMessage(
        toUserId: String, content: String, attachmentUri: Uri? = null,
        attachmentType: String? = null, attachmentName: String? = null, attachmentSize: String? = null
    ) {
        val uid = currentUserId ?: return
        val channelId = if (uid < toUserId) "${uid}_$toUserId" else "${toUserId}_$uid"

        val channelRef = db.collection("chat_channels").document(channelId)
        val snapshot = channelRef.get().await()
        if (!snapshot.exists()) {
            val channel = ChatChannel(
                channelId = channelId,
                type = ChannelType.DIRECT,
                participantIds = listOf(uid, toUserId)
            )
            channelRef.set(channel).await()
        }

        sendMessage(channelId, content, attachmentUri, attachmentType, attachmentName, attachmentSize)

        val myUser = getUserById(uid)
        sendNotification(
            toUserId = toUserId,
            type = NotificationType.NEW_DM,
            title = "New Message",
            message = "${myUser?.username ?: "Someone"} sent you a message.",
            relatedId = channelId
        )
    }

    fun getChatChannelsFlow(): Flow<List<ChatChannel>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        val sub = db.collection("chat_channels")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) { return@addSnapshotListener }
                if (snap != null) {
                    val channels = snap.toObjects(ChatChannel::class.java)
                    val sortedChannels = channels.sortedByDescending { it.lastMessageTimestamp }
                    trySend(sortedChannels)
                }
            }
        awaitClose { sub.remove() }
    }

    fun getDirectMessagesFlow(channelId: String): Flow<List<DirectMessage>> = callbackFlow {
        val sub = db.collection("chat_channels").document(channelId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) trySend(snap.toObjects(DirectMessage::class.java))
            }
        awaitClose { sub.remove() }
    }

    suspend fun getOtherUserInChannel(channel: ChatChannel): User? {
        val uid = currentUserId ?: return null
        val otherId = channel.participantIds.find { it != uid } ?: return null
        return getUserById(otherId)
    }

    fun getNotificationsFlow(): Flow<List<AppNotification>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val sub = db.collection("users").document(uid).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    val notifications = snap.documents.mapNotNull { doc ->
                        doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                    }
                    trySend(notifications)
                }
            }
        awaitClose { sub.remove() }
    }

    suspend fun sendNotification(
        toUserId: String,
        type: NotificationType,
        title: String,
        message: String,
        relatedId: String = "",
        senderId: String = ""
    ) {
        val ref = db.collection("users").document(toUserId).collection("notifications").document()
        val notif = AppNotification(
            id = ref.id,
            userId = toUserId,
            type = type,
            title = title,
            message = message,
            relatedId = relatedId,
            senderId = senderId
        )
        ref.set(notif).await()
    }

    suspend fun deleteNotification(notificationId: String) {
        val uid = currentUserId ?: return
        try {
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .delete().await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun markNotificationsAsRead(notificationIds: List<String>) {
        val uid = currentUserId ?: return
        if (notificationIds.isEmpty()) return
        try {
            val batch = db.batch()
            notificationIds.forEach { id ->
                val ref = db.collection("users").document(uid)
                    .collection("notifications").document(id)
                batch.update(ref, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun acceptProjectInvite(notificationId: String, projectId: String) {
        val uid = currentUserId ?: return
        db.collection("projects").document(projectId)
            .update("memberIds", FieldValue.arrayUnion(uid))
            .await()
        deleteNotification(notificationId)
    }
}