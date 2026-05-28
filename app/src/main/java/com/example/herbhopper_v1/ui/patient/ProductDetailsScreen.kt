package com.example.herbhopper_v1.ui.patient

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.ui.theme.*

/**
 * Pantalla de Detalle de Producto para el paciente.
 * Renderiza la información extendida de una formulación o artículo seleccionado.
 * Muestra la imagen principal, sello de calidad premium, concentración química (CBD/THC),
 * descripción de componentes, beneficios destacados (relajación, antiinflamatorio), precio por volumen,
 * modo de uso y certificaciones de laboratorio. Permite agregar directamente al carrito de compras.
 *
 * @param product El objeto [Product] del cual se muestran los detalles.
 * @param onBack Función callback para regresar a la pantalla anterior.
 * @param onAddToCart Función callback para agregar este producto al carrito.
 * @param cartItemCount Conteo actual de artículos en el carrito para mostrar en la insignia superior.
 * @param onCartClick Función callback para navegar al carrito de compras.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    product: Product,
    onBack: () -> Unit,
    onAddToCart: (Product) -> Unit,
    cartItemCount: Int,
    onCartClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.details), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.primary) }
                },
                actions = {
                    Box(modifier = Modifier.padding(end = 8.dp)) {
                        IconButton(onClick = onCartClick) { Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary) }
                        if (cartItemCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = CircleShape,
                                modifier = Modifier.size(16.dp).align(Alignment.TopEnd)
                            ) {
                                Text(
                                    cartItemCount.toString(),
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val isWebUrl = product.imageUrl?.startsWith("http") == true
            val imageResId = remember(product.imageUrl) {
                if (!product.imageUrl.isNullOrEmpty() && !isWebUrl) {
                    context.resources.getIdentifier(product.imageUrl, "drawable", context.packageName)
                } else {
                    0
                }
            }

            // Main Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (isWebUrl) {
                    coil.compose.AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else if (imageResId != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = imageResId),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(stringResource(id = R.string.premium_grade), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(stringResource(id = R.string.herb_hopper_premium), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
                Text(product.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))

                Spacer(modifier = Modifier.height(24.dp))

                // Specs Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpecCard(label = stringResource(id = R.string.concentration), value = "10% CBD", modifier = Modifier.weight(1f), isHighlighted = true)
                    SpecCard(label = stringResource(id = R.string.thc_content), value = "<0.2% THC", modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(stringResource(id = R.string.description), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Benefits Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BenefitChip(icon = Icons.Default.Spa, label = stringResource(id = R.string.relaxation))
                    BenefitChip(icon = Icons.Default.HealthAndSafety, label = stringResource(id = R.string.anti_inflammatory))
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pricing and Action
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$${String.format("%,.0f", product.price)} COP", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(id = R.string.per_bottle, "30ml"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onAddToCart(product) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.ShoppingBasket, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.add_to_cart), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Usage Section
                InfoRow(icon = Icons.Default.MedicalServices, title = stringResource(id = R.string.usage_mode), description = "Aplicar 2-3 gotas debajo de la lengua (sublingual), mantener por 60 segundos antes de tragar.")
                Spacer(modifier = Modifier.height(24.dp))
                InfoRow(icon = Icons.Default.Science, title = stringResource(id = R.string.certification), description = "Laboratorio certificado ISO-9001. Tercera parte probada para pureza y potencia.")

                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

/**
 * Tarjeta de especificación técnica del producto.
 * Muestra el nombre del parámetro (ej. THC Content) y su valor destacado (ej. <0.2% THC).
 *
 * @param label Nombre del parámetro.
 * @param value Valor correspondiente.
 * @param modifier Modificador de Compose para adaptar dimensiones.
 * @param isHighlighted Indica si la tarjeta debe tener un borde destacado.
 */
@Composable
fun SpecCard(label: String, value: String, modifier: Modifier = Modifier, isHighlighted: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * Chip para representar los beneficios terapéuticos del producto.
 * Muestra un icono pequeño y el nombre del beneficio.
 *
 * @param icon Icono representativo del beneficio.
 * @param label Nombre del beneficio.
 */
@Composable
fun BenefitChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = CircleShape
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/**
 * Fila informativa estructurada para modo de uso y certificaciones.
 * Consiste en un icono rodeado de un círculo primario y columnas de texto descriptivo a su derecha.
 *
 * @param icon Icono representativo del tema de información.
 * @param title Título representativo de la sección.
 * @param description Texto descriptivo extendido.
 */
@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

