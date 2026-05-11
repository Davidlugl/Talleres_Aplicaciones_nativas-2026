package com.example.herbhopper_v1.ui.seller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.data.OrderItem
import com.example.herbhopper_v1.viewmodel.OrderViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderViewModel = viewModel()
) {
    var order by remember { mutableStateOf<Order?>(null) }
    val gson = Gson()

    LaunchedEffect(orderId) {
        order = viewModel.getOrderById(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Pedido", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (order == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val currentOrder = order!!
            val listType = object : TypeToken<List<OrderItem>>() {}.type
            val items: List<OrderItem> = try {
                gson.fromJson(currentOrder.itemsJson, listType)
            } catch (e: Exception) {
                emptyList()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                // Header Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ID: ${currentOrder.orderId}", fontWeight = FontWeight.Bold)
                            DetailsOrderStatusChip(status = currentOrder.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cliente: ${currentOrder.userName}")
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(currentOrder.timestamp))
                        Text("Fecha: $date")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Productos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items) { item ->
                        ListItem(
                            headlineContent = { Text(item.productName) },
                            supportingContent = { Text("Cantidad: ${item.quantity}") },
                            trailingContent = { Text("$${item.price * item.quantity}") }
                        )
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Totals & Address
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dirección de Entrega:", fontWeight = FontWeight.Bold)
                        Text(currentOrder.address ?: "No especificada")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("$${currentOrder.totalAmount}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                if (currentOrder.status == "PENDING" || currentOrder.status == "PENDIENTE") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { 
                                viewModel.updateOrderStatus(currentOrder.orderId, "ACCEPTED")
                                order = currentOrder.copy(status = "ACCEPTED")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACEPTAR")
                        }
                        
                        Button(
                            onClick = { 
                                viewModel.updateOrderStatus(currentOrder.orderId, "CANCELLED")
                                order = currentOrder.copy(status = "CANCELLED")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Cancel, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CANCELAR")
                        }
                    }
                } else if (currentOrder.status == "ACCEPTED") {
                    Button(
                        onClick = { 
                            viewModel.updateOrderStatus(currentOrder.orderId, "DELIVERED")
                            order = currentOrder.copy(status = "DELIVERED")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("MARCAR COMO ENTREGADO")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsOrderStatusChip(status: String) {
    val color = when (status.uppercase()) {
        "PENDING", "PENDIENTE" -> Color(0xFFFFA000)
        "ACCEPTED", "ACEPTADO" -> Color(0xFF4CAF50)
        "CANCELLED", "CANCELADO" -> Color(0xFFF44336)
        "DELIVERED", "ENTREGADO" -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
