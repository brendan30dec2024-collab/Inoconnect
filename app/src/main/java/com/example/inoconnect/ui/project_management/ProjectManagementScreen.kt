package com.example.inoconnect.ui.project_management

import android.widget.Toast
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
    val context = LocalContext.current

    var project by remember { mutableStateOf<Project?>(null) }
    var pendingUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var memberUsers by remember { mutableStateOf<List<User>>(emptyList()) } // <--- NEW: Store Member Objects
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    fun refreshData() {
        scope.launch {
            val p = repository.getProjectById(projectId)
            project = p
            if (p != null) {
                // Fetch Pending Users
                if (p.pendingApplicantIds.isNotEmpty()) {
                    pendingUsers = repository.getUsersByIds(p.pendingApplicantIds)
                } else {
                    pendingUsers = emptyList()
                }
                // Fetch Member Users (Feature 1)
                if (p.memberIds.isNotEmpty()) {
                    memberUsers = repository.getUsersByIds(p.memberIds)
                } else {
                    memberUsers = emptyList()
                }
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
                modifier = Modifier.fillMaxSize().padding(top = 100.dp)
            ) {
                Text(p.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(30.dp))

                // --- TABS ---
                TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = BrandBlue, divider = {}) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Overview") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Milestones") })
                    if (isCreator) {
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Admin") })
                    }
                }

                Box(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
                    when (selectedTab) {
                        0 -> OverviewTab(p, memberUsers, isCreator, repository) { refreshData() }
                        1 -> MilestonesTab(p, repository, isCreator) { refreshData() } // Added isCreator
                        2 -> AdminTab(p, pendingUsers, repository, onProjectDeleted = onBackClick) { refreshData() }
                    }
                }
            }
        }
    }
}

// --- TAB 1: OVERVIEW (Updated for Member Mgmt) ---
@Composable
fun OverviewTab(
    project: Project,
    members: List<User>,
    isCreator: Boolean,
    repository: FirebaseRepository,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val total = project.milestones.size
    val completed = project.milestones.count { it.isCompleted }
    val progress = if (total > 0) completed.toFloat() / total else 0f

    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            // Progression Circle (Synced)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    drawArc(color = LightGrayInput, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 20.dp.toPx()))
                    drawArc(color = BrandBlue, startAngle = -90f, sweepAngle = 360 * progress, useCenter = false, style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(progress * 100).toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = BrandBlue)
                    Text("Complete", fontSize = 14.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text("Team Members", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
        }

        // List Members with Icons and Names
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
                    Surface(shape = CircleShape, color = BrandBlue.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Person, null, tint = BrandBlue, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(user.username, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                    // Remove Button (Only for Creator, can't remove self)
                    if (isCreator && user.userId != project.creatorId) {
                        IconButton(onClick = {
                            scope.launch {
                                repository.removeMember(project.projectId, user.userId)
                                onRefresh()
                            }
                        }) {
                            Icon(Icons.Default.Delete, "Remove", tint = Color.Red.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: MILESTONES (Updated for Delete) ---
@Composable
fun MilestonesTab(
    project: Project,
    repository: FirebaseRepository,
    isCreator: Boolean, // Pass this down if you want to restrict deleting
    onRefresh: () -> Unit
) {
    var newMilestoneTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column {
        // Add Milestone
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
                        // Delete Milestone Button
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

// --- TAB 3: ADMIN (Updated for Project Actions) ---
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
        // --- SECTION 1: PROJECT ACTIONS ---
        item {
            Text("Project Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Complete Button
                Button(
                    onClick = {
                        scope.launch {
                            val newStatus = if(project.status == "Active") "Completed" else "Active"
                            repository.updateProjectStatus(project.projectId, newStatus)
                            onRefresh()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if(project.status == "Active") Color(0xFF4CAF50) else BrandBlue)
                ) {
                    Text(if(project.status == "Active") "Mark Complete" else "Mark Active")
                }

                // Delete Button
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteProject(project.projectId)
                            onProjectDeleted()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Project")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- SECTION 2: JOIN REQUESTS ---
        item {
            Text("Pending Join Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        IconButton(onClick = {
                            scope.launch { repository.rejectJoinRequest(project.projectId, user.userId); onRefresh() }
                        }) {
                            Icon(Icons.Default.Close, "Reject", tint = Color.Red)
                        }
                        IconButton(onClick = {
                            scope.launch { repository.acceptJoinRequest(project.projectId, user.userId); onRefresh() }
                        }) {
                            Icon(Icons.Default.Check, "Accept", tint = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    }
}