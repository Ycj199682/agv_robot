package com.reeman.points.model.request

// 服务器响应数据
data class ApiResponse(
    val code: Int,
    val msg: String,
    val time: String,
    val data: Any?
)