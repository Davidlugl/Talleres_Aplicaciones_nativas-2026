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

/**
 * Pantalla de Seguimiento de Pedidos en Tiempo Real (Order Tracking).
 * Simula un mapa de despacho interactivo, muestra el estado actual del envío ("En Camino"),
 * una barra de progreso indicando la cercanía del repartidor, los datos identificativos del transportador,
 * el tipo de vehículo ecológico de reparto y la hora estimada de llegada (ETA).
 *
 * @param onHomeClick Función callback para navegar a la pantalla de Inicio.
 * @param onOrdersClick Función callback para navegar al historial de pedidos.
 * @param onScriptsClick Función callback para navegar a la validación de recetas médicas.
 * @param onProfileClick Función callback para navegar al perfil del usuario.
 * @param onCartClick Función callback para navegar al carrito de compras.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    onHomeClick: () -> Unit,
    onOrdersClick: () -> Unit,
    onScriptsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCartClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.size(40.dp).background(Color.Transparent, CircleShape))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onHomeClick) { Icon(Icons.Default.Menu, null, tint = MaterialTheme.colorScheme.primary) }
                },
                actions = {
                    IconButton(onClick = onCartClick) { Icon(Icons.Default.ShoppingBasket, null, tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White.copy(alpha = 0.8f))
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
        ) {
            item {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(stringResource(id = R.string.order_tracking_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(stringResource(id = R.string.order_tracking_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                // Map Simulation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(stringResource(id = R.string.map_placeholder), modifier = Modifier.align(Alignment.Center))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tracking Details
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape) {
                                Text(stringResource(id = R.string.on_the_way), modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Text(stringResource(id = R.string.dummy_order_id), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(stringResource(id = R.string.tracking_msg), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        LinearProgressIndicator(
                            progress = { 0.75f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(id = R.string.delivery_person), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(stringResource(id = R.string.delivery_name), fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(id = R.string.vehicle_type), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(stringResource(id = R.string.bolt_ev_2), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // ETA Card
                Surface(
                    modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(stringResource(id = R.string.estimated_arrival), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                            Text(stringResource(id = R.string.eta_time), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        }
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}
