package com.example.inoconnect.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.inoconnect.data.DirectMessage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    channelId: String,
    navController: NavController,
    onProfileClick: (String) -> Unit // --- NEW CALLBACK ---
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val currentUserId = repository.currentUserId

    // State
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var otherUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(channelId) {
        // 1. Listen for Messages
        launch {
            repository.getDirectMessagesFlow(channelId).collect { messages = it }
        }

        // 2. Fetch the "Other" User's Info for the Top Bar
        val parts = channelId.split("_")
        val otherId = parts.find { it != currentUserId }
        if (otherId != null) {
            otherUser = repository.getUserById(otherId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (otherUser != null) {
                        // --- CHANGED: Made Row clickable ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onProfileClick(otherUser!!.userId) }
                                .padding(8.dp) // Add padding for touch area
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.LightGray,
                                modifier = Modifier.size(40.dp)
                            ) {
                                if (otherUser!!.profileImageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = otherUser!!.profileImageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = otherUser!!.username,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text("Chat", color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandBlue)
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .background(Color.White),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        scope.launch {
                            val parts = channelId.split("_")
                            val otherId = parts.find { it != currentUserId } ?: return@launch

                            repository.sendDirectMessage(otherId, messageText)
                            messageText = ""
                        }
                    }
                }) {
                    Icon(Icons.Default.Send, null, tint = BrandBlue)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == currentUserId

                val timeString = remember(msg.timestamp) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(msg.timestamp.toDate())
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) BrandBlue else Color.White
                        ),
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 16.dp
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.widthIn(max = 280.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = if (isMe) Color.White else Color.Black,
                                fontSize = 16.sp
                            )

                            Text(
                                text = timeString,
                                color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
}