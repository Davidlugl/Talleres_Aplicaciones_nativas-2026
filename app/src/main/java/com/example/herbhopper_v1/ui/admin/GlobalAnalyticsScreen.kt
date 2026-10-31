package com.example.herbhopper_v1.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.network.AnalyticsResponse
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.BreakdownItem
import com.example.herbhopper_v1.data.network.apiErrorMessage
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar
import com.example.herbhopper_v1.ui.patient.formatCop

/**
 * Analítica global (administrador) con datos reales del backend (/api/admin/analytics):
 * ventas con pago aprobado, usuarios activos, pedidos y pagos aprobados, y su distribución
 * por estado del pedido, medio de pago y rol de usuario.
 *
 * @param onNavigate Función callback para navegar a diferentes paneles del administrador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAnalyticsScreen(onNavigate: (String) -> Unit = {}) {
    val api = remember { ApiService.instance }
    var data by remember { mutableStateOf<AnalyticsResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        try {
            data = api.getAnalytics()
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.apiErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.global_analytics_title), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { reloadKey++ }) { Icon(Icons.Default.Refresh, contentDescription = stringResource(id = R.string.refresh)) }
                }
            )
        },
        bottomBar = { AdminBottomNavBar(currentRoute = "analytics", onNavigate = onNavigate) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(stringResource(id = R.string.network_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            val d = data
            if (d == null) {
                if (errorMessage == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                return@Column
            }

            AnalyticsStat(stringResource(id = R.string.analytics_sales), formatCop(d.approvedSales))
            AnalyticsStat(stringResource(id = R.string.analytics_active_users), d.activeUsers.toString())
            AnalyticsStat(stringResource(id = R.string.analytics_orders), "${d.totalOrders} · ${stringResource(id = R.string.analytics_approved_payments, d.approvedPayments)}")

            Spacer(modifier = Modifier.height(24.dp))
            BreakdownSection(stringResource(id = R.string.analytics_orders_by_status), d.ordersByStatus, MaterialTheme.colorScheme.primary) { key ->
                when (key) {
                    "PENDING" -> "Pendientes"; "ACCEPTED" -> "Aceptados"; "DELIVERED" -> "Entregados"; "CANCELLED" -> "Cancelados"; else -> key ?: "—"
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            BreakdownSection(stringResource(id = R.string.analytics_sales_by_method), d.salesByPaymentMethod, MaterialTheme.colorScheme.secondary) { key ->
                when (key) { "CREDIT_CARD" -> "Tarjeta"; "PSE" -> "PSE"; "CASH" -> "Efectivo"; else -> key ?: "—" }
            }
            Spacer(modifier = Modifier.height(24.dp))
            BreakdownSection(stringResource(id = R.string.analytics_users_by_role), d.usersByRole, MaterialTheme.colorScheme.tertiary) { key ->
                when (key) { "PATIENT" -> "Pacientes"; "SELLER" -> "Vendedores"; "ADMIN" -> "Administradores"; else -> key ?: "—" }
            }
        }
    }
}

@Composable
private fun BreakdownSection(title: String, items: List<BreakdownItem>, color: Color, label: (String?) -> String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    if (items.isEmpty()) {
        Text(stringResource(id = R.string.no_data_yet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val total = items.sumOf { it.count }.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEach { item ->
            val suffix = item.amount?.let { " · ${formatCop(it)}" } ?: ""
            RegionRow("${label(item.key)} (${item.count})$suffix", item.count.toFloat() / total, color)
        }
    }
}

/**
 * Tarjeta para presentar una métrica.
 *
 * @param label Título de la métrica.
 * @param value Valor ya formateado.
 */
@Composable
fun AnalyticsStat(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Fila con barra de progreso proporcional.
 *
 * @param name Etiqueta de la fila.
 * @param progress Fracción de 0.0 a 1.0.
 * @param color Color de la barra.
 */
@Composable
fun RegionRow(name: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Transparent, RoundedCornerShape(4.dp)),
            color = color
        )
    }
}
