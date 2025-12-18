package com.example.inoconnect.ui.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.inoconnect.data.Event
import com.example.inoconnect.data.FirebaseRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerDashboard(
    onCreateEventClick: () -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    var myEvents by remember { mutableStateOf<List<Event>>(emptyList()) }

    // Helper function to refresh list
    fun refreshEvents() {
        scope.launch {
            myEvents = repository.getOrganizerEvents()
        }
    }

    LaunchedEffect(Unit) {
        refreshEvents()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Organizer Dashboard") },
                actions = {
                    IconButton(onClick = {
                        repository.logout()
                        onLogout()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateEventClick) {
                Icon(Icons.Default.Add, "Create Event")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(myEvents) { event ->
                EventItemWithDelete(
                    event = event,
                    onDeleteClick = {
                        scope.launch {
                            repository.deleteEvent(event.eventId)
                            refreshEvents() // Reload list to remove the item
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun EventItemWithDelete(event: Event, onDeleteClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text Info
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleLarge)
                // FIX: Using the new 'eventDate' and 'location' fields
                Text("Date: ${event.eventDate}", style = MaterialTheme.typography.bodyMedium)
                Text("Location: ${event.location}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Participants: ${event.participantIds.size}", style = MaterialTheme.typography.bodySmall)
            }

            // Delete Button
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Event",
                    tint = Color.Red
                )
            }
        }
    }
}