package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.model.CartItem
import com.example.herbhopper_v1.viewmodel.CartViewModel

/**
 * Pantalla del Carrito de Compras del paciente.
 * Permite visualizar los productos agregados, ajustar sus cantidades, eliminarlos y ver un resumen
 * detallado de costos (subtotal, envío y total) antes de proceder al pago.
 *
 * @param viewModel Instancia de [CartViewModel] que gestiona el estado y operaciones del carrito de compras.
 * @param onBack Función de retorno (callback) que se ejecuta al presionar el botón de regresar.
 * @param onCheckout Función de retorno (callback) que se ejecuta al presionar el botón de pagar (Checkout).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    val items by viewModel.items.collectAsState()

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
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(stringResource(id = R.string.shopping_cart), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Text(stringResource(id = R.string.cart_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (items.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(id = R.string.empty_cart), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                items(items) { item ->
                    // Solución al Issue 4: Usar CartItemRow para evitar conflictos de nombres con la entidad de modelo CartItem
                    CartItemRow(
                        item = item,
                        onUpdateQuantity = { delta -> viewModel.updateQuantity(item.product.id, delta) },
                        onRemove = { viewModel.removeFromCart(item.product.id) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    // Summary Section
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(stringResource(id = R.string.summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            SummaryRow(stringResource(id = R.string.subtotal), "$${String.format("%,.0f", viewModel.total)} COP")
                            SummaryRow(stringResource(id = R.string.clinical_shipping), stringResource(id = R.string.free), color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(
                                modifier = Modifier,
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                Text(stringResource(id = R.string.total), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("$${String.format("%,.0f", viewModel.total)} COP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onCheckout,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(stringResource(id = R.string.checkout), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.continue_shopping), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(64.dp))
                }
            }
        }
    }
}

/**
 * Fila individual que representa un producto dentro del carrito de compras.
 * Muestra la imagen, nombre, precio unitario y permite aumentar, disminuir o eliminar el producto.
 *
 * @param item El objeto de tipo [CartItem] que contiene la información del producto y su cantidad seleccionada.
 * @param onUpdateQuantity Función que recibe la diferencia (delta) para actualizar la cantidad del producto.
 * @param onRemove Función que se ejecuta para eliminar el producto del carrito.
 */
@Composable
fun CartItemRow(
    item: CartItem,
    onUpdateQuantity: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val isWebUrl = item.product.imageUrl?.startsWith("http") == true
            val imageResId = remember(item.product.imageUrl) {
                item.product.getDrawableResId(context)
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isWebUrl) {
                    coil.compose.AsyncImage(
                        model = item.product.imageUrl,
                        contentDescription = item.product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else if (imageResId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageResId),
                        contentDescription = item.product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(id = R.string.botanico), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                Text(item.product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("$${String.format("%,.0f", item.product.price)} COP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Row(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onUpdateQuantity(-1) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
                Text(item.quantity.toString(), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp)
                IconButton(onClick = { onUpdateQuantity(1) }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary) }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}

/**
 * Componente que representa una fila con etiqueta y valor de resumen de costo.
 * Utilizado para mostrar de forma limpia conceptos como subtotal y costos de envío.
 *
 * @param label Etiqueta o concepto a mostrar (ej. Subtotal).
 * @param value Valor correspondiente en formato de texto.
 * @param color Color del texto del valor, por defecto el color estándar de texto.
 */
@Composable
fun SummaryRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}
