package com.example.herbhopper_v1.model

import com.example.herbhopper_v1.data.Product

data class CartItem(
    val product: Product,
    val quantity: Int = 1
)
