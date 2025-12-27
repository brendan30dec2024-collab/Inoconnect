package com.example.inoconnect.ui.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // --- IMPORT ADDED
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.inoconnect.ui.auth.BrandBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyNetworkScreen(
    viewModel: MyNetworkViewModel = viewModel(),
    onUserClick: (String) -> Unit // --- ADDED: Callback for navigation
) {
    // Collect Real-Time Data
    val suggestedUsers by viewModel.suggestedUsers.collectAsState()
    val stats by viewModel.networkStats.collectAsState()

    // Sheet State
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- MAIN SCREEN CONTENT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6))
    ) {
        // Overview Section (Real Data)
        NetworkOverviewSection(
            invitesCount = 0,
            connectionsCount = stats["connections"] ?: 0,
            followingCount = stats["following"] ?: 0
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
                    onClick = { showBottomSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Text("Show all")
                }
            }
        }
    }

    // --- BOTTOM SHEET (SLIDE UP) ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
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
}

// Components (NetworkOverviewSection remains unchanged)
@Composable
fun NetworkOverviewSection(invitesCount: Int, connectionsCount: Int, followingCount: Int) {
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
            NetworkStatItem("Connections", connectionsCount)
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFE0E0E0)))
            NetworkStatItem("Following", followingCount)
        }
    }
}

@Composable
fun NetworkStatItem(label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrandBlue)
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun UserCard(
    networkUser: NetworkUser,
    modifier: Modifier = Modifier,
    viewModel: MyNetworkViewModel,
    onUserClick: (String) -> Unit
) {
    val user = networkUser.user

    // Check both Pending and Connected statuses
    val isPending = networkUser.connectionStatus == "pending_sent"
    val isConnected = networkUser.connectionStatus == "connected"

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(260.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Remove Button
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

                // Profile Image
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

                // 1. Name (Bigger)
                Text(
                    text = user.username.ifEmpty { "Inno User" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp, // Increased from 14.sp
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // 2. University (Bigger)
                if (user.university.isNotEmpty()) {
                    Text(
                        text = user.university,
                        fontSize = 13.sp, // Increased from 11.sp
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }

                // 3. Faculty (New Line & Bigger)
                if (user.faculty.isNotEmpty()) {
                    Text(
                        text = user.faculty,
                        fontSize = 12.sp, // Increased from 10.sp
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }

                // 4. Course (New Line & Bigger)
                if (user.course.isNotEmpty()) {
                    Text(
                        text = user.course,
                        fontSize = 12.sp, // Increased from 10.sp
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Top 3 Skills
                if (user.skills.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        user.skills.take(3).forEach { skill ->
                            Surface(
                                color = BrandBlue,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = skill,
                                    color = Color.White,
                                    fontSize = 10.sp, // Slightly bigger
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Connect Button
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
                        fontSize = 13.sp, // Increased from 12.sp
                        color = Color.White
                    )
                }
            }
        }
    }
}