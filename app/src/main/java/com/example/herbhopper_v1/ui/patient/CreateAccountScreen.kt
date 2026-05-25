package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R

/**
 * Pantalla para la Creación de Cuentas (Registro de nuevos pacientes).
 * Permite al usuario registrar sus datos personales como nombre completo, correo electrónico,
 * teléfono y contraseña. Incluye un modal (AlertDialog) para la selección rápida de perfiles de registro
 * (Paciente, Vendedor, Administrador), registro alternativo mediante Google y una sección de garantía
 * de pureza clínica.
 *
 * @param onSignUpSuccess Función callback ejecutada cuando el registro es exitoso.
 * @param onLoginClick Función callback para navegar a la pantalla de inicio de sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountScreen(
    onSignUpSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    profileViewModel: com.example.herbhopper_v1.viewmodel.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    var showProfileModal by remember { mutableStateOf(false) }

    // Solución al Issue 1: Declarar variables de estado local para los campos del formulario
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (showProfileModal) {
        AlertDialog(
            onDismissRequest = { showProfileModal = false },
            title = { Text(stringResource(id = R.string.select_profile_register_title), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showProfileModal = false }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(id = R.string.role_patient))
                    }
                    Button(onClick = { showProfileModal = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Text(stringResource(id = R.string.role_seller))
                    }
                    Button(onClick = { showProfileModal = false }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text(stringResource(id = R.string.role_admin))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProfileModal = false }) { Text(stringResource(id = R.string.close_btn)) } }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-1).sp
                        )
                    }
                },
                actions = {},
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(id = R.string.create_account),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.create_account_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Solución al Issue 1: Vincular los campos de texto a variables de estado mutable
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text(stringResource(id = R.string.full_name)) },
                placeholder = { Text(stringResource(id = R.string.john_doe)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(id = R.string.email)) },
                placeholder = { Text(stringResource(id = R.string.john_example_email)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = { input -> phone = input.filter { it.isDigit() }.take(10) },
                    label = { Text(stringResource(id = R.string.phone)) },
                    placeholder = { Text(stringResource(id = R.string.phone_placeholder)) },
                    prefix = { Text(stringResource(id = R.string.colombia_prefix)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(id = R.string.password)) },
                    placeholder = { Text(stringResource(id = R.string.dummy_password)) },
                    modifier = Modifier.weight(1f),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    val lowercaseEmail = trimmedEmail.lowercase()
                    val allowedKeywords = listOf("gmail", "outlook", "hotmail", "yahoo", "icloud", "live", "herbhopper")
                    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
                    val cleanPhone = phone.filter { it.isDigit() }.take(10)

                    if (fullName.isBlank() || email.isBlank()) {
                        Toast.makeText(context, context.getString(R.string.toast_fill_name_email), Toast.LENGTH_SHORT).show()
                    } else if (cleanPhone.length != 10) {
                        Toast.makeText(context, context.getString(R.string.toast_invalid_phone), Toast.LENGTH_LONG).show()
                    } else if (!trimmedEmail.matches(emailRegex) || !allowedKeywords.any { lowercaseEmail.contains(it) }) {
                        Toast.makeText(context, context.getString(R.string.toast_invalid_email), Toast.LENGTH_LONG).show()
                    } else {
                        val prefixVal = context.getString(R.string.colombia_prefix)
                        val formattedPhone = "$prefixVal$cleanPhone"
                        val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
                        if (auth != null && password.isNotBlank()) {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    val uid = auth.currentUser?.uid ?: "dummy_uid_patient"
                                    val newProfile = com.example.herbhopper_v1.data.UserProfile(
                                        uid = uid,
                                        name = fullName,
                                        email = email,
                                        phone = formattedPhone,
                                        role = "PATIENT"
                                    )
                                    profileViewModel.saveProfile(newProfile)
                                    onSignUpSuccess()
                                }
                                .addOnFailureListener {
                                    val newProfile = com.example.herbhopper_v1.data.UserProfile(
                                        uid = "dummy_uid_patient",
                                        name = fullName,
                                        email = email,
                                        phone = formattedPhone,
                                        role = "PATIENT"
                                    )
                                    profileViewModel.saveProfile(newProfile)
                                    onSignUpSuccess()
                                }
                        } else {
                            val newProfile = com.example.herbhopper_v1.data.UserProfile(
                                uid = "dummy_uid_patient",
                                name = fullName,
                                email = email,
                                phone = formattedPhone,
                                role = "PATIENT"
                            )
                            profileViewModel.saveProfile(newProfile)
                            onSignUpSuccess()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(id = R.string.create_account), fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Text(
                    stringResource(id = R.string.or_continue_with),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    val profile = com.example.herbhopper_v1.data.UserProfile(
                        uid = "dummy_uid_patient",
                        name = "David G.",
                        email = "david.g@gmail.com",
                        role = "PATIENT"
                    )
                    profileViewModel.saveProfile(profile)
                    onSignUpSuccess()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.outline.copy(0.2f), MaterialTheme.colorScheme.outline.copy(0.2f))))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).background(Color.Red, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(id = R.string.signup_google), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(
                onClick = onLoginClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(id = R.string.already_have_account),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            TextButton(
                onClick = { showProfileModal = true },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = stringResource(id = R.string.change_register_profile),
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(64.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Science, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(id = R.string.purity_guaranteed),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(id = R.string.purity_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
