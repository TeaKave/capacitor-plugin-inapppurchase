package com.windy.plugins.inapppurchase.core

import android.app.Activity
import android.os.Build
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.getcapacitor.JSObject
import com.getcapacitor.PluginCall
import com.google.gson.Gson
import com.windy.plugins.inapppurchase.constants.BillingConstants
import com.windy.plugins.inapppurchase.coroutines.BaseScope
import com.windy.plugins.inapppurchase.listeners.PurchaseDetailListener
import com.windy.plugins.inapppurchase.listeners.QueryListener
import com.windy.plugins.inapppurchase.listeners.SkuDetailListener
import com.windy.plugins.inapppurchase.mapper.mapToJsonObject
import com.windy.plugins.inapppurchase.mapper.mapToRestoreJsonObject
import com.windy.plugins.inapppurchase.mapper.toJsonObject
import com.windy.plugins.inapppurchase.model.Product
import com.windy.plugins.inapppurchase.model.UserCancelledResponse
import com.windy.plugins.inapppurchase.service.BillingServiceStatus
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

private const val TAG = "BillingPluginImpl"

class BillingImplementation(activity: Activity) : PurchasesUpdatedListener, BaseScope() {

    private var billingClient: BillingClient =
        BillingClient.newBuilder(activity).enablePendingPurchases().setListener(this).build()
    private var billingServiceStatus: BillingServiceStatus =
        BillingServiceStatus.BillingServiceDisconnected

