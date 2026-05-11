package com.example.herbhopper_v1.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfrastructureScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    var showRestartDialog by remember { mutableStateOf(false) }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text("Confirmar Reinicio") },
            text = { Text("¿Estás seguro de que deseas reiniciar los servicios de infraestructura? Esto desconectará temporalmente a los usuarios.") },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    Toast.makeText(context, context.getString(R.string.toast_services_restarting), Toast.LENGTH_SHORT).show()
                }) {
                    Text("Reiniciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.system_infra_title), fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = { AdminBottomNavBar(currentRoute = "system", onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(stringResource(id = R.string.server_status_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))

            SystemItem("Base de Datos Principal", "En Línea", true)
            SystemItem("Servicio de Autenticación", "En Línea", true)
            SystemItem("API de Pagos", "Mantenimiento", false)
            SystemItem("Servidor de Almacenamiento", "En Línea", true)

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showRestartDialog = true },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.restart_services_btn), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SystemItem(name: String, status: String, isOnline: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOnline) colorResource(id = R.color.system_online)
                        else colorResource(id = R.color.system_maintenance)
                    )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                status,
                style = MaterialTheme.typography.labelMedium,
                color = if (isOnline) colorResource(id = R.color.system_online) else colorResource(id = R.color.system_maintenance)
            )
        }
    }
}
