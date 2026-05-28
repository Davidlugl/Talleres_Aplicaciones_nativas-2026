package com.example.herbhopper_v1.data

import android.util.Base64
import com.example.herbhopper_v1.data.network.EpaycoApiService
import com.example.herbhopper_v1.data.network.EpaycoChargeRequest
import com.example.herbhopper_v1.data.network.EpaycoConfig
import com.example.herbhopper_v1.data.network.EpaycoCustomerRequest
import com.example.herbhopper_v1.data.network.EpaycoSecureApi
import com.example.herbhopper_v1.data.network.EpaycoTokenRequest
import com.example.herbhopper_v1.data.network.EpaycoTransactionData

/**
 * Resultado sellado del proceso de pago, permite manejar cada estado
 * de forma exhaustiva en el ViewModel sin necesidad de excepciones.
 */
sealed class PaymentResult {
    /** La transacción fue aceptada por el banco. */
    data class Success(val transaction: EpaycoTransactionData) : PaymentResult()

    /** La transacción fue rechazada por el banco o la tarjeta. */
    data class Rejected(val reason: String, val transaction: EpaycoTransactionData?) : PaymentResult()

    /** La transacción está pendiente de confirmación (ej: PSE). */
    data class Pending(val refPayco: String?, val transaction: EpaycoTransactionData?) : PaymentResult()

    /** Error técnico: red, configuración, datos inválidos, etc. */
    data class Error(val message: String, val cause: Throwable? = null) : PaymentResult()
}

/**
 * Datos del comprador necesarios para el cobro.
 */
data class BuyerInfo(
    val name: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val docNumber: String,
    val address: String
)

/**
 * Repositorio responsable de toda la comunicación con la pasarela ePayco.
 *
 * Flujo de un pago con tarjeta:
 * 1. [tokenizeCard] → Convierte datos RAW de tarjeta en un token seguro.
 * 2. [createCustomer] → Asocia el token a un cliente ePayco (reutilizable).
 * 3. [charge] → Realiza el débito real contra el banco emisor.
 * 4. [confirmTransaction] → Consulta el estado final (para polling post-pago).
 */
