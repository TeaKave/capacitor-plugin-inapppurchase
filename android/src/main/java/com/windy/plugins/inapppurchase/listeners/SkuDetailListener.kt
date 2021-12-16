package com.windy.plugins.inapppurchase.listeners

import com.android.billingclient.api.SkuDetails

interface SkuDetailListener {

    fun onReceive(skuDetail: List<SkuDetails>, queryListener: QueryListener)

    fun onFailed(message: String, queryListener: QueryListener)

}
