package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.theme.*
import com.example.herbhopper_v1.ui.components.PatientBottomNavBar
import com.example.herbhopper_v1.data.Order
import androidx.compose.foundation.lazy.items

/**
 * Pantalla de Historial de Pedidos (Órdenes) del paciente.
 * Obtiene y lista todos los pedidos del usuario logueado en tiempo real utilizando [OrderViewModel].
 * Permite monitorear el estado actual de cada pedido (Pendiente, Aceptado, Entregado, Cancelado)
 * y muestra un resumen gráfico de cantidades y costos totales.
 *
 * @param onHomeClick Función callback para navegar a la sección de Inicio.
 * @param onOrdersClick Función callback para navegar a esta misma sección (Historial).
 * @param onScriptsClick Función callback para navegar a la sección de Recetas.
 * @param onProfileClick Función callback para navegar a la sección de Perfil.
 * @param onCartClick Función callback para navegar a la sección de Carrito de Compras.
 * @param viewModel Instancia de [OrderViewModel] para la consulta y flujo de pedidos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onHomeClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScriptsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCartClick: () -> Unit = {},
    viewModel: com.example.herbhopper_v1.viewmodel.OrderViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val orders by viewModel.getOrdersByUser("david_g").collectAsState(initial = emptyList())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).background(Color.Transparent, CircleShape))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                actions = {
                    IconButton(onClick = onCartClick) { Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(id = R.string.order_history_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(id = R.string.order_history_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (orders.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No tienes pedidos aún",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tus compras aparecerán listadas aquí para hacerles seguimiento.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                items(
                    items = orders,
                    key = { it.orderId }
                ) { order ->
                    OrderCard(order = order)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

/**
 * Tarjeta individual que muestra los detalles resumidos de un pedido en el historial.
 * Deserializa la lista de ítems del pedido desde JSON, calcula la cantidad de artículos y
 * muestra un chip de estado de color dinámico (Verde para Entregado, Azul para Aceptado,
 * Rojo para Cancelado, Gris para Pendiente), junto con la fecha y el monto total en COP.
 *
 * @param order El objeto de datos [Order] a renderizar.
 */
@Composable
fun OrderCard(order: Order) {
    val items = remember(order.itemsJson) {
        val listType = object : com.google.gson.reflect.TypeToken<List<com.example.herbhopper_v1.data.OrderItem>>() {}.type
        try {
            com.google.gson.Gson().fromJson<List<com.example.herbhopper_v1.data.OrderItem>>(order.itemsJson, listType)
        } catch (e: Exception) {
            emptyList()
        }
    }

    val firstProductName = items.firstOrNull()?.productName ?: "Pedido"
    val itemsCount = items.sumOf { it.quantity }
    val dateStr = remember(order.timestamp) {
        java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(order.timestamp))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingBag, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("ID: ${order.orderId.takeLast(6)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(4.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(firstProductName + (if (items.size > 1) " + ${items.size - 1} más" else ""), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(id = R.string.items_count, itemsCount, "HerbHopper Store"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val statusText = when (order.status.uppercase()) {
                    "PENDING", "PENDIENTE" -> "PENDIENTE"
                    "ACCEPTED", "ACEPTADO" -> "ACEPTADO"
                    "CANCELLED", "CANCELADO" -> "CANCELADO"
                    "DELIVERED", "ENTREGADO" -> "ENTREGADO"
                    else -> order.status
                }
                
                Surface(
                    color = when(order.status.uppercase()) {
                        "DELIVERED", "ENTREGADO" -> MaterialTheme.colorScheme.tertiaryContainer
                        "ACCEPTED", "ACEPTADO" -> MaterialTheme.colorScheme.primaryContainer
                        "CANCELLED", "CANCELADO" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = CircleShape
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        val dotColor = when(order.status.uppercase()) {
                            "DELIVERED", "ENTREGADO" -> MaterialTheme.colorScheme.tertiary
                            "ACCEPTED", "ACEPTADO" -> MaterialTheme.colorScheme.primary
                            "CANCELLED", "CANCELADO" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                        Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            statusText.uppercase(), 
                            style = MaterialTheme.typography.labelSmall, 
                            fontWeight = FontWeight.Black, 
                            color = when(order.status.uppercase()) {
                                "DELIVERED", "ENTREGADO" -> MaterialTheme.colorScheme.onTertiaryContainer
                                "ACCEPTED", "ACEPTADO" -> MaterialTheme.colorScheme.onPrimaryContainer
                                "CANCELLED", "CANCELADO" -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Text("$${String.format("%,.0f", order.totalAmount)} COP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
