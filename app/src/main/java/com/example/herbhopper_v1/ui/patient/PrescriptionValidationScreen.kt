package com.example.herbhopper_v1.ui.patient

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.example.herbhopper_v1.R

/**
 * Pantalla de Validación de Recetas Médicas (Prescription Validation).
 * Permite al paciente digitalizar su receta médica mediante la cámara nativa del dispositivo
 * o seleccionarla desde la galería de imágenes para su correspondiente validación clínica.
 * Solicita permisos de cámara en tiempo de ejecución de manera segura y simula un visor de escáner.
 *
 * @param onBack Función callback para cerrar la pantalla o regresar a la sección anterior.
 * @param onHelpClick Función callback para abrir una sección de ayuda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionValidationScreen(onBack: () -> Unit, onHelpClick: () -> Unit = {}) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var flashMode by remember { mutableStateOf("off") } // "off", "on", "auto"
    var cameraInstance by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
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

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(flashMode, cameraInstance) {
        try {
            cameraInstance?.cameraControl?.enableTorch(flashMode == "on")
        } catch (e: Exception) {
            e.printStackTrace()
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
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    imageCapture = imageCapture,
                    flashMode = flashMode,
                    onCameraReady = { camera ->
                        cameraInstance = camera
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Esperando permiso de cámara...", color = Color.White)
                }
            }
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
                                    if (hasCameraPermission) {
                                        val executor = ContextCompat.getMainExecutor(context)
                                        imageCapture.flashMode = when (flashMode) {
                                            "on" -> ImageCapture.FLASH_MODE_ON
                                            "auto" -> ImageCapture.FLASH_MODE_AUTO
                                            else -> ImageCapture.FLASH_MODE_OFF
                                        }
                                        imageCapture.takePicture(
                                            executor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                                                    try {
                                                        val bitmap = image.toBitmap()
                                                        capturedBitmap = bitmap
                                                        image.close()
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        Toast.makeText(context, "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    exception.printStackTrace()
                                                    Toast.makeText(context, "Error al capturar foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
                        }

                        val (flashIcon, flashLabel, flashToast) = when (flashMode) {
                            "on" -> Triple(Icons.Default.FlashOn, "Encendido", "Flash encendido")
                            "auto" -> Triple(Icons.Default.FlashAuto, "Automático", "Flash automático activado")
                            else -> Triple(Icons.Default.FlashOff, "Apagado", "Flash apagado")
                        }

                        ControlButton(
                            icon = flashIcon, 
                            label = flashLabel,
                            onClick = { 
                                flashMode = when (flashMode) {
                                    "off" -> "on"
                                    "on" -> "auto"
                                    else -> "off"
                                }
                                Toast.makeText(context, flashToast, Toast.LENGTH_SHORT).show()
                            }
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

            capturedBitmap?.let { bitmap ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            "Verifique su receta",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                        
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Receta capturada",
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { capturedBitmap = null }, // retry
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reintentar")
                            }
                            
                            Button(
                                onClick = { 
                                    Toast.makeText(context, "Foto capturada con éxito", Toast.LENGTH_SHORT).show()
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aceptar")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Botón circular secundario de control para la interfaz de la cámara.
 * Usado para la selección de galería y encendido/apagado de flash.
 *
 * @param icon Icono descriptivo de tipo [ImageVector].
 * @param label Texto corto debajo del botón.
 * @param onClick Acción ejecutada al hacer clic sobre el botón.
 */
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

/**
 * Esquinas visuales del visor de escaneo de recetas.
 * Dibuja líneas en forma de L para simular el área focal donde se debe encuadrar el documento.
 *
 * @param alignment La posición de la esquina (ej. [Alignment.TopStart], [Alignment.BottomEnd]).
 */
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

/**
 * Composable que encapsula el PreviewView de CameraX para Jetpack Compose.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    imageCapture: ImageCapture,
    flashMode: String,
    onCameraReady: (androidx.camera.core.Camera) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    onCameraReady(camera)
                } catch (exc: Exception) {
                    exc.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
            
            previewView
        },
        modifier = modifier
    )
}
