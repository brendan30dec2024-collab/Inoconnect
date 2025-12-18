package com.example.inoconnect.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.inoconnect.data.UserRole
import com.example.inoconnect.ui.auth.LoginScreen
import com.example.inoconnect.ui.auth.RegisterScreen
import com.example.inoconnect.ui.organizer.CreateEventScreen
import com.example.inoconnect.ui.organizer.OrganizerDashboard
import com.example.inoconnect.ui.participant.CreateProjectScreen
import com.example.inoconnect.ui.participant.EventDetailScreen
import com.example.inoconnect.ui.participant.ParticipantMainScreen
import com.example.inoconnect.ui.participant.ProjectDetailScreen
import com.example.inoconnect.ui.project_management.ProjectManagementScreen // Make sure to import this!

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // --- Auth ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == UserRole.ORGANIZER) navController.navigate("organizer_dash")
                    else navController.navigate("participant_main")
                },
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

        // --- Organizer ---
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

        // --- Participant ---
        composable("participant_main") {
            ParticipantMainScreen(
                rootNavController = navController,
                onEventClick = { eventId ->
                    navController.navigate("event_detail/$eventId")
                },
                onProjectClick = { projectId ->
                    navController.navigate("project_detail/$projectId")
                }
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

        // --- Project Detail ---
        composable("project_detail/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectDetailScreen(
                projectId = projectId,
                onBackClick = { navController.popBackStack() },
                onManageClick = {
                    navController.navigate("project_management/$projectId")
                }
            )
        }

        // --- Project Management Hub ---
        composable("project_management/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProjectManagementScreen(
                projectId = projectId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}