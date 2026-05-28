package com.example.herbhopper_v1.viewmodel

import android.app.Application
import com.example.herbhopper_v1.R
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.data.BuyerInfo
import com.example.herbhopper_v1.data.PaymentRepository
import com.example.herbhopper_v1.data.PaymentResult
import com.example.herbhopper_v1.data.SessionManager
import com.example.herbhopper_v1.data.network.EpaycoTransactionData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Métodos de pago disponibles
// ---------------------------------------------------------------------------

/**
 * Representa los métodos de pago disponibles en el sistema.
 */
sealed class PaymentMethod(val key: String) {
    object CreditCard    : PaymentMethod("CREDIT_CARD")
    object BankTransfer  : PaymentMethod("BANK")
    object Cash          : PaymentMethod("CASH")

    companion object {
        fun fromKey(key: String): PaymentMethod = when (key) {
            "CREDIT_CARD" -> CreditCard
            "BANK"        -> BankTransfer
            "CASH"        -> Cash
            else          -> CreditCard
        }
    }
}

// ---------------------------------------------------------------------------
// Estado de la transacción ePayco
// ---------------------------------------------------------------------------

/** Estado del proceso de pago con ePayco. */
sealed class TransactionStatus {
    /** Sin transacción iniciada aún. */
    object Idle : TransactionStatus()

    /** Transacción en proceso. */
    object Loading : TransactionStatus()

    /** Pago aceptado por el banco. */
    data class Accepted(val data: EpaycoTransactionData) : TransactionStatus()

    /** Pago rechazado. */
    data class Rejected(val reason: String, val data: EpaycoTransactionData?) : TransactionStatus()

    /** Pago pendiente de confirmación (PSE, etc). */
    data class Pending(val refPayco: String?, val data: EpaycoTransactionData?) : TransactionStatus()

    /** Error técnico. */
    data class Failed(val message: String) : TransactionStatus()
}

// ---------------------------------------------------------------------------
// UI State del Checkout
// ---------------------------------------------------------------------------

/**
 * Estado inmutable de la interfaz de usuario para el proceso de Checkout.
 */
