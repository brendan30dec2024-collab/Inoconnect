package com.example.inoconnect.ui.participant

import androidx.compose.foundation.layout.Box // <--- WAS MISSING
import androidx.compose.foundation.layout.fillMaxSize // <--- USEFUL TO HAVE
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment // <--- ADDED FOR CENTER ALIGNMENT
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.ui.auth.BrandBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantMainScreen(
    rootNavController: NavController,
    onEventClick: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    val repository = remember { FirebaseRepository() }

    Scaffold(
        // 1. Top Bar with "InnoConnect" and Message Button
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "InnoConnect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = BrandBlue
                    )
                },
                actions = {
                    // Message Button with Notification Badge
                    IconButton(onClick = { /* TODO: Navigate to Messages */ }) {
                        BadgedBox(
                            badge = {
                                Badge { Text("3") } // Fake notification count
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Messages",
                                tint = Color.Gray
                            )
                        }
                    }
                }
            )
        },
        // 2. Bottom Navigation with 5 Sections
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                // Define the 5 Tabs
                val items = listOf("Home", "My Project", "Collab", "Connect", "Profile")
                val icons = listOf(
                    Icons.Default.Home,     // Home
                    Icons.Default.List,     // My Project (List view)
                    Icons.Default.ThumbUp,  // Collab (Like/Teamwork)
                    Icons.Default.Share,    // Connect (Network)
                    Icons.Default.Person    // Profile
                )

                var selectedItem by remember { mutableIntStateOf(0) }

                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item, fontSize = 10.sp) },
                        selected = selectedItem == index,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandBlue,
                            selectedTextColor = BrandBlue,
                            indicatorColor = BrandBlue.copy(alpha = 0.1f)
                        ),
                        onClick = {
                            selectedItem = index
                            val route = when(index) {
                                0 -> "home"
                                1 -> "my_project"
                                2 -> "collab"
                                3 -> "connect"
                                else -> "profile"
                            }

                            bottomNavController.navigate(route) {
                                popUpTo(bottomNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 3. Navigation Host for the Tabs
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // -- HOME --
            composable("home") {
                ParticipantHome(
                    onEventClick = onEventClick,
                    onCreateProjectClick = {
                        rootNavController.navigate("create_project")
                    }
                )
            }

            // -- MY PROJECT --
            composable("my_project") {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("My Projects Page (Coming Soon)")
                }
            }

            // -- COLLAB --
            composable("collab") {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Collaboration Hub (Coming Soon)")
                }
            }

            // -- CONNECT --
            composable("connect") {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Connect / Networking Page (Coming Soon)")
                }
            }

            // -- PROFILE --
            composable("profile") {
                // Simple Profile screen with Logout
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = {
                            repository.logout()
                            rootNavController.navigate("login") {
                                popUpTo(0)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Logout")
                    }
                }
            }
        }
    }
}