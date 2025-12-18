package com.example.inoconnect.ui.project_management

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Project
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.launch

@Composable
fun ProjectManagementScreen(
    projectId: String,
    onBackClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var project by remember { mutableStateOf<Project?>(null) }

    // User lists
    var pendingUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var memberUsers by remember { mutableStateOf<List<User>>(emptyList()) }

    // UI State
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // For Chat
    var currentUserName by remember { mutableStateOf("") }

    // --- REFRESH DATA FUNCTION ---
    fun refreshData() {
        scope.launch {
            val p = repository.getProjectById(projectId)
            project = p

            if (p != null) {
                // Fetch Pending Applicants
                if (p.pendingApplicantIds.isNotEmpty()) {
                    pendingUsers = repository.getUsersByIds(p.pendingApplicantIds)
                } else {
                    pendingUsers = emptyList()
                }

                // Fetch Current Members
                if (p.memberIds.isNotEmpty()) {
                    memberUsers = repository.getUsersByIds(p.memberIds)
                } else {
                    memberUsers = emptyList()
                }
            }

            // Fetch Current User Name (for Chat)
            val uid = repository.currentUserId
            if (uid != null) {
                val me = repository.getUserById(uid)
                currentUserName = me?.username ?: "Unknown"
            }

            isLoading = false
        }
    }

    LaunchedEffect(projectId) {
        refreshData()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // --- 1. Blue Curved Header ---
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 50)
                quadraticBezierTo(size.width / 2, size.height + 50, 0f, size.height - 50)
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.padding(top = 40.dp, start = 16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

        // --- 2. Main Content ---
        if (isLoading || project == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandBlue)
        } else {
            val p = project!!
            val isCreator = p.creatorId == repository.currentUserId

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp)
            ) {
                // Project Title
                Text(
                    p.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // --- TABS ---
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BrandBlue,
                    divider = {},
                    edgePadding = 16.dp
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Milestones") })
                    Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Chat") })
                    if (isCreator) {
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                val count = if (p.pendingApplicantIds.isNotEmpty()) " (${p.pendingApplicantIds.size})" else ""
                                Text("Admin$count")
                            }
                        )
                    }
                }

                // --- TAB CONTENT ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> OverviewTab(
                            project = p,
                            members = memberUsers,
                            isCreator = isCreator,
                            repository = repository,
                            onRefresh = { refreshData() }
                        )
                        1 -> MilestonesTab(
                            project = p,
                            repository = repository,
                            onRefresh = { refreshData() }
                        )
                        2 -> ChatTab(
                            projectId = p.projectId,
                            currentUserName = currentUserName
                        )
                        3 -> AdminTab(
                            project = p,
                            pendingUsers = pendingUsers,
                            repository = repository,
                            onProjectDeleted = onBackClick,
                            onRefresh = { refreshData() }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
//               TAB: OVERVIEW
// ==========================================
@Composable
fun OverviewTab(
    project: Project,
    members: List<User>,
    isCreator: Boolean,
    repository: FirebaseRepository,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Calculate Progress
    val total = project.milestones.size
    val completed = project.milestones.count { it.isCompleted }
    val progress = if (total > 0) completed.toFloat() / total else 0f

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Spacer(modifier = Modifier.height(20.dp))

            // Circular Progress Indicator
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    drawArc(
                        color = LightGrayInput,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx())
                    )
                    drawArc(
                        color = if (project.status == "Completed") Color(0xFF4CAF50) else BrandBlue,
                        startAngle = -90f,
                        sweepAngle = 360 * progress,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (project.status == "Completed") Color(0xFF4CAF50) else BrandBlue
                    )
                    Text(
                        text = project.status, // "Active" or "Completed"
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Team Members (${members.size}/${project.targetTeamSize})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Member List
        items(members) { user ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Placeholder
                    Surface(
                        shape = CircleShape,
                        color = BrandBlue.copy(alpha = 0.1f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Person, null, tint = BrandBlue, modifier = Modifier.padding(8.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.username, fontWeight = FontWeight.SemiBold)
                        Text(user.email, fontSize = 12.sp, color = Color.Gray)
                    }

                    // Remove Button (Only for Creator, cannot remove self)
                    if (isCreator && user.userId != project.creatorId) {
                        IconButton(onClick = {
                            scope.launch {
                                repository.removeMember(project.projectId, user.userId)
                                onRefresh()
                            }
                        }) {
                            Icon(Icons.Default.Delete, "Remove", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    } else if (user.userId == project.creatorId) {
                        Text("Owner", fontSize = 12.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==========================================
//               TAB: MILESTONES
// ==========================================
@Composable
fun MilestonesTab(
    project: Project,
    repository: FirebaseRepository,
    onRefresh: () -> Unit
) {
    var newMilestoneTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column {
        // Input Area
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newMilestoneTitle,
                onValueChange = { newMilestoneTitle = it },
                placeholder = { Text("Add new task...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBlue,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            IconButton(onClick = {
                if (newMilestoneTitle.isNotBlank()) {
                    scope.launch {
                        repository.addMilestone(project.projectId, newMilestoneTitle)
                        newMilestoneTitle = ""
                        onRefresh()
                    }
                }
            }) {
                Icon(Icons.Default.Add, "Add", tint = BrandBlue)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Milestones List
        if (project.milestones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No milestones yet. Add one to start!", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(project.milestones) { milestone ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (milestone.isCompleted) Color(0xFFF0F9EB) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = milestone.isCompleted,
                                onCheckedChange = {
                                    scope.launch {
                                        repository.toggleMilestone(project.projectId, milestone)
                                        onRefresh()
                                    }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = BrandBlue)
                            )

                            Text(
                                text = milestone.title,
                                modifier = Modifier.weight(1f),
                                style = if (milestone.isCompleted)
                                    MaterialTheme.typography.bodyLarge.copy(color = Color.Gray)
                                else
                                    MaterialTheme.typography.bodyLarge
                            )

                            // Delete Milestone
                            IconButton(onClick = {
                                scope.launch {
                                    repository.deleteMilestone(project.projectId, milestone)
                                    onRefresh()
                                }
                            }) {
                                Icon(Icons.Default.Close, "Delete", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
//               TAB: ADMIN
// ==========================================
@Composable
fun AdminTab(
    project: Project,
    pendingUsers: List<User>,
    repository: FirebaseRepository,
    onProjectDeleted: () -> Unit,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn {
        // Section 1: Project Actions
        item {
            Text("Project Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Toggle Completion
                Button(
                    onClick = {
                        scope.launch {
                            val newStatus = if(project.status == "Active") "Completed" else "Active"
                            repository.updateProjectStatus(project.projectId, newStatus)
                            onRefresh()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if(project.status == "Active") Color(0xFF4CAF50) else BrandBlue
                    )
                ) {
                    Text(if(project.status == "Active") "Mark Complete" else "Mark Active")
                }

                // Delete Project
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteProject(project.projectId)
                            onProjectDeleted() // Navigate back
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Project")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = LightGrayInput)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section 2: Join Requests
        item {
            Text("Pending Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (pendingUsers.isEmpty()) {
            item { Text("No pending requests.", color = Color.Gray) }
        } else {
            items(pendingUsers) { user ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.username, fontWeight = FontWeight.Bold)
                            Text(user.email, fontSize = 12.sp, color = Color.Gray)
                        }

                        // Reject
                        IconButton(onClick = {
                            scope.launch {
                                repository.rejectJoinRequest(project.projectId, user.userId)
                                onRefresh()
                            }
                        }) {
                            Icon(Icons.Default.Close, "Reject", tint = Color.Red)
                        }

                        // Accept
                        IconButton(onClick = {
                            scope.launch {
                                repository.acceptJoinRequest(project.projectId, user.userId)
                                onRefresh()
                            }
                        }) {
                            Icon(Icons.Default.Check, "Accept", tint = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    }
}