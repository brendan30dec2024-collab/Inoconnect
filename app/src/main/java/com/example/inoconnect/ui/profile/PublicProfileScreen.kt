package com.example.inoconnect.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List // --- CHANGED IMPORT
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    onBackClick: () -> Unit,
    onMessageClick: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val currentUserId = repository.currentUserId

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // --- Sheets State ---
    var showConnectionsSheet by remember { mutableStateOf(false) }
    var showFollowingSheet by remember { mutableStateOf(false) }
    var sheetUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()

    val connectionStatus by repository.getConnectionStatusFlow(userId).collectAsState(initial = "loading")

    LaunchedEffect(userId) {
        isLoading = true
        user = repository.getUserById(userId)
        isLoading = false
    }

    // --- Helper to Load List ---
    fun loadListAndShow(ids: List<String>, isFollowing: Boolean) {
        scope.launch {
            if (ids.isNotEmpty()) {
                sheetUsers = repository.getUsersByIds(ids)
            } else {
                sheetUsers = emptyList()
            }
            if (isFollowing) showFollowingSheet = true else showConnectionsSheet = true
        }
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
        } else if (user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("User not found", color = Color.Gray)
            }
        } else {
            val u = user!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .background(Color(0xFFF8F9FA))
                    .verticalScroll(rememberScrollState())
            ) {
                // ================= HEADER =================
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(BrandBlue))

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(top = 40.dp, start = 16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = u.profileImageUrl.ifEmpty { null },
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(u.username, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            u.headline.ifEmpty { "Student" },
                            fontSize = 14.sp, color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- ACTION BUTTONS ROW ---
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            // 1. Connect Button
                            if (currentUserId != null && currentUserId != userId) {
                                Button(
                                    onClick = {
                                        if (connectionStatus == "none") {
                                            scope.launch {
                                                repository.sendConnectionRequest(userId)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (connectionStatus) {
                                            "connected" -> Color.Gray
                                            "pending" -> Color(0xFFFF9800)
                                            else -> BrandBlue
                                        }
                                    ),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.height(36.dp),
                                    enabled = connectionStatus != "loading"
                                ) {
                                    val icon = when (connectionStatus) {
                                        "connected" -> Icons.Default.Check
                                        "pending" -> Icons.Default.Refresh
                                        else -> Icons.Default.Add
                                    }
                                    val text = when (connectionStatus) {
                                        "connected" -> "Connected"
                                        "pending" -> "Pending"
                                        else -> "Connect"
                                    }

                                    Icon(icon, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(text)
                                }
                            }

                            // 2. Message Button
                            if (currentUserId != null && currentUserId != userId) {
                                Button(
                                    onClick = {
                                        val channelId = if (currentUserId < userId)
                                            "${currentUserId}_$userId"
                                        else
                                            "${userId}_$currentUserId"
                                        onMessageClick(channelId)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = BrandBlue
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Email, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Message")
                                }
                            }
                        }
                    }
                }

                // ================= BODY =================
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(16.dp))

                    ProfileStatsGrid(
                        connections = u.connectionsCount,
                        following = u.followingCount,
                        projects = u.projectsCompleted,
                        onConnectionsClick = { loadListAndShow(u.connectionIds, false) },
                        onFollowingClick = { loadListAndShow(u.followingIds, true) },
                        onProjectsClick = {} // --- FIX: Added empty lambda for now
                    )

                    Spacer(Modifier.height(16.dp))

                    ProfileSectionCard(title = "Academic Biodata") {
                        InfoRow(Icons.Default.Info, "University", u.university)
                        InfoRow(Icons.Default.Home, "Faculty", u.faculty)
                        // --- FIX: Use AutoMirrored Icon ---
                        InfoRow(Icons.AutoMirrored.Filled.List, "Course", u.course)
                        InfoRow(Icons.Default.DateRange, "Year", u.yearOfStudy)
                        HorizontalDivider(
                            Modifier.padding(vertical = 12.dp),
                            color = LightGrayInput
                        )
                        Text("About", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(u.bio.ifEmpty { "No bio." }, color = Color.DarkGray)
                    }

                    if (u.skills.isNotEmpty()) {
                        ProfileSectionCard(title = "Skills") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                u.skills.forEach { skill ->
                                    SuggestionChip(onClick = {}, label = { Text(skill) })
                                }
                            }
                        }
                    }

                    ProfileSectionCard(title = "Contact Information") {
                        if (connectionStatus == "connected") {
                            ContactRow(Icons.Default.Email, u.email)
                            if (u.phoneNumber.isNotEmpty()) ContactRow(
                                Icons.Default.Phone,
                                u.phoneNumber
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.Gray)
                                Text(
                                    "Connect to view contacts",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }

    // --- BOTTOM SHEET FOR LISTS ---
    if (showConnectionsSheet || showFollowingSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showConnectionsSheet = false
                showFollowingSheet = false
            },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (showFollowingSheet) "Following" else "Connections",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))

                if (sheetUsers.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No users found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sheetUsers) { user ->
                            CompactUserRow(user) {
                                showConnectionsSheet = false
                                showFollowingSheet = false
                                onNavigateToProfile(user.userId)
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactUserRow(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            modifier = Modifier.size(50.dp),
            color = Color(0xFFE0E0E0)
        ) {
            if (user.profileImageUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(10.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (user.headline.isNotEmpty()) {
                Text(
                    user.headline,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}