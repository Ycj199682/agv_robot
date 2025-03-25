package com.reeman.commons.model.request

data class ResponseVO<T>(val code: Int, val msg: String, val data: T? = null)