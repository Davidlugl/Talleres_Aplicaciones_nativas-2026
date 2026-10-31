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
 * Pantalla "Cargar Fórmula" (validación de recetas médicas).
 * El paciente captura la fórmula con la cámara (CameraX, con flash) o elige una imagen o un PDF.
 * Antes de enviarla indica el nombre y el registro médico del profesional; el archivo se sube al
 * backend (almacenamiento privado) y queda PENDIENTE hasta que un farmacéutico verifique registro,
 * sello y firma (plazo máximo 24 h). El ícono de la barra superior muestra "Mis fórmulas" con su estado.
 *
 * @param onBack Función callback para cerrar la pantalla o regresar a la sección anterior.
 * @param onHelpClick Función callback para abrir una sección de ayuda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrescriptionValidationScreen(
    onBack: () -> Unit,
    onHelpClick: () -> Unit = {},
    prescriptionViewModel: com.example.herbhopper_v1.viewmodel.PrescriptionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    // Archivo listo para enviar: foto capturada (JPEG en caché), imagen o PDF elegido
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMime by remember { mutableStateOf("image/jpeg") }
    var doctorName by remember { mutableStateOf("") }
    var doctorRegistry by remember { mutableStateOf("") }
    var showMyPrescriptions by remember { mutableStateOf(false) }
    val isUploading by prescriptionViewModel.isUploading.collectAsState()
    val myPrescriptions by prescriptionViewModel.prescriptions.collectAsState()

    LaunchedEffect(Unit) { prescriptionViewModel.refresh() }

    fun clearSelection() {
        capturedBitmap = null
        selectedUri = null
    }
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

    // Galería / archivos: imagen de alta resolución o PDF
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedMime = context.contentResolver.getType(uri) ?: "image/jpeg"
            selectedUri = uri
            capturedBitmap = if (selectedMime.startsWith("image/")) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) }
                } catch (e: Exception) { null }
            } else null
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
                    IconButton(onClick = { prescriptionViewModel.refresh(); showMyPrescriptions = true }) {
                        Icon(Icons.Default.ListAlt, contentDescription = stringResource(id = R.string.my_prescriptions_title), tint = MaterialTheme.colorScheme.primary)
                    }
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
                            onClick = { galleryLauncher.launch(arrayOf("image/*", "application/pdf")) }
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
                                                        image.close()
                                                        // Se guarda como JPEG en la caché privada para enviarlo
                                                        val file = java.io.File(context.cacheDir, "formula_${System.currentTimeMillis()}.jpg")
                                                        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                                                        capturedBitmap = bitmap
                                                        selectedMime = "image/jpeg"
                                                        selectedUri = Uri.fromFile(file)
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

            // ---- Revisión y envío de la fórmula ----
            selectedUri?.let { uri ->
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
                            stringResource(id = R.string.prescription_review_title),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        val bitmap = capturedBitmap
                        if (bitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = stringResource(id = R.string.prescription_label),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                            )
                        } else {
                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.PictureAsPdf, null, tint = Color.White, modifier = Modifier.size(72.dp))
                                Text(uri.lastPathSegment ?: "PDF", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(id = R.string.prescription_checklist),
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        )
                        OutlinedTextField(
                            value = doctorName,
                            onValueChange = { doctorName = it },
                            label = { Text(stringResource(id = R.string.doctor_name_label)) },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = doctorRegistry,
                            onValueChange = { doctorRegistry = it },
                            label = { Text(stringResource(id = R.string.doctor_registry_label)) },
                            singleLine = true,
                            colors = fieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { clearSelection() },
                                enabled = !isUploading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Refresh, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.retry_btn))
                            }

                            Button(
                                onClick = {
                                    if (doctorName.isBlank() || doctorRegistry.isBlank()) {
                                        Toast.makeText(context, context.getString(R.string.toast_doctor_data_required), Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    prescriptionViewModel.upload(uri, selectedMime, doctorName, doctorRegistry) { error ->
                                        if (error == null) {
                                            Toast.makeText(context, context.getString(R.string.toast_prescription_sent), Toast.LENGTH_LONG).show()
                                            clearSelection()
                                            onBack()
                                        } else {
                                            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                enabled = !isUploading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isUploading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Icon(Icons.Default.CloudUpload, null)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.send_for_validation_btn))
                            }
                        }
                    }
                }
            }

            // ---- Mis fórmulas y su estado ----
            if (showMyPrescriptions) {
                AlertDialog(
                    onDismissRequest = { showMyPrescriptions = false },
                    title = { Text(stringResource(id = R.string.my_prescriptions_title)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (myPrescriptions.isEmpty()) {
                                Text(stringResource(id = R.string.no_prescriptions))
                            }
                            myPrescriptions.forEach { p ->
                                Column {
                                    Text(stringResource(id = R.string.prescription_item_title, p.id, p.doctorName), fontWeight = FontWeight.Bold)
                                    Text(prescriptionStatusLabel(p.status), style = MaterialTheme.typography.bodySmall)
                                    p.reviewNote?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showMyPrescriptions = false }) { Text(stringResource(id = R.string.close_btn)) }
                    }
                )
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
