package com.example.inoconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.inoconnect.navigation.AppNavigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    // Permission launcher for Android 13+ Notification Permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("FCM", "Notification permission granted")
        } else {
            Log.w("FCM", "Notification permission denied")
        }
    }

    // Listener to detect when a user logs in or out
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Initialize the Auth Listener (defines what happens when auth state changes)
        initializeAuthListener()

        // 2. Ask for Permission (Android 13+)
        askNotificationPermission()

        setContent {
            AppNavigation()
        }
    }

    override fun onStart() {
        super.onStart()
        // Start listening for auth changes (e.g., user logs in)
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        // Stop listening to avoid memory leaks
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
    }

    private fun initializeAuthListener() {
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                // User is signed in, safe to update the token now
                updateFcmToken(user.uid)
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun updateFcmToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Update in Firestore safely
            if (token != null) {
                FirebaseFirestore.getInstance().collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener {
                        Log.d("FCM", "Token updated successfully for user: $userId")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Error updating token in Firestore", e)
                    }
            }
        }
    }
}