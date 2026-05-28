package com.example.herbhopper_v1.ui.patient

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.data.network.EpaycoTransactionData
import com.example.herbhopper_v1.viewmodel.TransactionStatus
import com.example.herbhopper_v1.ui.theme.EpaycoLoaderSuccess
import com.example.herbhopper_v1.ui.theme.OrangeAmber
import com.example.herbhopper_v1.ui.theme.RedRejected
import com.example.herbhopper_v1.ui.theme.EpaycoPortalBackground
import com.example.herbhopper_v1.ui.theme.CardBg
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------
// Mapeo semántico de colores de la pantalla de confirmación usando el tema global
// ---------------------------------------------------------------------------
private val GreenAccepted  = EpaycoLoaderSuccess
private val OrangeAmber    = com.example.herbhopper_v1.ui.theme.OrangeAmber
private val RedRejected    = com.example.herbhopper_v1.ui.theme.RedRejected
private val NeutralSurface = EpaycoPortalBackground
private val CardBg         = com.example.herbhopper_v1.ui.theme.CardBg

// ---------------------------------------------------------------------------
// Pantalla principal
// ---------------------------------------------------------------------------

/**
 * Pantalla de confirmación de transacción ePayco.
 *
 * Muestra el resultado definitivo del pago (Aceptado / Pendiente / Rechazado / Error)
 * con los datos reales de la transacción retornados por la pasarela.
 *
 * @param status          Estado final de la transacción.
 * @param orderId         ID del pedido generado en la app.
 * @param totalAmount     Monto total cobrado.
 * @param onGoToOrders    Navega al historial de órdenes.
 * @param onRetry         Regresa al checkout para reintentar (solo si fue rechazado/error).
 */
