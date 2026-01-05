package com.example.inoconnect.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inoconnect.R
import com.example.inoconnect.data.FirebaseRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.launch
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    val repository = remember { FirebaseRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // --- 1. Google Sign-In Launcher ---
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    scope.launch {
                        isLoading = true
                        val role = repository.signInWithGoogle(idToken)
                        isLoading = false
                        if (role != null) {
                            onLoginSuccess(role)
                        } else {
                            Toast.makeText(context, "Google Sign-In Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google Sign-In Error: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchGoogleSignIn = {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    // --- 2. GitHub Trigger ---
    val launchGithubLogin = {
        val provider = OAuthProvider.newBuilder("github.com")
        provider.addCustomParameter("allow_signup", "true")

        val activity = context as? Activity
        if (activity != null) {
            auth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener {
                    scope.launch {
                        isLoading = true
                        val role = repository.onSignInWithGithubSuccess()
                        isLoading = false
                        if (role != null) {
                            onLoginSuccess(role)
                        } else {
                            Toast.makeText(context, "GitHub Account Setup Failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "GitHub Login Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // --- 3. Facebook Setup ---
    val loginManager = LoginManager.getInstance()
    val callbackManager = remember { CallbackManager.Factory.create() }

    DisposableEffect(Unit) {
        val callback = object : FacebookCallback<LoginResult> {
            override fun onCancel() { }
            override fun onError(error: FacebookException) {
                Toast.makeText(context, "Facebook Login Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
            override fun onSuccess(result: LoginResult) {
                scope.launch {
                    isLoading = true
                    val role = repository.signInWithFacebook(result.accessToken)
                    isLoading = false
                    if (role != null) {
                        onLoginSuccess(role)
                    } else {
                        Toast.makeText(context, "Facebook Auth Failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        loginManager.registerCallback(callbackManager, callback)
        onDispose { loginManager.unregisterCallback(callbackManager) }
    }

    val launchFacebookLogin = {
        loginManager.logInWithReadPermissions(
            context as Activity,
            listOf("email", "public_profile")
        )
    }

    // --- UI CONTENT ---
    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        // Blue Header
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // FIXED: Added Scroll State
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(140.dp))

            // Logo Placeholder
            Surface(
                shape = CircleShape, color = Color.White, shadowElevation = 8.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person, contentDescription = "Logo",
                    tint = Color.Black, modifier = Modifier.padding(16.dp).fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(0.85f).shadow(10.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Welcome", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Sign in to your account", color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    StyledTextField(value = email, onValueChange = { email = it }, label = "Email", icon = Icons.Default.AccountCircle)
                    Spacer(modifier = Modifier.height(16.dp))
                    StyledTextField(value = password, onValueChange = { password = it }, label = "Password", icon = Icons.Default.Lock, isPassword = true)

                    // --- FORGOT PASSWORD LINK ---
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(onClick = onForgotPasswordClick) {
                            Text("Forgot Password?", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator(color = BrandBlue)
                    } else {
                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        isLoading = true
                                        val role = repository.loginUser(email, password)
                                        isLoading = false
                                        if (role != null) onLoginSuccess(role)
                                        else Toast.makeText(context, "Login Failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Or sign in with", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Surface(
                            modifier = Modifier.size(50.dp).clickable { launchGoogleSignIn() },
                            shape = CircleShape, color = Color.White, shadowElevation = 4.dp
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.google_color_icon), contentDescription = "Google", modifier = Modifier.fillMaxSize())
                            }
                        }

                        Surface(
                            modifier = Modifier.size(50.dp).clickable { launchFacebookLogin() },
                            shape = CircleShape, color = Color.White, shadowElevation = 4.dp
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.facebook), contentDescription = "Facebook", modifier = Modifier.fillMaxSize())
                            }
                        }

                        Surface(
                            modifier = Modifier.size(50.dp).clickable { launchGithubLogin() },
                            shape = CircleShape, color = Color.White, shadowElevation = 4.dp
                        ) {
                            Box(modifier = Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
                                Image(painter = painterResource(id = R.drawable.github_icon), contentDescription = "Github", modifier = Modifier.fillMaxSize())
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Don't have an account?", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            text = " Register", color = BrandBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                            modifier = Modifier.clickable { onRegisterClick() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp)) // Bottom padding for scroll
        }
    }
}