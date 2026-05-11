package com.example.herbhopper_v1.ui.seller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.viewmodel.OrderViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingOrdersScreen(
    onBack: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
    viewModel: OrderViewModel = viewModel()
) {
    val orders by viewModel.allOrders.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.incoming_orders_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                }
            )
        },
        bottomBar = {
            SellerBottomNavBar(
                currentRoute = "orders",
                onNavigate = onNavigate
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(orders) { order ->
                OrderCard(
                    order = order,
                    onAccept = { viewModel.updateOrderStatus(order.orderId, "ACCEPTED") },
                    onDetails = { onNavigate("order_details/${order.orderId}") }
                )
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    onAccept: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(id = R.string.order_number, order.orderId.takeLast(4)),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IncomingOrderStatusChip(status = order.status)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(id = R.string.order_client, order.userName), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(id = R.string.order_total, "$${String.format("%,.0f", order.totalAmount)} COP"), fontWeight = FontWeight.Bold)

            val date = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(order.timestamp))
            Text(stringResource(id = R.string.order_date, date), style = MaterialTheme.typography.labelSmall)

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (order.status == "PENDING") {
                    Button(onClick = onAccept, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text(stringResource(id = R.string.accept_btn))
                    }
                }
                OutlinedButton(onClick = onDetails, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text(stringResource(id = R.string.details_btn))
                }
            }
        }
    }
}

@Composable
fun IncomingOrderStatusChip(status: String) {
    val color = when (status.uppercase()) {
        "PENDING" -> colorResource(id = R.color.status_pending)
        "ACCEPTED" -> colorResource(id = R.color.status_accepted)
        "CANCELLED" -> colorResource(id = R.color.status_cancelled)
        "DELIVERED" -> colorResource(id = R.color.status_delivered)
        else -> MaterialTheme.colorScheme.secondary
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