    /**
     * The first is productId, the second is the current plugin call.
     */
    private var callback: Pair<String, PluginCall>? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.registerActivityLifecycleCallbacks(this)
        }
        billingClient.startConnection(object : BillingClientStateListener {

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                println("DEBUGY billing setup finished: ${billingResult.responseCode} ${billingResult.responseCode}")
                billingServiceStatus =
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        BillingServiceStatus.BillingServiceConnected
                    } else {
                        BillingServiceStatus.BillingServiceDisconnected
                    }
            }

            override fun onBillingServiceDisconnected() {
                println("DEBUGY billing service disconnected")
                billingServiceStatus = BillingServiceStatus.BillingServiceDisconnected
            }

        })
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchase(purchases)
            BillingClient.BillingResponseCode.USER_CANCELED -> onUserCancelled()
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> onUserCancelled()
            else -> rejectCall("Something went wrong. ResponseCode: ${billingResult.responseCode}, purchases size: $purchases")
        }
    }

    private fun processPurchase(purchases: MutableList<Purchase>?) {
        callback?.second?.let { pluginCall ->
            if (purchases.isNullOrEmpty()) {
                rejectCall("Purchases are empty")
            } else {
                try {
                    purchases.firstOrNull()?.let { purchase ->
                        acknowledgePurchase(purchase, pluginCall)
                    } ?: run {
                        rejectCall("Purchase is null")
                    }
                } catch (e: JSONException) {
                    rejectCall("Can not parse purchase")
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase, pluginCall: PluginCall) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val result = JSObject(purchase.mapToJsonObject().toString())
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                launch {
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams.build()) {
                        pluginCall.resolve(result)
                    }
                }
            } else {
                pluginCall.resolve(result)
            }
        } else {
            rejectCall("Something went wrong. Purchase state: ${purchase.purchaseState}")
        }
    }

    private fun rejectCall(message: String) = callback?.second?.reject(Gson().toJson(message))

    private fun rejectCall(call: PluginCall, message: String) = call.reject(Gson().toJson(message))

    private fun onUserCancelled() = callback?.second?.reject(Gson().toJson(UserCancelledResponse()))

    fun makePurchase(call: PluginCall, activity: Activity, productId: String, productType: String) {
        if (!isSkuTypeValid(productType)) {
            rejectCall(call, "Invalid prodduct type: $productType")
            return
        }
        if (billingServiceStatus != BillingServiceStatus.BillingServiceConnected) {
            rejectCall(call, "Billing service status: $billingServiceStatus")
        } else {
            callback = Pair(productId, call)
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(listOf(productId)).setType(productType)
            billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
                skuDetailsList?.let {
                    it.forEach { skuDetails ->
                        val purchaseParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build()
                        billingClient.launchBillingFlow(activity, purchaseParams)
                    }
                } ?: run {
                    rejectCall(
                        call,
                        "Can not load data for product: $productId. Error: ${billingResult.debugMessage}"
                    )
                }
            }
        }
    }

    fun getSkuDetails(call: PluginCall, products: List<Product>) {
        println("DEBUGY getSku called")
        val productsMap = HashMap<Pair<String, List<String>>, SkuDetailListener>()

        val inAppListener = constructSkuDetailListener()
        val subsListener = constructSkuDetailListener()

        val subscriptionsList = ArrayList<String>()
        val inAppList = ArrayList<String>()

        sortProductsByType(products, subscriptionsList, inAppList)

        productsMap[Pair(BillingClient.SkuType.INAPP, inAppList)] = inAppListener
        productsMap[Pair(BillingClient.SkuType.SUBS, subscriptionsList)] = subsListener
        val queryListener = object : QueryListener {
            var finishCounter = 0

            override fun onFinish() {
                finishCounter++
                println("DEBUGY finish = $finishCounter")
                if (finishCounter == productsMap.size) {
                    println("DEBUGY inAppResult: ${inAppListener.result} subResult: ${subsListener.result}")
                    val skuDetailsJson =
                        convertSkuDetailsToJson(inAppListener.result, subsListener.result)
                    println("DEBUGY calling resolve: $skuDetailsJson")
                    call.resolve(JSObject(skuDetailsJson))
                }
            }

        }
        productsMap.forEach {
            getSkuDetails(it.key.second, it.key.first, it.value, queryListener)
        }
    }

    private fun sortProductsByType(
        products: List<Product>,
        subscriptionsList: ArrayList<String>,
        inAppList: ArrayList<String>
    ) {
        products.forEach { product ->
            if (product.isSubscription) {
                subscriptionsList.add(product.productId)
            } else {
                inAppList.add(product.productId)
            }
        }
    }

    fun getPurchases(call: PluginCall) {
        val inAppPurchaseListener = constructPurchaseDetailListener()
        val subsPurchaseListener = constructPurchaseDetailListener()

        val purchasesMap = HashMap<String, PurchaseDetailListener>()
        purchasesMap[BillingClient.SkuType.INAPP] = inAppPurchaseListener
        purchasesMap[BillingClient.SkuType.SUBS] = subsPurchaseListener

        val queryListener = object : QueryListener {
            var finishCounter = 0

            override fun onFinish() {
                finishCounter++
                if (finishCounter == purchasesMap.size) {
                    val purchases = ArrayList<JSONObject>()
                    inAppPurchaseListener.result?.let {
                        purchases.addAll(it)
                    }
                    subsPurchaseListener.result?.let {
                        purchases.addAll(it)
                    }
                    val result =
                        JSObject(JSONObject().put(BillingConstants.VALUES, purchases).toString())
                    call.resolve(result)
                }
            }
        }

        purchasesMap.forEach {
            getPurchasesByType(it.key, it.value, queryListener)
        }
    }

    fun consumePurchase(
        purchaseToken: String?,
        productId: String?,
        transactionId: String?,
        call: PluginCall
    ) {
        if (purchaseToken == null || productId == null || transactionId == null) {
            rejectCall(
                call,
                "Invalid arguments. Token: $purchaseToken, ProductId: $productId, TransactionId: $transactionId"
            )
        } else {
            val consumeParams =
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build()
            launch {
                billingClient.consumeAsync(consumeParams) { billingResult, purchaseToken ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val pluginResponse = JSONObject().apply {
                            put(BillingConstants.TRANSACTION_ID, transactionId)
                            put(BillingConstants.PRODUCT_ID, productId)
                            put(BillingConstants.TOKEN, purchaseToken)
                        }
                        call.resolve(JSObject(pluginResponse.toString()))
                    } else {
                        rejectCall(
                            call,
                            "Something went wrong: ${billingResult.responseCode}, Message: ${billingResult.debugMessage}"
                        )
                    }
                }
            }
        }
    }

    private fun getPurchasesByType(
        skuType: String,
        purchaseDetailListener: PurchaseDetailListener,
        queryListener: QueryListener
    ) {
        if (isSkuTypeValid(skuType)) {
            billingClient.queryPurchasesAsync(skuType) { billingResult, purchases ->
                val result = ArrayList<JSONObject>()
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.forEach { purchase ->
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            result.add(purchase.mapToRestoreJsonObject(skuType))
                        }
                    }
                }
                Log.d(TAG, "onReceive SUCCESS called for $skuType. Result size: ${result.size}")
                purchaseDetailListener.onReceive(result, queryListener)
            }
        } else {
            Log.d(TAG, "onReceive FAILED called for $skuType")
            purchaseDetailListener.onReceive(null, queryListener)
        }
    }

    private fun constructPurchaseDetailListener() = object : PurchaseDetailListener {
        var result: List<JSONObject>? = null

        override fun onReceive(purchases: List<JSONObject>?, queryListener: QueryListener) {
            result = purchases
            queryListener.onFinish()
        }
    }

    private fun constructSkuDetailListener() = object : SkuDetailListener {
        var result: List<SkuDetails>? = null
        var errorMessage: String? = null

        override fun onReceive(skuDetail: List<SkuDetails>, queryListener: QueryListener) {
            result = skuDetail
            queryListener.onFinish()
        }

        override fun onFailed(message: String, queryListener: QueryListener) {
            errorMessage = message
            queryListener.onFinish()
        }
    }

    private fun getSkuDetails(
        inAppList: List<String>,
        skuType: String,
        skuDetailListener: SkuDetailListener,
        queryListener: QueryListener
    ) {
        if (billingServiceStatus == BillingServiceStatus.BillingServiceConnected && isSkuTypeValid(skuType)) {
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(inAppList).setType(skuType)
            billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetails ->
                if (!skuDetails.isNullOrEmpty()) {
                    skuDetailListener.onReceive(skuDetails, queryListener)
                } else {
                    skuDetailListener.onFailed(
                        "Received sku details are empty. Error message: $${billingResult.debugMessage}",
                        queryListener
                    )
                }
            }
        } else {
            skuDetailListener.onFailed(
                "Billing service status: $billingServiceStatus, SkuType: $skuType",
                queryListener
            )
        }
    }

    private fun isSkuTypeValid(type: String) =
        (BillingClient.SkuType.INAPP == type || BillingClient.SkuType.SUBS == type)

    private fun convertSkuDetailsToJson(vararg skuDetails: List<SkuDetails>?): String {
        val skuDetailsJsonList = ArrayList<JSONObject>()
        skuDetails.forEach {
            it?.forEach { skuDetails ->
                skuDetailsJsonList.add(skuDetails.toJsonObject())
            }
        }
        return JSONObject()
            .put(BillingConstants.VALUES, JSONArray(skuDetailsJsonList.toString()))
            .toString()
    }

}