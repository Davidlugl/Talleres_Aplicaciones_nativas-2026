package com.example.herbhopper_v1.ui.patient

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.herbhopper_v1.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionValidationScreen(onBack: () -> Unit, onHelpClick: () -> Unit = {}) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            Toast.makeText(context, "Foto capturada con éxito", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Imagen seleccionada", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Box(modifier = Modifier.size(40.dp).background(Color.Transparent, CircleShape))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.primary) }
                },
                actions = {
                    IconButton(onClick = onHelpClick) { Icon(Icons.AutoMirrored.Filled.HelpOutline, null, tint = MaterialTheme.colorScheme.primary) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            // Viewfinder cutout simulation
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)))
                Row(modifier = Modifier.height(420.dp)) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.4f)))
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                    ) {
                        // Corner Markers
                        ScannerCorner(Alignment.TopStart)
                        ScannerCorner(Alignment.TopEnd)
                        ScannerCorner(Alignment.BottomStart)
                        ScannerCorner(Alignment.BottomEnd)
                        
                        // Scanning line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.Center)
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.Black.copy(alpha = 0.4f)))
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(id = R.string.prescription_frame_msg),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    stringResource(id = R.string.prescription_instruction),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }

            // Controls
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ControlButton(
                            icon = Icons.Default.PhotoLibrary, 
                            label = stringResource(id = R.string.gallery),
                            onClick = { galleryLauncher.launch("image/*") }
                        )
                        
                        // Shutter Button
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .border(6.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)))
                                .clickable { 
                                    when (PackageManager.PERMISSION_GRANTED) {
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) -> {
                                            cameraLauncher.launch()
                                        }
                                        else -> {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }

                        ControlButton(
                            icon = Icons.Default.FlashOn, 
                            label = stringResource(id = R.string.flash),
                            onClick = { Toast.makeText(context, "Flash automático activado", Toast.LENGTH_SHORT).show() }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                stringResource(id = R.string.prescription_footer_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun BoxScope.ScannerCorner(alignment: Alignment) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .align(alignment)
    ) {
        val color = MaterialTheme.colorScheme.secondaryContainer
        val stroke = 4.dp
        val radius = 12.dp
        
        when (alignment) {
            Alignment.TopStart -> {
                Box(modifier = Modifier.fillMaxWidth().height(stroke).background(color, RoundedCornerShape(radius)))
                Box(modifier = Modifier.width(stroke).fillMaxHeight().background(color, RoundedCornerShape(radius)))
            }
            Alignment.TopEnd -> {
                Box(modifier = Modifier.fillMaxWidth().height(stroke).background(color, RoundedCornerShape(radius)))
                Box(modifier = Modifier.width(stroke).fillMaxHeight().align(Alignment.TopEnd).background(color, RoundedCornerShape(radius)))
            }
            Alignment.BottomStart -> {
                Box(modifier = Modifier.fillMaxWidth().height(stroke).align(Alignment.BottomStart).background(color, RoundedCornerShape(radius)))
                Box(modifier = Modifier.width(stroke).fillMaxHeight().background(color, RoundedCornerShape(radius)))
            }
            Alignment.BottomEnd -> {
                Box(modifier = Modifier.fillMaxWidth().height(stroke).align(Alignment.BottomEnd).background(color, RoundedCornerShape(radius)))
                Box(modifier = Modifier.width(stroke).fillMaxHeight().align(Alignment.BottomEnd).background(color, RoundedCornerShape(radius)))
            }
        }
    }
}
