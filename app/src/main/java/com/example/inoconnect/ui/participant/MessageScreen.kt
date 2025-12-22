package com.example.inoconnect.ui.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.inoconnect.data.*
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// Enum to track which popup is open
enum class MessageTab { NONE, INVITATIONS, NOTIFICATIONS, FOLLOWERS }

@Composable
fun MessagesScreen(navController: NavController) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    // Data States
    var chatChannels by remember { mutableStateOf<List<ChatChannel>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var connectionRequests by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }

    // UI State for Dialog
    var activeTab by remember { mutableStateOf(MessageTab.NONE) }

    // Load Data
    LaunchedEffect(Unit) {
        launch { repository.getChatChannelsFlow().collect { chatChannels = it } }
        launch { repository.getNotificationsFlow().collect { notifications = it } }
        launch { repository.getIncomingConnectionRequestsFlow().collect { connectionRequests = it } }
    }

    // Filtered Lists for Counts & Display
    val projectInvites = notifications.filter { it.type == NotificationType.PROJECT_INVITE }
    val generalNotifs = notifications.filter { it.type == NotificationType.SYSTEM_ALERT || it.type == NotificationType.PROJECT_DECLINE || it.type == NotificationType.NEW_DM }

    // Counts
    val inviteCount = projectInvites.size
    val notifCount = generalNotifs.size
    val followCount = connectionRequests.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // --- TOP SECTION (3 CARDS) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Invitations Card
                HeaderCard(
                    title = "Invitations",
                    icon = Icons.Default.Email,
                    count = inviteCount,
                    color = Color(0xFFE3F2FD),
                    iconColor = BrandBlue,
                    onClick = { activeTab = MessageTab.INVITATIONS }
                )
                // 2. Notifications Card
                HeaderCard(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    count = notifCount,
                    color = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFFF9800),
                    onClick = { activeTab = MessageTab.NOTIFICATIONS }
                )
                // 3. Followers Card
                HeaderCard(
                    title = "Followers",
                    icon = Icons.Default.Person,
                    count = followCount,
                    color = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF4CAF50),
                    onClick = { activeTab = MessageTab.FOLLOWERS }
                )
            }

            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            // --- CHAT LIST ---
            Text(
                text = "Messages",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(16.dp),
                color = Color.Black
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(chatChannels) { channel ->
                    ChatChannelItem(channel = channel, repository = repository) {
                        navController.navigate("direct_chat/${channel.channelId}")
                    }
                }
            }
        }

        // --- POPUP DIALOG LOGIC ---
        if (activeTab != MessageTab.NONE) {
            CategoryDialog(
                tab = activeTab,
                onDismiss = { activeTab = MessageTab.NONE },
                projectInvites = projectInvites,
                notifications = generalNotifs,
                connectionRequests = connectionRequests,
                repository = repository
            )
        }
    }
}

// --- SUB-COMPONENTS ---

@Composable
fun HeaderCard(
    title: String,
    icon: ImageVector,
    count: Int,
    color: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(110.dp)
            .height(100.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(32.dp))
                if (count > 0) {
                    Badge(
                        containerColor = Color.Red,
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
                    ) { Text("$count", color = Color.White) }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}

@Composable
fun CategoryDialog(
    tab: MessageTab,
    onDismiss: () -> Unit,
    projectInvites: List<AppNotification>,
    notifications: List<AppNotification>,
    connectionRequests: List<ConnectionRequest>,
    repository: FirebaseRepository
) {
    val scope = rememberCoroutineScope()
    val title = when(tab) {
        MessageTab.INVITATIONS -> "Project Invitations"
        MessageTab.NOTIFICATIONS -> "Notifications"
        MessageTab.FOLLOWERS -> "Connection Requests"
        else -> ""
    }

    // DialogProperties usePlatformDefaultWidth = false allows us to control the width freely
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f) // 95% Width
                .fillMaxHeight(0.75f) // 75% Height (Requested Change)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                // Content List
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 1. INVITATIONS LIST
                    if (tab == MessageTab.INVITATIONS) {
                        if (projectInvites.isEmpty()) item { EmptyState("No pending invitations") }
                        items(projectInvites) { invite ->
                            NotificationItem(
                                title = invite.title,
                                body = invite.message,
                                time = invite.timestamp,
                                icon = Icons.Default.Email,
                                showActions = true,
                                onConfirm = {
                                    scope.launch {
                                        repository.acceptProjectInvite(invite.id, invite.relatedId)
                                        onDismiss()
                                    }
                                },
                                onCancel = { /* TODO: Decline logic */ }
                            )
                        }
                    }

                    // 2. NOTIFICATIONS LIST
                    if (tab == MessageTab.NOTIFICATIONS) {
                        if (notifications.isEmpty()) item { EmptyState("No new notifications") }
                        items(notifications) { notif ->
                            NotificationItem(
                                title = notif.title,
                                body = notif.message,
                                time = notif.timestamp,
                                icon = Icons.Default.Notifications,
                                showActions = false
                            )
                        }
                    }

                    // 3. FOLLOWERS (CONNECTION REQUESTS) LIST
                    if (tab == MessageTab.FOLLOWERS) {
                        if (connectionRequests.isEmpty()) item { EmptyState("No pending requests") }
                        items(connectionRequests) { req ->
                            var senderName by remember { mutableStateOf("User") }
                            LaunchedEffect(req.fromUserId) {
                                val user = repository.getUserById(req.fromUserId)
                                if (user != null) senderName = user.username
                            }

                            NotificationItem(
                                title = "Connection Request",
                                body = "$senderName wants to connect with you.",
                                time = req.timestamp,
                                icon = Icons.Default.Person,
                                showActions = true,
                                onConfirm = {
                                    scope.launch { repository.acceptConnectionRequest(req.id, req.fromUserId) }
                                },
                                onCancel = {
                                    scope.launch { repository.rejectConnectionRequest(req.id) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    title: String,
    body: String,
    time: com.google.firebase.Timestamp,
    icon: ImageVector,
    showActions: Boolean = false,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFF0F0F0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color.Gray)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    SimpleDateFormat("dd/MM", Locale.getDefault()).format(time.toDate()),
                    fontSize = 10.sp, color = Color.Gray
                )
            }
            Text(body, fontSize = 12.sp, color = Color.DarkGray)

            if (showActions) {
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Accept", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Decline", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(msg: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun ChatChannelItem(channel: ChatChannel, repository: FirebaseRepository, onClick: () -> Unit) {
    var otherUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(channel) {
        otherUser = repository.getOtherUserInChannel(channel)
    }

    if (otherUser == null) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, modifier = Modifier.size(50.dp), color = Color.LightGray) {
            if (otherUser!!.profileImageUrl.isNotEmpty()) {
                AsyncImage(model = otherUser!!.profileImageUrl, contentDescription = null, contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp), tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(otherUser!!.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(channel.lastMessageTimestamp.toDate()), fontSize = 12.sp, color = Color.Gray)
            }
            Text(text = channel.lastMessage, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.Gray, fontSize = 14.sp)
        }
    }
}