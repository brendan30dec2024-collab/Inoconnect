package com.example.inoconnect.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State ---
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Bottom Sheet State
    var showEditSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- Temporary Edit State (Hoisted for Sheet) ---
    var editName by remember { mutableStateOf("") }
    var editHeadline by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editFaculty by remember { mutableStateOf("") }
    var editCourse by remember { mutableStateOf("") }
    var editYear by remember { mutableStateOf("") }

    // Image Picker (Used inside Sheet)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    fun loadUserData() {
        scope.launch {
            val uid = repository.currentUserId
            if (uid != null) {
                val fetchedUser = repository.getUserById(uid)
                user = fetchedUser
                // Pre-fill edit fields
                fetchedUser?.let {
                    editName = it.username
                    editHeadline = it.headline
                    editBio = it.bio
                    editFaculty = it.faculty
                    editCourse = it.course
                    editYear = it.yearOfStudy
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadUserData() }

    // Save Logic
    fun saveChanges() {
        scope.launch {
            sheetState.hide() // Animate sheet closing
            showEditSheet = false

            // TODO: Call repository.updateUser(...) here
            kotlinx.coroutines.delay(500) // Simulate save

            snackbarHostState.showSnackbar("Profile Updated")
            loadUserData()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        // NO TopBar here! This guarantees the layout never shifts.
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
        } else {
            // === MAIN CONTENT ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Apply ONLY bottom padding to avoid nav bar,
                    // NO top padding so header hits the top edge.
                    .padding(bottom = padding.calculateBottomPadding())
                    .background(Color(0xFFF8F9FA))
                    .verticalScroll(rememberScrollState())
            ) {
                // ================= HEADER =================
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 1. Blue Cover
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(BrandBlue))

                    // 2. Avatar & Name
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val imageModel = selectedImageUri ?: user?.profileImageUrl.takeIf { !it.isNullOrEmpty() }
                        AsyncImage(
                            model = imageModel,
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(user?.username ?: "User", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            user?.headline?.ifEmpty { "Student" } ?: "",
                            fontSize = 14.sp, color = Color.Gray
                        )
                    }
                }

                // ================= BODY =================
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(16.dp))
                    ProfileStatsGrid(
                        connections = user?.connectionsCount ?: 0,
                        following = user?.followingCount ?: 0,
                        projects = user?.projectsCompleted ?: 0
                    )

                    Spacer(Modifier.height(16.dp))

                    // EDIT BUTTON - Opens the Sheet
                    OutlinedButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlue)
                    ) {
                        Text("Edit Profile", color = BrandBlue)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionCard(title = "Academic Biodata") {
                        InfoRow(Icons.Default.Home, "Faculty", user?.faculty)
                        InfoRow(Icons.Default.List, "Course", user?.course)
                        InfoRow(Icons.Default.DateRange, "Year", user?.yearOfStudy)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = LightGrayInput)
                        Text("About", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(user?.bio?.ifEmpty { "No bio." } ?: "", color = Color.Gray)
                    }

                    if (user?.skills?.isNotEmpty() == true) {
                        ProfileSectionCard(title = "Skills") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                user!!.skills.forEach { skill ->
                                    SuggestionChip(onClick = {}, label = { Text(skill) })
                                }
                            }
                        }
                    }

                    ProfileSectionCard(title = "Contact Information") {
                        ContactRow(Icons.Default.Email, user?.email ?: "")
                        if (!user?.phoneNumber.isNullOrEmpty()) ContactRow(Icons.Default.Phone, user!!.phoneNumber)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    TextButton(onClick = { repository.logout(); onLogout() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Log Out", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // ================= EDIT SHEET =================
        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp) // Extra bottom padding for sheet
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Edit Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Edit Photo Option
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Profile Photo", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            Text("Change Photo")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    EditTextField("Full Name", editName) { editName = it }
                    EditTextField("Headline", editHeadline) { editHeadline = it }
                    EditTextField("Faculty", editFaculty) { editFaculty = it }
                    EditTextField("Course", editCourse) { editCourse = it }
                    EditTextField("Year of Study", editYear) { editYear = it }
                    EditTextField("Bio", editBio, minLines = 3) { editBio = it }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { saveChanges() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}