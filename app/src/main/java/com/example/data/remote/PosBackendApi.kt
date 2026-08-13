package com.example.data.remote

import retrofit2.Response
import retrofit2.http.*

interface PosBackendApi {

    @POST("customer/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponseDto>

    @GET("pos/networks")
    suspend fun getNetworks(
        @Header("Authorization") tokenHeader: String? = null
    ): Response<List<NetworkDto>>

    @GET("pos/networks/{network_id}/packages")
    suspend fun getNetworkPackages(
        @Path("network_id") networkId: String,
        @Header("Authorization") tokenHeader: String? = null
    ): Response<List<PackageDto>>

    @POST("pos/vouchers/purchase")
    suspend fun purchaseVoucher(
        @Header("Authorization") tokenHeader: String? = null,
        @Body request: PurchaseRequestDto
    ): Response<PurchaseResponseDto>
}
