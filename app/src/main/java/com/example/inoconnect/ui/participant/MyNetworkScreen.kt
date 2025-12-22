package com.example.inoconnect.ui.participant

import androidx.compose.foundation.background
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
    viewModel: MyNetworkViewModel = viewModel() // Auto-injects the ViewModel
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
            invitesCount = 0, // We can add this to stats later if needed
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
                            UserCard(initialUsers[0], Modifier.weight(1f), viewModel)
                        } else { Spacer(Modifier.weight(1f)) }

                        if (initialUsers.size > 1) {
                            UserCard(initialUsers[1], Modifier.weight(1f), viewModel)
                        } else { Spacer(Modifier.weight(1f)) }
                    }
                    // Row 2
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (initialUsers.size > 2) {
                            UserCard(initialUsers[2], Modifier.weight(1f), viewModel)
                        } else { Spacer(Modifier.weight(1f)) }

                        if (initialUsers.size > 3) {
                            UserCard(initialUsers[3], Modifier.weight(1f), viewModel)
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
                                UserCard(user, Modifier.weight(1f), viewModel)
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

// Components
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
    viewModel: MyNetworkViewModel
) {
    val user = networkUser.user
    val isPending = networkUser.connectionStatus == "pending_sent"

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
                Spacer(modifier = Modifier.height(12.dp))

                // Profile Image
                Surface(shape = CircleShape, modifier = Modifier.size(70.dp), color = Color(0xFFE0E0E0)) {
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

                Spacer(modifier = Modifier.height(10.dp))

                // Name
                Text(
                    text = user.username.ifEmpty { "Inno User" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Headline / Role
                Text(
                    text = user.headline.ifEmpty { "Student" },
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    modifier = Modifier.height(28.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Connect Button
                Button(
                    onClick = { viewModel.connectWithUser(user.userId) },
                    enabled = !isPending,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPending) Color.Gray else BrandBlue
                    )
                ) {
                    Text(
                        text = if (isPending) "Pending" else "Connect",
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}