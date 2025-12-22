package com.example.inoconnect.ui.participant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.inoconnect.data.DirectMessage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(channelId: String, navController: NavController) {
    val repository = remember { FirebaseRepository() }
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val currentUserId = repository.currentUserId

    LaunchedEffect(channelId) {
        repository.getDirectMessagesFlow(channelId).collect { messages = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
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
                            // Extract the "other" user ID from channelId (hacky but works for now)
                            // A better way is passing the user ID via nav arguments
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
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) BrandBlue else Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = msg.content,
                            modifier = Modifier.padding(12.dp),
                            color = if (isMe) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}