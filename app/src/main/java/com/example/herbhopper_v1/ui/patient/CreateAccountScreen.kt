package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    var selectedRole by remember { mutableStateOf("PATIENT") }

    // Solución al Issue 1: Declarar variables de estado local para los campos del formulario
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.create_account),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.weight(1f)
                )
                
                val roleDisplay = when(selectedRole) {
                    "SELLER" -> "Vendedor"
                    "ADMIN" -> "Admin"
                    else -> "Paciente"
                }
                val roleColor = when(selectedRole) {
                    "SELLER" -> MaterialTheme.colorScheme.secondary
                    "ADMIN" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                }
                
                Surface(
                    color = roleColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, roleColor)
                ) {
                    Text(
                        text = roleDisplay,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = roleColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.create_account_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "¿Cómo deseas registrarte?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Option 1: Comprador (PATIENT)
                val isPatientSelected = selectedRole == "PATIENT"
                val buyerColor = MaterialTheme.colorScheme.primary
                Surface(
                    onClick = { selectedRole = "PATIENT" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isPatientSelected) buyerColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isPatientSelected) 2.dp else 1.dp,
                        color = if (isPatientSelected) buyerColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = if (isPatientSelected) buyerColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Comprador",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isPatientSelected) buyerColor else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Adquiere productos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                // Option 2: Vendedor (SELLER)
                val isSellerSelected = selectedRole == "SELLER"
                val sellerColor = MaterialTheme.colorScheme.secondary
                Surface(
                    onClick = { selectedRole = "SELLER" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSellerSelected) sellerColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSellerSelected) 2.dp else 1.dp,
                        color = if (isSellerSelected) sellerColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = if (isSellerSelected) sellerColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Vendedor",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSellerSelected) sellerColor else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gestiona inventario",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

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
                        scope.launch {
                            val result = profileViewModel.registerToBackend(
                                name = fullName,
                                email = lowercaseEmail,
                                password = password,
                                role = selectedRole
                            )
                            if (result.isSuccess) {
                                val profile = result.getOrNull()
                                if (profile != null) {
                                    val updatedProfile = profile.copy(phone = formattedPhone)
                                    profileViewModel.saveProfile(updatedProfile)
                                }
                                onSignUpSuccess()
                            } else {
                                val error = result.exceptionOrNull()
                                val errorMessage = error?.message ?: ""
                                if (errorMessage.contains("400") || errorMessage.contains("registrado", ignoreCase = true)) {
                                    Toast.makeText(context, "El correo electrónico ya está registrado", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Error al registrarse. Revisa tu conexión.", Toast.LENGTH_LONG).show()
                                }
                            }
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

            // Las opciones de rol se presentan de manera integrada en el formulario para una mejor experiencia de usuario.

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
