package com.example.inoconnect.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.data.UserRole
import com.example.inoconnect.ui.auth.ForgotPasswordScreen
import com.example.inoconnect.ui.auth.LoginScreen
import com.example.inoconnect.ui.auth.RegisterScreen
import com.example.inoconnect.ui.chat.DirectChatScreen
import com.example.inoconnect.ui.organizer.CreateEventScreen
import com.example.inoconnect.ui.organizer.OrganizerDashboard
import com.example.inoconnect.ui.participant.CreateProjectScreen
import com.example.inoconnect.ui.participant.EventDetailScreen
import com.example.inoconnect.ui.participant.ParticipantMainScreen
import com.example.inoconnect.ui.participant.ProjectDetailScreen
import com.example.inoconnect.ui.profile.PublicProfileScreen
import com.example.inoconnect.ui.project_management.ProjectManagementScreen
import com.example.inoconnect.ui.profile.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val repository = remember { FirebaseRepository() }

    NavHost(navController = navController, startDestination = "login") {

        // ... (Login, Register, Organizer, Participant Main, Event Detail, Create Project, Project Detail routes remain unchanged) ...
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == UserRole.ORGANIZER) navController.navigate("organizer_dash")
                    else navController.navigate("participant_main")
                },
                onForgotPasswordClick = { navController.navigate("forgot_password") } ,
                onRegisterClick = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = { role ->
                    if (role == UserRole.ORGANIZER) {
                        navController.navigate("organizer_dash") { popUpTo("login") { inclusive = true } }
                    } else {
                        navController.navigate("participant_main") { popUpTo("login") { inclusive = true } }
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("organizer_dash") {
            OrganizerDashboard(
                onCreateEventClick = { navController.navigate("create_event") },
                onLogout = {
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }

        composable("create_event") {
            CreateEventScreen(
                onEventCreated = { navController.popBackStack() }
            )
        }

        composable(
            route = "participant_main?tab={tab}",
            arguments = listOf(navArgument("tab") {
                type = NavType.StringType
                defaultValue = "home"
            })
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab") ?: "home"
            ParticipantMainScreen(
                rootNavController = navController,
                initialTab = tab,
                onEventClick = { eventId -> navController.navigate("event_detail/$eventId") },
                onProjectClick = { projectId -> navController.navigate("project_detail/$projectId") }
            )
        }

        composable("event_detail/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("create_project") {
            CreateProjectScreen(
                onProjectCreated = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("project_detail/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectDetailScreen(
                projectId = projectId,
                onBackClick = { navController.popBackStack() },
                onManageClick = { navController.navigate("project_management/$projectId") }
            )
        }

        // --- UPDATED: Public Profile Route ---
        composable("public_profile/{userId}") { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("userId") ?: ""
            PublicProfileScreen(
                userId = uid,
                onBackClick = { navController.popBackStack() },
                onMessageClick = { channelId ->
                    navController.navigate("direct_chat/$channelId")
                },
                onNavigateToProfile = { targetUserId ->
                    // If the user clicks on themselves in the list, go to the "Profile" tab
                    val currentUserId = repository.currentUserId
                    if (currentUserId != null && targetUserId == currentUserId) {
                        navController.navigate("participant_main?tab=profile")
                    } else {
                        // Otherwise push a new public profile screen
                        navController.navigate("public_profile/$targetUserId")
                    }
                }
            )
        }

        // ... (Project Management, Direct Chat, Settings, Forgot Password remain unchanged) ...
        composable("project_management/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectManagementScreen(
                projectId = projectId,
                onBackClick = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    val currentUserId = repository.currentUserId
                    if (currentUserId != null && userId == currentUserId) {
                        navController.navigate("participant_main?tab=profile")
                    } else {
                        navController.navigate("public_profile/$userId")
                    }
                }
            )
        }

        composable("direct_chat/{channelId}") { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId") ?: ""
            DirectChatScreen(
                channelId = channelId,
                navController = navController,
                onProfileClick = { userId ->
                    val currentUserId = repository.currentUserId
                    if (currentUserId != null && userId == currentUserId) {
                        navController.navigate("participant_main?tab=profile")
                    } else {
                        navController.navigate("public_profile/$userId")
                    }
                }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogout = {
                    repository.logout()
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }
    }
}