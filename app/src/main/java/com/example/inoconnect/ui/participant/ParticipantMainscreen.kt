package com.example.inoconnect.ui.participant

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List // --- CHANGED: Updated Import
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.inoconnect.ui.auth.BrandBlue
import com.example.inoconnect.ui.chat.MessagesScreen
import com.example.inoconnect.ui.profile.ProfileScreen
import com.example.inoconnect.ui.project_management.MyProjectScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantMainScreen(
    rootNavController: NavController,
    onEventClick: (String) -> Unit,
    onProjectClick: (String) -> Unit,
    initialTab: String = "home"
) {
    val bottomNavController = rememberNavController()

    // Observe current route to toggle TopBar visibility
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: initialTab

    // Determine initial index
    val initialIndex = remember(initialTab) {
        when(initialTab) {
            "home" -> 0
            "my_project" -> 1
            "messages" -> 2
            "connect" -> 3
            "profile" -> 4
            else -> 0
        }
    }

    Scaffold(
        topBar = {
            // Only show the global TopAppBar if NOT on the Home screen
            if (currentRoute != "home") {
                TopAppBar(
                    title = {
                        Text(
                            text = "InnoConnect",
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = BrandBlue
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val items = listOf("Home", "My Project", "Messages", "Connect", "Profile")
                val icons = listOf(
                    Icons.Default.Home,
                    Icons.AutoMirrored.Filled.List, // --- CHANGED: Updated Icon
                    Icons.Default.Email,
                    Icons.Default.Share,
                    Icons.Default.Person
                )

                var selectedItem by remember { mutableIntStateOf(initialIndex) }

                // Sync selectedItem with route changes
                LaunchedEffect(currentRoute) {
                    when(currentRoute) {
                        "home" -> selectedItem = 0
                        "my_project" -> selectedItem = 1
                        "messages" -> selectedItem = 2
                        "connect" -> selectedItem = 3
                        "profile" -> selectedItem = 4
                    }
                }

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
                                2 -> "messages"
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
            startDestination = initialTab,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                ParticipantHome(
                    onEventClick = onEventClick,
                    onProjectClick = onProjectClick,
                    onCreateProjectClick = { rootNavController.navigate("create_project") }
                )
            }
            composable("my_project") {
                MyProjectScreen(onProjectClick = { projectId -> rootNavController.navigate("project_management/$projectId") })
            }
            composable("messages") { MessagesScreen(navController = rootNavController) }
            composable("connect") {
                MyNetworkScreen(onUserClick = { userId -> rootNavController.navigate("public_profile/$userId") })
            }
            composable("profile") {
                ProfileScreen(
                    onLogout = { rootNavController.navigate("login") { popUpTo(0) } },
                    onSettingsClick = { rootNavController.navigate("settings") }
                )
            }
        }
    }
}