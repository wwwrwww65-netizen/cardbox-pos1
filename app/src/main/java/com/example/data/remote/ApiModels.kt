package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "phone") val phone: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponseDto(
    @Json(name = "token") val token: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class NetworkDto(
    @Json(name = "id") val id: Any,
    @Json(name = "name") val name: String?,
    @Json(name = "network_code") val networkCode: String? = null,
    @Json(name = "governorate") val governorate: String? = null,
    @Json(name = "city") val city: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class PackageDto(
    @Json(name = "id") val id: Any,
    @Json(name = "name") val name: String?,
    @Json(name = "price") val price: Double?,
    @Json(name = "validity") val validity: Any? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "stock") val stock: Int? = null
)

@JsonClass(generateAdapter = true)
data class PurchaseRequestDto(
    @Json(name = "network_id") val networkId: Any,
    @Json(name = "package_id") val packageId: Any,
    @Json(name = "quantity") val quantity: Int = 1,
    @Json(name = "customer_phone") val customerPhone: String? = null
)

@JsonClass(generateAdapter = true)
data class VoucherDto(
    @Json(name = "voucher_code") val voucherCode: String? = null,
    @Json(name = "pin") val pin: String? = null,
    @Json(name = "price") val price: Double? = null,
    @Json(name = "expiry_date") val expiryDate: String? = null,
    @Json(name = "transaction_id") val transactionId: String? = null
)

@JsonClass(generateAdapter = true)
data class PurchaseResponseDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "vouchers") val vouchers: List<VoucherDto>? = null,
    @Json(name = "network_name") val networkName: String? = null,
    @Json(name = "total_deducted") val totalDeducted: Double? = null
)
