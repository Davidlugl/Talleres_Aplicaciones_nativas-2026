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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
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
                title = { Text(stringResource(id = R.string.order_details_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(id = R.string.order_id_label, currentOrder.orderId), fontWeight = FontWeight.Bold)
                            DetailsOrderStatusChip(status = currentOrder.status)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(id = R.string.client_label, currentOrder.userName))
                        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(currentOrder.timestamp))
                        Text(stringResource(id = R.string.date_label, date))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(id = R.string.products_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(items) { item ->
                        ListItem(
                            headlineContent = { Text(item.productName) },
                            supportingContent = { Text(stringResource(id = R.string.quantity_label, item.quantity)) },
                            trailingContent = { Text("$${String.format("%,.0f", item.price * item.quantity)} COP") }
                        )
                        HorizontalDivider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(id = R.string.delivery_address_label), fontWeight = FontWeight.Bold)
                        Text(currentOrder.address ?: stringResource(id = R.string.no_address))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(id = R.string.total_label), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("$${String.format("%,.0f", currentOrder.totalAmount)} COP", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (currentOrder.status == "PENDING" || currentOrder.status == "PENDIENTE") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(currentOrder.orderId, "ACCEPTED")
                                order = currentOrder.copy(status = "ACCEPTED")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.status_accepted))
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.accept_order))
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
                            Text(stringResource(id = R.string.cancel_order))
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
                        Text(stringResource(id = R.string.mark_as_delivered))
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsOrderStatusChip(status: String) {
    val color = when (status.uppercase()) {
        "PENDING", "PENDIENTE" -> colorResource(id = R.color.status_pending)
        "ACCEPTED", "ACEPTADO" -> colorResource(id = R.color.status_accepted)
        "CANCELLED", "CANCELADO" -> colorResource(id = R.color.status_cancelled)
        "DELIVERED", "ENTREGADO" -> colorResource(id = R.color.status_delivered)
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
