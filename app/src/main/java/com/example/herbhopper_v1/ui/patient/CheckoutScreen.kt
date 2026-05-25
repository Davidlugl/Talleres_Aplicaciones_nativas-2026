package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.viewmodel.CheckoutViewModel
import com.example.herbhopper_v1.viewmodel.PaymentMethod

/**
 * Pantalla de Checkout (Pasarela de Pago).
 * Permite seleccionar el método de pago (Tarjeta de Crédito, Transferencia Bancaria, Pago en Efectivo),
 * muestra el resumen de compra con el valor total a pagar y recolecta la información de tarjeta de crédito
 * si es seleccionada. Incluye retroalimentación de carga cuando se realiza el pago.
 *
 * @param totalAmount Monto total a pagar de tipo [Double].
 * @param onBack Función callback para regresar a la pantalla anterior.
 * @param onPaymentSuccess Función callback ejecutada al procesar el pago con éxito.
 * @param viewModel Instancia del [CheckoutViewModel] para la gestión de estados unificados (UDF).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    totalAmount: Double,
    onBack: () -> Unit,
    onPaymentSuccess: () -> Unit,
    viewModel: CheckoutViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(id = R.string.payment_summary_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(id = R.string.total_to_pay))
                        Text("$${String.format("%,.0f", totalAmount)} COP", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    }
                }
            }

            Text(stringResource(id = R.string.select_payment_method), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            PaymentMethodItem(
                title = stringResource(id = R.string.credit_card_method),
                icon = Icons.Default.CreditCard,
                isSelected = uiState.selectedMethod == PaymentMethod.CreditCard,
                onClick = { viewModel.onPaymentMethodChange(PaymentMethod.CreditCard) }
            )

            PaymentMethodItem(
                title = stringResource(id = R.string.bank_transfer_method),
                icon = Icons.Default.AccountBalance,
                isSelected = uiState.selectedMethod == PaymentMethod.BankTransfer,
                onClick = { viewModel.onPaymentMethodChange(PaymentMethod.BankTransfer) }
            )

            PaymentMethodItem(
                title = stringResource(id = R.string.cash_method),
                icon = Icons.Default.Payments,
                isSelected = uiState.selectedMethod == PaymentMethod.Cash,
                onClick = { viewModel.onPaymentMethodChange(PaymentMethod.Cash) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.selectedMethod == PaymentMethod.CreditCard) {
                CreditCardForm(
                    cardNumber = uiState.cardNumber,
                    onCardNumberChange = { viewModel.onCardNumberChange(it) },
                    expiry = uiState.expiryDate,
                    onExpiryChange = { viewModel.onExpiryDateChange(it) },
                    cvv = uiState.cvv,
                    onCvvChange = { viewModel.onCvvChange(it) }
                )

                uiState.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Nota: Se ha eliminado cualquier Spacer(Modifier.weight(1f)) de este Column con scroll.
            // En su lugar, utilizamos un Spacer con un alto fijo para dar respiración al layout antes del botón.
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.executePayment(onPaymentSuccess)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(stringResource(id = R.string.pay_now), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Elemento de selección de método de pago.
 * Muestra el nombre del método, un icono descriptivo, un indicador de selección (RadioButton) y bordes resaltados
 * cuando está seleccionado.
 *
 * @param title Nombre legible del método de pago.
 * @param icon Icono de tipo [ImageVector] para identificar visualmente el método.
 * @param isSelected Booleano que indica si este método está seleccionado actualmente.
 * @param onClick Función ejecutada al hacer clic sobre el elemento para seleccionarlo.
 */
@Composable
fun PaymentMethodItem(
    title: String,
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
            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            Spacer(modifier = Modifier.weight(1f))
            RadioButton(selected = isSelected, onClick = onClick)
        }
    }
}

/**
 * Formulario puro y sin estado (UDF) para el ingreso de datos de tarjeta de crédito.
 * Contiene campos de entrada para el número de tarjeta, la fecha de vencimiento y el CVV,
 * comunicando todos los cambios a la capa superior mediante lambdas.
 */
@Composable
fun CreditCardForm(
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expiry: String,
    onExpiryChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardNumberLabel = stringResource(id = R.string.card_number_label)
    val cardNumberPlaceholder = stringResource(id = R.string.card_number_placeholder)
    val expiryLabel = stringResource(id = R.string.expiry_label)
    val expiryPlaceholder = stringResource(id = R.string.expiry_placeholder)
    val cvvLabel = stringResource(id = R.string.cvv_label)
    val cvvPlaceholder = stringResource(id = R.string.cvv_placeholder)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = cardNumber,
            onValueChange = { onCardNumberChange(it.filter { it.isDigit() }.take(16)) },
            label = { Text(cardNumberLabel) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(cardNumberPlaceholder) },
            leadingIcon = { Icon(Icons.Default.CreditCard, null) },
            visualTransformation = CheckoutCreditCardTransformation()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = expiry,
                onValueChange = { onExpiryChange(it.filter { it.isDigit() }.take(4)) },
                label = { Text(expiryLabel) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(expiryPlaceholder) },
                visualTransformation = CheckoutExpiryDateTransformation()
            )
            OutlinedTextField(
                value = cvv,
                onValueChange = { onCvvChange(it.filter { it.isDigit() }.take(3)) },
                label = { Text(cvvLabel) },
                modifier = Modifier.weight(1f),
                placeholder = { Text(cvvPlaceholder) }
            )
        }
    }
}

/**
 * Transformación visual para tarjetas de crédito (agrupa dígitos de 4 en 4 separados por espacios).
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
