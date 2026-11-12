package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.SegmentedPillTabs
import com.example.ui.theme.NasAmber
import com.example.ui.theme.NasAmberSoft
import com.example.ui.theme.NasBorder
import com.example.ui.theme.NasCanvas
import com.example.ui.theme.NasCrimson
import com.example.ui.theme.NasCrimsonSoft
import com.example.ui.theme.NasEmerald
import com.example.ui.theme.NasEmeraldSoft
import com.example.ui.theme.NasIndigo
import com.example.ui.theme.NasIndigoDark
import com.example.ui.theme.NasIndigoSoft
import com.example.ui.theme.NasSlate600
import com.example.ui.theme.NasSlate800
import com.example.ui.theme.NasSurfaceVariant
import com.example.ui.theme.NasWhite
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PendingRegistrationStore {
    var pendingDisplayName: String? = null
    var pendingStudioRole: String? = null
    var pendingInitialStudioName: String? = null
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    val activityContext = context.findActivity() ?: context

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
    val activityContext = context.findActivity() ?: context

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received from Credential Manager.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google credentials found on device: ${e.message}", e)
            onAuthError("No Google account found on this device. Please sign into a Google account in device Settings and try again.")
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    onAuthCancelled: () -> Unit = {},
    buttonText: String = "Sign in with Google",
    loadingText: String = "Authenticating with Google...",
    onBeforeLaunch: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            onBeforeLaunch()
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { errorMsg ->
                    isLoading = false
                    onAuthError(errorMsg)
                },
                scope = coroutineScope,
                onAuthCancelled = {
                    isLoading = false
                    onAuthCancelled()
                }
            )
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NasEmerald,
            contentColor = Color.White
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = loadingText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        } else {
            GoogleLogoBadge()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoogleLogoBadge() {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = 3.2.dp.toPx()
            val arcSize = Size(size.width, size.height)
            drawArc(
                color = Color(0xFFEA4335),
                startAngle = -145f,
                sweepAngle = 95f,
                useCenter = false,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFFBBC05),
                startAngle = 140f,
                sweepAngle = 75f,
                useCenter = false,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF34A853),
                startAngle = 45f,
                sweepAngle = 95f,
                useCenter = false,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF4285F4),
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawLine(
                color = Color(0xFF4285F4),
                start = Offset(size.width * 0.52f, size.height * 0.5f),
                end = Offset(size.width * 0.95f, size.height * 0.5f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var selectedAuthMode by remember { mutableIntStateOf(0) } // 0: Sign In, 1: Create Account
    var fullNameInput by remember { mutableStateOf("") }
    var studioRoleInput by remember { mutableStateOf("Studio Owner & Photographer") }
    var initialStudioNameInput by remember { mutableStateOf("") }
    var isAutoChecking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var visibleStep by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
        } else {
            isAutoChecking = true
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isAutoChecking = false
                    onAuthSuccess()
                },
                onUnauthenticated = {
                    isAutoChecking = false
                },
                scope = coroutineScope
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NasCanvas)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("auth_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Stagger 1: Split Hero Card
            AnimatedVisibility(
                visible = visibleStep >= 1,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 30 }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(NasIndigo, NasIndigoDark)
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Verified Cloud Sync",
                                        tint = NasEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "SECURE STUDIO CLOUD WORKSPACE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "NAS Finance Studio Icon",
                                        tint = NasIndigo,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "NAS Finance",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Personal Accounting, CRM & AI Future Forecasting for Photography Studios",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        AuthFeatureRow(
                            icon = Icons.Default.CameraAlt,
                            iconBg = NasIndigoSoft,
                            iconTint = NasIndigo,
                            title = "Multi-Business Studio Profiles",
                            subtitle = "Switch between wedding, portrait, and commercial brands with preset per-picture rates."
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AuthFeatureRow(
                            icon = Icons.Default.AutoAwesome,
                            iconBg = NasEmeraldSoft,
                            iconTint = NasEmerald,
                            title = "AI Receipt Scanner & AI Future Advisor",
                            subtitle = "Scan invoices in seconds and forecast next month's studio revenue with Gemini AI."
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AuthFeatureRow(
                            icon = Icons.Default.PeopleAlt,
                            iconBg = NasAmberSoft,
                            iconTint = NasAmber,
                            title = "Client CRM & Per-Picture Accounting",
                            subtitle = "Store client phone numbers, communication logs, and Cash vs. Bank Transfer reports."
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stagger 2: Sign In / Create Account Card
            AnimatedVisibility(
                visible = visibleStep >= 2,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 30 }
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = NasWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Sign In vs Create Account Mode Tabs
                        SegmentedPillTabs(
                            options = listOf("Sign In", "Create Account"),
                            selectedIndex = selectedAuthMode,
                            onSelect = {
                                selectedAuthMode = it
                                errorMessage = null
                                infoMessage = null
                            },
                            modifier = Modifier.testTag("auth_mode_tabs")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        AnimatedContent(
                            targetState = selectedAuthMode,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            },
                            label = "auth_mode_content"
                        ) { mode ->
                            if (mode == 0) {
                                // Sign In Mode
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Login,
                                            contentDescription = "Sign In",
                                            tint = NasIndigo,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Welcome Back — Sign In",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = NasSlate800
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Sign in with your Google account to restore your photography studios, client CRM, and monthly financial reports.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NasSlate600,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                // Create Account Mode
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Create Account",
                                            tint = NasEmerald,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Create Studio Account",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = NasSlate800
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Set up your photographer profile and connect your Google identity to create your secure cloud studio account.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NasSlate600,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = fullNameInput,
                                        onValueChange = { fullNameInput = it },
                                        label = { Text("Your Full Name / Photographer Name") },
                                        placeholder = { Text("e.g., Khalil Nassa") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_full_name_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = studioRoleInput,
                                        onValueChange = { studioRoleInput = it },
                                        label = { Text("Your Studio Role") },
                                        placeholder = { Text("e.g., Studio Owner & Lead Photographer") },
                                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_role_input")
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = initialStudioNameInput,
                                        onValueChange = { initialStudioNameInput = it },
                                        label = { Text("First Photography Studio Name (Optional)") },
                                        placeholder = { Text("e.g., GLOD PHOTO") },
                                        leadingIcon = { Icon(Icons.Default.BusinessCenter, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("signup_studio_name_input")
                                    )
                                }
                            }
                        }

                        if (isAutoChecking) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = NasSurfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = NasIndigo,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Checking saved Google credentials...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NasSlate600
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = NasCrimsonSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_error_banner")
                            ) {
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasCrimson,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (infoMessage != null && errorMessage == null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = NasIndigoSoft,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_info_banner")
                            ) {
                                Text(
                                    text = infoMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NasIndigo,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        GoogleSignInButton(
                            buttonText = if (selectedAuthMode == 1) {
                                "Create Account with Google"
                            } else {
                                "Sign in with Google"
                            },
                            loadingText = if (selectedAuthMode == 1) {
                                "Creating Account with Google..."
                            } else {
                                "Signing in with Google..."
                            },
                            onBeforeLaunch = {
                                if (selectedAuthMode == 1) {
                                    PendingRegistrationStore.pendingDisplayName = fullNameInput.trim().takeIf { it.isNotEmpty() }
                                    PendingRegistrationStore.pendingStudioRole = studioRoleInput.trim().takeIf { it.isNotEmpty() }
                                    PendingRegistrationStore.pendingInitialStudioName = initialStudioNameInput.trim().takeIf { it.isNotEmpty() }
                                } else {
                                    PendingRegistrationStore.pendingDisplayName = null
                                    PendingRegistrationStore.pendingStudioRole = null
                                    PendingRegistrationStore.pendingInitialStudioName = null
                                }
                            },
                            onAuthSuccess = {
                                errorMessage = null
                                infoMessage = null
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                infoMessage = null
                                errorMessage = msg
                            },
                            onAuthCancelled = {
                                errorMessage = null
                                infoMessage = "Authentication was cancelled. Tap '${if (selectedAuthMode == 1) "Create Account with Google" else "Sign in with Google"}' to select an account."
                            }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                infoMessage = null
                                if (selectedAuthMode == 1) {
                                    PendingRegistrationStore.pendingDisplayName = fullNameInput.trim().takeIf { it.isNotEmpty() }
                                    PendingRegistrationStore.pendingStudioRole = studioRoleInput.trim().takeIf { it.isNotEmpty() }
                                    PendingRegistrationStore.pendingInitialStudioName = initialStudioNameInput.trim().takeIf { it.isNotEmpty() }
                                }
                                coroutineScope.launch {
                                    try {
                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                    } catch (e: Exception) {
                                        Log.w("Auth", "Clear credential state ignored: ${e.message}")
                                    }
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = onAuthSuccess,
                                        onAuthError = { msg -> errorMessage = msg },
                                        scope = coroutineScope,
                                        onAuthCancelled = {
                                            infoMessage = "Account selection dismissed. Tap the Google button when ready."
                                        }
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("switch_google_account_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = NasIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Choose a Different Google Account",
                                style = MaterialTheme.typography.labelLarge,
                                color = NasIndigo
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = NasBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrustBadgeItem(
                                icon = Icons.Default.CheckCircle,
                                label = "Google OAuth 2.0"
                            )
                            TrustBadgeItem(
                                icon = Icons.Default.CloudDone,
                                label = "Firebase Auth Sync"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stagger 3: Trust Indicators Footer
            AnimatedVisibility(
                visible = visibleStep >= 3,
                enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { 20 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NasBorder, RoundedCornerShape(16.dp))
                        .background(NasWhite.copy(alpha = 0.7f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrustBadgeItem(
                        icon = Icons.Default.Lock,
                        label = "Zero-Trust Cloud Isolation"
                    )
                    TrustBadgeItem(
                        icon = Icons.Default.VerifiedUser,
                        label = "Real-Time Ledger Sync"
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthFeatureRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = NasSlate800
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NasSlate600
            )
        }
    }
}

@Composable
private fun TrustBadgeItem(
    icon: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = NasEmerald,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NasSlate600
        )
    }
}
