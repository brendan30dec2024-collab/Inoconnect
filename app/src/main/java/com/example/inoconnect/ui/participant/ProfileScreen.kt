package com.example.inoconnect.ui.participant

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Form Fields
    var bio by remember { mutableStateOf("") }
    var githubLink by remember { mutableStateOf("") }
    var currentSkill by remember { mutableStateOf("") }
    val skills = remember { mutableStateListOf<String>() }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    // Fetch Data
    LaunchedEffect(Unit) {
        val uid = repository.currentUserId
        if (uid != null) {
            user = repository.getUserById(uid)
            user?.let {
                bio = it.bio
                githubLink = it.githubLink
                skills.addAll(it.skills)
            }
        }
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Blue Header
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 50)
                quadraticBezierTo(size.width / 2, size.height + 50, 0f, size.height - 50)
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandBlue)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // --- PROFILE IMAGE ---
                Box(contentAlignment = Alignment.BottomEnd) {
                    val imageModel = selectedImageUri ?: user?.profileImageUrl.takeIf { !it.isNullOrEmpty() }

                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = LightGrayInput) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                        }
                    }

                    // Edit Icon
                    SmallFloatingActionButton(
                        onClick = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        containerColor = BrandBlue,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(user?.username ?: "User", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(user?.email ?: "", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(30.dp))

                // --- EDIT FORM ---
                Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {

                    // Bio
                    Text("Bio", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Github
                    Text("GitHub / Portfolio Link", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = githubLink,
                        onValueChange = { githubLink = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.LightGray)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Skills
                    Text("Skills", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = currentSkill,
                            onValueChange = { currentSkill = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Add skill (e.g. Java)") }
                        )
                        IconButton(onClick = {
                            if (currentSkill.isNotBlank() && !skills.contains(currentSkill)) {
                                skills.add(currentSkill.trim())
                                currentSkill = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, "Add", tint = BrandBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        skills.forEach { skill ->
                            InputChip(
                                selected = true,
                                onClick = { skills.remove(skill) },
                                label = { Text(skill) },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(16.dp)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // --- SAVE BUTTON ---
                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                repository.updateUserProfile(bio, skills.toList(), githubLink, selectedImageUri)
                                isSaving = false
                                Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(color = Color.White) else Text("Save Changes")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- LOGOUT BUTTON ---
                    Button(
                        onClick = {
                            repository.logout()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f), contentColor = Color.Red),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("Logout")
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}