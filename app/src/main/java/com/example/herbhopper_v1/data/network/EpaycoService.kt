package com.example.herbhopper_v1.data.network

import com.example.herbhopper_v1.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// Configuración de ePayco
//
// La app solo conoce la llave PÚBLICA, que ePayco permite exponer en el cliente.
// La llave privada y el P_KEY viven únicamente en el backend (variables de entorno),
// que es quien valida la firma de la confirmación. Los datos de tarjeta nunca pasan
// por la app ni por el backend: se digitan en el checkout oficial de ePayco (PCI DSS).
// ---------------------------------------------------------------------------
object EpaycoConfig {
    val PUBLIC_KEY: String = BuildConfig.EPAYCO_PUBLIC_KEY

    /** true = sandbox. Se controla desde build.gradle (EPAYCO_TEST). */
    val IS_TEST: Boolean = BuildConfig.EPAYCO_TEST

    const val CURRENCY = "COP"
    const val COUNTRY = "CO"

    const val BASE_URL_VALIDATION = "https://secure.epayco.co/"

    /** Códigos de medio de pago del checkout de ePayco, para ocultar los que el usuario no eligió. */
    const val METHOD_CARD = "TDC"
    const val METHOD_PSE = "PSE"
    const val METHOD_CASH = "CASH"
    val ALL_METHODS = listOf("TDC", "PSE", "CASH", "SP", "DP")
}

/** Datos de la transacción devueltos por ePayco. */
data class EpaycoTransactionData(
    val ref_payco: String?,
    val x_ref_payco: String?,
    val x_transaction_id: String?,
    val x_amount: String?,
    val x_amount_country: String?,
    val x_currency_code: String?,
    val x_bank_name: String?,
    val x_cardnumber: String?,           // Enmascarado por ePayco
    val x_cod_response: String?,         // 1=Aceptada, 2=Rechazada, 3=Pendiente, 4=Fallida
    val x_response: String?,
    val x_response_reason_text: String?,
    val x_franchise: String?,
    val x_transaction_date: String?,
    val x_approval_code: String?,
    val x_id_invoice: String? = null
)

data class EpaycoTransactionQueryResponse(
    val success: Boolean,
    val data: EpaycoTransactionData?
)

/**
 * Consulta pública del resultado de una transacción por su referencia (ref_payco).
 * No requiere llaves; es el mismo servicio que usa la página de respuesta de ePayco.
 */
interface EpaycoValidationApi {
    @GET("validation/v1/reference/{refPayco}")
    suspend fun getTransactionByRef(@Path("refPayco") refPayco: String): EpaycoTransactionQueryResponse

    companion object {
        fun create(): EpaycoValidationApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(EpaycoConfig.BASE_URL_VALIDATION)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(EpaycoValidationApi::class.java)
        }
    }
}
