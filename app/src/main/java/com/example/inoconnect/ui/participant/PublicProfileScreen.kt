package com.example.inoconnect.ui.participant

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
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
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        user = repository.getUserById(userId)
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Header
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
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

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BrandBlue)
        } else if (user == null) {
            Text("User not found", modifier = Modifier.align(Alignment.Center))
        } else {
            val u = user!!
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // Avatar
                if (u.profileImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = u.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(modifier = Modifier.size(120.dp), shape = CircleShape, color = LightGrayInput) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(u.username, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(u.email, fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(30.dp))

                // Bio
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = LightGrayInput)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("About", fontWeight = FontWeight.Bold, color = BrandBlue)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(if(u.bio.isNotEmpty()) u.bio else "No bio available.")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Skills
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = LightGrayInput)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Skills", fontWeight = FontWeight.Bold, color = BrandBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (u.skills.isEmpty()) {
                            Text("No skills listed.")
                        } else {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                u.skills.forEach { skill ->
                                    SuggestionChip(onClick = {}, label = { Text(skill) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}