class PaymentRepository(
    private val secureApi: EpaycoSecureApi = EpaycoSecureApi.create(),
    private val apiService: EpaycoApiService = EpaycoApiService.create()
) {

    // -----------------------------------------------------------------------
    // Header de autenticación (Basic Auth con Public + Private Key en Base64)
    // -----------------------------------------------------------------------
    private val authHeader: String by lazy {
        val credentials = "${EpaycoConfig.PUBLIC_KEY}:${EpaycoConfig.PRIVATE_KEY}"
        val encoded = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        "Basic $encoded"
    }

    // -----------------------------------------------------------------------
    // 1. Tokenización de tarjeta
    // -----------------------------------------------------------------------

    /**
     * Envía los datos de la tarjeta a ePayco y retorna un token seguro.
     * Los datos crudos de la tarjeta NUNCA se almacenan ni se transmiten al backend propio.
     *
     * @param cardNumber 16 dígitos sin espacios.
     * @param expMonth   Mes de vencimiento (2 dígitos, ej: "06").
     * @param expYear    Año de vencimiento completo (4 dígitos, ej: "2027").
     * @param cvv        Código de seguridad (3 dígitos).
     * @return Token de tarjeta o null si falló.
     */
    suspend fun tokenizeCard(
        cardNumber: String,
        expMonth: String,
        expYear: String,
        cvv: String
    ): Result<String> = runCatching {
        val response = secureApi.tokenizeCard(
            authHeader = authHeader,
            request = EpaycoTokenRequest(
                card_number   = cardNumber,
                card_exp_month = expMonth,
                card_exp_year  = expYear,
                card_cvc       = cvv
            )
        )
        if (response.status && response.id != null) {
            response.id
        } else {
            throw IllegalStateException(response.error ?: response.message ?: "Error al tokenizar tarjeta")
        }
    }

    // -----------------------------------------------------------------------
    // 2. Crear cliente ePayco
    // -----------------------------------------------------------------------

    /**
     * Crea un cliente en ePayco asociándolo a un token de tarjeta.
     * En una implementación real, el customerId obtenido se guardaría en la BD del usuario
     * para futuros cobros sin pedir de nuevo los datos de tarjeta.
     *
     * @return customerId del cliente creado en ePayco.
     */
    suspend fun createCustomer(
        cardToken: String,
        buyer: BuyerInfo
    ): Result<String> = runCatching {
        val response = secureApi.createCustomer(
            authHeader = authHeader,
            request = EpaycoCustomerRequest(
                token_card = cardToken,
                name       = buyer.name,
                last_name  = buyer.lastName,
                email      = buyer.email,
                phone      = buyer.phone
            )
        )
        if (response.status && response.data?.customerId != null) {
            response.data.customerId
        } else {
            throw IllegalStateException(response.message ?: "Error al crear cliente ePayco")
        }
    }

    // -----------------------------------------------------------------------
    // 3. Cobro principal
    // -----------------------------------------------------------------------

    /**
     * Realiza el cobro completo en ePayco.
     * Internamente ejecuta los pasos de tokenización y creación de cliente si es necesario.
     *
     * @param cardNumber   Número de tarjeta (16 dígitos).
     * @param expiry       Fecha en formato "MMAA" (ej: "0627").
     * @param cvv          CVV de 3 dígitos.
     * @param amount       Monto en COP (ej: 45000.0).
     * @param orderId      ID único del pedido (usado como referencia de factura).
     * @param description  Descripción del cobro.
     * @param buyer        Datos del comprador.
     */
    suspend fun processCardPayment(
        cardNumber: String,
        expiry: String,         // MMAA (4 dígitos del formulario)
        cvv: String,
        amount: Double,
        orderId: String,
        description: String,
        buyer: BuyerInfo
    ): PaymentResult {
        // Parsear mes y año del campo "MMAA"
        val expMonth = if (expiry.length >= 2) expiry.substring(0, 2) else "01"
        val expYear  = if (expiry.length == 4) "20${expiry.substring(2, 4)}" else "2025"

        // Paso 1: tokenizar
        val tokenResult = tokenizeCard(cardNumber, expMonth, expYear, cvv)
        if (tokenResult.isFailure) {
            return PaymentResult.Error(
                message = tokenResult.exceptionOrNull()?.message ?: "Error de tokenización",
                cause   = tokenResult.exceptionOrNull()
            )
        }
        val cardToken = tokenResult.getOrThrow()

        // Paso 2: crear cliente
        val customerResult = createCustomer(cardToken, buyer)
        if (customerResult.isFailure) {
            return PaymentResult.Error(
                message = customerResult.exceptionOrNull()?.message ?: "Error al crear cliente",
                cause   = customerResult.exceptionOrNull()
            )
        }
        val customerId = customerResult.getOrThrow()

        // Paso 3: cobrar
        return runCatching {
            val amountStr = amount.toInt().toString()
            val response = secureApi.createCharge(
                authHeader = authHeader,
                request = EpaycoChargeRequest(
                    token_card   = cardToken,
                    customer_id  = customerId,
                    doc_number   = buyer.docNumber,
                    name         = buyer.name,
                    last_name    = buyer.lastName,
                    email        = buyer.email,
                    bill         = orderId,
                    description  = description,
                    amount       = amountStr,
                    address      = buyer.address,
                    phone        = buyer.phone,
                    cell_phone   = buyer.phone
                )
            )

            if (!response.status || response.data == null) {
                return PaymentResult.Error(response.message ?: "Error en el cobro")
            }

            val transaction = response.data
            interpretTransactionCode(transaction)
        }.getOrElse { e ->
            PaymentResult.Error(
                message = e.message ?: "Error de red o servidor",
                cause   = e
            )
        }
    }

    // -----------------------------------------------------------------------
    // 4. Confirmación / Consulta post-pago
    // -----------------------------------------------------------------------

    /**
     * Consulta el estado actualizado de una transacción dado su refPayco.
     * Útil para refrescar el estado después de una respuesta "Pendiente".
     */
    suspend fun confirmTransaction(refPayco: String): PaymentResult {
        return runCatching {
            val response = apiService.getTransactionByRef(authHeader, refPayco)
            if (response.success && response.data != null) {
                interpretTransactionCode(response.data)
            } else {
                PaymentResult.Error("No se pudo consultar la transacción")
            }
        }.getOrElse { e ->
            PaymentResult.Error(e.message ?: "Error al confirmar transacción", e)
        }
    }

    // -----------------------------------------------------------------------
    // Utilidad: interpretación de código de respuesta ePayco
    // -----------------------------------------------------------------------

    /**
     * Interpreta el campo x_cod_response de ePayco:
     * 1 = Aceptada, 2 = Rechazada, 3 = Pendiente, 4 = Error
     */
    private fun interpretTransactionCode(transaction: EpaycoTransactionData): PaymentResult {
        return when (transaction.x_cod_response) {
            "1"  -> PaymentResult.Success(transaction)
            "2"  -> PaymentResult.Rejected(
                reason      = transaction.x_response_reason_text ?: transaction.x_response ?: "Transacción rechazada",
                transaction = transaction
            )
            "3"  -> PaymentResult.Pending(
                refPayco    = transaction.ref_payco ?: transaction.x_ref_payco,
                transaction = transaction
            )
            else -> PaymentResult.Error(
                message = transaction.x_response_reason_text ?: "Error desconocido en la transacción"
            )
        }
    }
}
