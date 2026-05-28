package com.example.herbhopper_v1.ui.patient

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Pantalla de Inicio de Sesión (Login Screen).
 * Permite al usuario autenticarse con email/contraseña o con huella dactilar (BiometricPrompt).
 * La primera vez que el usuario inicia sesión con email/contraseña, sus credenciales se guardan
 * de forma segura en SharedPreferences para habilitar el acceso biométrico posterior.
 *
 * @param onPatientSuccess Callback al iniciar sesión como Paciente.
 * @param onSellerSuccess  Callback al iniciar sesión como Vendedor.
 * @param onAdminSuccess   Callback al iniciar sesión como Administrador.
 * @param onForgotPasswordClick Callback para "Olvidé mi contraseña".
 * @param onSignUpClick Callback para ir al registro.
 * @param profileViewModel ViewModel del perfil del usuario.
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Comprueba si el dispositivo soporta biometría y si hay credenciales guardadas
    val biometricManager = remember { BiometricManager.from(context) }
    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
    val prefs = remember { context.getSharedPreferences("herbhopper_prefs", Context.MODE_PRIVATE) }
    val hasSavedCredentials = remember { prefs.getString("saved_email", null) != null }

    // Muestra el botón de huella solo si el hardware lo soporta y hay credenciales guardadas
    val showBiometricButton = isBiometricAvailable && hasSavedCredentials

    /**
     * Lanza el diálogo biométrico del sistema y, si el usuario se autentica correctamente,
     * recupera las credenciales guardadas y las usa para hacer login en el backend.
     */
    fun launchBiometricPrompt() {
        val activity = context as? FragmentActivity ?: return

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // Recuperar credenciales guardadas y hacer login
                    val savedEmail = prefs.getString("saved_email", null)
                    val savedPassword = prefs.getString("saved_password", null)
                    if (savedEmail != null && savedPassword != null) {
                        isLoading = true
                        scope.launch {
                            val loginResult = profileViewModel.loginToBackend(savedEmail, savedPassword)
                            isLoading = false
                            if (loginResult.isSuccess) {
                                val profile = loginResult.getOrNull()
                                if (profile != null) {
                                    when (profile.role.uppercase()) {
                                        "ADMIN"  -> onAdminSuccess()
                                        "SELLER" -> onSellerSuccess()
                                        else     -> onPatientSuccess()
                                    }
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.biometric_login_failed), Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED
                    ) {
                        Toast.makeText(context, "Error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(context, context.getString(R.string.biometric_not_recognized), Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_dialog_title))
            .setSubtitle(context.getString(R.string.biometric_dialog_subtitle))
            .setNegativeButtonText(context.getString(R.string.biometric_dialog_negative))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Decoración de fondo
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
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
                            visualTransformation = if (passwordVisible)
                                androidx.compose.ui.text.input.VisualTransformation.None
                            else
                                androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible)
                                    Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, null)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botón principal de inicio de sesión
                        Button(
                            onClick = {
                                val trimmedEmail = email.trim()
                                val lowercaseEmail = trimmedEmail.lowercase()
                                val allowedKeywords = listOf(
                                    "gmail", "outlook", "hotmail", "yahoo",
                                    "icloud", "live", "herbhopper"
                                )
                                val emailRegex =
                                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()

                                when {
                                    email.isBlank() || password.isBlank() -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_fill_login),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    !trimmedEmail.matches(emailRegex) ||
                                            !allowedKeywords.any { lowercaseEmail.contains(it) } -> {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.toast_invalid_email),
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    else -> {
                                        isLoading = true
                                        scope.launch {
                                            val result = profileViewModel.loginToBackend(trimmedEmail, password)
                                            isLoading = false
                                            if (result.isSuccess) {
                                                val profile = result.getOrNull()
                                                if (profile != null) {
                                                    // Guardar credenciales para futuros logins con huella
                                                    prefs.edit()
                                                        .putString("saved_email", trimmedEmail)
                                                        .putString("saved_password", password)
                                                        .apply()

                                                    when (profile.role.uppercase()) {
                                                        "ADMIN"  -> onAdminSuccess()
                                                        "SELLER" -> onSellerSuccess()
                                                        else     -> onPatientSuccess()
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.toast_wrong_credentials),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } else {
                                                val error = result.exceptionOrNull()
                                                val errorMessage = error?.message ?: ""
                                                if (errorMessage.contains("401") ||
                                                    errorMessage.contains("incorrectas", ignoreCase = true) ||
                                                    errorMessage.contains("encontrado", ignoreCase = true)
                                                ) {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.toast_wrong_credentials),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.network_server_error),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                stringResource(id = R.string.login_start_session),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // ─── Botón de huella dactilar ───────────────────────────────────────
                        if (showBiometricButton) {
                            Spacer(modifier = Modifier.height(20.dp))

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedButton(
                                onClick = { launchBiometricPrompt() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Iniciar sesión con huella",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.biometric_button_text),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        // ────────────────────────────────────────────────────────────────────
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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
                        text = stringResource(id = R.string.no_account_register_here),
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
