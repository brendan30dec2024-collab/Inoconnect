package com.example.inoconnect.ui.project_management

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Milestone
import com.example.inoconnect.data.Project
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProjectManagementScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()

    var project by remember { mutableStateOf<Project?>(null) }

    var pendingUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var memberUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var currentUserName by remember { mutableStateOf("") }

    // UI State
    var selectedTab by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // --- REFRESH DATA ---
    fun refreshData() {
        scope.launch {
            val p = repository.getProjectById(projectId)
            project = p

            if (p != null) {
                pendingUsers = if (p.pendingApplicantIds.isNotEmpty()) repository.getUsersByIds(p.pendingApplicantIds) else emptyList()
                memberUsers = if (p.memberIds.isNotEmpty()) repository.getUsersByIds(p.memberIds) else emptyList()
            }

            val uid = repository.currentUserId
            if (uid != null) {
                currentUserName = repository.getUserById(uid)?.username ?: "Unknown"
            }
            isLoading = false
        }
    }

    // --- OPTIMISTIC UPDATE HANDLERS (FIXED) ---
    val onToggleMilestone: (Milestone) -> Unit = { milestone ->
        val currentProject = project
        if (currentProject != null) {
            // 1. Instant UI Update
            val updatedList = currentProject.milestones.map {
                if (it.id == milestone.id) it.copy(isCompleted = !it.isCompleted) else it
            }
            project = currentProject.copy(milestones = updatedList)

            // 2. Background Sync (Detached from UI Lifecycle)
            // Using IO Dispatcher ensures this runs even if the screen is destroyed immediately
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.toggleMilestone(currentProject.projectId, milestone)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val onDeleteMilestone: (Milestone) -> Unit = { milestone ->
        val currentProject = project
        if (currentProject != null) {
            // 1. Instant UI Update
            val updatedList = currentProject.milestones.filter { it.id != milestone.id }
            project = currentProject.copy(milestones = updatedList)

            // 2. Background Sync (Detached from UI Lifecycle)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.deleteMilestone(currentProject.projectId, milestone)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(projectId) {
        refreshData()
    }

    if (isLoading || project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandBlue)
        }
    } else {
        val p = project!!
        val isCreator = p.creatorId == repository.currentUserId

        val tabs = remember(isCreator) {
            if (isCreator) listOf("Overview", "Milestones", "Chat", "Admin")
            else listOf("Overview", "Milestones", "Chat")
        }

        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

            // --- 1. HEADER SECTION ---
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height - 50)
                        quadraticBezierTo(size.width / 2, size.height + 50, 0f, size.height - 50)
                        close()
                    }
                    drawPath(path = path, color = BrandBlue)
                }

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(top = 40.dp, start = 16.dp).align(Alignment.TopStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }

                Text(
                    text = p.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).padding(top = 20.dp, start = 40.dp, end = 40.dp)
                )
            }

            // --- 2. TABS SECTION ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = BrandBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = BrandBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            if (title == "Admin" && p.pendingApplicantIds.isNotEmpty()) {
                                Text("$title (${p.pendingApplicantIds.size})")
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }

            // --- 3. CONTENT SECTION ---
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> OverviewTab(p, memberUsers, isCreator, repository) { refreshData() }
                    1 -> MilestonesTab(
                        project = p,
                        repository = repository,
                        onRefresh = { refreshData() },
                        onToggle = onToggleMilestone,
                        onDelete = onDeleteMilestone
                    )
                    2 -> ChatTab(p.projectId, currentUserName)
                    3 -> if (isCreator) AdminTab(p, pendingUsers, repository, onBackClick, { refreshData() }, onNavigateToProfile)
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
    val total = project.milestones.size
    val completed = project.milestones.count { it.isCompleted }
    val progress = if (total > 0) completed.toFloat() / total else 0f

    LazyColumn(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    drawArc(color = LightGrayInput, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 20.dp.toPx()))
                    drawArc(
                        color = if (project.status == "Completed") Color(0xFF4CAF50) else BrandBlue,
                        startAngle = -90f,
                        sweepAngle = 360 * progress,
                        useCenter = false,
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(progress * 100).toInt()}%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = if (project.status == "Completed") Color(0xFF4CAF50) else BrandBlue)
                    Text(project.status, fontSize = 14.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text("Team Members (${members.size}/${project.targetTeamSize})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(members) { user ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = BrandBlue.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Person, null, tint = BrandBlue, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.username, fontWeight = FontWeight.SemiBold)
                        Text(user.email, fontSize = 12.sp, color = Color.Gray)
                    }
                    if (isCreator && user.userId != project.creatorId) {
                        IconButton(onClick = { scope.launch { repository.removeMember(project.projectId, user.userId); onRefresh() } }) {
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
    onRefresh: () -> Unit,
    onToggle: (Milestone) -> Unit,
    onDelete: (Milestone) -> Unit
) {
    var newMilestoneTitle by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Sort into lists
    val todoList = project.milestones.filter { !it.isCompleted }
    val completedList = project.milestones.filter { it.isCompleted }

    val total = project.milestones.size
    val completedCount = project.milestones.count { it.isCompleted }
    val progress = if (total > 0) completedCount.toFloat() / total else 0f

    LazyColumn(modifier = Modifier.fillMaxSize()) {

        // 1. Progress Bar Header
        item {
            Text(
                text = "Project Progress: ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = BrandBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = BrandBlue,
                trackColor = LightGrayInput
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 2. Add New Task Input
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newMilestoneTitle,
                    onValueChange = { newMilestoneTitle = it },
                    placeholder = { Text("Add new task...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (newMilestoneTitle.isNotBlank()) {
                            scope.launch {
                                repository.addMilestone(project.projectId, newMilestoneTitle)
                                newMilestoneTitle = ""
                                onRefresh()
                            }
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. "To Do" Section
        if (todoList.isNotEmpty()) {
            item {
                Text("To Do", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(todoList) { milestone ->
                MilestoneTaskCard(milestone, false, onToggle, onDelete)
            }
        } else if (project.milestones.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No tasks yet. Start planning!", color = Color.Gray)
                }
            }
        }

        // 4. "Completed" Section
        if (completedList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Completed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(completedList) { milestone ->
                MilestoneTaskCard(milestone, true, onToggle, onDelete)
            }
        }
    }
}

@Composable
fun MilestoneTaskCard(
    milestone: Milestone,
    isCompleted: Boolean,
    onToggle: (Milestone) -> Unit,
    onDelete: (Milestone) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(if (isCompleted) 0.dp else 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isCompleted) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle(milestone) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) BrandBlue else Color.Transparent)
                    .then(if (!isCompleted) Modifier.background(Color.LightGray.copy(alpha = 0.2f)) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = Color.Gray, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = milestone.title,
                fontSize = 16.sp,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                color = if (isCompleted) Color.Gray else Color.Black,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { onDelete(milestone) }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Delete", tint = Color.LightGray)
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
    onRefresh: () -> Unit,
    onViewProfile: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn {
        item {
            Text("Project Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

                Button(
                    onClick = { scope.launch { repository.deleteProject(project.projectId); onProjectDeleted() } },
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

                        Column(modifier = Modifier.weight(1f).clickable { onViewProfile(user.userId) }) {
                            Text(user.username, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                            Text("Tap to view profile", fontSize = 10.sp, color = BrandBlue)
                        }

                        IconButton(onClick = { scope.launch { repository.rejectJoinRequest(project.projectId, user.userId); onRefresh() } }) {
                            Icon(Icons.Default.Close, "Reject", tint = Color.Red)
                        }

                        IconButton(onClick = { scope.launch { repository.acceptJoinRequest(project.projectId, user.userId); onRefresh() } }) {
                            Icon(Icons.Default.Check, "Accept", tint = Color(0xFF4CAF50))
                        }
                    }
                }
            }
        }
    }
}