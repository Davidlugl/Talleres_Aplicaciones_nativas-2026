package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.components.PatientBottomNavBar
import com.example.herbhopper_v1.viewmodel.ProfileViewModel
import com.example.herbhopper_v1.navigation.ProfileOptionDestination
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collect

/**
 * Pantalla de Visualización del Perfil del paciente.
 * Obtiene de forma reactiva y segura los datos de perfil desde Firebase u origen local simulado,
 * gestionando el flujo mediante [ProfileViewModel] dentro de un `produceState` para evitar fugas.
 * Permite acceder a sub-secciones como Configuración, Métodos de Pago, Direcciones y Notificaciones,
 * así como cerrar la sesión de forma segura.
 *
 * @param onHomeClick Función callback para ir a la pantalla de Inicio.
 * @param onOrdersClick Función callback para ir al historial de pedidos.
 * @param onScriptsClick Función callback para ir a la validación de recetas médicas.
 * @param onLogout Función callback para cerrar la sesión actual.
 * @param onOptionClick Función callback para navegar a las sub-opciones de perfil especificadas por su identificador.
 * @param viewModel Instancia de [ProfileViewModel] para observar de manera reactiva la información del usuario.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onHomeClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScriptsClick: () -> Unit,
    onLogout: () -> Unit,
    onOptionClick: (String) -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val uid = com.example.herbhopper_v1.data.SessionManager.getUid()
    
    // Solución al Issue 3: Envolver la suscripción al Flow en produceState para evitar
    // recrear la suscripción en cada recomposición y prevenir fugas de memoria.
    val profile by produceState<com.example.herbhopper_v1.data.UserProfile?>(initialValue = null, uid, viewModel) {
        viewModel.observeProfile(uid).collect { value = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.profile), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            PatientBottomNavBar(
                currentRoute = "profile",
                onHomeClick = onHomeClick,
                onOrdersClick = onOrdersClick,
                onScriptsClick = onScriptsClick,
                onProfileClick = {}
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                profile?.name ?: stringResource(id = R.string.simulated_patient_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                profile?.email ?: stringResource(id = R.string.no_email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Options - Solución al Issue 7: Usar el enum type-safe ProfileOptionDestination y stringResource
            ProfileOption(
                icon = Icons.Default.Settings,
                title = stringResource(id = R.string.profile_option_settings),
                onClick = { onOptionClick(ProfileOptionDestination.CONFIGURATION.key) }
            )
            ProfileOption(
                icon = Icons.Default.Payment,
                title = stringResource(id = R.string.profile_option_payment),
                onClick = { onOptionClick(ProfileOptionDestination.PAYMENT_METHODS.key) }
            )
            ProfileOption(
                icon = Icons.Default.LocationOn,
                title = stringResource(id = R.string.profile_option_addresses),
                onClick = { onOptionClick(ProfileOptionDestination.ADDRESSES.key) }
            )
            ProfileOption(
                icon = Icons.Default.Notifications,
                title = stringResource(id = R.string.profile_option_notifications),
                onClick = { onOptionClick(ProfileOptionDestination.NOTIFICATIONS.key) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    com.example.herbhopper_v1.data.SessionManager.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(stringResource(id = R.string.logout), color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Fila de opción para la pantalla de perfil.
 * Renderiza de manera uniforme un icono ilustrativo, un título claro y una flecha de navegación a la derecha.
 *
 * @param icon Icono descriptivo del módulo o destino.
 * @param title Título legible de la opción.
 * @param onClick Acción ejecutada al pulsar la opción.
 */
@Composable
fun ProfileOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit = {}) {
    Surface(
        modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(title, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
