package com.example.inoconnect.ui.chat

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.inoconnect.data.DirectMessage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatScreen(
    channelId: String,
    navController: NavController,
    onProfileClick: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val currentUserId = repository.currentUserId
    val listState = rememberLazyListState()

    // State
    var messages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var otherUser by remember { mutableStateOf<User?>(null) }

    // Attachment State
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    var selectedAttachmentUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachmentType by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var isSending by remember { mutableStateOf(false) }

    // --- Permission Launchers ---
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted. Tap again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Content Launchers ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAttachmentUri = uri
            val type = context.contentResolver.getType(uri)
            selectedAttachmentType = if (type?.startsWith("video") == true) "video" else "image"
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAttachmentUri = uri
            selectedAttachmentType = "file"
        }
    }

    val cameraPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedAttachmentUri = tempCameraUri
            selectedAttachmentType = "image"
        }
    }

    val cameraVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && tempCameraUri != null) {
            selectedAttachmentUri = tempCameraUri
            selectedAttachmentType = "video"
        }
    }

    fun createTempUri(isVideo: Boolean): Uri? {
        val type = if (isVideo) "mp4" else "jpg"
        val prefix = if (isVideo) "VID_" else "IMG_"
        val file = File.createTempFile(prefix, ".$type", context.cacheDir)
        return try {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun checkPermissionAndLaunchCamera(isVideo: Boolean) {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            val uri = createTempUri(isVideo)
            if (uri != null) {
                tempCameraUri = uri
                if (isVideo) {
                    cameraVideoLauncher.launch(uri)
                } else {
                    cameraPhotoLauncher.launch(uri)
                }
            }
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    LaunchedEffect(channelId) {
        launch {
            repository.getDirectMessagesFlow(channelId).collect {
                messages = it.sortedByDescending { msg -> msg.timestamp }
            }
        }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (otherUser != null) onProfileClick(otherUser!!.userId)
                            }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE0E0E0),
                            modifier = Modifier.size(42.dp)
                        ) {
                            if (otherUser?.profileImageUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = otherUser!!.profileImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = otherUser?.username ?: "Chat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (otherUser != null) {
                                Text(
                                    text = "Tap to view profile",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
            ) {
                if (selectedAttachmentUri != null) {
                    AttachmentPreview(
                        type = selectedAttachmentType,
                        onRemove = {
                            selectedAttachmentUri = null
                            selectedAttachmentType = null
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    IconButton(
                        onClick = { showBottomSheet = true },
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(40.dp)
                            .background(Color(0xFFF0F0F0), CircleShape)
                    ) {
                        Icon(Icons.Default.Add, "Attach", tint = BrandBlue)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message...", fontSize = 15.sp, color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFF0F0F0)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF0F0F0),
                            unfocusedContainerColor = Color(0xFFF0F0F0),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 5
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = messageText.isNotBlank() || selectedAttachmentUri != null
                    IconButton(
                        onClick = {
                            if (!isSending) {
                                isSending = true
                                scope.launch {
                                    val parts = channelId.split("_")
                                    val otherId = parts.find { it != currentUserId }
                                    if (otherId != null) {
                                        repository.sendDirectMessage(
                                            toUserId = otherId,
                                            content = messageText,
                                            attachmentUri = selectedAttachmentUri,
                                            attachmentType = selectedAttachmentType
                                        )
                                        messageText = ""
                                        selectedAttachmentUri = null
                                        selectedAttachmentType = null
                                    }
                                    isSending = false
                                }
                            }
                        },
                        enabled = canSend && !isSending,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .size(45.dp)
                            .background(
                                if (canSend) BrandBlue else Color.LightGray,
                                CircleShape
                            )
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFFF7F7F7)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            reverseLayout = true
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == currentUserId
                MessageBubble(msg = msg, isMe = isMe, context = context)
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text(
                    "Share Content",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.Image,
                        label = "Gallery",
                        onClick = {
                            showBottomSheet = false
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera",
                        onClick = {
                            showBottomSheet = false
                            checkPermissionAndLaunchCamera(false)
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Videocam,
                        label = "Video",
                        onClick = {
                            showBottomSheet = false
                            checkPermissionAndLaunchCamera(true)
                        }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Folder,
                        label = "File",
                        onClick = {
                            showBottomSheet = false
                            fileLauncher.launch(arrayOf("*/*"))
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// --- MISSING COMPONENTS ADDED BELOW ---

@Composable
fun AttachmentOptionItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color(0xFFF5F5F5), CircleShape)
                .border(1.dp, Color(0xFFE0E0E0), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = BrandBlue, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, color = Color.Black)
    }
}

@Composable
fun MessageBubble(msg: DirectMessage, isMe: Boolean, context: Context) {
    val timeString = remember(msg.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(msg.timestamp.toDate())
    }

    val bubbleColor = if (isMe) BrandBlue else Color.White
    val contentColor = if (isMe) Color.White else Color.Black
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Render Media/File
                if (msg.attachmentUrl != null) {
                    when (msg.attachmentType) {
                        "image" -> {
                            AsyncImage(
                                model = msg.attachmentUrl,
                                contentDescription = "Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.attachmentUrl))
                                        context.startActivity(intent)
                                    },
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        "video" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black)
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.attachmentUrl))
                                        intent.setDataAndType(Uri.parse(msg.attachmentUrl), "video/*")
                                        context.startActivity(intent)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                        "file" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        if (isMe) Color.White.copy(alpha = 0.2f) else Color(0xFFF5F5F5),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(msg.attachmentUrl))
                                        context.startActivity(intent)
                                    }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    null,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Attachment",
                                    color = contentColor,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }

                if (msg.content.isNotEmpty()) {
                    Text(
                        text = msg.content,
                        color = contentColor,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }

                Text(
                    text = timeString,
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AttachmentPreview(type: String?, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(BrandBlue.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when(type) {
                    "image" -> Icons.Default.Image
                    "video" -> Icons.Default.Videocam
                    else -> Icons.Default.Description
                },
                contentDescription = null,
                tint = BrandBlue
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (type == "file") "File attached" else "Media selected",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Ready to send",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Close, null, tint = Color.Gray)
        }
    }
}
