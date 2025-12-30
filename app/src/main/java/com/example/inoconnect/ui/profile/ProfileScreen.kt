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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.User
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit
    // onSettingsClick REMOVED: Handled in Main Screen TopBar now
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- State ---
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Bottom Sheets State
    var showEditSheet by remember { mutableStateOf(false) }
    var showSkillsSheet by remember { mutableStateOf(false) }

    // --- NEW STATES FOR USER LISTS ---
    var showUserListSheet by remember { mutableStateOf(false) }
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
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadUserData() }

    // --- Helper to fetch and show users ---
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

    fun saveChanges() {
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
                loadUserData()
                sheetState.hide()
                showEditSheet = false
                selectedImageUri = null
                selectedBackgroundUri = null
                snackbarHostState.showSnackbar("Profile Updated Successfully")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to update: ${e.message}")
            }
        }
    }

    fun saveSkills() {
        scope.launch {
            try {
                repository.updateUserSkills(tempSkills)
                loadUserData()
                showSkillsSheet = false
                snackbarHostState.showSnackbar("Skills Updated")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error saving skills: ${e.message}")
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
            // === MAIN CONTENT ===
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

                    // --- REMOVED SETTINGS ICON FROM HERE ---

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
                        Text(user?.username ?: "User", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(user?.headline?.ifEmpty { "Student" } ?: "", fontSize = 14.sp, color = Color.Gray)
                    }
                }

                // ================= BODY =================
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(16.dp))

                    ProfileStatsGrid(
                        connections = user?.connectionsCount ?: 0,
                        following = user?.followingCount ?: 0,
                        projects = user?.projectsCompleted ?: 0,
                        onConnectionsClick = {
                            openUserList("Connections", user?.connectionIds ?: emptyList())
                        },
                        onFollowingClick = {
                            openUserList("Following", user?.followingIds ?: emptyList())
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, BrandBlue)
                    ) {
                        Text("Edit Profile Details", color = BrandBlue)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileSectionCard(title = "Academic Biodata") {
                        InfoRow(Icons.Default.Place, "University", user?.university)
                        InfoRow(Icons.Default.Home, "Faculty", user?.faculty)
                        InfoRow(Icons.Default.List, "Course", user?.course)
                        InfoRow(Icons.Default.DateRange, "Year", user?.yearOfStudy)
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = LightGrayInput)
                        Text("About", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(user?.bio?.ifEmpty { "No bio." } ?: "", color = Color.Gray)
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
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                user!!.skills.forEach { skill ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(skill) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Color(0xFFE3F2FD),
                                            labelColor = BrandBlue
                                        ),
                                        border = BorderStroke(0.dp, Color.Transparent)
                                    )
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

        // ================= MAIN EDIT SHEET =================
        if (showEditSheet) {
            ModalBottomSheet(onDismissRequest = { showEditSheet = false }, sheetState = sheetState) {
                Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 40.dp).verticalScroll(rememberScrollState())) {
                    Text("Edit Profile Details", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Profile Photo", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { profileImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Change") }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Cover Photo", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { backgroundImageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Text("Change") }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    EditTextField("Full Name", editName) { editName = it }
                    EditTextField("Headline", editHeadline) { editHeadline = it }
                    EditTextField("University", editUniversity) { editUniversity = it }
                    EditTextField("Faculty", editFaculty) { editFaculty = it }
                    EditTextField("Course", editCourse) { editCourse = it }
                    EditTextField("Year of Study", editYear) { editYear = it }
                    EditTextField("Bio", editBio, minLines = 3) { editBio = it }

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { saveChanges() }, modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                        Text("Save Details")
                    }
                }
            }
        }

        // ================= SKILLS EDIT SHEET =================
        if (showSkillsSheet) {
            ModalBottomSheet(onDismissRequest = { showSkillsSheet = false }, sheetState = sheetState) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 50.dp)
                ) {
                    Text("Manage Skills", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Add skills to show on your profile", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSkillInput,
                            onValueChange = { newSkillInput = it },
                            placeholder = { Text("e.g. Kotlin, Leadership") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newSkillInput.isNotBlank() && !tempSkills.contains(newSkillInput.trim())) {
                                    tempSkills = tempSkills + newSkillInput.trim()
                                    newSkillInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chip Group
                    Text("Your Skills", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (tempSkills.isEmpty()) {
                        Text("No skills added yet.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tempSkills.forEach { skill ->
                                InputChip(
                                    selected = false,
                                    onClick = { },
                                    label = { Text(skill) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(16.dp).clickable {
                                                tempSkills = tempSkills - skill
                                            }
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        containerColor = Color(0xFFF5F5F5),
                                        labelColor = Color.Black
                                    ),
                                    border = BorderStroke(1.dp, Color.LightGray)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { saveSkills() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                    ) {
                        Text("Save Skills")
                    }
                }
            }
        }

        // ================= USER LIST SHEET =================
        if (showUserListSheet) {
            ModalBottomSheet(
                onDismissRequest = { showUserListSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 50.dp)
                        .heightIn(min = 200.dp, max = 500.dp)
                ) {
                    Text(
                        text = userListTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp).align(Alignment.CenterHorizontally)
                    )

                    if (isListLoading) {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = BrandBlue)
                        }
                    } else if (displayedUsers.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("No users found.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(displayedUsers) { listUser ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        modifier = Modifier.size(50.dp),
                                        color = Color.LightGray
                                    ) {
                                        if (listUser.profileImageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = listUser.profileImageUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Person,
                                                null,
                                                tint = Color.White,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(listUser.username, fontWeight = FontWeight.Bold)
                                        Text(
                                            listUser.headline.ifEmpty { "Student" },
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Divider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}