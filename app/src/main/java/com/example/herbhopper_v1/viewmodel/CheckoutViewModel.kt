package com.example.herbhopper_v1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.herbhopper_v1.R
import com.example.herbhopper_v1.data.AppDatabase
import com.example.herbhopper_v1.data.DeliveryAddress
import com.example.herbhopper_v1.data.Order
import com.example.herbhopper_v1.data.SessionManager
import com.example.herbhopper_v1.data.network.ApiService
import com.example.herbhopper_v1.data.network.CreateOrderRequest
import com.example.herbhopper_v1.data.network.EpaycoTransactionData
import com.example.herbhopper_v1.data.network.OrderItemRequest
import com.example.herbhopper_v1.data.network.PrescriptionResponse
import com.example.herbhopper_v1.data.network.apiErrorMessage
import com.example.herbhopper_v1.model.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Métodos de pago disponibles (todos se pagan dentro del checkout oficial de ePayco)
// ---------------------------------------------------------------------------

sealed class PaymentMethod(val key: String) {
    object CreditCard : PaymentMethod("CREDIT_CARD")
    object BankTransfer : PaymentMethod("PSE")
    object Cash : PaymentMethod("CASH")
}

// ---------------------------------------------------------------------------
// Estado de la transacción ePayco
// ---------------------------------------------------------------------------

sealed class TransactionStatus {
    object Idle : TransactionStatus()
    object Loading : TransactionStatus()
    data class Accepted(val data: EpaycoTransactionData) : TransactionStatus()
    data class Rejected(val reason: String, val data: EpaycoTransactionData?) : TransactionStatus()
    /** PSE en proceso o cupón de efectivo generado y aún sin pagar. */
    data class Pending(val refPayco: String?, val data: EpaycoTransactionData?) : TransactionStatus()
    data class Failed(val message: String) : TransactionStatus()
}

data class CheckoutUiState(
    val selectedMethod: PaymentMethod = PaymentMethod.CreditCard,
    val addresses: List<DeliveryAddress> = emptyList(),
    val selectedAddress: DeliveryAddress? = null,
    val prescriptions: List<PrescriptionResponse> = emptyList(),
    val selectedPrescriptionId: Int? = null,
    val isCreatingOrder: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Checkout: arma el pedido (dirección con coordenadas GPS, fórmula médica si aplica y método
 * de pago), lo registra en el backend —que recalcula precios, reserva stock y valida la fórmula—
 * y entrega el pedido creado para abrir el checkout de ePayco.
 */
class CheckoutViewModel(application: Application) : AndroidViewModel(application) {

    private val addressDao = AppDatabase.getDatabase(application).deliveryAddressDao()
    private val api = ApiService.instance

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        val uid = SessionManager.getUid()
        viewModelScope.launch {
            addressDao.observeAddresses(uid).collect { addresses ->
                _uiState.update { state ->
                    val keep = state.selectedAddress?.let { sel -> addresses.firstOrNull { it.id == sel.id } }
                    state.copy(
                        addresses = addresses,
                        selectedAddress = keep ?: addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
                    )
                }
            }
        }
        refreshPrescriptions()
    }

    fun refreshPrescriptions() {
        viewModelScope.launch {
            try {
                // Solo sirven fórmulas pendientes de revisión o aprobadas
                val usable = api.getPrescriptions().filter { it.status != "REJECTED" }
                _uiState.update { state ->
                    state.copy(
                        prescriptions = usable,
                        selectedPrescriptionId = state.selectedPrescriptionId?.takeIf { id -> usable.any { it.id == id } }
                            ?: usable.firstOrNull { it.status == "APPROVED" }?.id
                            ?: usable.firstOrNull()?.id
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onPaymentMethodChange(method: PaymentMethod) = _uiState.update { it.copy(selectedMethod = method, errorMessage = null) }

    fun onAddressSelected(address: DeliveryAddress) = _uiState.update { it.copy(selectedAddress = address, errorMessage = null) }

    fun onPrescriptionSelected(id: Int?) = _uiState.update { it.copy(selectedPrescriptionId = id, errorMessage = null) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    /**
     * Registra el pedido en el servidor. Si todo está bien, [onCreated] recibe el pedido
     * (con el total recalculado por el servidor) para abrir el checkout de ePayco.
     */
    fun createOrder(cartItems: List<CartItem>, orderId: String, onCreated: (Order, PaymentMethod) -> Unit) {
        val state = _uiState.value
        val app = getApplication<Application>()
        val address = state.selectedAddress
        val needsPrescription = cartItems.any { it.product.requiresPrescription }

        val validationError = when {
            cartItems.isEmpty() -> app.getString(R.string.checkout_empty_cart)
            address == null -> app.getString(R.string.checkout_missing_address)
            needsPrescription && state.selectedPrescriptionId == null -> app.getString(R.string.checkout_missing_prescription)
            else -> null
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingOrder = true, errorMessage = null) }
            try {
                val order = api.createOrder(
                    CreateOrderRequest(
                        orderId = orderId,
                        items = cartItems.map { OrderItemRequest(it.product.id, it.quantity) },
                        address = address!!.fullAddress,
                        latitude = address.latitude,
                        longitude = address.longitude,
                        paymentMethod = state.selectedMethod.key,
                        prescriptionId = state.selectedPrescriptionId
                    )
                )
                AppDatabase.getDatabase(app).orderDao().insertOrder(order)
                _uiState.update { it.copy(isCreatingOrder = false) }
                onCreated(order, state.selectedMethod)
            } catch (e: Exception) {
                _uiState.update { it.copy(isCreatingOrder = false, errorMessage = e.apiErrorMessage()) }
            }
        }
    }
}
