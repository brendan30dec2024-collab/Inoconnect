package com.example.inoconnect.ui.organizer

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun CreateEventScreen(
    onEventCreated: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // Date State
    var eventDate by remember { mutableStateOf("") }
    var joiningDeadline by remember { mutableStateOf("") }

    // Image State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    // Helper to Show Date Picker Dialog
    fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, day: Int ->
                // Format: YYYY-MM-DD
                onDateSelected("$year-${month + 1}-$day")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            Text("Create New Event", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- IMAGE PICKER ---
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Button(
                onClick = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (selectedImageUri == null) "Upload Event Poster" else "Change Photo")
            }

            Spacer(Modifier.height(16.dp))

            // --- TEXT FIELDS ---
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))

            // --- DATE PICKERS ---
            // Event Date
            OutlinedTextField(
                value = eventDate,
                onValueChange = {}, // Read only, set by dialog
                label = { Text("Event Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker { date -> eventDate = date } }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )

            Spacer(Modifier.height(8.dp))

            // Joining Deadline
            OutlinedTextField(
                value = joiningDeadline,
                onValueChange = {},
                label = { Text("Joining Deadline") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker { date -> joiningDeadline = date } }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Deadline")
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            // --- SUBMIT BUTTON ---
            if (isUploading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isUploading = true
                            var finalImageUrl = ""
                            if (selectedImageUri != null) {
                                finalImageUrl = repository.uploadImage(selectedImageUri!!) ?: ""
                            }

                            repository.createEvent(title, description, location, eventDate, joiningDeadline, finalImageUrl)

                            isUploading = false
                            onEventCreated()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotEmpty() && eventDate.isNotEmpty() // Basic validation
                ) {
                    Text("Publish Event")
                }
            }
        }
    }
}