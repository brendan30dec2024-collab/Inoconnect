package com.example.inoconnect.ui.participant

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

@Composable
fun EventDetailScreen(
    eventId: String,
    onNavigateBack: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var event by remember { mutableStateOf<Event?>(null) }
    var organizerName by remember { mutableStateOf("Loading...") } 
    var isLoading by remember { mutableStateOf(true) }
    var isJoining by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        val fetchedEvent = repository.getEventById(eventId)
        event = fetchedEvent
        if (fetchedEvent != null) {
            val user = repository.getUserById(fetchedEvent.organizerId)
            organizerName = user?.username ?: "Unknown Organizer"
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Blue Curved Header 
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 80)
                quadraticBezierTo(
                    size.width / 2, size.height + 40,
                    0f, size.height - 80
                )
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        // Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Main Content
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandBlue)
        } else if (event == null) {
            Text("Event not found", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
        } else {
            val e = event!!
            val isJoined = e.participantIds.contains(repository.currentUserId)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Text
                Text(
                    text = "Event Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Floating Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 30.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        // Image Header
                        if (e.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = e.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No Image Available", color = Color.Gray)
                            }
                        }

                        // Content Body
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(e.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                            // --- NEW: Organizer Name ---
                            Text(
                                text = "Organized by $organizerName",
                                fontSize = 14.sp,
                                color = BrandBlue,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(16.dp))

                            // Info Rows
                            DetailRow(Icons.Default.DateRange, e.eventDate)
                            Spacer(Modifier.height(8.dp))
                            DetailRow(Icons.Default.LocationOn, e.location)
                            Spacer(Modifier.height(8.dp))
                            DetailRow(Icons.Default.Warning, "Deadline: ${e.joiningDeadline}", isWarning = true)

                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))

                            Text("Description", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(e.description, fontSize = 14.sp, color = Color.Gray, lineHeight = 22.sp)

                            Spacer(Modifier.height(32.dp))

                            // Action Button
                            Button(
                                onClick = {
                                    if (!isJoined) {
                                        scope.launch {
                                            isJoining = true
                                            repository.joinEvent(eventId)
                                            isJoining = false
                                            Toast.makeText(context, "Successfully Joined!", Toast.LENGTH_SHORT).show()
                                            onNavigateBack()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isJoined) Color.Gray else BrandBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isJoined && !isJoining
                            ) {
                                if (isJoining) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = if (isJoined) "Already Joined" else "Join Event",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, isWarning: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isWarning) Color.Red else BrandBlue,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isWarning) Color.Red else Color.DarkGray
        )
    }
}
