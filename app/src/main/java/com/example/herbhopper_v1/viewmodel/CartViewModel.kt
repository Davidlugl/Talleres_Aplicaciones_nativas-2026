package com.example.herbhopper_v1.viewmodel

import androidx.lifecycle.ViewModel
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.model.CartItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CartViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items.asStateFlow()

    fun addToCart(product: Product) {
        _items.update { currentList ->
            val existing = currentList.find { it.product.id == product.id }
            if (existing != null) {
                currentList.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                currentList + CartItem(product)
            }
        }
    }

    fun removeFromCart(productId: Int) {
        _items.update { currentList ->
            currentList.filter { it.product.id != productId }
        }
    }

    fun updateQuantity(productId: Int, delta: Int) {
        _items.update { currentList ->
            currentList.map {
                if (it.product.id == productId) {
                    val newQty = (it.quantity + delta).coerceAtLeast(1)
                    it.copy(quantity = newQty)
                } else it
            }
        }
    }

    fun clearCart() {
        _items.value = emptyList()
    }

    val total: Double
        get() = _items.value.sumOf { it.product.price * it.quantity }
}
