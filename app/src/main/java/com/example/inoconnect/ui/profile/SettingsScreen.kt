package com.example.inoconnect.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.inoconnect.data.FirebaseRepository
import com.example.inoconnect.ui.auth.BrandBlue
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dialog States
    var showChangePassDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // --- Unified Section ---
            Text("Account Security", fontSize = 14.sp, color = BrandBlue, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Change Password
            SettingsItemCard(
                title = "Change Password",
                icon = Icons.Default.Lock,
                onClick = { showChangePassDialog = true }
            )

            Spacer(Modifier.height(8.dp)) // Small spacer between items

            // Delete Account (Red, but in same section)
            SettingsItemCard(
                title = "Delete Account",
                icon = Icons.Default.Delete,
                color = Color.Red,
                onClick = { showDeleteAccountDialog = true }
            )
        }
    }

    // --- CHANGE PASSWORD DIALOG ---
    if (showChangePassDialog) {
        SecurityActionDialog(
            title = "Change Password",
            confirmText = "Update",
            onDismiss = { showChangePassDialog = false },
            onConfirm = { currentPass, newPass ->
                scope.launch {
                    val success = repository.reauthenticate(currentPass)
                    if (success) {
                        try {
                            repository.updatePassword(newPass)
                            Toast.makeText(context, "Password updated!", Toast.LENGTH_SHORT).show()
                            showChangePassDialog = false
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Incorrect current password", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            showNewPassField = true
        )
    }

    // --- DELETE ACCOUNT DIALOG ---
    if (showDeleteAccountDialog) {
        SecurityActionDialog(
            title = "Delete Account",
            confirmText = "DELETE PERMANENTLY",
            isDestructive = true,
            onDismiss = { showDeleteAccountDialog = false },
            onConfirm = { currentPass, _ ->
                scope.launch {
                    val success = repository.reauthenticate(currentPass)
                    if (success) {
                        try {
                            repository.deleteAccount()
                            Toast.makeText(context, "Account deleted", Toast.LENGTH_LONG).show()
                            onLogout()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Incorrect password", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            showNewPassField = false
        )
    }
}

@Composable
fun SettingsItemCard(title: String, icon: ImageVector, color: Color = Color.Black, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color)
            Spacer(Modifier.width(16.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = color)
        }
    }
}

@Composable
fun SecurityActionDialog(
    title: String,
    confirmText: String,
    isDestructive: Boolean = false,
    showNewPassField: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = currentPass,
                    onValueChange = { currentPass = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showNewPassField) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(currentPass, newPass) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDestructive) Color.Red else BrandBlue)
                    ) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}