package com.windy.plugins.inapppurchase.model

import com.google.gson.annotations.SerializedName

data class Products(
    @SerializedName("products")
    val products: List<Product>
)