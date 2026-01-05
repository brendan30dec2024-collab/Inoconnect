package com.example.inoconnect.ui.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.inoconnect.data.DirectMessage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    channelId: String,
    navController: NavController
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val currentUserId = repository.currentUserId
    val listState = rememberLazyListState()

    // --- State ---
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }

    // Group Info
    var groupName by remember { mutableStateOf("Group Chat") }
    var groupImage by remember { mutableStateOf<String?>(null) }
    var isCreator by remember { mutableStateOf(false) }

    // Dialogs
    var showRenameDialog by remember { mutableStateOf(false) }
    var newNameInput by remember { mutableStateOf("") }

    // Attachment State
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachmentType by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentName by remember { mutableStateOf<String?>(null) }
    var selectedAttachmentSize by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    // --- LOAD DATA ---
    LaunchedEffect(channelId) {
        // 1. Get Messages
        launch {
            repository.getDirectMessagesFlow(channelId).collect {
                messages = it.sortedByDescending { msg -> msg.timestamp }
            }
        }
        // 2. Get Group Info (from channel)
        val channelRef = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("chat_channels").document(channelId)

        channelRef.addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                groupName = snapshot.getString("groupName") ?: "Group Chat"
                groupImage = snapshot.getString("groupImageUrl")
                val projectId = snapshot.getString("projectId")

                // Check if admin (Creator of project)
                if (projectId != null) {
                    scope.launch {
                        val project = repository.getProjectById(projectId)
                        isCreator = project?.creatorId == currentUserId
                    }
                }
            }
        }
    }

    // --- ATTACHMENT HANDLERS (Simplified for brevity, assumes helpers exist in same package) ---
    // Note: Assuming getFileDetails exists from DirectChatScreen or copied here.
    fun updateFileDetails(uri: Uri) {
        val details = getFileDetails(context, uri)
        selectedAttachmentName = details.first
        selectedAttachmentSize = details.second
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedAttachmentUri = uri
            selectedAttachmentType = if(context.contentResolver.getType(uri)?.startsWith("video") == true) "video" else "image"
            updateFileDetails(uri)
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null) {
            selectedAttachmentUri = uri
            selectedAttachmentType = "file"
            updateFileDetails(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(enabled = isCreator) {
                            if (isCreator) {
                                newNameInput = groupName
                                showRenameDialog = true
                            }
                        }
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFFE0E0E0), modifier = Modifier.size(40.dp)) {
                            if (groupImage != null) {
                                AsyncImage(model = groupImage, contentDescription = null, contentScale = ContentScale.Crop)
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Groups, null, tint = Color.Gray)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(groupName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (isCreator) {
                                Text("Tap to rename", fontSize = 10.sp, color = BrandBlue)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color.White).imePadding()) {
                if (selectedAttachmentUri != null) {
                    AttachmentPreview(selectedAttachmentType, selectedAttachmentName, selectedAttachmentSize) {
                        selectedAttachmentUri = null
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier.background(Color(0xFFF0F0F0), CircleShape).size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = BrandBlue)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message group...") },
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF0F0F0),
                            unfocusedContainerColor = Color(0xFFF0F0F0)
                        ),
                        maxLines = 4
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (!isSending && (messageText.isNotBlank() || selectedAttachmentUri != null)) {
                                isSending = true
                                scope.launch {
                                    repository.sendMessage(
                                        channelId = channelId,
                                        content = messageText,
                                        attachmentUri = selectedAttachmentUri,
                                        attachmentType = selectedAttachmentType,
                                        attachmentName = selectedAttachmentName,
                                        attachmentSize = selectedAttachmentSize
                                    )
                                    messageText = ""
                                    selectedAttachmentUri = null
                                    isSending = false
                                }
                            }
                        },
                        modifier = Modifier.background(BrandBlue, CircleShape).size(45.dp)
                    ) {
                        if (isSending) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        else Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF7F7F7)),
            contentPadding = PaddingValues(16.dp),
            reverseLayout = true,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == currentUserId
                GroupMessageBubble(msg, isMe)
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState) {
            Column(Modifier.padding(16.dp)) {
                Text("Share", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    AttachmentOptionItem(Icons.Default.Image, "Gallery") { showBottomSheet = false; galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
                    AttachmentOptionItem(Icons.Default.Folder, "File") { showBottomSheet = false; fileLauncher.launch(arrayOf("*/*")) }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Group") },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Group Name") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repository.updateGroupChatName(channelId, newNameInput)
                        showRenameDialog = false
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupMessageBubble(msg: DirectMessage, isMe: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe && msg.senderName != null) {
            Text(
                text = msg.senderName,
                fontSize = 11.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        Surface(
            color = if (isMe) BrandBlue else Color.White,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp
            ),
            shadowElevation = 1.dp
        ) {
            Column(Modifier.padding(12.dp)) {
                if (msg.attachmentUrl != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if(msg.attachmentType == "image") Icons.Default.Image else Icons.Default.Description,
                            contentDescription = null,
                            tint = if (isMe) Color.White else BrandBlue
                        )
                        if (msg.attachmentName != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(msg.attachmentName, color = if(isMe) Color.White else Color.Black, fontSize = 12.sp)
                        }
                    }
                    if (msg.attachmentType == "image") {
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = msg.attachmentUrl,
                            contentDescription = null,
                            modifier = Modifier.height(150.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                if (msg.content.isNotEmpty()) {
                    Text(
                        text = msg.content,
                        color = if (isMe) Color.White else Color.Black,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}