package com.example.inoconnect.ui.project_management

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Project
import com.example.inoconnect.ui.auth.BrandBlue

@Composable
fun MyProjectScreen(
    onProjectClick: (String) -> Unit
) {
    val repository = remember { FirebaseRepository() }

    // --- CHANGE STARTS HERE ---
    // Instead of fetching once, we "collect" the flow.
    // This variable will now automatically update whenever the database changes.
    val myProjects by repository.getUserProjectsFlow().collectAsState(initial = emptyList())
    // --- CHANGE ENDS HERE ---

    // Stats Logic (This will now auto-recalculate whenever 'myProjects' updates)
    val activeCount = myProjects.count { it.status == "Active" }
    val completedCount = myProjects.count { it.status == "Completed" }

    // Count total pending applicants for projects YOU created
    val pendingRequestsCount = myProjects
        .filter { it.creatorId == repository.currentUserId }
        .sumOf { it.pendingApplicantIds.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("My Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        // --- 1. MINI DASHBOARD (Stats Cards) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardStatCard("Active", activeCount.toString(), BrandBlue, Modifier.weight(1f))
            DashboardStatCard("Done", completedCount.toString(), Color(0xFF4CAF50), Modifier.weight(1f)) // Green
            DashboardStatCard("Pending", pendingRequestsCount.toString(), Color(0xFFFF9800), Modifier.weight(1f)) // Orange
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Your Projects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        // --- 2. PROJECT LIST ---
        // Note: We removed the "isLoading" check because 'collectAsState' handles the stream.
        // If the list is empty (either no projects or still loading the very first time),
        // it will show the empty message, which is fine for this flow.
        if (myProjects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You haven't joined any projects yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(myProjects) { project ->
                    MyProjectItem(project, onClick = { onProjectClick(project.projectId) })
                }
            }
        }
    }
}

// Keep your existing helper composables (DashboardStatCard and MyProjectItem) below this...
@Composable
fun DashboardStatCard(label: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MyProjectItem(project: Project, onClick: () -> Unit) {
    // Calculate Milestone Progress
    val total = project.milestones.size
    val completed = project.milestones.count { it.isCompleted }
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Thumbnail
            if (project.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = project.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(60.dp).background(Color.LightGray, RoundedCornerShape(8.dp)))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(project.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(4.dp))

                // Status & Members
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if(project.status == "Active") BrandBlue.copy(alpha = 0.1f) else Color.Green.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = project.status,
                            fontSize = 10.sp,
                            color = if(project.status == "Active") BrandBlue else Color(0xFF4CAF50),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(" ${project.memberIds.size}/${project.targetTeamSize}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // Progress Indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(40.dp),
                    color = BrandBlue,
                    trackColor = Color.LightGray.copy(alpha = 0.3f),
                )
                Text("${(progress * 100).toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}