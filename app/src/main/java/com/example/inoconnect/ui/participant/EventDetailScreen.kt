package com.example.inoconnect.ui.participant

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange // <--- FIXED: Standard Icon
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning // <--- FIXED: Standard Icon for Deadline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.Event
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var event by remember { mutableStateOf<Event?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isJoining by remember { mutableStateOf(false) }

    // 1. Fetch Event Data
    LaunchedEffect(eventId) {
        event = repository.getEventById(eventId)
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Event Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            // Join Button at the bottom
            if (event != null) {
                // Check if current user is already in the list
                val isJoined = event!!.participantIds.contains(repository.currentUserId)

                Button(
                    onClick = {
                        if (!isJoined) {
                            scope.launch {
                                isJoining = true
                                repository.joinEvent(eventId)
                                isJoining = false
                                Toast.makeText(context, "Successfully Joined!", Toast.LENGTH_SHORT).show()
                                onNavigateBack() // Go back after joining
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isJoined) Color.Gray else BrandBlue
                    ),
                    enabled = !isJoined && !isJoining
                ) {
                    if (isJoining) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (isJoined) "Already Joined" else "Join Event")
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (event == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Event not found.")
            }
        } else {
            // Show Event Details
            val e = event!!
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Header
                if (e.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = e.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    // Title
                    Text(e.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Info Row 1: Date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Date: ${e.eventDate}", style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Info Row 2: Location
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = BrandBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Location: ${e.location}", style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Info Row 3: Deadline
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Swapped Timer for Warning (Standard icon)
                        Icon(Icons.Default.Warning, null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deadline: ${e.joiningDeadline}", style = MaterialTheme.typography.bodyLarge, color = Color.Red)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text("About this Event", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(e.description, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)
                }
            }
        }
    }
}