package com.windy.plugins.inapppurchase.model

import com.google.gson.annotations.SerializedName

const val CODE_KEY = "code"
const val CODE = -5
const val ERROR_CODE_KEY = "errorCode"
const val ERROR_CODE = -5
const val MESSAGE_KEY = "message"
const val MESSAGE = "Purchase Cancelled"
const val RESPONSE_KEY = "response"
const val RESPONSE = -1005
const val TEXT_KEY = "text"
const val TEXT = "User canceled. (response: -1005:User cancelled)"

data class UserCancelledResponse(
    @SerializedName(CODE_KEY)
    val code: Int = CODE,

    @SerializedName(ERROR_CODE_KEY)
    val errorCode: Int = ERROR_CODE,

    @SerializedName(MESSAGE_KEY)
    val message: String = MESSAGE,

    @SerializedName(RESPONSE_KEY)
    val response: Int = RESPONSE,

    @SerializedName(TEXT_KEY)
    val text: String = TEXT
)
