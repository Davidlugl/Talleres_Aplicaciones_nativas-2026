package com.example.herbhopper_v1.ui.patient

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.data.OrderItem
import com.example.herbhopper_v1.ui.components.PatientBottomNavBar
import com.example.herbhopper_v1.viewmodel.OrderViewModel

/**
 * Rastreo de un pedido (Mis Pedidos → pedido).
 *
 * Muestra la línea de tiempo real del pedido según el estado que registra el dispensario
 * (Recibido → Aceptado/en preparación → Entregado, o Cancelado), el estado del pago informado
 * por ePayco, el estado de la fórmula médica y la dirección de entrega capturada por GPS,
 * que se puede abrir en la aplicación de mapas del teléfono.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    orderId: String,
    viewModel: OrderViewModel,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScriptsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val order by viewModel.observeOrder(orderId).collectAsState(initial = null)

    LaunchedEffect(orderId) { viewModel.refreshOrder(orderId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.order_tracking_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshOrder(orderId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(id = R.string.refresh))
                    }
                }
            )
        },
        bottomBar = {
            PatientBottomNavBar(
                currentRoute = "orders",
                onHomeClick = onHomeClick,
                onOrdersClick = onOrdersClick,
                onScriptsClick = onScriptsClick,
                onProfileClick = onProfileClick
            )
        }
    ) { padding ->
        val current = order
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val items = remember(current.itemsJson) {
            val type = object : com.google.gson.reflect.TypeToken<List<OrderItem>>() {}.type
            try { com.google.gson.Gson().fromJson<List<OrderItem>>(current.itemsJson, type) } catch (e: Exception) { emptyList() }
        }
        val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("es", "CO")) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text("${stringResource(id = R.string.order_label)} ${current.orderId}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(dateFormat.format(java.util.Date(current.timestamp)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // ---- Línea de tiempo ----
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    TrackingTimeline(current)
                }
            }

            // ---- Pago y fórmula ----
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackingInfoRow(Icons.Default.Payments, stringResource(id = R.string.payment_status_label), paymentStatusLabel(current.paymentStatus))
                    current.paymentRef?.let { TrackingInfoRow(Icons.Default.Receipt, stringResource(id = R.string.payment_reference_label), it) }
                    TrackingInfoRow(Icons.Default.CreditCard, stringResource(id = R.string.payment_method_label), paymentMethodLabel(current.paymentMethod))
                    if (current.prescriptionId != null) {
                        TrackingInfoRow(Icons.Default.Description, stringResource(id = R.string.prescription_label), prescriptionStatusLabel(current.prescriptionStatus))
                    }
                }
            }

            // ---- Entrega ----
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TrackingInfoRow(Icons.Default.LocationOn, stringResource(id = R.string.delivery_address_title), current.address ?: "—")
                    if (current.latitude != null && current.longitude != null) {
                        Text(
                            "GPS: %.5f, %.5f".format(current.latitude, current.longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val query = if (current.latitude != null && current.longitude != null)
                                "geo:${current.latitude},${current.longitude}?q=${current.latitude},${current.longitude}(Entrega)"
                            else
                                "geo:0,0?q=${Uri.encode(current.address ?: "")}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(query)))
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.no_maps_app), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(id = R.string.open_in_maps))
                    }
                }
            }

            // ---- Productos ----
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(id = R.string.products_label), fontWeight = FontWeight.Bold)
                    items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity} × ${item.productName}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatCop(item.price * item.quantity), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(id = R.string.total_to_pay), fontWeight = FontWeight.Bold)
                        Text(formatCop(current.totalAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ---- Cancelar (retracto antes del pago/aceptación) ----
            if (current.status == "PENDING" && current.paymentStatus != "APPROVED") {
                OutlinedButton(
                    onClick = {
                        viewModel.cancelMyOrder(current.orderId) { error ->
                            Toast.makeText(context, error ?: context.getString(R.string.order_cancelled_toast), Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(id = R.string.cancel_order_btn))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TrackingTimeline(order: Order) {
    val cancelled = order.status == "CANCELLED"
    val steps = listOf(
        Triple(Icons.Default.ReceiptLong, stringResource(id = R.string.tracking_step_received), true),
        Triple(Icons.Default.Science, stringResource(id = R.string.tracking_step_preparing), order.status in listOf("ACCEPTED", "DELIVERED")),
        Triple(Icons.Default.CheckCircle, stringResource(id = R.string.tracking_step_delivered), order.status == "DELIVERED")
    )
    if (cancelled) {
        TimelineStep(Icons.Default.Cancel, stringResource(id = R.string.tracking_step_cancelled), done = true, isError = true, isLast = true)
        return
    }
    steps.forEachIndexed { index, (icon, label, done) ->
        TimelineStep(icon, label, done, isError = false, isLast = index == steps.lastIndex)
    }
}

@Composable
private fun TimelineStep(icon: ImageVector, label: String, done: Boolean, isError: Boolean, isLast: Boolean) {
    val color = when {
        isError -> MaterialTheme.colorScheme.error
        done -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    }
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(32.dp).background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            if (!isLast) {
                Box(Modifier.width(2.dp).height(24.dp).background(color.copy(alpha = 0.4f)))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            modifier = Modifier.padding(top = 6.dp),
            fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
            color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrackingInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun paymentStatusLabel(status: String?): String = when (status) {
    "APPROVED" -> stringResource(id = R.string.payment_status_approved)
    "REJECTED" -> stringResource(id = R.string.payment_status_rejected)
    else -> stringResource(id = R.string.payment_status_pending)
}

@Composable
fun paymentMethodLabel(method: String?): String = when (method) {
    "CREDIT_CARD" -> stringResource(id = R.string.credit_card_method)
    "PSE" -> stringResource(id = R.string.bank_transfer_method)
    "CASH" -> stringResource(id = R.string.cash_method)
    else -> "—"
}
