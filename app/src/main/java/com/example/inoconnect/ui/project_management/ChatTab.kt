package com.example.inoconnect.ui.project_management

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.data.ChatMessage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatTab(projectId: String, currentUserName: String) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Real-time messages state
    val messages by repository.getProjectMessages(projectId).collectAsState(initial = emptyList())

    var inputText by remember { mutableStateOf("") }
    val currentUserId = repository.currentUserId

    Column(modifier = Modifier.fillMaxSize()) {

        // --- MESSAGES LIST ---
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            state = listState,
            reverseLayout = true // Chat starts from bottom
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == currentUserId
                ChatBubble(message = msg, isMe = isMe)
            }
        }

        // --- INPUT AREA ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .background(Color.White),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LightGrayInput,
                    unfocusedContainerColor = LightGrayInput,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        scope.launch {
                            repository.sendMessage(projectId, inputText, currentUserName)
                            inputText = ""
                            // Scroll to bottom (which is index 0 because of reverseLayout)
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BrandBlue)
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isMe: Boolean) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = message.timestamp.toDate().let { timeFormat.format(it) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(message.senderName, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 8.dp))
        }

        Surface(
            shape = if (isMe) RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
            else RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp),
            color = if (isMe) BrandBlue else LightGrayInput,
            modifier = Modifier.widthIn(max = 250.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.message,
                    color = if (isMe) Color.White else Color.Black,
                    fontSize = 14.sp
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