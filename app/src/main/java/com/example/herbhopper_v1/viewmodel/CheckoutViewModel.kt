package com.example.herbhopper_v1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Representa los métodos de pago disponibles en el sistema.
 */
sealed class PaymentMethod(val key: String) {
    object CreditCard : PaymentMethod("CREDIT_CARD")
    object BankTransfer : PaymentMethod("BANK")
    object Cash : PaymentMethod("CASH")

    companion object {
        fun fromKey(key: String): PaymentMethod {
            return when (key) {
                "CREDIT_CARD" -> CreditCard
                "BANK" -> BankTransfer
                "CASH" -> Cash
                else -> CreditCard
            }
        }
    }
}

/**
 * Estado inmutable de la interfaz de usuario para el proceso de Checkout.
 */
data class CheckoutUiState(
    val cardNumber: String = "",
    val expiryDate: String = "",
    val cvv: String = "",
    val selectedMethod: PaymentMethod = PaymentMethod.CreditCard,
    val isLoading: Boolean = false,
    val isPaymentSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel encargado de la lógica de negocio y gestión del estado (UDF) de la pantalla de Checkout.
 */
class CheckoutViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun onCardNumberChange(number: String) {
        // Permitimos solo dígitos y limitamos a 16 caracteres
        val clean = number.filter { it.isDigit() }.take(16)
        _uiState.update { it.copy(cardNumber = clean, errorMessage = null) }
    }

    fun onExpiryDateChange(expiry: String) {
        // Permitimos solo dígitos y limitamos a 4 caracteres para el formato MM/AA
        val clean = expiry.filter { it.isDigit() }.take(4)
        _uiState.update { it.copy(expiryDate = clean, errorMessage = null) }
    }

    fun onCvvChange(cvvValue: String) {
        // Permitimos solo dígitos y limitamos a 3 caracteres
        val clean = cvvValue.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(cvv = clean, errorMessage = null) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(selectedMethod = method, errorMessage = null) }
    }

    fun executePayment(onSuccess: () -> Unit) {
        val state = _uiState.value
        
        // Validación de negocio antes de proceder con el pago con tarjeta
        if (state.selectedMethod == PaymentMethod.CreditCard) {
            if (state.cardNumber.length != 16) {
                _uiState.update { it.copy(errorMessage = "El número de tarjeta debe tener 16 dígitos") }
                return
            }
            if (state.expiryDate.length != 4) {
                _uiState.update { it.copy(errorMessage = "La fecha de vencimiento debe tener el formato MM/AA") }
                return
            }
            if (state.cvv.length != 3) {
                _uiState.update { it.copy(errorMessage = "El CVV debe tener 3 dígitos") }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Simulación de transacción de pasarela de pago (latencia de red ficticia)
            delay(1500)
            
            _uiState.update { it.copy(isLoading = false, isPaymentSuccess = true) }
            onSuccess()
        }
    }
}
