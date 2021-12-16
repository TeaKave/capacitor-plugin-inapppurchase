package com.windy.plugins.inapppurchase.mapper

import com.android.billingclient.api.SkuDetails
import com.windy.plugins.inapppurchase.constants.BillingConstants
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private const val RAW_PRICE_FORMAT = "#.00####"
private const val PRICE_COEFFICIENT = 1000000.0

fun SkuDetails.toJsonObject() = JSONObject().apply {
    put(BillingConstants.PRODUCT_ID, sku)
    put(BillingConstants.TITLE, title)
    put(BillingConstants.DESCRIPTION, description)
    put(BillingConstants.PRICE_AS_DECIMAL, priceAmountMicros / PRICE_COEFFICIENT)
    put(BillingConstants.PRICE, price)
    put(BillingConstants.PRICE_RAW, getRawPrice(priceAmountMicros))
    put(BillingConstants.CURRENCY, priceCurrencyCode)
    put(BillingConstants.COUNTRY, "-")
    put(BillingConstants.TYPE, type)
}

private fun getRawPrice(priceMicros: Long): String {
    val formatter = DecimalFormat(RAW_PRICE_FORMAT)
    formatter.decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.ENGLISH)
    return formatter.format(priceMicros / PRICE_COEFFICIENT)
}