data class CheckoutUiState(
    // Datos del formulario de tarjeta
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cvv: String = "",
    val cardHolderName: String = "",

    // Datos del comprador (se prellenan desde el perfil/sesión)
    val buyerName: String = "",
    val buyerLastName: String = "",
    val buyerEmail: String = "",
    val buyerPhone: String = "",
    val buyerDoc: String = "",
    val buyerAddress: String = "",

    // Selección de método de pago
    val selectedMethod: PaymentMethod = PaymentMethod.CreditCard,

    // Estado de la transacción
    val transactionStatus: TransactionStatus = TransactionStatus.Idle,

    // Mensajes de validación
    val errorMessage: String? = null,

    // Aliases convenientes
    val isLoading: Boolean = transactionStatus is TransactionStatus.Loading,
    val isPaymentSuccess: Boolean = transactionStatus is TransactionStatus.Accepted
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * ViewModel encargado de la lógica de negocio del Checkout con integración real a ePayco.
 *
 * Flujo para tarjeta:
 * 1. El usuario llena el formulario.
 * 2. [executePayment] valida los datos localmente.
 * 3. Se llama a [PaymentRepository.processCardPayment] que tokeniza, crea cliente y cobra.
 * 4. El resultado se mapea a [TransactionStatus] y se expone al UI.
 */
class CheckoutViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PaymentRepository()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        SessionManager.init(application)
        
        // Pre-llenar datos del comprador desde la sesión activa
        if (SessionManager.isLoggedIn()) {
            val fullName = SessionManager.getName()
            val nameParts = fullName.trim().split(" ")
            val email = SessionManager.getEmail()
            val phone = SessionManager.getPhone() ?: ""
            
            _uiState.update {
                it.copy(
                    buyerName     = nameParts.firstOrNull() ?: fullName,
                    buyerLastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else "",
                    buyerEmail    = email,
                    buyerPhone    = phone
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Actualizadores del formulario de tarjeta
    // -----------------------------------------------------------------------

    fun onCardNumberChange(number: String) {
        val clean = number.filter { it.isDigit() }.take(16)
        _uiState.update { it.copy(cardNumber = clean, errorMessage = null) }
    }

    fun onExpiryDateChange(expiry: String) {
        val clean = expiry.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(expiryDate = clean, errorMessage = null) }
    }

    fun onCvvChange(cvvValue: String) {
        val clean = cvvValue.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(cvv = clean, errorMessage = null) }
    }

    fun onCardHolderNameChange(name: String) {
        _uiState.update { it.copy(cardHolderName = name, errorMessage = null) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method, errorMessage = null) }
    }

    // Datos del comprador (para usuarios que editan su información en el checkout)
    fun onBuyerPhoneChange(phone: String) {
        _uiState.update { it.copy(buyerPhone = phone, errorMessage = null) }
    }

    fun onBuyerDocChange(doc: String) {
        _uiState.update { it.copy(buyerDoc = doc, errorMessage = null) }
    }

    fun onBuyerAddressChange(address: String) {
        _uiState.update { it.copy(buyerAddress = address, errorMessage = null) }
    }

    // -----------------------------------------------------------------------
    // Ejecutar pago
    // -----------------------------------------------------------------------

    /**
     * Inicia el proceso de pago.
     *
     * Para tarjeta: valida el formulario y delega a [PaymentRepository.processCardPayment].
     * Para PSE / Efectivo: registra la orden como PENDING y notifica el éxito.
     *
     * @param totalAmount  Monto total del pedido en COP.
     * @param orderId      Referencia única del pedido.
     * @param description  Descripción breve del contenido del pedido.
     * @param onSuccess    Callback invocado cuando el pago se procesa (Aceptado, Pendiente o Efectivo).
     */
    fun executePayment(
        totalAmount: Double,
        orderId: String,
        description: String,
        onSuccess: (TransactionStatus) -> Unit
    ) {
        val state = _uiState.value

        // Validación de negocio para tarjeta de crédito/débito
        if (state.selectedMethod == PaymentMethod.CreditCard) {
            val validationError = validateCardForm(state)
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    transactionStatus = TransactionStatus.Loading,
                    errorMessage      = null
                )
            }

            val newStatus: TransactionStatus = when (state.selectedMethod) {
                is PaymentMethod.CreditCard -> {
                    val buyer = BuyerInfo(
                        name      = state.buyerName,
                        lastName  = state.buyerLastName,
                        email     = state.buyerEmail,
                        phone     = state.buyerPhone,
                        docNumber = state.buyerDoc,
                        address   = state.buyerAddress
                    )

                    val result = repository.processCardPayment(
                        cardNumber  = state.cardNumber,
                        expiry      = state.expiryDate,
                        cvv         = state.cvv,
                        amount      = totalAmount,
                        orderId     = orderId,
                        description = description,
                        buyer       = buyer
                    )

                    mapPaymentResult(result)
                }

                // PSE – Simulado como PENDING hasta integrar el flujo de redirección web
                is PaymentMethod.BankTransfer -> {
                    TransactionStatus.Pending(
                        refPayco = null,
                        data     = null
                    )
                }

                // Efectivo – Siempre registra como PENDING (el vendedor confirma al recibir)
                is PaymentMethod.Cash -> {
                    TransactionStatus.Pending(
                        refPayco = null,
                        data     = null
                    )
                }
            }

            _uiState.update { it.copy(transactionStatus = newStatus) }

            // Disparar callback para navegación/creación de orden si fue exitoso o pendiente
            if (newStatus is TransactionStatus.Accepted || newStatus is TransactionStatus.Pending) {
                onSuccess(newStatus)
            }
        }
    }

    /**
     * Consulta el estado de una transacción pendiente.
     * Útil para polling tras la respuesta de PSE.
     */
    fun refreshTransactionStatus(refPayco: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(transactionStatus = TransactionStatus.Loading) }
            val result = repository.confirmTransaction(refPayco)
            _uiState.update { it.copy(transactionStatus = mapPaymentResult(result)) }
        }
    }

    /** Reinicia el estado para permitir un nuevo intento de pago. */
    fun resetPaymentState() {
        _uiState.update {
            it.copy(
                transactionStatus = TransactionStatus.Idle,
                errorMessage      = null
            )
        }
    }

    // -----------------------------------------------------------------------
    // Utilidades privadas
    // -----------------------------------------------------------------------

    private fun validateCardForm(state: CheckoutUiState): String? {
        val app = getApplication<Application>()
        if (state.cardNumber.length != 16) return app.getString(R.string.toast_invalid_card)
        if (state.expiryDate.length != 4) return app.getString(R.string.toast_invalid_expiry)
        if (state.cvv.length != 3)        return app.getString(R.string.toast_invalid_cvv)
        if (state.buyerName.isBlank() || state.buyerLastName.isBlank() ||
            state.buyerEmail.isBlank() || state.buyerPhone.isBlank() ||
            state.buyerDoc.isBlank() || state.buyerAddress.isBlank()) {
            return app.getString(R.string.toast_fill_credentials)
        }
        return null
    }

    private fun mapPaymentResult(result: PaymentResult): TransactionStatus = when (result) {
        is PaymentResult.Success  -> TransactionStatus.Accepted(result.transaction)
        is PaymentResult.Rejected -> TransactionStatus.Rejected(result.reason, result.transaction)
        is PaymentResult.Pending  -> TransactionStatus.Pending(result.refPayco, result.transaction)
        is PaymentResult.Error    -> TransactionStatus.Failed(result.message)
    }
}
