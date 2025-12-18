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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Milestone
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

    var project by remember { mutableStateOf<Project?>(null) }
    var pendingUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Overview, 1: Milestones, 2: Admin
    var isLoading by remember { mutableStateOf(true) }

    // Helper to refresh data
    fun refreshData() {
        scope.launch {
            val p = repository.getProjectById(projectId)
            project = p
            if (p != null && p.pendingApplicantIds.isNotEmpty()) {
                pendingUsers = repository.getUsersByIds(p.pendingApplicantIds)
            } else {
                pendingUsers = emptyList()
            }
            isLoading = false
        }
    }

    LaunchedEffect(projectId) { refreshData() }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // --- Header ---
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

        IconButton(onClick = onBackClick, modifier = Modifier.padding(top = 40.dp, start = 16.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

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
                // Title
                Text(
                    p.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(30.dp))

                // --- TABS ---
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BrandBlue,
                    divider = {}
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Milestones") })
                    if (isCreator) {
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text("Admin" + if (p.pendingApplicantIds.isNotEmpty()) " (${p.pendingApplicantIds.size})" else "")
                            }
                        )
                    }
                }

                // --- CONTENT AREA ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> OverviewTab(p)
                        1 -> MilestonesTab(p, repository) { refreshData() }
                        2 -> AdminTab(p, pendingUsers, repository) { refreshData() }
                    }
                }
            }
        }
    }
}

// --- TAB 1: OVERVIEW ---
@Composable
fun OverviewTab(project: Project) {
    val total = project.milestones.size
    val completed = project.milestones.count { it.isCompleted }
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(20.dp))

        // Circular Progress
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
                    color = BrandBlue,
                    startAngle = -90f,
                    sweepAngle = 360 * progress,
                    useCenter = false,
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${(progress * 100).toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                Text("Complete", fontSize = 14.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Team Section
        Text("Team Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            // Just displaying counts/placeholders for now.
            // Ideally, you fetch User objects for memberIds like we did for pending users.
            project.memberIds.forEach { _ ->
                Surface(
                    shape = CircleShape,
                    color = BrandBlue.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp).padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Person, null, tint = BrandBlue, modifier = Modifier.padding(8.dp))
                }
            }

            // Invite Button Placeholder
            Surface(
                shape = CircleShape,
                color = LightGrayInput,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

// --- TAB 2: MILESTONES ---
@Composable
fun MilestonesTab(
    project: Project,
    repository: FirebaseRepository,
    onRefresh: () -> Unit
) {
    var newMilestoneTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column {
        // Add Milestone Input
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newMilestoneTitle,
                onValueChange = { newMilestoneTitle = it },
                placeholder = { Text("Add new task...") },
                modifier = Modifier.weight(1f),
                singleLine = true
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

        // List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(project.milestones) { milestone ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (milestone.isCompleted) Color(0xFFF0F9EB) else Color.White),
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
                            }
                        )
                        Text(
                            text = milestone.title,
                            modifier = Modifier.weight(1f),
                            style = if (milestone.isCompleted) MaterialTheme.typography.bodyLarge.copy(color = Color.Gray) else MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 3: ADMIN ---
@Composable
fun AdminTab(
    project: Project,
    pendingUsers: List<User>,
    repository: FirebaseRepository,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Column {
        Text("Pending Join Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (pendingUsers.isEmpty()) {
            Text("No pending requests.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pendingUsers) { user ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
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
}