@Composable
fun PaymentConfirmationScreen(
    status: TransactionStatus,
    orderId: String,
    totalAmount: Double,
    onGoToOrders: () -> Unit,
    onRetry: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(NeutralSurface, Color(0xFF0F3460))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // --- Icono animado de estado ---
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.4f, animationSpec = spring(dampingRatio = 0.5f)) + fadeIn()
            ) {
                StatusIcon(status)
            }

            // --- Título y subtítulo ---
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = statusTitle(status),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor(status),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusSubtitle(status),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // --- Tarjeta de detalles de la transacción ---
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(500, delayMillis = 200)) + fadeIn()
            ) {
                TransactionDetailsCard(status, orderId, totalAmount)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Botones de acción ---
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 400))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onGoToOrders,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = statusColor(status)
                        )
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (status is TransactionStatus.Accepted) "Ver Mis Pedidos" else "Ir al Historial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Solo muestra "Reintentar" si el pago fue rechazado o falló
                    if (status is TransactionStatus.Rejected || status is TransactionStatus.Failed) {
                        OutlinedButton(
                            onClick = onRetry,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Intentar de nuevo", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Pie de página con referencia ePayco ---
            val refPayco = extractRefPayco(status)
            if (refPayco != null) {
                Text(
                    text = "Ref. ePayco: $refPayco",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = "Herb Hopper © 2024 · Pagos procesados por ePayco",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Icono de estado animado
// ---------------------------------------------------------------------------

@Composable
private fun StatusIcon(status: TransactionStatus) {
    val color  = statusColor(status)
    val icon   = statusIcon(status)

    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue  = if (status is TransactionStatus.Accepted) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(3.dp, color.copy(alpha = 0.6f), CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(56.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Tarjeta de detalles
// ---------------------------------------------------------------------------

@Composable
private fun TransactionDetailsCard(
    status: TransactionStatus,
    orderId: String,
    totalAmount: Double
) {
    val transactionData = extractTransactionData(status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Detalles de la Transacción",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Referencia del pedido
            DetailRow(
                icon  = Icons.Default.Tag,
                label = "N° de Pedido",
                value = orderId
            )

            // Monto cobrado
            DetailRow(
                icon  = Icons.Default.AttachMoney,
                label = "Monto",
                value = "$ ${String.format("%,.0f", totalAmount)} COP",
                valueColor = statusColor(status)
            )

            // Datos de la transacción (si están disponibles)
            transactionData?.let { tx ->
                tx.x_transaction_id?.let {
                    DetailRow(
                        icon  = Icons.Default.Receipt,
                        label = "ID Transacción",
                        value = it
                    )
                }
                tx.x_franchise?.let {
                    DetailRow(
                        icon  = Icons.Default.CreditCard,
                        label = "Franquicia",
                        value = it
                    )
                }
                tx.x_cardnumber?.let {
                    DetailRow(
                        icon  = Icons.Default.Lock,
                        label = "Tarjeta",
                        value = "•••• •••• •••• $it"
                    )
                }
                tx.x_bank_name?.let {
                    DetailRow(
                        icon  = Icons.Default.AccountBalance,
                        label = "Banco",
                        value = it
                    )
                }
                tx.x_approval_code?.let {
                    DetailRow(
                        icon  = Icons.Default.CheckCircle,
                        label = "Código de aprobación",
                        value = it
                    )
                }
                tx.x_transaction_date?.let {
                    DetailRow(
                        icon  = Icons.Default.CalendarToday,
                        label = "Fecha",
                        value = it
                    )
                }
                // Estado del banco
                tx.x_response?.let {
                    DetailRow(
                        icon       = statusIcon(status),
                        label      = "Respuesta banco",
                        value      = it,
                        valueColor = statusColor(status)
                    )
                }
            }

            // Si es PSE o Efectivo (sin datos de transacción)
            if (transactionData == null) {
                DetailRow(
                    icon  = Icons.Default.Info,
                    label = "Estado",
                    value = if (status is TransactionStatus.Pending)
                        "Pendiente de confirmación" else "En proceso"
                )
                DetailRow(
                    icon  = Icons.Default.NotificationsActive,
                    label = "Aviso",
                    value = "Recibirás una notificación al confirmar tu pago."
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Componente de fila de detalle
// ---------------------------------------------------------------------------

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Funciones de apoyo para mapear el estado a presentación
// ---------------------------------------------------------------------------

private fun statusTitle(status: TransactionStatus): String = when (status) {
    is TransactionStatus.Accepted -> "¡Pago Exitoso!"
    is TransactionStatus.Pending  -> "Pago Pendiente"
    is TransactionStatus.Rejected -> "Pago Rechazado"
    is TransactionStatus.Failed   -> "Error en el Pago"
    else                          -> "Procesando..."
}

private fun statusSubtitle(status: TransactionStatus): String = when (status) {
    is TransactionStatus.Accepted ->
        "Tu transacción fue aprobada por el banco.\nTu pedido está siendo preparado."
    is TransactionStatus.Pending  ->
        "Tu pago está siendo verificado.\nTe notificaremos cuando se confirme."
    is TransactionStatus.Rejected ->
        "El banco rechazó la transacción.\n${status.reason}"
    is TransactionStatus.Failed   ->
        "Ocurrió un error al procesar el pago.\n${status.message}"
    else -> ""
}

@Composable
private fun statusColor(status: TransactionStatus): Color = when (status) {
    is TransactionStatus.Accepted -> GreenAccepted
    is TransactionStatus.Pending  -> OrangeAmber
    is TransactionStatus.Rejected -> RedRejected
    is TransactionStatus.Failed   -> RedRejected
    else                          -> Color.Gray
}

private fun statusIcon(status: TransactionStatus): ImageVector = when (status) {
    is TransactionStatus.Accepted -> Icons.Default.CheckCircle
    is TransactionStatus.Pending  -> Icons.Default.HourglassTop
    is TransactionStatus.Rejected -> Icons.Default.Cancel
    is TransactionStatus.Failed   -> Icons.Default.ErrorOutline
    else                          -> Icons.Default.Info
}

private fun extractTransactionData(status: TransactionStatus): EpaycoTransactionData? = when (status) {
    is TransactionStatus.Accepted -> status.data
    is TransactionStatus.Rejected -> status.data
    is TransactionStatus.Pending  -> status.data
    else                          -> null
}

private fun extractRefPayco(status: TransactionStatus): String? = when (status) {
    is TransactionStatus.Accepted -> status.data.ref_payco ?: status.data.x_ref_payco
    is TransactionStatus.Pending  -> status.refPayco
    is TransactionStatus.Rejected -> status.data?.ref_payco
    else                          -> null
}
