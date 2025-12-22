package com.example.inoconnect.ui.participant

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.inoconnect.ui.profile.ProfileScreen
import com.example.inoconnect.ui.project_management.MyProjectScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantMainScreen(
    rootNavController: NavController,
    onEventClick: (String) -> Unit,
    onProjectClick: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    // We don't need repository here unless for logout, which is handled in Profile

    Scaffold(
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
                // --- CHANGED: REMOVED ACTIONS (No top-right icon) ---
                actions = {}
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                // --- CHANGED: Renamed 'Collab' to 'Messages' ---
                val items = listOf("Home", "My Project", "Messages", "Connect", "Profile")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.Default.List,
                    Icons.Default.Email, // Used Email icon for Messages
                    Icons.Default.Share,
                    Icons.Default.Person
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
                                2 -> "messages" // Route name changed
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
        NavHost(
            navController = bottomNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // -- 1. HOME --
            composable("home") {
                ParticipantHome(
                    onEventClick = onEventClick,
                    onProjectClick = onProjectClick,
                    onCreateProjectClick = {
                        rootNavController.navigate("create_project")
                    }
                )
            }

            // -- 2. MY PROJECT --
            composable("my_project") {
                MyProjectScreen(
                    onProjectClick = { projectId ->
                        rootNavController.navigate("project_management/$projectId")
                    }
                )
            }

            // -- 3. MESSAGES (Was Collab) --
            composable("messages") {
                // We pass rootNavController so clicking a chat opens it full screen
                MessagesScreen(navController = rootNavController)
            }

            // -- 4. CONNECT --
            composable("connect") {
                MyNetworkScreen()
            }

            // -- 5. PROFILE --
            composable("profile") {
                ProfileScreen(
                    onLogout = {
                        rootNavController.navigate("login") { popUpTo(0) }
                    }
                )
            }
        }
    }
}