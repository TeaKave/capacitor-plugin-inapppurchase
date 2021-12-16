package com.windy.plugins.inapppurchase.service

sealed class BillingServiceStatus {

    object BillingServiceConnected : BillingServiceStatus()
    object BillingServiceDisconnected : BillingServiceStatus()

}