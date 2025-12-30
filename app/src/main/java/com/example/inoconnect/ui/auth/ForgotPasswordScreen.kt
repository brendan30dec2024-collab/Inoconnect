package com.example.inoconnect.ui.auth

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock // Changed from LockReset to Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.data.FirebaseRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Countdown State
    var countdown by remember { mutableIntStateOf(0) }
    val canResend by remember { derivedStateOf { countdown == 0 } }

    // Timer Logic
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    // Explicitly define this as a function returning Unit to satisfy the compiler
    val sendResetLink: () -> Unit = {
        if (email.isBlank()) {
            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
        } else {
            scope.launch {
                isLoading = true
                val success = repository.sendPasswordReset(email)
                isLoading = false
                if (success) {
                    Toast.makeText(context, "Reset link sent to $email", Toast.LENGTH_LONG).show()
                    countdown = 30 // Start 30s countdown
                } else {
                    Toast.makeText(context, "Failed to send. Check email or network.", Toast.LENGTH_SHORT).show()
                }
            }
            // Explicitly return Unit ensures the lambda signature matches what Button expects
            Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 1. Blue Header
        Canvas(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height - 100)
                quadraticBezierTo(size.width / 2, size.height + 50, 0f, size.height - 100)
                close()
            }
            drawPath(path = path, color = BrandBlue)
        }

        // 2. Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // 3. Main Content
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            // Icon Header
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.size(100.dp)
            ) {
                // FIXED: Changed Icons.Default.LockReset to Icons.Default.Lock
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Forgot Password",
                    tint = BrandBlue,
                    modifier = Modifier.padding(20.dp).fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text("Forgot Password?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(
                "Enter your email address to receive a\npassword reset link.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email Input
            Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                StyledTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    icon = Icons.Default.Email
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BrandBlue)
                    }
                } else {
                    // MAIN SEND BUTTON
                    Button(
                        onClick = sendResetLink,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (canResend) BrandBlue else Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = canResend
                    ) {
                        if (countdown > 0) {
                            Text("Wait ${countdown}s to resend", fontSize = 16.sp)
                        } else {
                            Text("Send Reset Link", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "Email not received?" Section
            if (countdown > 0) {
                Text("Email not received?", color = Color.Gray, fontSize = 14.sp)
                Text(
                    text = "You can resend in $countdown seconds",
                    color = BrandBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}