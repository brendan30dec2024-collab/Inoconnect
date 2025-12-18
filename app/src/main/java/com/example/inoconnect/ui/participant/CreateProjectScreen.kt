package com.example.inoconnect.ui.participant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import kotlinx.coroutines.launch

@Composable
fun CreateProjectScreen(
    onProjectCreated: () -> Unit,
    onBackClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Tag State
    var currentTagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() } // Holds the list of tags

    // Image State
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    Scaffold(
        topBar = {
            Text("Create New Project", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
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
                Text(if (selectedImageUri == null) "Upload Project Cover" else "Change Photo")
            }

            Spacer(Modifier.height(16.dp))

            // --- TEXT FIELDS ---
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Project Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Spacer(Modifier.height(16.dp))

            // --- CUSTOM TAGS SECTION ---
            Text("Targeted Skills / Tags:", style = MaterialTheme.typography.titleSmall)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = currentTagInput,
                    onValueChange = { currentTagInput = it },
                    label = { Text("Add Tag (e.g. AI, Design)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (currentTagInput.isNotBlank() && !tags.contains(currentTagInput)) {
                            tags.add(currentTagInput.trim())
                            currentTagInput = "" // Clear input
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Tag")
                }
            }

            // Display Added Tags
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    InputChip(
                        selected = true,
                        onClick = { tags.remove(tag) }, // Click to remove
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp)) }
                    )
                }
            }

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

                            // Create Project with the list of tags
                            repository.createProject(title, description, finalImageUrl, tags.toList())

                            isUploading = false
                            onProjectCreated()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotEmpty()
                ) {
                    Text("Publish Project")
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}