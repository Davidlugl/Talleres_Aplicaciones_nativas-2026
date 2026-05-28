package com.example.herbhopper_v1.ui.patient

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.herbhopper_v1.data.PaymentRepository
import com.example.herbhopper_v1.viewmodel.TransactionStatus
import kotlinx.coroutines.launch
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.ui.theme.EpaycoPortalBackground
import com.example.herbhopper_v1.ui.theme.EpaycoLoaderSuccess
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb

/**
 * Pantalla que carga la pasarela de pago oficial de ePayco en un WebView seguro.
 * Utiliza el widget oficial de ePayco y la llave pública del usuario.
 *
 * @param amount       Monto total del pedido.
 * @param orderId      Referencia de la orden.
 * @param description  Descripción de la orden.
 * @param onBack       Callback para regresar.
 * @param onFinished   Callback al completar la transacción con el estado obtenido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpaycoWebViewScreen(
    amount: Double,
    orderId: String,
    description: String,
    onBack: () -> Unit,
    onFinished: (TransactionStatus) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val paymentRepository = remember { PaymentRepository() }

    val context = LocalContext.current
    val connectingText = remember { context.getString(R.string.connecting_epayco) }
    val openingText = remember { context.getString(R.string.opening_epayco_secure) }
    val epaycoPortalTitle = remember { context.getString(R.string.epayco_portal_title) }
    val orderName = remember { context.getString(R.string.order_epayco_title) }

    // Generar el HTML dinámico con el Script oficial de ePayco usando la llave del usuario
    val htmlContent = remember(amount, orderId, description) {
        val amountInt = amount.toInt().toString()
        val publicKey = com.example.herbhopper_v1.data.network.EpaycoConfig.PUBLIC_KEY
        val testMode = com.example.herbhopper_v1.data.network.EpaycoConfig.IS_TEST.toString()
        val bgColorHex = String.format("#%06X", (0xFFFFFF and EpaycoPortalBackground.toArgb()))
        val successColorHex = String.format("#%06X", (0xFFFFFF and EpaycoLoaderSuccess.toArgb()))

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>$epaycoPortalTitle</title>
            <style>
                body {
                    background-color: $bgColorHex;
                    color: #FFFFFF;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                    display: flex;
                    flex-direction: column;
                    justify-content: center;
                    align-items: center;
                    height: 100vh;
                    margin: 0;
                    padding: 20px;
                    box-sizing: border-box;
                    text-align: center;
                }
                .loader-container {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                }
                .spinner {
                    border: 4px solid rgba(255, 255, 255, 0.1);
                    width: 50px;
                    height: 50px;
                    border-radius: 50%;
                    border-left-color: $successColorHex;
                    animation: spin 1s linear infinite;
                    margin-bottom: 20px;
                }
                @keyframes spin {
                    0% { transform: rotate(0deg); }
                    100% { transform: rotate(360deg); }
                }
                h3 {
                    margin: 0 0 10px 0;
                    font-size: 1.2rem;
                    font-weight: 600;
                }
                p {
                    color: rgba(255, 255, 255, 0.6);
                    font-size: 0.9rem;
                    margin: 0;
                }
                /* Ocultar el formulario y botón original de ePayco para auto-activarlo */
                #payment-form {
                    display: none;
                }
            </style>
        </head>
        <body>
            <div class="loader-container" id="loader">
                <div class="spinner"></div>
                <h3>$connectingText</h3>
                <p>$openingText</p>
            </div>

            <form id="payment-form">
                <script src="https://checkout.epayco.co/checkout.js"
                    data-epayco-key="$publicKey" 
                    class="epayco-button" 
                    data-epayco-amount="$amountInt" 
                    data-epayco-tax="0.00"  
                    data-epayco-tax-ico="0.00"               
                    data-epayco-tax-base="$amountInt"
                    data-epayco-name="$orderName" 
                    data-epayco-description="$description" 
                    data-epayco-currency="COP"    
                    data-epayco-country="CO" 
                    data-epayco-test="$testMode" 
                    data-epayco-external="false" 
                    data-epayco-extra1="$orderId"
                    data-epayco-response="https://herb-hopper-v2.onrender.com/api/payment/response"  
                    data-epayco-confirmation="https://herb-hopper-v2.onrender.com/api/payment/confirmation"
                    data-epayco-button="https://multimedia.epayco.co/dashboard/btns/btn5.png"> 
                </script> 
            </form> 

            <script>
                // Intervalo para auto-cliquear el botón generado por ePayco apenas esté listo
                var checkInterval = setInterval(function() {
                    var btn = document.querySelector('.epayco-button-render');
                    if (btn) {
                        clearInterval(checkInterval);
                        btn.click();
                        // Ocultar el cargador local una vez se abra el modal
                        setTimeout(function() {
                            document.getElementById('loader').style.display = 'none';
                        }, 1000);
                    }
                }, 100);
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.epayco_payment_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EpaycoPortalBackground,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(EpaycoPortalBackground)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            supportZoom()
                            builtInZoomControls = false
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            // Interceptar la redirección final de ePayco para obtener la referencia de pago
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: ""
                                
                                // ePayco suele redirigir a URLs de confirmación que contienen la variable de referencia
                                if (url.contains("refPayco=") || url.contains("ref_payco=")) {
                                    val refPayco = extractQueryParam(url, "refPayco") ?: extractQueryParam(url, "ref_payco")
                                    if (refPayco != null) {
                                        // Consultar el estado real de la transacción usando el Repositorio de la app
                                        coroutineScope.launch {
                                            isLoading = true
                                            val result = paymentRepository.confirmTransaction(refPayco)
                                            val mappedStatus = when (result) {
                                                is com.example.herbhopper_v1.data.PaymentResult.Success -> 
                                                    TransactionStatus.Accepted(result.transaction)
                                                is com.example.herbhopper_v1.data.PaymentResult.Rejected -> 
                                                    TransactionStatus.Rejected(result.reason, result.transaction)
                                                is com.example.herbhopper_v1.data.PaymentResult.Pending -> 
                                                    TransactionStatus.Pending(result.refPayco, result.transaction)
                                                is com.example.herbhopper_v1.data.PaymentResult.Error -> 
                                                    TransactionStatus.Failed(result.message)
                                            }
                                            onFinished(mappedStatus)
                                        }
                                        return true
                                    }
                                }
                                
                                // Si el usuario cancela o regresa a una página local de éxito
                                if (url.contains("herb-hopper-v2.onrender.com/api/payment/response")) {
                                    val refPayco = extractQueryParam(url, "ref_payco") ?: extractQueryParam(url, "refPayco")
                                    if (refPayco != null) {
                                        coroutineScope.launch {
                                            isLoading = true
                                            val result = paymentRepository.confirmTransaction(refPayco)
                                            val mappedStatus = when (result) {
                                                is com.example.herbhopper_v1.data.PaymentResult.Success -> 
                                                    TransactionStatus.Accepted(result.transaction)
                                                is com.example.herbhopper_v1.data.PaymentResult.Rejected -> 
                                                    TransactionStatus.Rejected(result.reason, result.transaction)
                                                is com.example.herbhopper_v1.data.PaymentResult.Pending -> 
                                                    TransactionStatus.Pending(result.refPayco, result.transaction)
                                                is com.example.herbhopper_v1.data.PaymentResult.Error -> 
                                                    TransactionStatus.Failed(result.message)
                                            }
                                            onFinished(mappedStatus)
                                        }
                                        return true
                                    }
                                }

                                return false
                            }
                        }

                        // Cargar el HTML pre-generado localmente
                        loadDataWithBaseURL("https://checkout.epayco.co", htmlContent, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EpaycoPortalBackground.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EpaycoLoaderSuccess)
                }
            }
        }
    }
}

/**
 * Utilidad simple para extraer parámetros de consulta de una URL.
 */
private fun extractQueryParam(url: String, paramName: String): String? {
    return try {
        val uri = android.net.Uri.parse(url)
        uri.getQueryParameter(paramName)
    } catch (e: Exception) {
        null
    }
}
