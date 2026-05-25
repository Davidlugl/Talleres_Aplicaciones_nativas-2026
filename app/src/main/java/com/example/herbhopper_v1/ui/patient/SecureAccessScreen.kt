package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.theme.*

/**
 * Pantalla de Acceso Seguro (Secure Access Screen).
 * Proporciona opciones seguras y estéticas para iniciar sesión en la aplicación.
 * Ofrece la integración con Google Sign-In mediante un selector de roles (Paciente, Vendedor, Administrador)
 * simulado mediante un diálogo modal para pruebas rápidas de desarrollo, y una opción tradicional
 * de acceso mediante correo electrónico y contraseña.
 *
 * @param onGoogleLogin Callback ejecutado al autenticarse con la cuenta Google de Paciente.
 * @param onBiometricClick Callback ejecutado al intentar iniciar sesión de forma biométrica.
 * @param onPinClick Callback ejecutado para ir al inicio de sesión tradicional.
 * @param onSignUpClick Callback ejecutado al pulsar en "Registrarse".
 * @param onForgotPasswordClick Callback ejecutado al pulsar sobre "Restablecer credenciales".
 * @param onAdminAccess Callback ejecutado al seleccionar la cuenta Google de Administrador.
 * @param onSellerAccess Callback ejecutado al seleccionar la cuenta Google de Vendedor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureAccessScreen(
    onGoogleLogin: () -> Unit,
    onBiometricClick: () -> Unit,
    onPinClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onAdminAccess: () -> Unit,
    onSellerAccess: () -> Unit,
    profileViewModel: com.example.herbhopper_v1.viewmodel.ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
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
                    Text("Selecciona una cuenta para continuar a HerbHopper:", style = MaterialTheme.typography.bodyMedium)
                    
                    // Cuenta 1: Cliente / Paciente
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
                            onGoogleLogin()
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

                    // Cuenta 2: Vendedor
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
                            onSellerAccess()
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

                    // Cuenta 3: Administrador / Desarrollador
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
                            onAdminAccess()
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
            dismissButton = {
                TextButton(onClick = { showGoogleSelector = false }) { Text("Cancelar") }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background artistic elements
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .offset(x = 150.dp, y = (-100).dp)
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-100).dp, y = 100.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Herb Hopper Logo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = stringResource(id = R.string.secure_access_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(id = R.string.secure_access_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Login Container
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Google Login
                        Button(
                            onClick = { showGoogleSelector = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Google Logo Placeholder
                                Box(modifier = Modifier.size(20.dp).background(Color.Red, CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(stringResource(id = R.string.continue_with_google), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Text(
                                stringResource(id = R.string.other_options),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Email Login Option
                        AccessOption(
                            icon = Icons.Default.Email,
                            title = stringResource(id = R.string.email_and_password),
                            subtitle = stringResource(id = R.string.email_and_password_subtitle),
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onPinClick // Reutilizamos onPinClick para ir a login
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Footer Links
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(id = R.string.access_problems),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onForgotPasswordClick) {
                        Text(stringResource(id = R.string.reset_credentials), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))


                    
                    HorizontalDivider(modifier = Modifier.width(200.dp).padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    
                    Text(
                        stringResource(id = R.string.no_account),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onSignUpClick) {
                        Text(stringResource(id = R.string.sign_up_here), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = stringResource(id = R.string.copyright),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Componente visual de fila para representar una opción de acceso.
 * Muestra un icono de color personalizable a la izquierda, título y subtítulo detallados,
 * y un indicador de flecha derecha para invitar a la interacción.
 *
 * @param icon Icono ilustrativo de la opción.
 * @param title Título legible.
 * @param subtitle Descripción breve.
 * @param containerColor Color de fondo del contenedor del icono circular.
 * @param iconColor Color del vector de icono.
 * @param onClick Acción ejecutada cuando el usuario pulsa sobre la opción de acceso.
 */
@Composable
fun AccessOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = containerColor,
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
