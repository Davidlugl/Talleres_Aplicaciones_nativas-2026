package com.example.herbhopper_v1.ui.seller

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.navigation.ProfileOptionDestination

/**
 * Pantalla de Perfil de la Tienda del Vendedor (Store Profile Screen).
 * Muestra la información del comercio verificado y organiza las opciones de configuración
 * (Editar Información, Ubicación del Local, Notificaciones y Cuentas de Cobro) de forma estructurada.
 * Facilita el cierre de sesión seguro del vendedor de vuelta al login.
 *
 * @param onBack Función callback para retornar a la pantalla anterior.
 * @param onNavigate Función callback para navegar a otras secciones generales del vendedor.
 * @param onLogout Función callback para cerrar sesión de manera definitiva.
 * @param onOptionClick Función callback para despachar la navegación a sub-secciones detalladas de perfil de tienda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreProfileScreen(
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    onLogout: () -> Unit = {},
    onOptionClick: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.store_profile_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            SellerBottomNavBar(
                currentRoute = "profile",
                onNavigate = onNavigate
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
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Store, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(id = R.string.store_name), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(stringResource(id = R.string.verified_seller), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(32.dp))

            // Solución al Issue 7: Usar el enum type-safe ProfileOptionDestination
            ProfileSettingItem(
                icon = Icons.Default.Edit, 
                title = stringResource(id = R.string.edit_info), 
                onClick = { onOptionClick(ProfileOptionDestination.CONFIGURATION.key) }
            )
            ProfileSettingItem(
                icon = Icons.Default.LocationOn, 
                title = stringResource(id = R.string.store_location), 
                onClick = { onOptionClick(ProfileOptionDestination.ADDRESSES.key) }
            )
            ProfileSettingItem(
                icon = Icons.Default.Notifications, 
                title = stringResource(id = R.string.notifications), 
                onClick = { onOptionClick(ProfileOptionDestination.NOTIFICATIONS.key) }
            )
            ProfileSettingItem(
                icon = Icons.Default.Payment, 
                title = stringResource(id = R.string.payment_account), 
                onClick = { onOptionClick(ProfileOptionDestination.PAYMENT_METHODS.key) }
            )
            ProfileSettingItem(icon = Icons.Default.Security, title = stringResource(id = R.string.security))

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(id = R.string.close_session), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Componente de fila individual para las opciones del perfil de la tienda.
 * Muestra el icono representativo, título e indicador Chevron derecho de manera uniforme.
 *
 * @param icon Icono descriptivo de tipo [ImageVector].
 * @param title Nombre de la opción de configuración.
 * @param onClick Acción ejecutada cuando el usuario pulsa sobre la opción.
 */
@Composable
fun ProfileSettingItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
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
