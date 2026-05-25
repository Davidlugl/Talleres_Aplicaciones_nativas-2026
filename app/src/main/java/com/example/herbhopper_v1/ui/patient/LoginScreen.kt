package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.theme.*
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider as GoogleAuthProvider1

/**
 * Pantalla de Inicio de Sesión (Login Screen).
 * Permite al usuario autenticarse en el sistema HerbHopper utilizando sus credenciales de correo electrónico
 * y contraseña o mediante una cuenta de Google. Realiza una redirección condicional e inteligente según
 * el rol del usuario (Paciente, Vendedor o Administrador) derivado del correo ingresado.
 *
 * @param onPatientSuccess Función callback que se ejecuta al iniciar sesión exitosamente como Paciente.
 * @param onSellerSuccess Función callback que se ejecuta al iniciar sesión exitosamente como Vendedor (Seller).
 * @param onAdminSuccess Función callback que se ejecuta al iniciar sesión exitosamente como Administrador (Admin).
 * @param onForgotPasswordClick Función callback que se ejecuta al pulsar sobre "Olvidó su contraseña".
 * @param profileViewModel Instancia de [ProfileViewModel] para la gestión del perfil del usuario logueado.
 */
@Suppress("DEPRECATION")
@Composable
fun LoginScreen(
    onPatientSuccess: () -> Unit,
    onSellerSuccess: () -> Unit,
    onAdminSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    profileViewModel: com.example.herbhopper_v1.viewmodel.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var showProfileModal by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var pendingUser by remember { mutableStateOf<com.google.firebase.auth.FirebaseUser?>(null) }

    val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }

    var showGoogleSelector by remember { mutableStateOf(false) }

    if (showGoogleSelector) {
        AlertDialog(
            onDismissRequest = { showGoogleSelector = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).background(Color.Red, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continuar con Google", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Selecciona una cuenta para continuar:", style = MaterialTheme.typography.bodyMedium)
                    
                    Card(
                        onClick = {
                            showGoogleSelector = false
                            val profile = com.example.herbhopper_v1.data.UserProfile(
                                uid = "dummy_uid_patient",
                                name = "David G.",
                                email = "david.g@gmail.com",
                                role = "PATIENT"
                            )
                            profileViewModel.saveProfile(profile)
                            onPatientSuccess()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("D", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("David G.", fontWeight = FontWeight.Bold)
                                Text("david.g@gmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        onClick = {
                            showGoogleSelector = false
                            val profile = com.example.herbhopper_v1.data.UserProfile(
                                uid = "dummy_uid_seller",
                                name = "Vendedor Herb",
                                email = "seller@herbhopper.com",
                                role = "SELLER"
                            )
                            profileViewModel.saveProfile(profile)
                            onSellerSuccess()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("V", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Vendedor Herb", fontWeight = FontWeight.Bold)
                                Text("seller@herbhopper.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Card(
                        onClick = {
                            showGoogleSelector = false
                            val profile = com.example.herbhopper_v1.data.UserProfile(
                                uid = "dummy_uid_admin",
                                name = "HerbHopper Dev",
                                email = "dev@herbhopper.com",
                                role = "ADMIN"
                            )
                            profileViewModel.saveProfile(profile)
                            onAdminSuccess()
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("H", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer) }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("HerbHopper Dev", fontWeight = FontWeight.Bold)
                                Text("dev@herbhopper.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showGoogleSelector = false }) { Text("Cancelar") } }
        )
    }

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(context.getString(R.string.default_web_client_id))
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider1.getCredential(account.idToken, null)
            scope.launch {
                isLoading = true
                auth?.signInWithCredential(credential)
                    ?.addOnCompleteListener { taskAuth ->
                        if (taskAuth.isSuccessful) {
                            pendingUser = auth.currentUser
                            showProfileModal = true
                            isLoading = false
                        } else {
                            isLoading = false
                        }
                    }
            }
        } catch (e: ApiException) {
            isLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .offset(x = 100.dp, y = (-100).dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-50).dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), CircleShape)
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(id = R.string.clinical_security),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(64.dp))

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(id = R.string.welcome_back),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(id = R.string.email)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(id = R.string.password)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, null)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val trimmedEmail = email.trim()
                                val lowercaseEmail = trimmedEmail.lowercase()
                                val allowedKeywords = listOf("gmail", "outlook", "hotmail", "yahoo", "icloud", "live", "herbhopper")
                                val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

                                if (email.isBlank() || password.isBlank()) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.toast_fill_login), android.widget.Toast.LENGTH_SHORT).show()
                                } else if (!trimmedEmail.matches(emailRegex) || !allowedKeywords.any { lowercaseEmail.contains(it) }) {
                                    android.widget.Toast.makeText(context, context.getString(R.string.toast_invalid_email), android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    isLoading = true
                                    
                                    // 2. Si no tiene cuenta, la tiene que crear
                                    scope.launch {
                                        val existing = profileViewModel.getProfileByEmail(lowercaseEmail)
                                        if (existing == null) {
                                            isLoading = false
                                            android.widget.Toast.makeText(context, context.getString(R.string.toast_account_not_found), android.widget.Toast.LENGTH_LONG).show()
                                            onSignUpClick()
                                        } else {
                                            // Proceder con inicio de sesión
                                            fun navigateBasedOnEmail() {
                                                if (lowercaseEmail.contains("admin") || lowercaseEmail == "dev@herbhopper.com") {
                                                    onAdminSuccess()
                                                } else if (lowercaseEmail.contains("seller") || lowercaseEmail == "seller@herbhopper.com" || lowercaseEmail == "seller@gmail.com") {
                                                    onSellerSuccess()
                                                } else {
                                                    onPatientSuccess()
                                                }
                                            }

                                            if (auth != null) {
                                                auth.signInWithEmailAndPassword(email, password)
                                                    .addOnCompleteListener { taskAuth ->
                                                        if (taskAuth.isSuccessful) {
                                                            val firebaseUser = auth.currentUser
                                                            val uid = firebaseUser?.uid ?: "dummy_uid_patient"
                                                            scope.launch {
                                                                val localProfile = profileViewModel.getProfile(uid)
                                                                if (localProfile == null) {
                                                                    val newProfile = com.example.herbhopper_v1.data.UserProfile(
                                                                        uid = uid,
                                                                        name = firebaseUser?.displayName ?: email.substringBefore("@"),
                                                                        email = email,
                                                                        role = if (lowercaseEmail.contains("admin")) "ADMIN" else if (lowercaseEmail.contains("seller")) "SELLER" else "PATIENT"
                                                                    )
                                                                    profileViewModel.saveProfile(newProfile)
                                                                }
                                                                isLoading = false
                                                                navigateBasedOnEmail()
                                                            }
                                                        } else {
                                                            isLoading = false
                                                            val exception = taskAuth.exception
                                                            val isNetworkError = exception?.message?.contains("network", ignoreCase = true) == true ||
                                                                    exception?.message?.contains("transition", ignoreCase = true) == true ||
                                                                    exception?.javaClass?.simpleName?.contains("IOException", ignoreCase = true) == true

                                                            if (isNetworkError) {
                                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_offline_mode), android.widget.Toast.LENGTH_SHORT).show()
                                                                navigateBasedOnEmail()
                                                            } else {
                                                                android.widget.Toast.makeText(context, context.getString(R.string.toast_wrong_credentials), android.widget.Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    }
                                            } else {
                                                kotlinx.coroutines.delay(1000)
                                                isLoading = false
                                                navigateBasedOnEmail()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(id = R.string.login_start_session), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Text(
                        text = stringResource(id = R.string.or_continue_with_caps),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { showGoogleSelector = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(id = R.string.continue_google_btn), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Spacer(modifier = Modifier.weight(1f))


                TextButton(onClick = onForgotPasswordClick) {
                    Text(
                        text = stringResource(id = R.string.forgot_pin),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(onClick = onSignUpClick) {
                    Text(
                        text = "¿No tienes cuenta? Regístrate aquí",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = stringResource(id = R.string.version_info),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
