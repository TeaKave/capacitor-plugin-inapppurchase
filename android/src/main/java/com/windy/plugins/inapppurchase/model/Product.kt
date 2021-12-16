package com.windy.plugins.inapppurchase.model

import com.google.gson.annotations.SerializedName

data class Product(
    @SerializedName("isSubscription")
    val isSubscription: Boolean,
    @SerializedName("productId")
    val productId: String
)