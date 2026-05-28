package com.example.herbhopper_v1.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit
import com.example.herbhopper_v1.BuildConfig

// ---------------------------------------------------------------------------
// Constantes de configuración de ePayco
// NOTA: En producción, mover a BuildConfig o a un archivo de configuración seguro.
// ---------------------------------------------------------------------------
object EpaycoConfig {
    /**
     * Clave pública de ePayco (visible en el dashboard -> Configuración -> Llaves API).
     * Modo sandbox para desarrollo; reemplazar con la llave de producción al lanzar.
     */
    val PUBLIC_KEY = BuildConfig.EPAYCO_PUBLIC_KEY

    /**
     * Llave privada (sólo para tokenizar en el backend nunca exponer en el cliente).
     * Este campo se usa para el header de autenticación en la API server-side.
     * En una arquitectura real, este llamado se haría desde el backend propio.
     */
    val PRIVATE_KEY = BuildConfig.EPAYCO_PRIVATE_KEY

    /** Indicador de modo prueba (true = sandbox, false = producción). */
    const val IS_TEST = true

    /** Moneda de los cobros. */
    const val CURRENCY = "COP"

    /** País. */
    const val COUNTRY = "CO"

    // Endpoints base
    const val BASE_URL_SECURE = "https://secure.epayco.io/"
    const val BASE_URL_API    = "https://api.secure.epayco.io/"
}

// ---------------------------------------------------------------------------
// DTO – Tokenización de tarjeta
// ---------------------------------------------------------------------------

/** Solicitud para crear un token de tarjeta (card token) en ePayco. */
data class EpaycoTokenRequest(
    val card_number: String,
    val card_exp_year: String,   // Ej: "2026"
    val card_exp_month: String,  // Ej: "12"
    val card_cvc: String,
    val hasCvv: Boolean = true
)

/** Respuesta de la tokenización. */
data class EpaycoTokenResponse(
    val status: Boolean,
    val id: String?,             // Token de tarjeta (ej: "tok_xxxxx")
    val message: String?,
    val error: String?
)

// ---------------------------------------------------------------------------
// DTO – Creación del cliente en ePayco
// ---------------------------------------------------------------------------

data class EpaycoCustomerRequest(
    val token_card: String,
    val name: String,
    val last_name: String,
    val email: String,
    val phone: String,
    val default: Boolean = true
)

data class EpaycoCustomerResponse(
    val status: Boolean,
    val data: EpaycoCustomerData?,
    val message: String?
)

data class EpaycoCustomerData(
    val customerId: String,
    val email: String,
    val token: String
)

// ---------------------------------------------------------------------------
// DTO – Cobro / Cargo (Charge)
// ---------------------------------------------------------------------------

/** Solicitud de cobro contra un cliente o token ya creado. */
data class EpaycoChargeRequest(
    val token_card: String,
    val customer_id: String,
    val doc_type: String = "CC",
    val doc_number: String,
    val name: String,
    val last_name: String,
    val email: String,
    val bill: String,            // Referencia única del pedido
    val description: String,
    val amount: String,          // Monto como String (ej: "45000")
    val currency: String = EpaycoConfig.CURRENCY,
    val country: String = EpaycoConfig.COUNTRY,
    val city: String = "Bogotá",
    val address: String,
    val phone: String,
    val cell_phone: String,
    val dues: String = "1",
    val ip: String = "190.0.0.1",
    val url_response: String = "https://herb-hopper-v2.onrender.com/api/payment/response",
    val url_confirmation: String = "https://herb-hopper-v2.onrender.com/api/payment/confirmation",
    val use_default_card_customer: Boolean = true,
    val test: Boolean = EpaycoConfig.IS_TEST
)

/** Respuesta del cobro. */
data class EpaycoChargeResponse(
    val status: Boolean,
    val data: EpaycoTransactionData?,
    val message: String?
)

/** Datos de la transacción procesada. */
data class EpaycoTransactionData(
    val ref_payco: String?,              // Referencia ePayco
    val x_ref_payco: String?,
    val x_transaction_id: String?,
    val x_amount: String?,
    val x_amount_country: String?,
    val x_currency_code: String?,
    val x_bank_name: String?,
    val x_cardnumber: String?,           // Últimos 4 dígitos
    val x_cod_response: String?,         // 1=Aceptada, 2=Rechazada, 3=Pendiente, 4=Error
    val x_response: String?,             // "Aceptada", "Rechazada", etc.
    val x_response_reason_text: String?,
    val x_franchise: String?,            // VISA, MC, AMEX, etc.
    val x_transaction_date: String?,
    val x_approval_code: String?
)

// ---------------------------------------------------------------------------
// DTO – Consulta de transacción por referencia
// ---------------------------------------------------------------------------

data class EpaycoTransactionQueryResponse(
    val success: Boolean,
    val data: EpaycoTransactionData?
)

// ---------------------------------------------------------------------------
// Retrofit interface – ePayco Secure (tokenización y cobros)
// ---------------------------------------------------------------------------

interface EpaycoSecureApi {

    /**
     * Tokeniza los datos de la tarjeta.
     * Header: Authorization: Basic base64(publicKey:privateKey)
     */
    @POST("v1/tokens")
    suspend fun tokenizeCard(
        @Header("Authorization") authHeader: String,
        @Body request: EpaycoTokenRequest
    ): EpaycoTokenResponse

    /**
     * Crea o actualiza un cliente con el token de tarjeta.
     */
    @POST("v1/customers/create")
    suspend fun createCustomer(
        @Header("Authorization") authHeader: String,
        @Body request: EpaycoCustomerRequest
    ): EpaycoCustomerResponse

    /**
     * Realiza el cobro sobre el cliente/token creado.
     */
    @POST("v1/charges/create")
    suspend fun createCharge(
        @Header("Authorization") authHeader: String,
        @Body request: EpaycoChargeRequest
    ): EpaycoChargeResponse

    companion object {
        fun create(): EpaycoSecureApi {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(EpaycoConfig.BASE_URL_SECURE)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(EpaycoSecureApi::class.java)
        }
    }
}

// ---------------------------------------------------------------------------
// Retrofit interface – ePayco API (consulta de transacciones)
// ---------------------------------------------------------------------------

interface EpaycoApiService {

    /**
     * Consulta el estado de una transacción por su referencia de ePayco.
     * Se usa para la confirmación post-pago (webhook o polling).
     */
    @GET("payment/refPayco/{refPayco}")
    suspend fun getTransactionByRef(
        @Header("Authorization") authHeader: String,
        @Path("refPayco") refPayco: String
    ): EpaycoTransactionQueryResponse

    companion object {
        fun create(): EpaycoApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(EpaycoConfig.BASE_URL_API)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(EpaycoApiService::class.java)
        }
    }
}
