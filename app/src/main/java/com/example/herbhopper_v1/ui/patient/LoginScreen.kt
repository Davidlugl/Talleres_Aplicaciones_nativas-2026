package com.example.herbhopper_v1.ui.patient

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.UserProfile
import com.example.herbhopper_v1.data.Validators
import com.example.herbhopper_v1.ui.components.BiometricAuth
import kotlinx.coroutines.launch

/**
 * Pantalla de Inicio de Sesión.
 *
 * - Correo y contraseña contra el backend (JWT). No existe ningún acceso oculto ni credencial fija.
 * - Tras el primer ingreso exitoso se pregunta al usuario si desea activar la huella. Si acepta,
 *   el servidor entrega un token de dispositivo que se cifra con una clave AES-256 del Android
 *   Keystore que solo se desbloquea con la huella. La contraseña nunca se guarda.
 * - "Entrar con huella dactilar" descifra ese token tras la huella y lo cambia por una sesión nueva.
 */
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
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val vault = profileViewModel.biometricVault
    val isBiometricAvailable = remember { BiometricAuth.isAvailable(context) }
    var biometricEnabled by remember { mutableStateOf(vault.isEnabled()) }
    // Perfil recién autenticado que espera la decisión de activar la huella
    var pendingProfile by remember { mutableStateOf<UserProfile?>(null) }

    fun goToHome(profile: UserProfile) {
        when (profile.role.uppercase()) {
            "ADMIN" -> onAdminSuccess()
            "SELLER" -> onSellerSuccess()
            else -> onPatientSuccess()
        }
    }

    fun showToast(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()

    fun loginWithFingerprint() {
        if (activity == null) return
        BiometricAuth.authenticate(
            activity = activity,
            title = context.getString(R.string.biometric_dialog_title),
            subtitle = context.getString(R.string.biometric_dialog_subtitle),
            negativeText = context.getString(R.string.biometric_dialog_negative),
            onSuccess = {
                val deviceToken = vault.read()
                if (deviceToken == null) {
                    biometricEnabled = false
                    showToast(context.getString(R.string.biometric_login_failed))
                    return@authenticate
                }
                isLoading = true
                scope.launch {
                    val result = profileViewModel.loginWithBiometricToken(deviceToken)
                    isLoading = false
                    result.onSuccess { goToHome(it) }
                        .onFailure {
                            biometricEnabled = vault.isEnabled()
                            showToast(it.message ?: context.getString(R.string.biometric_login_failed))
                        }
                }
            },
            onError = { message -> message?.let { showToast(it) } }
        )
    }

    /** Pide un token de dispositivo, confirma con la huella y lo guarda cifrado. */
    fun enableFingerprint(profile: UserProfile) {
        if (activity == null) {
            goToHome(profile); return
        }
        isLoading = true
        scope.launch {
            val tokenResult = profileViewModel.requestDeviceToken()
            isLoading = false
            val deviceToken = tokenResult.getOrNull()
            if (deviceToken == null) {
                showToast(tokenResult.exceptionOrNull()?.message ?: context.getString(R.string.network_server_error))
                goToHome(profile)
                return@launch
            }
            BiometricAuth.authenticate(
                activity = activity,
                title = context.getString(R.string.biometric_enable_title),
                subtitle = context.getString(R.string.biometric_dialog_subtitle),
                negativeText = context.getString(R.string.biometric_dialog_negative),
                onSuccess = {
                    try {
                        vault.store(profile.email, deviceToken)
                        showToast(context.getString(R.string.biometric_enabled_toast))
                    } catch (e: Exception) {
                        vault.disable()
                        showToast(context.getString(R.string.biometric_login_failed))
                    }
                    goToHome(profile)
                },
                onError = { goToHome(profile) }
            )
        }
    }

    pendingProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingProfile = null; goToHome(profile) },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null) },
            title = { Text(stringResource(id = R.string.biometric_enable_title)) },
            text = { Text(stringResource(id = R.string.biometric_enable_message)) },
            confirmButton = {
                Button(onClick = { pendingProfile = null; enableFingerprint(profile) }) {
                    Text(stringResource(id = R.string.biometric_enable_accept))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingProfile = null; goToHome(profile) }) {
                    Text(stringResource(id = R.string.biometric_enable_decline))
                }
            }
        )
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
                                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, null)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Botón principal de inicio de sesión
                        Button(
                            onClick = {
                                val trimmedEmail = email.trim().lowercase()
                                when {
                                    trimmedEmail.isBlank() || password.isBlank() ->
                                        showToast(context.getString(R.string.toast_fill_login))
                                    !Validators.isValidEmail(trimmedEmail) ->
                                        showToast(context.getString(R.string.toast_invalid_email))
                                    else -> {
                                        isLoading = true
                                        scope.launch {
                                            val result = profileViewModel.loginToBackend(trimmedEmail, password)
                                            isLoading = false
                                            result.onSuccess { profile ->
                                                password = ""
                                                if (isBiometricAvailable && !vault.isEnabled()) {
                                                    pendingProfile = profile
                                                } else {
                                                    goToHome(profile)
                                                }
                                            }.onFailure {
                                                showToast(it.message ?: context.getString(R.string.toast_wrong_credentials))
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading,
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
                        if (isBiometricAvailable && biometricEnabled) {
                            Spacer(modifier = Modifier.height(20.dp))

                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedButton(
                                onClick = { loginWithFingerprint() },
                                enabled = !isLoading,
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
                                    contentDescription = null,
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
                            vault.enabledEmail()?.let { savedEmail ->
                                Text(
                                    text = savedEmail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
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
