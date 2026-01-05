package com.example.inoconnect.ui.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.inoconnect.data.NetworkUser
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.network.MyNetworkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNetworkScreen(
    viewModel: MyNetworkViewModel = viewModel(),
    onUserClick: (String) -> Unit
) {
    // Collect Real-Time Data
    val suggestedUsers by viewModel.suggestedUsers.collectAsState()
    val stats by viewModel.networkStats.collectAsState()

    // --- NEW: Collect Lists ---
    val connectionsList by viewModel.connectionList.collectAsState()
    val followingList by viewModel.followingList.collectAsState()

    // Sheet State
    var showSuggestionsSheet by remember { mutableStateOf(false) }
    var showConnectionsSheet by remember { mutableStateOf(false) } // NEW
    var showFollowingSheet by remember { mutableStateOf(false) }   // NEW

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- MAIN SCREEN CONTENT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
            .verticalScroll(rememberScrollState())
    ) {
        // Overview Section (Clickable)
        NetworkOverviewSection(
            invitesCount = 0,
            connectionsCount = stats["connections"] ?: 0,
            followingCount = stats["following"] ?: 0,
            onConnectionsClick = {
                viewModel.loadConnections()
                showConnectionsSheet = true
            },
            onFollowingClick = {
                viewModel.loadFollowing()
                showFollowingSheet = true
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Suggestions Container (Top 4)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "Suggested for you",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (suggestedUsers.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No suggestions available right now.", color = Color.Gray)
                }
            } else {
                // 2x2 Grid Preview
                val initialUsers = suggestedUsers.take(4)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Row 1
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (initialUsers.isNotEmpty()) {
                            UserCard(initialUsers[0], Modifier.weight(1f), viewModel, onUserClick)
                        } else { Spacer(Modifier.weight(1f)) }

                        if (initialUsers.size > 1) {
                            UserCard(initialUsers[1], Modifier.weight(1f), viewModel, onUserClick)
                        } else { Spacer(Modifier.weight(1f)) }
                    }
                    // Row 2
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (initialUsers.size > 2) {
                            UserCard(initialUsers[2], Modifier.weight(1f), viewModel, onUserClick)
                        } else { Spacer(Modifier.weight(1f)) }

                        if (initialUsers.size > 3) {
                            UserCard(initialUsers[3], Modifier.weight(1f), viewModel, onUserClick)
                        } else { Spacer(Modifier.weight(1f)) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SHOW ALL BUTTON
                OutlinedButton(
                    onClick = { showSuggestionsSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Show all")
                }
            }
        }
    }

    // --- SUGGESTIONS BOTTOM SHEET ---
    if (showSuggestionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSuggestionsSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "People you may know",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally),
                    color = Color.Black
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 30.dp)
                ) {
                    items(suggestedUsers.chunked(2)) { rowUsers ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowUsers.forEach { user ->
                                UserCard(user, Modifier.weight(1f), viewModel, onUserClick)
                            }
                            if (rowUsers.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // --- NEW: CONNECTIONS BOTTOM SHEET ---
    if (showConnectionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConnectionsSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp)) {
                Text("Your Connections", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                if (connectionsList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No connections yet.") }
                } else {
                    LazyColumn {
                        items(connectionsList) { user ->
                            CompactUserRow(user) {
                                showConnectionsSheet = false
                                onUserClick(user.userId) // Navigate to Profile
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    // --- NEW: FOLLOWING BOTTOM SHEET ---
    if (showFollowingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFollowingSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(16.dp)) {
                Text("Following", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                if (followingList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Not following anyone.") }
                } else {
                    LazyColumn {
                        items(followingList) { user ->
                            CompactUserRow(user) {
                                showFollowingSheet = false
                                onUserClick(user.userId) // Navigate to Profile
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

// --- UPDATED: NetworkOverviewSection with Click Listeners ---
@Composable
fun NetworkOverviewSection(
    invitesCount: Int,
    connectionsCount: Int,
    followingCount: Int,
    onConnectionsClick: () -> Unit = {},
    onFollowingClick: () -> Unit = {}
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RectangleShape,
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NetworkStatItem("Invites Sent", invitesCount)
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE0E0E0)))
            NetworkStatItem("Connections", connectionsCount, onClick = onConnectionsClick)
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE0E0E0)))
            NetworkStatItem("Following", followingCount, onClick = onFollowingClick)
        }
    }
}

@Composable
fun NetworkStatItem(label: String, count: Int, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(8.dp) // Added Clickable
    ) {
        Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrandBlue)
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}

// --- NEW: Compact User Row for Lists ---
@Composable
fun CompactUserRow(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(user.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            if (user.headline.isNotEmpty()) {
                Text(user.headline, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// UserCard remains the same...
@Composable
fun UserCard(
    networkUser: NetworkUser,
    modifier: Modifier = Modifier,
    viewModel: MyNetworkViewModel,
    onUserClick: (String) -> Unit
) {
    val user = networkUser.user
    val isPending = networkUser.connectionStatus == "pending_sent"
    val isConnected = networkUser.connectionStatus == "connected"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(260.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { viewModel.removeSuggestion(user.userId) },
                modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp)
            ) {
                Icon(Icons.Default.Close, "Remove", tint = Color.Gray, modifier = Modifier.size(16.dp))
            }

            Column(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = CircleShape,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .clickable { onUserClick(user.userId) },
                    color = Color(0xFFE0E0E0)
                ) {
                    if (user.profileImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = user.profileImageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(12.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = user.username.ifEmpty { "Inno User" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (user.university.isNotEmpty()) {
                    Text(
                        text = user.university,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.connectWithUser(user.userId) },
                    enabled = !isPending && !isConnected,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPending || isConnected) Color.Gray else BrandBlue
                    )
                ) {
                    Text(
                        text = when {
                            isConnected -> "Connected"
                            isPending -> "Pending"
                            else -> "Connect"
                        },
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}