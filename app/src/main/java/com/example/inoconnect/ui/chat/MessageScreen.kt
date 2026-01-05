package com.example.inoconnect.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// Enum to track which popup is open
enum class MessageTab { NONE, INVITATIONS, NOTIFICATIONS, FOLLOWERS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(navController: NavController) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    // Data States
    var chatChannels by remember { mutableStateOf<List<ChatChannel>>(emptyList()) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var connectionRequests by remember { mutableStateOf<List<ConnectionRequest>>(emptyList()) }

    // Search States
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<User>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // UI State for Dialog
    var activeTab by remember { mutableStateOf(MessageTab.NONE) }

    // Load Data (Chats & Notifications)
    LaunchedEffect(Unit) {
        launch { repository.getChatChannelsFlow().collect { chatChannels = it } }
        launch { repository.getNotificationsFlow().collect { notifications = it } }
        launch { repository.getIncomingConnectionRequestsFlow().collect { connectionRequests = it } }
    }

    // Load Search Results
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            isSearching = true
            repository.searchUsers(searchQuery).collect {
                searchResults = it
            }
        } else {
            isSearching = false
            searchResults = emptyList()
        }
    }

    // Filter Logic
    val projectInvites = notifications.filter {
        it.type == NotificationType.PROJECT_INVITE ||
                it.type == NotificationType.PROJECT_JOIN_REQUEST
    }

    val generalNotifs = notifications.filter {
        it.type == NotificationType.SYSTEM_ALERT ||
                it.type == NotificationType.PROJECT_DECLINE ||
                it.type == NotificationType.NEW_DM ||
                it.type == NotificationType.CONNECTION_ACCEPTED ||
                it.type == NotificationType.PROJECT_ACCEPTED ||
                it.type == NotificationType.PROJECT_REMOVAL ||
                it.type == NotificationType.NEW_EVENT ||
                it.type == NotificationType.WELCOME_MESSAGE
    }

    val inviteCount = projectInvites.size
    val notifCount = generalNotifs.count { !it.isRead }
    val followCount = connectionRequests.size

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // --- 1. SEARCH BAR ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Find users to chat...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color.LightGray,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                singleLine = true
            )

            // --- 2. MAIN CONTENT (If NOT Searching) ---
            if (!isSearching) {
                // Top Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    HeaderCard(
                        title = "Invitations",
                        icon = Icons.Default.Email,
                        count = inviteCount,
                        color = Color(0xFFE3F2FD),
                        iconColor = BrandBlue,
                        onClick = { activeTab = MessageTab.INVITATIONS }
                    )
                    HeaderCard(
                        title = "Notifications",
                        icon = Icons.Default.Notifications,
                        count = notifCount,
                        color = Color(0xFFFFF3E0),
                        iconColor = Color(0xFFFF9800),
                        onClick = { activeTab = MessageTab.NOTIFICATIONS }
                    )
                    HeaderCard(
                        title = "Followers",
                        icon = Icons.Default.Person,
                        count = followCount,
                        color = Color(0xFFE8F5E9),
                        iconColor = Color(0xFF4CAF50),
                        onClick = { activeTab = MessageTab.FOLLOWERS }
                    )
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(top = 8.dp))

                // Chat List
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
                    if (chatChannels.isEmpty()) {
                        item {
                            EmptyState("No recent chats")
                        }
                    } else {
                        items(chatChannels) { channel ->
                            ChatChannelItem(
                                channel = channel,
                                repository = repository,
                                onClick = {
                                    // Use channelId for both types. DirectChatScreen may need to be aliased
                                    // or replaced by GroupChatScreen if unified, but assuming
                                    // 'direct_chat' handles DMs and 'group_chat' handles groups:
                                    if (channel.type == ChannelType.PROJECT_GROUP) {
                                        navController.navigate("group_chat/${channel.channelId}")
                                    } else {
                                        navController.navigate("direct_chat/${channel.channelId}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            // --- 3. SEARCH RESULTS (If Searching) ---
            else {
                Text(
                    text = "Search Results",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = BrandBlue
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        item { EmptyState("No users found") }
                    } else {
                        items(searchResults) { user ->
                            UserSearchItem(user = user) {
                                val currentUid = repository.currentUserId ?: return@UserSearchItem
                                val channelId = if (currentUid < user.userId)
                                    "${currentUid}_${user.userId}"
                                else
                                    "${user.userId}_${currentUid}"

                                navController.navigate("direct_chat/$channelId")
                            }
                        }
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
    if (tab == MessageTab.NOTIFICATIONS) {
        val unreadIds = remember(notifications) {
            notifications.filter { !it.isRead }.map { it.id }
        }
        LaunchedEffect(unreadIds) {
            if (unreadIds.isNotEmpty()) {
                repository.markNotificationsAsRead(unreadIds)
            }
        }
    }

    val title = when(tab) {
        MessageTab.INVITATIONS -> "Project Invitations"
        MessageTab.NOTIFICATIONS -> "Notifications"
        MessageTab.FOLLOWERS -> "Connection Requests"
        else -> ""
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.75f)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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

                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tab == MessageTab.INVITATIONS) {
                        if (projectInvites.isEmpty()) item { EmptyState("No pending invitations") }
                        items(projectInvites) { invite ->
                            val isJoinRequest = invite.type == NotificationType.PROJECT_JOIN_REQUEST
                            NotificationItem(
                                title = invite.title,
                                body = invite.message,
                                time = invite.timestamp,
                                icon = if (isJoinRequest) Icons.Default.Person else Icons.Default.Email,
                                showActions = true,
                                onConfirm = {
                                    scope.launch {
                                        if (isJoinRequest) {
                                            repository.acceptJoinRequest(projectId = invite.relatedId, applicantId = invite.senderId)
                                            repository.deleteNotification(invite.id)
                                        } else {
                                            repository.acceptProjectInvite(invite.id, invite.relatedId)
                                        }
                                        onDismiss()
                                    }
                                },
                                onCancel = {
                                    scope.launch {
                                        if (isJoinRequest) {
                                            repository.rejectJoinRequest(projectId = invite.relatedId, applicantId = invite.senderId)
                                            repository.deleteNotification(invite.id)
                                        }
                                        onDismiss()
                                    }
                                }
                            )
                        }
                    }

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
    time: Timestamp,
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
fun UserSearchItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, modifier = Modifier.size(50.dp), color = Color.LightGray) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(model = user.profileImageUrl, contentDescription = null, contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp), tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(user.headline.ifEmpty { "Student" }, fontSize = 12.sp, color = Color.Gray)
        }
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Default.Email, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
    }
}

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
fun ChatChannelItem(
    channel: ChatChannel,
    repository: FirebaseRepository,
    onClick: () -> Unit
) {
    var displayTitle by remember { mutableStateOf("") }
    var displayImage by remember { mutableStateOf<String?>(null) }

    // Determine what to show based on channel type
    if (channel.type == ChannelType.PROJECT_GROUP) {
        // --- GROUP / PROJECT CHAT ---
        displayTitle = channel.groupName ?: "Group Chat"
        displayImage = channel.groupImageUrl
    } else {
        // --- DIRECT MESSAGE ---
        var otherUser by remember { mutableStateOf<User?>(null) }
        LaunchedEffect(channel) {
            otherUser = repository.getOtherUserInChannel(channel)
            displayTitle = otherUser?.username ?: "User"
            displayImage = otherUser?.profileImageUrl
        }
    }

    // Only render if we have a title (prevents flicker of empty items)
    if (displayTitle.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, modifier = Modifier.size(50.dp), color = Color.LightGray) {
                if (!displayImage.isNullOrEmpty()) {
                    AsyncImage(model = displayImage, contentDescription = null, contentScale = ContentScale.Crop)
                } else {
                    // Different icon for Group vs Person
                    val icon = if (channel.type == ChannelType.PROJECT_GROUP) Icons.Default.Groups else Icons.Default.Person
                    Icon(icon, null, modifier = Modifier.padding(10.dp), tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(displayTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(channel.lastMessageTimestamp.toDate()),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Text(
                    text = channel.lastMessage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}