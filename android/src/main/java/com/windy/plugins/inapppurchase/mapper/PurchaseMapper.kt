package com.windy.plugins.inapppurchase.mapper

import com.android.billingclient.api.Purchase
import com.windy.plugins.inapppurchase.constants.BillingConstants
import org.json.JSONObject

fun Purchase.mapToJsonObject(): JSONObject = JSONObject().run {
    put(BillingConstants.ORDER_ID, orderId)
    put(BillingConstants.PACKAGE_NAME, packageName)
    put(BillingConstants.PURCHASE_TIME, purchaseTime)
    put(BillingConstants.PURCHASE_STATE, purchaseState)
    put(BillingConstants.PURCHASE_TOKEN, purchaseToken)
    put(BillingConstants.SIGNATURE, signature)
    put(BillingConstants.RECEIPT, originalJson)
}

fun Purchase.mapToRestoreJsonObject(productType: String): JSONObject = JSONObject().run {
    put(BillingConstants.DATE, purchaseTime)
    put(BillingConstants.PRODUCT_TYPE, productType)
    put(BillingConstants.TYPE, productType)
    put(BillingConstants.RECEIPT, originalJson)
    put(BillingConstants.SIGNATURE, signature)
    put(BillingConstants.TRANSACTION_ID, orderId)
    put(BillingConstants.PRODUCT_ID, skus.getOrNull(0) ?: "")
    put(BillingConstants.STATE, purchaseState)
}
