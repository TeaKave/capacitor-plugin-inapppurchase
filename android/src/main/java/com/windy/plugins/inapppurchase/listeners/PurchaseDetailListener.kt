package com.windy.plugins.inapppurchase.listeners

import org.json.JSONObject

interface PurchaseDetailListener {

    fun onReceive(purchases: List<JSONObject>?, queryListener: QueryListener)

}