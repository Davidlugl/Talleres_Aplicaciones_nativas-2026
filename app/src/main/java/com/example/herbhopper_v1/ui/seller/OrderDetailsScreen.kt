package com.example.herbhopper_v1.ui.seller

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.OrderItem
import com.example.herbhopper_v1.ui.patient.formatCop
import com.example.herbhopper_v1.ui.patient.paymentMethodLabel
import com.example.herbhopper_v1.ui.patient.paymentStatusLabel
import com.example.herbhopper_v1.ui.patient.prescriptionStatusLabel
import com.example.herbhopper_v1.viewmodel.OrderViewModel
import com.example.herbhopper_v1.viewmodel.PrescriptionViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * Detalle de pedido para el vendedor (dispensario).
 *
 * Permite auditar los productos, la dirección de entrega con sus coordenadas GPS (abre el mapa),
 * el estado del pago informado por ePayco y la fórmula médica adjunta: el farmacéutico la abre,
 * verifica registro médico, sello y firma, y la aprueba o rechaza. Luego puede ACEPTAR (solo con
 * la fórmula aprobada), CANCELAR (devuelve el stock) o MARCAR COMO ENTREGADO.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel(),
    prescriptionViewModel: PrescriptionViewModel = viewModel()
) {
    val context = LocalContext.current
    val order by viewModel.observeOrder(orderId).collectAsState(initial = null)
    val prescriptions by prescriptionViewModel.prescriptions.collectAsState()
    var rejectNote by remember { mutableStateOf("") }
    var showRejectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) {
        viewModel.refreshOrder(orderId)
        prescriptionViewModel.refresh()
    }

    fun changeStatus(status: String) {
        viewModel.updateOrderStatus(orderId, status) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.order_details_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        val currentOrder = order
        if (currentOrder == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val items: List<OrderItem> = remember(currentOrder.itemsJson) {
            try {
                Gson().fromJson(currentOrder.itemsJson, object : TypeToken<List<OrderItem>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        }
        val prescription = prescriptions.firstOrNull { it.id == currentOrder.prescriptionId }

        if (showRejectDialog && prescription != null) {
            AlertDialog(
                onDismissRequest = { showRejectDialog = false },
                title = { Text(stringResource(id = R.string.reject_prescription_title)) },
                text = {
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = { rejectNote = it },
                        label = { Text(stringResource(id = R.string.reject_reason_label)) }
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRejectDialog = false
                            prescriptionViewModel.review(prescription.id, approve = false, note = rejectNote) { error ->
                                Toast.makeText(context, error ?: context.getString(R.string.prescription_rejected_toast), Toast.LENGTH_LONG).show()
                                viewModel.refreshOrder(orderId)
                            }
                        },
                        enabled = rejectNote.isNotBlank()
                    ) { Text(stringResource(id = R.string.reject_btn)) }
                },
                dismissButton = { TextButton(onClick = { showRejectDialog = false }) { Text(stringResource(id = R.string.cancel_btn)) } }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(id = R.string.order_id_label, currentOrder.orderId), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        DetailsOrderStatusChip(status = currentOrder.status)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(id = R.string.client_label, currentOrder.userName))
                    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "CO")).format(Date(currentOrder.timestamp))
                    Text(stringResource(id = R.string.date_label, date))
                    Text("${stringResource(id = R.string.payment_method_label)}: ${paymentMethodLabel(currentOrder.paymentMethod)}")
                    Text("${stringResource(id = R.string.payment_status_label)}: ${paymentStatusLabel(currentOrder.paymentStatus)}", fontWeight = FontWeight.SemiBold)
                }
            }

            // ---- Productos ----
            Text(stringResource(id = R.string.products_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            items.forEach { item ->
                ListItem(
                    headlineContent = { Text(item.productName) },
                    supportingContent = { Text(stringResource(id = R.string.quantity_label, item.quantity)) },
                    trailingContent = { Text(formatCop(item.price * item.quantity)) }
                )
                HorizontalDivider()
            }

            // ---- Fórmula médica ----
            if (currentOrder.prescriptionId != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.prescription_label), fontWeight = FontWeight.Bold)
                        }
                        Text(prescriptionStatusLabel(prescription?.status ?: currentOrder.prescriptionStatus))
                        prescription?.let { p ->
                            Text(stringResource(id = R.string.prescription_doctor_info, p.doctorName, p.doctorRegistry), style = MaterialTheme.typography.bodySmall)
                            p.reviewNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                            p.fileUrl?.let { url ->
                                OutlinedButton(
                                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(stringResource(id = R.string.view_prescription_btn)) }
                            }
                            if (p.status == "PENDING") {
                                Text(stringResource(id = R.string.prescription_checklist), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            prescriptionViewModel.review(p.id, approve = true, note = null) { error ->
                                                Toast.makeText(context, error ?: context.getString(R.string.prescription_approved_toast), Toast.LENGTH_LONG).show()
                                                viewModel.refreshOrder(orderId)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(stringResource(id = R.string.approve_btn)) }
                                    OutlinedButton(
                                        onClick = { rejectNote = ""; showRejectDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(stringResource(id = R.string.reject_btn)) }
                                }
                            }
                        }
                    }
                }
            }

            // ---- Entrega ----
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(id = R.string.delivery_address_label), fontWeight = FontWeight.Bold)
                    Text(currentOrder.address ?: stringResource(id = R.string.no_address))
                    if (currentOrder.latitude != null && currentOrder.longitude != null) {
                        Text("GPS: %.5f, %.5f".format(currentOrder.latitude, currentOrder.longitude), style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = {
                            val uri = Uri.parse("geo:${currentOrder.latitude},${currentOrder.longitude}?q=${currentOrder.latitude},${currentOrder.longitude}(Entrega)")
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.no_maps_app), Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(id = R.string.open_in_maps))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(id = R.string.total_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(formatCop(currentOrder.totalAmount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // ---- Acciones ----
            if (currentOrder.status == "PENDING") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { changeStatus("ACCEPTED") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.status_accepted))
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.accept_order))
                    }
                    Button(
                        onClick = { changeStatus("CANCELLED") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Cancel, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.cancel_order))
                    }
                }
            } else if (currentOrder.status == "ACCEPTED") {
                Button(
                    onClick = { changeStatus("DELIVERED") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(id = R.string.mark_as_delivered))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Chip indicador de estado del pedido en la pantalla de detalles del vendedor.
 *
 * @param status Estado del pedido (PENDING, ACCEPTED, CANCELLED, DELIVERED).
 */
@Composable
fun DetailsOrderStatusChip(status: String) {
    val (color, label) = when (status.uppercase()) {
        "PENDING" -> colorResource(id = R.color.status_pending) to stringResource(id = R.string.status_pending)
        "ACCEPTED" -> colorResource(id = R.color.status_accepted) to stringResource(id = R.string.status_accepted)
        "CANCELLED" -> colorResource(id = R.color.status_cancelled) to stringResource(id = R.string.status_cancelled)
        "DELIVERED" -> colorResource(id = R.color.status_delivered) to stringResource(id = R.string.status_delivered)
        else -> MaterialTheme.colorScheme.secondary to status
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
