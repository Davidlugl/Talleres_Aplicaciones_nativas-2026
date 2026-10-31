package com.example.herbhopper_v1.ui.patient

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.model.CartItem
import com.example.herbhopper_v1.viewmodel.CheckoutViewModel
import com.example.herbhopper_v1.viewmodel.PaymentMethod

/**
 * Pantalla de Checkout.
 *
 * 1. Resumen con el precio final (IVA incluido, Ley 1480).
 * 2. Dirección de entrega (guardada con coordenadas GPS desde Perfil → Direcciones).
 * 3. Fórmula médica, obligatoria si el carrito tiene productos de control especial.
 * 4. Método de pago: tarjeta, PSE o efectivo. Todos se pagan dentro del checkout oficial de ePayco;
 *    la app no recibe datos de tarjeta.
 *
 * Al pulsar "Proceder al Pago" el pedido se registra en el servidor y se abre ePayco.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    orderId: String,
    onBack: () -> Unit,
    onOrderCreated: (Order, PaymentMethod) -> Unit,
    onManageAddresses: () -> Unit,
    onUploadPrescription: () -> Unit,
    viewModel: CheckoutViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val totalAmount = cartItems.sumOf { it.product.price * it.quantity }
    val needsPrescription = cartItems.any { it.product.requiresPrescription }

    // Al volver de cargar una fórmula o de agregar una dirección, se recargan los datos
    LaunchedEffect(Unit) { viewModel.refreshPrescriptions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.payment_gateway_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---- Resumen del pedido ----
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.payment_summary_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    cartItems.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity} × ${item.product.name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text(formatCop(item.product.price * item.quantity), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.total_to_pay))
                        Text(formatCop(totalAmount), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 22.sp)
                    }
                    Text(
                        stringResource(id = R.string.checkout_price_includes_taxes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(id = R.string.order_reference_number), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(orderId, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ---- Dirección de entrega ----
            SectionTitle(icon = Icons.Default.LocationOn, text = stringResource(id = R.string.checkout_address_title))
            if (uiState.addresses.isEmpty()) {
                Text(stringResource(id = R.string.checkout_no_addresses), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            uiState.addresses.forEach { address ->
                PaymentMethodItem(
                    title = address.label,
                    subtitle = address.fullAddress + if (address.latitude != null) " · GPS" else "",
                    icon = Icons.Default.Home,
                    isSelected = uiState.selectedAddress?.id == address.id,
                    onClick = { viewModel.onAddressSelected(address) }
                )
            }
            OutlinedButton(onClick = onManageAddresses, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.checkout_manage_addresses))
            }

            // ---- Fórmula médica ----
            if (needsPrescription) {
                SectionTitle(icon = Icons.Default.Description, text = stringResource(id = R.string.checkout_prescription_title))
                Text(stringResource(id = R.string.checkout_prescription_required), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                uiState.prescriptions.forEach { prescription ->
                    PaymentMethodItem(
                        title = stringResource(id = R.string.prescription_item_title, prescription.id, prescription.doctorName),
                        subtitle = prescriptionStatusLabel(prescription.status),
                        icon = Icons.Default.Description,
                        isSelected = uiState.selectedPrescriptionId == prescription.id,
                        onClick = { viewModel.onPrescriptionSelected(prescription.id) }
                    )
                }
                OutlinedButton(onClick = onUploadPrescription, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.upload_prescription_btn))
                }
            }

            // ---- Método de pago ----
            SectionTitle(icon = Icons.Default.Payments, text = stringResource(id = R.string.select_payment_method))
            PaymentMethodItem(
                title = stringResource(id = R.string.credit_card_method),
                subtitle = stringResource(id = R.string.payment_method_card_desc),
                icon = Icons.Default.CreditCard,
                isSelected = uiState.selectedMethod == PaymentMethod.CreditCard,
                onClick = { viewModel.onPaymentMethodChange(PaymentMethod.CreditCard) }
            )
            PaymentMethodItem(
                title = stringResource(id = R.string.bank_transfer_method),
                subtitle = stringResource(id = R.string.payment_method_pse_desc),
                icon = Icons.Default.AccountBalance,
                isSelected = uiState.selectedMethod == PaymentMethod.BankTransfer,
                onClick = { viewModel.onPaymentMethodChange(PaymentMethod.BankTransfer) }
            )
            PaymentMethodItem(
                title = stringResource(id = R.string.cash_method),
                subtitle = stringResource(id = R.string.payment_method_cash_desc),
                icon = Icons.Default.Store,
                isSelected = uiState.selectedMethod == PaymentMethod.Cash,
                onClick = { viewModel.onPaymentMethodChange(PaymentMethod.Cash) }
            )

            InfoCard(
                text = stringResource(
                    id = when (uiState.selectedMethod) {
                        PaymentMethod.CreditCard -> R.string.card_info_text
                        PaymentMethod.BankTransfer -> R.string.bank_transfer_info_text
                        PaymentMethod.Cash -> R.string.cash_info_text
                    }
                )
            )

            // ---- Error ----
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ---- Botón principal ----
            Button(
                onClick = { viewModel.createOrder(cartItems, orderId, onOrderCreated) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isCreatingOrder && cartItems.isNotEmpty()
            ) {
                if (uiState.isCreatingOrder) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(id = R.string.processing_payment), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (uiState.selectedMethod) {
                            PaymentMethod.Cash -> stringResource(id = R.string.pay_with_cash_btn)
                            PaymentMethod.BankTransfer -> stringResource(id = R.string.pay_with_pse_btn)
                            PaymentMethod.CreditCard -> stringResource(id = R.string.pay_with_card_btn)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(id = R.string.payment_security_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Formato de pesos colombianos: $ 65.000 COP */
fun formatCop(amount: Double): String =
    "$ " + String.format(java.util.Locale("es", "CO"), "%,.0f", amount) + " COP"

@Composable
fun prescriptionStatusLabel(status: String?): String = when (status) {
    "APPROVED" -> stringResource(id = R.string.prescription_status_approved)
    "REJECTED" -> stringResource(id = R.string.prescription_status_rejected)
    "PENDING" -> stringResource(id = R.string.prescription_status_pending)
    else -> stringResource(id = R.string.prescription_status_none)
}

// ---------------------------------------------------------------------------
// Componente: ítem seleccionable (método de pago, dirección, fórmula)
// ---------------------------------------------------------------------------

@Composable
fun PaymentMethodItem(
    title: String,
    subtitle: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}
