package com.example.mascotasapp.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.mascotasapp.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.mascotasapp.core.ApiConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onSignIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // Configure Google Sign-In client
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleClient = remember { GoogleSignIn.getClient(context, gso) }

    // Show diagnostics info on open (spreadsheet_id from backend)
    LaunchedEffect(Unit) {
        try {
            val txt = withContext(Dispatchers.IO) {
                val url = URL(ApiConfig.BASE_URL + "/diagnostics")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $code"
                }
            }
            snackbar.showSnackbar("Diagnostics: ${'$'}txt")
        } catch (e: Exception) {
            snackbar.showSnackbar("Diagnostics error: ${'$'}{e.message}")
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        scope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val auth = FirebaseAuth.getInstance()
                // Sign in to Firebase with Google credential (off main thread)
                val user = withContext(Dispatchers.IO) {
                    Tasks.await(auth.signInWithCredential(credential)).user
                }
                if (user != null) {
                    val idToken = withContext(Dispatchers.IO) { Tasks.await(user.getIdToken(true)).token }
                    if (!idToken.isNullOrBlank()) {
                        val code = withContext(Dispatchers.IO) {
                            val url = URL(ApiConfig.BASE_URL + "/createUser")
                            val conn = (url.openConnection() as HttpURLConnection).apply {
                                requestMethod = "POST"
                                doOutput = true
                                setRequestProperty("Content-Type", "application/json")
                                setRequestProperty("Authorization", "Bearer $idToken")
                            }
                            val displayName = user.displayName ?: ""
                            val payload = """
                                {"email": ${jsonQ(user.email ?: "")}, "name": ${jsonQ(displayName)}}
                            """.trimIndent()
                            conn.outputStream.use { it.write(payload.toByteArray()) }
                            conn.responseCode.also { if (it !in 200..299) {
                                // read error to include in snackbar later
                                conn.errorStream?.bufferedReader()?.use { br -> br.readText() }?.let { err ->
                                    // show on main
                                    snackbar.showSnackbar("Auth error $it: $err")
                                }
                            }}
                        }
                        if (code in 200..299) {
                            onSignIn()
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is ApiException) {
                    snackbar.showSnackbar("Google Sign-In failed (${e.statusCode}): ${e.message}")
                } else {
                    snackbar.showSnackbar("Google Sign-In failed: ${e.message}")
                }
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Create Account", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF111827)) },
                actions = {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF10B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    scrolledContainerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF9FAFB),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Welcome to PetCare Control",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                "Create your account to start managing your pet's health",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF6B7280)
            )

            // Google Sign-In button (filled, purple)
            Button(
                onClick = {
                    isLoading = true
                    googleClient.signOut() // ensure fresh intent
                    launcher.launch(googleClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B5CF6),
                    contentColor = Color.White
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Simple 'G' badge to emulate Google icon
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color(0xFF6B7280), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(if (isLoading) "Connecting..." else "Continue with Google", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// naive JSON string escaper for simple usage in this screen
private fun jsonQ(v: String): String = buildString {
    append('"')
    v.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
    append('"')
}
