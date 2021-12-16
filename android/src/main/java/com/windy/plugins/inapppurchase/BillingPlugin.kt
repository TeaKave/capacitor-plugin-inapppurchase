package com.windy.plugins.inapppurchase

import com.android.billingclient.api.BillingClient
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.google.gson.Gson
import com.windy.plugins.inapppurchase.constants.BillingConstants
import com.windy.plugins.inapppurchase.core.BillingImplementation
import com.windy.plugins.inapppurchase.model.Products
import org.json.JSONException

@CapacitorPlugin(
    name = "Billing"
)
class BillingPlugin : Plugin() {

    private var billing: BillingImplementation? = null

    override fun load() {
        initBilling()
    }

    private fun initBilling() {
        billing = BillingImplementation(activity)
    }

    @PluginMethod
    fun consume(call: PluginCall) {
        billing?.consumePurchase(
            call.getString(BillingConstants.PURCHASE_TOKEN),
            call.getString(BillingConstants.PRODUCT_ID),
            call.getString(BillingConstants.TRANSACTION_ID),
            call
        ) ?: run {
            call.reject("Billing not initialized")
        }
    }

    @PluginMethod
    fun getReceipt(call: PluginCall) = call.resolve(JSObject())

    @PluginMethod
    fun restorePurchases(call: PluginCall) {
        billing?.getPurchases(call) ?: run {
            call.reject("Billing not initialized")
        }
    }

    @PluginMethod
    fun getProducts(call: PluginCall) {
        println("DEBUGY")
        billing?.let {
            try {
                val data = call.data.toString()
                println("DEBUGY data = $data")
                val convertedProducts = Gson().fromJson(data, Products::class.java)
                println("DEBUGY convertedProducts = $convertedProducts")
                if (!convertedProducts.products.isNullOrEmpty()) {
                    it.getSkuDetails(call, convertedProducts.products)
                } else {
                    call.reject("Invalid product ids")
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                call.reject("Invalid product ids. Error: ${e.message}")
            }
        } ?: run {
            call.reject("Billing not initialized")
        }
    }

    @PluginMethod
    fun subscribe(call: PluginCall) {
        makePurchase(call, BillingClient.SkuType.SUBS)
    }

    @PluginMethod
    fun buy(call: PluginCall) {
        makePurchase(call, BillingClient.SkuType.INAPP)
    }

    private fun makePurchase(call: PluginCall, productType: String) {
        billing?.let {
            val productId = call.getString(BillingConstants.PRODUCT_ID)
            if (!productId.isNullOrBlank()) {
                it.makePurchase(call, activity, productId, productType)
            } else {
                call.reject("Invalid product ID")
            }
        } ?: run {
            call.reject("Billing not initialized")
        }
    }
}
