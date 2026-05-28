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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider

import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.viewmodel.CheckoutViewModel
import com.example.herbhopper_v1.viewmodel.PaymentMethod
import com.example.herbhopper_v1.viewmodel.TransactionStatus
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions

/**
 * Pantalla de Checkout integrada con ePayco.
 *
 * Permite seleccionar el método de pago (Tarjeta de Crédito/Débito, PSE, Efectivo),
 * muestra el resumen del pedido con el monto total y recolecta los datos de la tarjeta
 * cuando ese método está seleccionado. El ViewModel coordina la tokenización y el cobro
 * real a través de la API de ePayco.
 *
 * @param totalAmount       Monto total a pagar en COP.
 * @param orderId           Referencia única del pedido.
 * @param orderDescription  Descripción del contenido del pedido.
 * @param onBack            Regresa a la pantalla anterior.
 * @param onPaymentResult   Callback con el estado final de la transacción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    totalAmount: Double,
    orderId: String = "ORD-${(1000..9999).random()}",
    orderDescription: String = "",
    onBack: () -> Unit,
    onPaymentResult: (TransactionStatus) -> Unit,
    onLaunchWebCheckout: (Double, String, String) -> Unit = { _, _, _ -> },
    viewModel: CheckoutViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val finalDescription = orderDescription.ifBlank { stringResource(id = R.string.app_name) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.payment_gateway_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
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
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(id = R.string.payment_summary_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(id = R.string.total_to_pay))
                        Text(
                            "$ ${String.format("%,.0f", totalAmount)} COP",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(id = R.string.order_reference_number),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            orderId,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Selección de método de pago ----
            Text(
                stringResource(id = R.string.select_payment_method),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

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
                icon = Icons.Default.Payments,
                isSelected = uiState.selectedMethod == PaymentMethod.Cash,
                onClick = { viewModel.onPaymentMethodChange(PaymentMethod.Cash) }
            )

            // ---- Formulario de tarjeta ----
            if (uiState.selectedMethod == PaymentMethod.CreditCard) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(id = R.string.card_data_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                CreditCardForm(
                    cardNumber        = uiState.cardNumber,
                    onCardNumberChange = viewModel::onCardNumberChange,
                    expiry            = uiState.expiryDate,
                    onExpiryChange    = viewModel::onExpiryDateChange,
                    cvv               = uiState.cvv,
                    onCvvChange       = viewModel::onCvvChange,
                    cardHolderName    = uiState.cardHolderName,
                    onHolderNameChange = viewModel::onCardHolderNameChange
                )

                // ---- Datos del comprador requeridos por ePayco ----
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(id = R.string.card_holder_info_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = uiState.buyerPhone,
                    onValueChange = viewModel::onBuyerPhoneChange,
                    label = { Text(stringResource(id = R.string.buyer_phone_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    placeholder = { Text(stringResource(id = R.string.buyer_phone_placeholder)) },
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.buyerDoc,
                    onValueChange = viewModel::onBuyerDocChange,
                    label = { Text(stringResource(id = R.string.buyer_doc_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Badge, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.buyerAddress,
                    onValueChange = viewModel::onBuyerAddressChange,
                    label = { Text(stringResource(id = R.string.buyer_address_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Home, null) },
                    singleLine = true
                )
            }

            // ---- Información de PSE ----
            if (uiState.selectedMethod == PaymentMethod.BankTransfer) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.bank_transfer_info_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Información de Efectivo ----
            if (uiState.selectedMethod == PaymentMethod.Cash) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.cash_info_text),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---- Mensaje de error de validación ----
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ---- Estado de transacción rechazada ----
            if (uiState.transactionStatus is TransactionStatus.Rejected) {
                val rejected = uiState.transactionStatus as TransactionStatus.Rejected
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Cancel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                stringResource(id = R.string.payment_rejected_title),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            rejected.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.resetPaymentState() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(id = R.string.retry_other_details_btn))
                        }
                    }
                }
            }

            // ---- Estado de transacción fallida (errores de credenciales, red, etc) ----
            if (uiState.transactionStatus is TransactionStatus.Failed) {
                val failed = uiState.transactionStatus as TransactionStatus.Failed
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                stringResource(id = R.string.technical_error_title),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Detalle: ${failed.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.technical_error_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.resetPaymentState() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(id = R.string.clear_error_and_retry))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ---- Botón principal de pago ----
            Button(
                onClick = {
                    if (uiState.selectedMethod == PaymentMethod.Cash) {
                        viewModel.executePayment(
                            totalAmount = totalAmount,
                            orderId     = orderId,
                            description = finalDescription,
                            onSuccess   = { status -> onPaymentResult(status) }
                        )
                    } else {
                        onLaunchWebCheckout(totalAmount, orderId, finalDescription)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(id = R.string.processing_payment), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (uiState.selectedMethod) {
                            is PaymentMethod.Cash        -> stringResource(id = R.string.pay_with_cash_btn)
                            is PaymentMethod.BankTransfer -> stringResource(id = R.string.pay_with_pse_btn)
                            else                          -> stringResource(id = R.string.pay_with_card_btn)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ---- Badge de seguridad ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    stringResource(id = R.string.payment_security_badge),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Componente: Ítem de método de pago
// ---------------------------------------------------------------------------

/**
 * Elemento de selección de método de pago con título, subtítulo e icono.
 */
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
        border = if (isSelected) androidx.compose.foundation.BorderStroke(
            2.dp, MaterialTheme.colorScheme.primary
        ) else null,
        color = if (isSelected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

// ---------------------------------------------------------------------------
// Componente: Formulario de tarjeta de crédito
// ---------------------------------------------------------------------------

/**
 * Formulario sin estado para el ingreso de datos de tarjeta de crédito.
 * Incluye campo de nombre del titular además de número, vencimiento y CVV.
 */
@Composable
fun CreditCardForm(
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expiry: String,
    onExpiryChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit,
    cardHolderName: String = "",
    onHolderNameChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Número de tarjeta
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { onCardNumberChange(it.filter { c -> c.isDigit() }.take(16)) },
            label = { Text(stringResource(id = R.string.card_number_label)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(id = R.string.card_number_placeholder)) },
            leadingIcon = { Icon(Icons.Default.CreditCard, null) },
            visualTransformation = CheckoutCreditCardTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )

        // Nombre en la tarjeta
        OutlinedTextField(
            value = cardHolderName,
            onValueChange = onHolderNameChange,
            label = { Text(stringResource(id = R.string.card_holder_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Person, null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Characters
            ),
            placeholder = { Text(stringResource(id = R.string.card_holder_name_placeholder)) },
            singleLine = true
        )

        // Vencimiento + CVV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = expiry,
                onValueChange = { onExpiryChange(it.filter { c -> c.isDigit() }.take(4)) },
                label = { Text(stringResource(id = R.string.expiry_label)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(id = R.string.expiry_placeholder)) },
                leadingIcon = { Icon(Icons.Default.CalendarToday, null) },
                visualTransformation = CheckoutExpiryDateTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = cvv,
                onValueChange = { onCvvChange(it.filter { c -> c.isDigit() }.take(3)) },
                label = { Text(stringResource(id = R.string.cvv_label)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(stringResource(id = R.string.cvv_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Transformaciones visuales
// ---------------------------------------------------------------------------

/**
 * Transformación visual para tarjetas de crédito (agrupa dígitos de 4 en 4).
 */
class CheckoutCreditCardTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 16) text.text.substring(0, 16) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i < 15) out += " "
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 8) return offset + 1
                if (offset <= 12) return offset + 2
                if (offset <= 16) return offset + 3
                return 19
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                if (offset <= 19) return offset - 3
                return 16
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

/**
 * Transformación visual para fecha de vencimiento en formato MM/AA.
 */
class CheckoutExpiryDateTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0, 4) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1) out += "/"
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 4) return offset + 1
                return 5
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 5) return offset - 1
                return 4
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
