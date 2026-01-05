package com.example.inoconnect.ui.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Project
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.CancellationException // Import this!
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State ---
    var user by remember { mutableStateOf<User?>(null) }
    var userProjects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Bottom Sheets State
    var showEditSheet by remember { mutableStateOf(false) }
    var showSkillsSheet by remember { mutableStateOf(false) }
    var showUserListSheet by remember { mutableStateOf(false) }
    var showProjectListSheet by remember { mutableStateOf(false) }

    // Saving state for Edit Sheet to show progress
    var isSavingProfile by remember { mutableStateOf(false) }

    // List Data
    var userListTitle by remember { mutableStateOf("") }
    var displayedUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isListLoading by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Edit fields state
    var editName by remember { mutableStateOf("") }
    var editHeadline by remember { mutableStateOf("") }
    var editBio by remember { mutableStateOf("") }
    var editUniversity by remember { mutableStateOf("") }
    var editFaculty by remember { mutableStateOf("") }
    var editCourse by remember { mutableStateOf("") }
    var editYear by remember { mutableStateOf("") }

    var tempSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var newSkillInput by remember { mutableStateOf("") }

    // Image Launchers
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val profileImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedImageUri = uri }

    var selectedBackgroundUri by remember { mutableStateOf<Uri?>(null) }
    val backgroundImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> selectedBackgroundUri = uri }

    fun loadUserData() {
        scope.launch {
            val uid = repository.currentUserId
            if (uid != null) {
                // 1. Fetch User Profile
                val fetchedUser = repository.getUserById(uid)
                user = fetchedUser
                fetchedUser?.let {
                    editName = it.username
                    editHeadline = it.headline
                    editBio = it.bio
                    editUniversity = it.university
                    editFaculty = it.faculty
                    editCourse = it.course
                    editYear = it.yearOfStudy
                }

                // 2. Fetch User Projects
                launch {
                    repository.getUserProjectsFlow().collect { projects ->
                        userProjects = projects
                    }
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadUserData() }

    fun openUserList(title: String, userIds: List<String>) {
        userListTitle = title
        showUserListSheet = true
        isListLoading = true
        scope.launch {
            if (userIds.isNotEmpty()) {
                displayedUsers = repository.getUsersByIds(userIds)
            } else {
                displayedUsers = emptyList()
            }
            isListLoading = false
        }
    }

    // --- CRITICAL FIX IN THIS FUNCTION ---
    fun saveChanges() {
        if (isSavingProfile) return // Prevent double click

        isSavingProfile = true
        scope.launch {
            try {
                val currentSkills = user?.skills ?: emptyList()
                repository.updateUserComplete(
                    username = editName,
                    headline = editHeadline,
                    bio = editBio,
                    university = editUniversity,
                    faculty = editFaculty,
                    course = editCourse,
                    yearOfStudy = editYear,
                    imageUri = selectedImageUri,
                    backgroundUri = selectedBackgroundUri,
                    skills = currentSkills,
                )
                // Refresh user data explicitly
                val uid = repository.currentUserId
                if(uid != null) user = repository.getUserById(uid)

                selectedImageUri = null
                selectedBackgroundUri = null

                // Hide sheet only if scope is still active
                if (isActive) {
                    sheetState.hide()
                    showEditSheet = false
                    snackbarHostState.showSnackbar("Profile Updated Successfully")
                }
            } catch (e: Exception) {
                // IMPORTANT: Check if the error is due to cancellation (user quitting)
                if (e is CancellationException) {
                    // Do nothing or log it. DO NOT show Snackbar here, it causes the crash/freeze.
                } else {
                    // Only show error if the screen is still active
                    if (isActive) {
                        snackbarHostState.showSnackbar("Failed to update: ${e.message}")
                    }
                }
            } finally {
                isSavingProfile = false
            }
        }
    }

    fun saveSkills() {
        scope.launch {
            try {
                repository.updateUserSkills(tempSkills)
                val uid = repository.currentUserId
                if(uid != null) user = repository.getUserById(uid)
                showSkillsSheet = false
                snackbarHostState.showSnackbar("Skills Updated")
            } catch (e: Exception) {
                if (e !is CancellationException && isActive) {
                    snackbarHostState.showSnackbar("Error saving skills: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .background(Color(0xFFF8F9FA))
                    .verticalScroll(rememberScrollState())
            ) {
                // ================= HEADER =================
                Box(modifier = Modifier.fillMaxWidth()) {
                    val bgModel: Any? = selectedBackgroundUri ?: user?.backgroundImageUrl?.takeIf { it.isNotEmpty() }
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(BrandBlue)) {
                        if (bgModel != null) {
                            AsyncImage(
                                model = bgModel,
                                contentDescription = "Cover Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val imageModel: Any? = selectedImageUri ?: user?.profileImageUrl?.takeIf { it.isNotEmpty() }
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
                        Text(user?.username ?: "User", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(user?.headline?.ifEmpty { "Student" } ?: "", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                // ================= BODY =================
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(24.dp))

                    ProfileStatsGrid(
                        connections = user?.connectionsCount ?: 0,
                        following = user?.followingCount ?: 0,
                        projects = userProjects.size,
                        onConnectionsClick = { openUserList("Connections", user?.connectionIds ?: emptyList()) },
                        onFollowingClick = { openUserList("Following", user?.followingIds ?: emptyList()) },
                        onProjectsClick = { showProjectListSheet = true }
                    )

                    Spacer(Modifier.height(20.dp))

                    OutlinedButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BrandBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Edit Profile Details", color = BrandBlue)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionCard(title = "Academic Biodata") {
                        InfoRow(Icons.Default.Place, "University", user?.university)
                        InfoRow(Icons.Default.Home, "Faculty", user?.faculty)
                        InfoRow(Icons.AutoMirrored.Filled.List, "Course", user?.course)
                        InfoRow(Icons.Default.DateRange, "Year", user?.yearOfStudy)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = LightGrayInput)
                        Text("About", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            user?.bio?.ifEmpty { "No bio added yet." } ?: "",
                            color = Color.DarkGray,
                            lineHeight = 20.sp
                        )
                    }

                    ProfileSectionCard(
                        title = "Skills",
                        onEditClick = {
                            tempSkills = user?.skills ?: emptyList()
                            newSkillInput = ""
                            showSkillsSheet = true
                        }
                    ) {
                        if (user?.skills.isNullOrEmpty()) {
                            Text("No skills added yet.", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                user!!.skills.forEach { skill ->
                                    Surface(
                                        color = BrandBlue.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = skill,
                                            color = BrandBlue,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ProfileSectionCard(title = "Contact Information") {
                        ContactRow(Icons.Default.Email, user?.email ?: "")
                        if (!user?.phoneNumber.isNullOrEmpty()) ContactRow(Icons.Default.Phone, user!!.phoneNumber)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { repository.logout(); onLogout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE), contentColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Log Out", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // ================= MODERN EDIT SHEET =================
        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    // Allow dismissal even if saving, the coroutine cancellation logic will handle it safely now
                    showEditSheet = false
                },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp)
                        .fillMaxHeight(0.9f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Edit Profile", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (isSavingProfile) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandBlue)
                        } else {
                            TextButton(onClick = { showEditSheet = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Images Section
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Avatar
                            Box {
                                val imageModel: Any? = selectedImageUri ?: user?.profileImageUrl?.takeIf { it.isNotEmpty() }
                                AsyncImage(
                                    model = imageModel,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.LightGray)
                                        .clickable { profileImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    contentScale = ContentScale.Crop
                                )
                                Icon(
                                    Icons.Default.Edit, "Edit",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(BrandBlue, CircleShape)
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Change Profile Photo", color = BrandBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)

                            Spacer(Modifier.height(16.dp))

                            // Cover
                            OutlinedButton(
                                onClick = { backgroundImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Change Cover Photo")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Form Fields - Grouped
                    Text("Identity", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    EditTextField("Full Name", editName) { editName = it }
                    EditTextField("Headline", editHeadline) { editHeadline = it }
                    EditTextField("Bio", editBio, minLines = 3) { editBio = it }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))

                    Text("Academic Details", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    EditTextField("University", editUniversity) { editUniversity = it }
                    EditTextField("Faculty", editFaculty) { editFaculty = it }
                    EditTextField("Course", editCourse) { editCourse = it }
                    EditTextField("Year of Study", editYear) { editYear = it }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { saveChanges() },
                        enabled = !isSavingProfile,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        if (isSavingProfile) {
                            Text("Saving...")
                        } else {
                            Text("Save Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        // ================= SKILLS SHEET =================
        if (showSkillsSheet) {
            ModalBottomSheet(onDismissRequest = { showSkillsSheet = false }, sheetState = sheetState) {
                Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).padding(bottom = 50.dp)) {
                    Text("Manage Skills", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSkillInput,
                            onValueChange = { newSkillInput = it },
                            placeholder = { Text("e.g. Kotlin") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (newSkillInput.isNotBlank() && !tempSkills.contains(newSkillInput.trim())) {
                                    tempSkills = tempSkills + newSkillInput.trim()
                                    newSkillInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tempSkills.forEach { skill ->
                            InputChip(
                                selected = false,
                                onClick = { },
                                label = { Text(skill) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, "Remove", Modifier.size(16.dp).clickable { tempSkills = tempSkills - skill })
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { saveSkills() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                        Text("Save Skills")
                    }
                }
            }
        }

        // ================= USER LIST SHEET =================
        if (showUserListSheet) {
            ModalBottomSheet(
                onDismissRequest = { showUserListSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFFF8F9FA)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.9f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = userListTitle,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .align(Alignment.CenterHorizontally),
                        color = Color.Black
                    )

                    if (isListLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandBlue)
                        }
                    } else if (displayedUsers.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No users found.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 40.dp)
                        ) {
                            items(displayedUsers) { listUser ->
                                ElevatedCard(
                                    onClick = {
                                        showUserListSheet = false
                                        onNavigateToProfile(listUser.userId)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(shape = CircleShape, modifier = Modifier.size(50.dp), color = Color(0xFFE0E0E0)) {
                                            if (listUser.profileImageUrl.isNotEmpty()) {
                                                AsyncImage(model = listUser.profileImageUrl, contentDescription = null, contentScale = ContentScale.Crop)
                                            } else {
                                                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(10.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(16.dp))
                                        Column {
                                            Text(listUser.username, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            if (listUser.headline.isNotEmpty()) {
                                                Text(listUser.headline, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= PROJECT LIST SHEET =================
        if (showProjectListSheet) {
            ModalBottomSheet(
                onDismissRequest = { showProjectListSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFFF8F9FA)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 16.dp)
                ) {
                    Text("My Projects", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp).align(Alignment.CenterHorizontally))
                    if (userProjects.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No projects yet.", color = Color.Gray) }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 40.dp)) {
                            items(userProjects) { project ->
                                val isCompleted = project.status.equals("Completed", ignoreCase = true)
                                val statusColor = if (isCompleted) Color(0xFF4CAF50) else BrandBlue
                                val statusText = if (isCompleted) "Completed" else "Active"
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(project.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            if (project.description.isNotEmpty()) {
                                                Text(project.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(50), modifier = Modifier.padding(start = 12.dp)) {
                                            Text(text = statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}