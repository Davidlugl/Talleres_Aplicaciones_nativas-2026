package com.example.herbhopper_v1.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.components.AdminBottomNavBar

/**
 * Pantalla de Analíticas Globales del Administrador (Global Analytics Screen).
 * Agrupa y visualiza métricas de alto nivel del sistema, tales como ventas totales facturadas,
 * usuarios activos registrados, volumen de transacciones financieras y un desglose interactivo
 * del rendimiento operativo y comercial por regiones geográficas principales en Colombia.
 *
 * @param onNavigate Función callback para navegar a diferentes paneles del administrador.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAnalyticsScreen(onNavigate: (String) -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.global_analytics_title), fontWeight = FontWeight.Bold) }
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
            Spacer(modifier = Modifier.height(24.dp))

            AnalyticsStat("Ventas Totales", "$124,500.00", "+8.2%")
            AnalyticsStat("Usuarios Activos", "1,240", "+12.5%")
            AnalyticsStat("Transacciones", "5,820", "-2.1%")

            Spacer(modifier = Modifier.height(32.dp))

            Text(stringResource(id = R.string.performance_by_region), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RegionRow("Bogotá", 0.85f, MaterialTheme.colorScheme.primary)
                RegionRow("Medellín", 0.65f, MaterialTheme.colorScheme.secondary)
                RegionRow("Cali", 0.45f, MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

/**
 * Tarjeta individual para presentar una estadística analítica.
 * Colorea el texto de cambio porcentual en verde si es positivo (+) y en rojo si es negativo (-).
 *
 * @param label Título de la métrica (ej. "Ventas Totales").
 * @param value Valor actual formateado en texto (ej. "$124,500.00").
 * @param change Cadena que representa el cambio porcentual de crecimiento o decrecimiento.
 */
@Composable
fun AnalyticsStat(label: String, value: String, change: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                change,
                color = if (change.startsWith("+")) colorResource(id = R.color.status_accepted) else colorResource(id = R.color.status_cancelled),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Fila analítica de barra de progreso lineal por región comercial.
 *
 * @param name Nombre de la ciudad o región geográfica.
 * @param progress Fracción float de 0.0 a 1.0 que indica la cuota o desempeño.
 * @param color Color de la barra de progreso lineal.
 */
@Composable
fun RegionRow(name: String, progress: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelSmall)
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
