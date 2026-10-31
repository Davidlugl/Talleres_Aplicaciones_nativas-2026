package com.example.herbhopper_v1.data

import com.example.herbhopper_v1.data.network.EpaycoTransactionData
import com.example.herbhopper_v1.data.network.EpaycoValidationApi

/**
 * Resultado sellado del proceso de pago, permite manejar cada estado
 * de forma exhaustiva en el ViewModel sin necesidad de excepciones.
 */
sealed class PaymentResult {
    /** La transacción fue aceptada por el banco. */
    data class Success(val transaction: EpaycoTransactionData) : PaymentResult()

    /** La transacción fue rechazada por el banco o la tarjeta. */
    data class Rejected(val reason: String, val transaction: EpaycoTransactionData?) : PaymentResult()

    /** La transacción está pendiente de confirmación (PSE en proceso o cupón de efectivo sin pagar). */
    data class Pending(val refPayco: String?, val transaction: EpaycoTransactionData?) : PaymentResult()

    /** Error técnico: red, referencia inválida, etc. */
    data class Error(val message: String, val cause: Throwable? = null) : PaymentResult()
}

/**
 * Consulta el resultado de los pagos hechos en el checkout oficial de ePayco.
 *
 * El cobro no lo hace la app: el usuario paga dentro del checkout de ePayco (tarjeta, PSE o
 * efectivo) y ePayco notifica al backend en la URL de confirmación, que actualiza el pedido.
 * Aquí solo se lee el resultado para mostrarlo en pantalla.
 */
class PaymentRepository(
    private val validationApi: EpaycoValidationApi = EpaycoValidationApi.create()
) {
    suspend fun confirmTransaction(refPayco: String): PaymentResult {
        return runCatching {
            val response = validationApi.getTransactionByRef(refPayco)
            if (response.success && response.data != null) {
                interpretTransactionCode(response.data)
            } else {
                PaymentResult.Error("No se pudo consultar la transacción")
            }
        }.getOrElse { e ->
            PaymentResult.Error(e.message ?: "Error al confirmar transacción", e)
        }
    }

    /**
     * Interpreta el campo x_cod_response de ePayco:
     * 1 = Aceptada, 2 = Rechazada, 3 = Pendiente, 4 = Fallida
     */
    private fun interpretTransactionCode(transaction: EpaycoTransactionData): PaymentResult {
        return when (transaction.x_cod_response) {
            "1" -> PaymentResult.Success(transaction)
            "2" -> PaymentResult.Rejected(
                reason = transaction.x_response_reason_text ?: transaction.x_response ?: "Transacción rechazada",
                transaction = transaction
            )
            "3" -> PaymentResult.Pending(
                refPayco = transaction.ref_payco ?: transaction.x_ref_payco,
                transaction = transaction
            )
            else -> PaymentResult.Error(
                message = transaction.x_response_reason_text ?: "Error desconocido en la transacción"
            )
        }
    }
}
