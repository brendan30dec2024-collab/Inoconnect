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
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential
import java.util.UUID
import com.google.firebase.auth.FacebookAuthProvider
import com.facebook.AccessToken
import android.util.Log



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

    // --- AUTH (Updated for Welcome Message) ---
    suspend fun registerUser(email: String, pass: String, role: String, username: String): Boolean {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: return false
            val newUser = User(userId = uid, email = email, role = role, username = username)
            db.collection("users").document(uid).set(newUser).await()

            // 5. Send Welcome Notification
            sendNotification(
                toUserId = uid,
                type = NotificationType.WELCOME_MESSAGE,
                title = "Welcome to InnoConnect!",
                message = "We are glad to have you here. Start by setting up your profile."
            )
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

        // 4. Notify ALL Users about New Event
        try {
            // Fetch ALL users to send the blast
            val allUsersSnapshot = db.collection("users").get().await()
            val organizerName = getUserById(uid)?.username ?: "An organizer"

            for (doc in allUsersSnapshot.documents) {
                // Don't send notification to the organizer themselves
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

    // --- HELPER: Delete Notification ---
    suspend fun deleteNotification(notificationId: String) {
        val uid = currentUserId ?: return
        try {
            db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
                .delete().await()
        } catch (e: Exception) { e.printStackTrace() }
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
        val myUser = getUserById(uid)
        val myName = myUser?.username ?: "A user"

        return try {
            // A. Transaction to add to pending list
            val projectCreatorId = db.runTransaction { transaction ->
                val ref = db.collection("projects").document(projectId)
                val snapshot = transaction.get(ref)
                val project = snapshot.toObject(Project::class.java) ?: throw Exception("Project not found")

                if (project.memberIds.size >= project.targetTeamSize) throw Exception("Full")
                if (project.pendingApplicantIds.contains(uid) || project.memberIds.contains(uid)) return@runTransaction null // Already applied

                transaction.update(ref, "pendingApplicantIds", FieldValue.arrayUnion(uid))
                project.creatorId // Return creator ID for notification
            }.await()

            // B. Send Notification to Owner (Scenario 1)
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
                false // Already applied
            }
        } catch (e: Exception) { false }
    }

    suspend fun acceptJoinRequest(projectId: String, applicantId: String) {
        val ref = db.collection("projects").document(projectId)

        // A. Update Project
        db.runTransaction { transaction ->
            transaction.update(ref, "pendingApplicantIds", FieldValue.arrayRemove(applicantId))
            transaction.update(ref, "memberIds", FieldValue.arrayUnion(applicantId))
        }.await()

        // B. Notify Applicant (Scenario 3)
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
        // A. Update Project
        db.collection("projects").document(projectId)
            .update("pendingApplicantIds", FieldValue.arrayRemove(applicantId))
            .await()

        // B. Notify Applicant
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
        // A. Remove from DB
        db.collection("projects").document(projectId)
            .update("memberIds", FieldValue.arrayRemove(memberId))
            .await()

        // B. Notify Member (Scenario 2)
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

        // 1. Check if request already exists
        val querySnapshot = db.collection("connection_requests")
            .whereEqualTo("fromUserId", uid)
            .whereEqualTo("toUserId", toUserId)
            .get().await()

        // 2. Handle existing requests (e.g. Reactivate a rejected one)
        if (!querySnapshot.isEmpty) {
            val existingDoc = querySnapshot.documents.first()
            val status = existingDoc.getString("status")

            if (status == "pending" || status == "accepted") {
                return false // Already active
            }

            if (status == "rejected") {
                // Reactivate the rejected request
                existingDoc.reference.update(
                    mapOf(
                        "status" to "pending",
                        "timestamp" to Timestamp.now()
                    )
                ).await()
                // CRITICAL: Do NOT call followUser here.
                return true
            }
        }

        // 3. Create NEW request
        val newRef = db.collection("connection_requests").document()
        val request = ConnectionRequest(
            id = newRef.id,
            fromUserId = uid,
            toUserId = toUserId,
            status = "pending"
        )
        newRef.set(request).await()

        // CRITICAL: I removed followUser(toUserId) here.
        // Now, clicking "Connect" sends the invite but DOES NOT change your stats.
        return true
    }

    // 2. Accept a Request
    suspend fun acceptConnectionRequest(requestId: String, fromUserId: String) {
        val currentUid = currentUserId ?: return

        // 1. Fetch User Details (To get names for the notification)
        val myUser = getUserById(currentUid)
        val otherUser = getUserById(fromUserId)
        val myName = myUser?.username ?: "User"
        val otherName = otherUser?.username ?: "User"

        try {
            db.runTransaction { transaction ->
                // A. Update Request Status
                val requestRef = db.collection("connection_requests").document(requestId)
                transaction.update(requestRef, "status", "accepted")

                // B. Prepare User References
                val myRef = db.collection("users").document(currentUid)
                val senderRef = db.collection("users").document(fromUserId)

                // C. Update Connections & Following (Mutual)
                transaction.update(myRef, "connectionIds", FieldValue.arrayUnion(fromUserId))
                transaction.update(myRef, "connectionsCount", FieldValue.increment(1))
                transaction.update(myRef, "followingIds", FieldValue.arrayUnion(fromUserId))
                transaction.update(myRef, "followingCount", FieldValue.increment(1))

                transaction.update(senderRef, "connectionIds", FieldValue.arrayUnion(currentUid))
                transaction.update(senderRef, "connectionsCount", FieldValue.increment(1))
                transaction.update(senderRef, "followingIds", FieldValue.arrayUnion(currentUid))
                transaction.update(senderRef, "followingCount", FieldValue.increment(1))
            }.await()

            // D. SEND NOTIFICATIONS (Create the database entries)

            // Notify the Other Person
            sendNotification(
                toUserId = fromUserId,
                type = NotificationType.CONNECTION_ACCEPTED,
                title = "Connection Accepted",
                message = "You are now connected with $myName",
                relatedId = currentUid
            )

            // Notify Myself
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

    // 3. Follow a User
    suspend fun followUser(targetUserId: String) {
        val uid = currentUserId ?: return
        val myRef = db.collection("users").document(uid)

        // Add to local 'followingIds'
        myRef.update("followingIds", FieldValue.arrayUnion(targetUserId)).await()
        myRef.update("followingCount", FieldValue.increment(1)).await()
    }

    // 4. REAL-TIME: Get Suggested Users (Robust & Instant Updates)
    fun getSuggestedUsersFlow(): Flow<List<NetworkUser>> {
        val uid = currentUserId ?: return flowOf(emptyList())

        // Flow A: Real-time Listener for ALL Users
        val usersFlow = callbackFlow {
            val listener = db.collection("users")
                .addSnapshotListener { snapshot, _ ->
                    val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                    trySend(users)
                }
            awaitClose { listener.remove() }
        }

        // Flow B: Real-time Listener for MY SENT REQUESTS (Pending only)
        // We listen to this so if a request becomes "rejected", it leaves this list, and we know to update the UI.
        val requestsFlow = callbackFlow {
            val listener = db.collection("connection_requests")
                .whereEqualTo("fromUserId", uid)
                .whereEqualTo("status", "pending")
                .addSnapshotListener { snapshot, _ ->
                    // We only care about the IDs of people we sent requests to
                    val ids = snapshot?.documents?.mapNotNull { it.getString("toUserId") }?.toSet()
                        ?: emptySet()
                    trySend(ids)
                }
            awaitClose { listener.remove() }
        }

        // Flow C: Real-time Listener for MY PROFILE (To know my connections)
        val myProfileFlow = callbackFlow {
            val listener = db.collection("users").document(uid)
                .addSnapshotListener { snapshot, _ ->
                    val user = snapshot?.toObject(User::class.java)
                    val connectionIds = user?.connectionIds?.toSet() ?: emptySet()
                    trySend(connectionIds)
                }
            awaitClose { listener.remove() }
        }

        // COMBINE: Merge all 3 real-time streams
        return combine(usersFlow, requestsFlow, myProfileFlow) { allUsers, pendingSentIds, myConnectionIds ->
            allUsers.mapNotNull { user ->
                // Always hide myself
                if (user.userId == uid) return@mapNotNull null

                val isConnected = myConnectionIds.contains(user.userId)

                // Optional: Hide connected users if list is large
                if (allUsers.size >= 20 && isConnected) {
                    return@mapNotNull null
                }

                // Determine Status strictly from real-time data
                val status = when {
                    isConnected -> "connected"
                    pendingSentIds.contains(user.userId) -> "pending_sent" // If it's in the pending list
                    else -> "not_connected" // If it was rejected, it's removed from pending list -> returns here
                }

                NetworkUser(user, status)
            }
        }
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
        senderId: String = "" // Added senderId
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
    // --- 2. DIRECT MESSAGES ---

    // Get list of active conversations
    fun getChatChannelsFlow(): Flow<List<ChatChannel>> = callbackFlow {
        val uid = currentUserId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }

        // FIXED: Removed .orderBy server-side to prevent "Missing Index" failure.
        // We now fetch all relevant channels and sort them in the app (Client-side sorting).
        val sub = db.collection("chat_channels")
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    // Ideally log this error: Log.e("Firestore", "Chat query failed", error)
                    return@addSnapshotListener
                }

                if (snap != null) {
                    val channels = snap.toObjects(ChatChannel::class.java)
                    // Sort by timestamp descending (newest first) locally
                    val sortedChannels = channels.sortedByDescending { it.lastMessageTimestamp }
                    trySend(sortedChannels)
                }
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

        // 1. Get My Username for the notification
        val myUser = getUserById(uid)
        val myName = myUser?.username ?: "Someone"

        val channelId = if (uid < toUserId) "${uid}_$toUserId" else "${toUserId}_$uid"
        val channelRef = db.collection("chat_channels").document(channelId)

        // 2. Update Channel
        val channelUpdate = mapOf(
            "channelId" to channelId,
            "participantIds" to listOf(uid, toUserId),
            "lastMessage" to content,
            "lastMessageTimestamp" to Timestamp.now(),
            "lastSenderId" to uid
        )
        channelRef.set(channelUpdate, com.google.firebase.firestore.SetOptions.merge()).await()

        // 3. Add Message
        val msgRef = channelRef.collection("messages").document()
        val msg = DirectMessage(id = msgRef.id, senderId = uid, content = content)
        msgRef.set(msg).await()

        // 4. Send Notification to the Receiver
        sendNotification(
            toUserId = toUserId,
            type = NotificationType.NEW_DM,
            title = "New Message",
            message = "$myName sent you a message.",
            relatedId = channelId
        )
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
        try {
            // Just mark as rejected. No need to touch stats.
            db.collection("connection_requests")
                .document(requestId)
                .update("status", "rejected")
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- FOR "INVITATIONS" TAB (Project Invites) ---
    // Simulating accepting a project invite notification
    suspend fun acceptProjectInvite(notificationId: String, projectId: String) {
        val uid = currentUserId ?: return
        db.collection("projects").document(projectId)
            .update("memberIds", FieldValue.arrayUnion(uid))
            .await()

        // Delete notification
        deleteNotification(notificationId)
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

    // --- NEW: SEARCH USERS FOR CHAT ---
    fun searchUsers(query: String): Flow<List<User>> = callbackFlow {
        // 1. If query is empty, return empty list immediately
        if (query.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        // 2. Firestore Prefix Search Query
        // This finds usernames that start with 'query'
        val subscription = db.collection("users")
            .whereGreaterThanOrEqualTo("username", query)
            .whereLessThan("username", query + "\uf8ff")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error) // Close flow on error
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                    // Optional: Filter out myself locally
                    val filtered = users.filter { it.userId != currentUserId }
                    trySend(filtered)
                }
            }

        awaitClose { subscription.remove() }
    }

    // --- NEW: Mark Notifications as Read ---
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

    // --- GOOGLE SIGN IN ---
    suspend fun signInWithGoogle(idToken: String): String? {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return null

            val docRef = db.collection("users").document(user.uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                // Return existing role
                snapshot.getString("role")
            } else {
                // First time login? Create default user document
                // You might want to default to "participant" or handle role selection later
                val newUser = User(
                    userId = user.uid,
                    email = user.email ?: "",
                    role = "participant", // Default role
                    username = user.displayName ?: "User"
                )
                docRef.set(newUser).await()

                // Send welcome notification
                sendNotification(
                    toUserId = user.uid,
                    type = NotificationType.WELCOME_MESSAGE,
                    title = "Welcome to InnoConnect!",
                    message = "Your Google account is now connected."
                )
                "participant"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- GITHUB SIGN IN ---
    // This is called AFTER the UI successfully completes the OAuth flow
    suspend fun onSignInWithGithubSuccess(): String? {
        val user = auth.currentUser ?: return null
        return try {
            val docRef = db.collection("users").document(user.uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                snapshot.getString("role")
            } else {
                // First time login via GitHub -> Create Profile
                val newUser = User(
                    userId = user.uid,
                    email = user.email ?: "",
                    role = "participant", // Default role
                    username = user.displayName ?: "GitHub User"
                )
                docRef.set(newUser).await()

                sendNotification(
                    toUserId = user.uid,
                    type = NotificationType.WELCOME_MESSAGE,
                    title = "Welcome to InnoConnect!",
                    message = "Your GitHub account is now connected."
                )
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

            // Check if user exists in Firestore; if not, create them
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) {
                val newUser = User(
                    userId = uid,
                    email = result.user?.email ?: "",
                    role = "Participant", // Default role
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
            Log.d("FirebaseRepo", "Reset email sent to $email")
            true
        } catch (e: Exception) {
            Log.e("FirebaseRepo", "Error sending reset email", e)
            false
        }
    }


}