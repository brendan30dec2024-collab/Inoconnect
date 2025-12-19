package com.example.inoconnect.ui.participant

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.Project
import com.example.inoconnect.ui.auth.BrandBlue
import com.google.firebase.auth.FirebaseAuth // Make sure this import is here
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onBackClick: () -> Unit,
    onManageClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- MISSING VARIABLES FIXED HERE ---
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var isSendingRequest by remember { mutableStateOf(false) }

    // Logic to refresh the screen
    var refreshTrigger by remember { mutableIntStateOf(0) }
    fun refreshProject() { refreshTrigger++ }

    // State
    var project by remember { mutableStateOf<Project?>(null) }
    var creatorName by remember { mutableStateOf("Loading...") }
    var isLoading by remember { mutableStateOf(true) }

    // Added refreshTrigger to the key so it re-runs when called
    LaunchedEffect(projectId, refreshTrigger) {
        isLoading = true
        val fetchedProject = repository.getProjectById(projectId)
        project = fetchedProject

        // Fetch Creator Name
        if (fetchedProject != null) {
            val user = repository.getUserById(fetchedProject.creatorId)
            creatorName = user?.username ?: "Unknown User"
        }
        isLoading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 1. Blue Curved Header ---
        Canvas(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 80)
                quadraticBezierTo(
                    size.width / 2, size.height + 40,
                    0f, size.height - 80
                )
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // --- 2. Main Content ---
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandBlue)
        } else if (project == null) {
            Text("Project not found", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
        } else {
            val p = project!!

            // Logic: Determine User Status
            val isCreator = p.creatorId == currentUserId
            val isMember = p.memberIds.contains(currentUserId)
            val isPending = p.pendingApplicantIds.contains(currentUserId)

            // New Feature: Capacity Check
            val isFull = p.memberIds.size >= p.targetTeamSize

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Text
                Text(
                    text = "Project Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Floating Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 30.dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column {
                        // Image Header
                        if (p.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = p.imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No Cover Image", color = Color.Gray)
                            }
                        }

                        // Content Body
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(p.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)

                            // --- NEW: Creator Name ---
                            Text(
                                text = "Created by $creatorName",
                                fontSize = 14.sp,
                                color = BrandBlue,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(Modifier.height(16.dp))

                            // Stats Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                // Team Size
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Person, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("${p.memberIds.size}/${p.targetTeamSize} Members", fontSize = 14.sp)
                                }

                                // Deadline
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Due: ${p.recruitmentDeadline}", fontSize = 14.sp)
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))

                            // Description
                            Text("About Project", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(p.description, fontSize = 14.sp, color = Color.Gray, lineHeight = 22.sp)

                            Spacer(Modifier.height(24.dp))

                            // Looking For Tags
                            if (p.tags.isNotEmpty()) {
                                Text("Looking For", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    p.tags.forEach { tag ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(tag, color = BrandBlue, fontWeight = FontWeight.Medium) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = BrandBlue.copy(alpha = 0.1f)
                                            ),
                                            border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.3f))
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            // Action Button
                            Button(
                                onClick = {
                                    if (isCreator) {
                                        onManageClick()
                                    } else if (!isMember && !isPending && !isFull) {
                                        scope.launch {
                                            isSendingRequest = true
                                            // FIX: Removed 'viewModel' call and used 'repository' directly as you set it up
                                            val success = repository.requestToJoinProject(projectId)
                                            isSendingRequest = false
                                            if (success) {
                                                refreshProject()
                                                Toast.makeText(context, "Request sent!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not join (Project Full or Error)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCreator) BrandBlue
                                    else if (isFull && !isMember) Color.Red.copy(alpha = 0.7f)
                                    else if (isMember || isPending) Color.Gray
                                    else BrandBlue
                                ),
                                shape = RoundedCornerShape(12.dp),
                                enabled = isCreator || (!isMember && !isPending && !isSendingRequest && !isFull)
                            ) {
                                if (isSendingRequest) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    val buttonText = when {
                                        isCreator -> "Manage Project"
                                        isMember -> "Already a Member"
                                        isPending -> "Request Sent"
                                        isFull -> "Full Capacity"
                                        else -> "Request to Join Team"
                                    }
                                    Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}