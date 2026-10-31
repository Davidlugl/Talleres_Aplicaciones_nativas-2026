package com.example.herbhopper_v1.ui.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.RestockRequestResponse
import com.example.herbhopper_v1.data.network.SystemHealthResponse
import com.example.herbhopper_v1.data.network.apiErrorMessage
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar
import kotlinx.coroutines.launch

/**
 * Infraestructura del sistema (administrador).
 *
 * Datos reales del backend (/api/admin/health): estado y latencia de la base de datos,
 * porcentaje de integridad (verificaciones de consistencia superadas), conteos y memoria.
 * "Reiniciar Servicios" recicla el pool de conexiones de la API y repite el diagnóstico.
 * También lista las solicitudes de reabastecimiento que envían los vendedores.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemInfrastructureScreen(onNavigate: (String) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ApiService.instance }
    var health by remember { mutableStateOf<SystemHealthResponse?>(null) }
    var restocks by remember { mutableStateOf<List<RestockRequestResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }

    fun load() {
        isLoading = true
        scope.launch {
            try {
                health = api.getSystemHealth()
                restocks = api.getRestockRequests()
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = e.apiErrorMessage()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { showRestartDialog = false },
            title = { Text(stringResource(id = R.string.restart_confirm_title)) },
            text = { Text(stringResource(id = R.string.restart_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRestartDialog = false
                    isLoading = true
                    scope.launch {
                        try {
                            val result = api.restartServices()
                            health = result.health
                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.apiErrorMessage(), Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }) {
                    Text(stringResource(id = R.string.restart_btn), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestartDialog = false }) { Text(stringResource(id = R.string.cancel_btn)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.system_infra_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, contentDescription = stringResource(id = R.string.refresh)) }
                }
            )
        },
        bottomBar = { AdminBottomNavBar(currentRoute = "system", onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(id = R.string.server_status_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            errorMessage?.let {
                SystemItem(stringResource(id = R.string.api_label), it, false)
            }

            health?.let { h ->
                SystemItem(stringResource(id = R.string.database_label), "${if (h.database == "UP") "En línea" else "Caída"} · ${h.latencyMs} ms", h.database == "UP")
                SystemItem(stringResource(id = R.string.api_label), "En línea · ${h.uptimeSeconds / 60} min · ${h.memoryMb} MB", true)

                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = R.string.db_integrity_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = { h.integrityPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
                Text(stringResource(id = R.string.db_integrity_value, h.integrityPercent), fontWeight = FontWeight.Bold)
                h.checks.forEach { check ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (check.ok) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (check.ok) colorResource(id = R.color.system_online) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (check.ok) check.name else "${check.name}: ${check.issues}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(
                        id = R.string.system_counts,
                        h.counts["users"] ?: 0,
                        h.counts["products"] ?: 0,
                        h.counts["orders"] ?: 0,
                        h.counts["pendingPrescriptions"] ?: 0
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showRestartDialog = true },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.restart_services_btn), fontWeight = FontWeight.Bold)
            }

            // ---- Solicitudes de reabastecimiento ----
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(id = R.string.restock_requests_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            val pending = restocks.filter { it.status == "PENDING" }
            if (pending.isEmpty()) {
                Text(stringResource(id = R.string.no_restock_requests), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            pending.forEach { request ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(request.productName, fontWeight = FontWeight.Bold)
                            Text(stringResource(id = R.string.restock_detail, request.quantity, request.stock), style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = {
                            scope.launch {
                                try {
                                    Toast.makeText(context, api.fulfillRestock(request.id).message, Toast.LENGTH_SHORT).show()
                                    load()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.apiErrorMessage(), Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text(stringResource(id = R.string.fulfill_btn)) }
                    }
                }
            }
        }
    }
}

/**
 * Fila con el estado de un servicio del sistema.
 *
 * @param name Nombre del servicio.
 * @param status Texto de estado.
 * @param isOnline true = operativo (verde); false = caído o con error.
 */
@Composable
fun SystemItem(name: String, status: String, isOnline: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
