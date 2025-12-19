package com.example.inoconnect.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.User
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.auth.LightGrayInput

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PublicProfileScreen(
    userId: String,
    onBackClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val uriHandler = LocalUriHandler.current

    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isConnected by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        user = repository.getUserById(userId)
        isLoading = false
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandBlue)
            }
        } else if (user == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("User not found", color = Color.Gray)
            }
        } else {
            val u = user!!

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
                    .background(Color(0xFFF8F9FA))
                    .verticalScroll(rememberScrollState())
            ) {
                // ================= HEADER =================
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(BrandBlue))

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(top = 40.dp, start = 16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = u.profileImageUrl.ifEmpty { null },
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White, CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(u.username, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            u.headline.ifEmpty { "Student" },
                            fontSize = 14.sp, color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { isConnected = !isConnected },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) Color.Gray else BrandBlue
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(if (isConnected) Icons.Default.Check else Icons.Default.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isConnected) "Connected" else "Connect")
                        }
                    }
                }

                // ================= BODY =================
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(Modifier.height(16.dp))
                    ProfileStatsGrid(u.connectionsCount, u.followingCount, u.projectsCompleted)

                    Spacer(Modifier.height(16.dp))

                    ProfileSectionCard(title = "Academic Biodata") {
                        InfoRow(Icons.Default.Home, "Faculty", u.faculty)
                        InfoRow(Icons.Default.List, "Course", u.course)
                        InfoRow(Icons.Default.DateRange, "Year", u.yearOfStudy)
                        HorizontalDivider(
                            Modifier.padding(vertical = 12.dp),
                            color = LightGrayInput
                        )
                        Text("About", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(u.bio.ifEmpty { "No bio." }, color = Color.DarkGray)
                    }

                    if (u.skills.isNotEmpty()) {
                        ProfileSectionCard(title = "Skills") {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                u.skills.forEach { skill ->
                                    SuggestionChip(onClick = {}, label = { Text(skill) })
                                }
                            }
                        }
                    }

                    ProfileSectionCard(title = "Contact Information") {
                        if (isConnected) {
                            ContactRow(Icons.Default.Email, u.email)
                            if (u.phoneNumber.isNotEmpty()) ContactRow(
                                Icons.Default.Phone,
                                u.phoneNumber
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Lock, null, tint = Color.Gray)
                                Text(
                                    "Connect to view contacts",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}