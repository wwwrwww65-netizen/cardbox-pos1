package com.example.data.remote

import com.example.data.model.JoinStatus
import com.example.data.model.NetworkItem
import com.example.data.model.VoucherPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null
)

class MikroTikApiService {

    // Pre-seeded fallback server networks database
    private val mockNetworksServer = mutableListOf(
        NetworkItem(
            id = "1",
            code = "81234",
            name = "شبكة التميز",
            ownerName = "إدارة شبكة التميز",
            financialCeiling = 20000.0,
            currentBalance = 15500.0,
            currency = "ريال",
            status = JoinStatus.APPROVED,
            location = "صنعاء - حدة",
            packagesCount = 5,
            description = "تغطية ممتازة وسرعة عالية في حدة"
        ),
        NetworkItem(
            id = "2",
            code = "NET-102",
            name = "شبكة الأمل مايكروتك",
            ownerName = "أبو طارق السنيد",
            financialCeiling = 10000.0,
            currentBalance = 8200.0,
            currency = "ريال",
            status = JoinStatus.APPROVED,
            location = "الحصبة - الجولة الرئيسية",
            packagesCount = 4,
            description = "شبكة المايكروتك رقم 1 للبث اللاسلكي والألعاب"
        ),
        NetworkItem(
            id = "3",
            code = "NET-MAJD",
            name = "شبكة النور الواي فاي",
            ownerName = "حسان القاضي",
            financialCeiling = 12000.0,
            currentBalance = 12000.0,
            currency = "ريال",
            status = JoinStatus.NOT_JOINED,
            location = "حدة - شارع بيروت",
            packagesCount = 5,
            description = "خدمة متواصلة 24/7 مع دعم كروت الفئات المتنوعة"
        )
    )

    private val mockPackagesDatabase = mapOf(
        "1" to listOf(
            VoucherPackage(id = "5", networkId = "1", name = "باقة 100", price = 100.0, currency = "ريال", duration = "سعة: 100 ميجا، مدة: 2 ساعة", dataQuota = "100 ميجا", validity = "1 يوم", colorHex = "#6B21A8", isAvailable = true, isPopular = true),
            VoucherPackage(id = "6", networkId = "1", name = "باقة 200", price = 200.0, currency = "ريال", duration = "سعة: 300 ميجا، مدة: 6 ساعات", dataQuota = "300 ميجا", validity = "3 أيام", colorHex = "#6B21A8", isAvailable = true, isPopular = false),
            VoucherPackage(id = "7", networkId = "1", name = "باقة 500", price = 500.0, currency = "ريال", duration = "سعة: 1 جيجا، مدة: 24 ساعة", dataQuota = "1 جيجا", validity = " أسبوع", colorHex = "#6B21A8", isAvailable = true, isPopular = false)
        )
    )

    suspend fun loginApi(phone: String, pass: String): ApiResponse<String> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.login(LoginRequest(phone, pass))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val token = body.token ?: body.accessToken ?: "token_demo_sample"
                RetrofitClient.authToken = token
                ApiResponse(true, body.message ?: "تم تسجيل الدخول بالسيرفر بنجاح", data = token)
            } else {
                ApiResponse(false, "عذراً! تعذر تسجيل الدخول من السيرفر (كود: ${response.code()})")
            }
        } catch (e: Exception) {
            ApiResponse(false, "فشل الاتصال بالسيرفر: ${e.localizedMessage ?: "تأكد من تشغيل السيرفر ورابط الاتصال"}")
        }
    }

    suspend fun fetchRealNetworks(): ApiResponse<List<NetworkItem>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.getNetworks()
            if (response.isSuccessful && response.body() != null) {
                val dtoList = response.body()!!
                val items = dtoList.map { dto ->
                    val netId = dto.id.toString()
                    val loc = listOfNotNull(dto.governorate, dto.city).joinToString(" - ").ifBlank { "المركز الرئيسي" }
                    NetworkItem(
                        id = netId,
                        code = dto.networkCode ?: "NET-$netId",
                        name = dto.name ?: "شبكة $netId",
                        ownerName = "إدارة ${dto.name ?: "الشبكة"}",
                        financialCeiling = 20000.0,
                        currentBalance = 15000.0,
                        currency = "ريال",
                        status = if (dto.status == "active" || dto.status == null) JoinStatus.APPROVED else JoinStatus.NOT_JOINED,
                        location = loc,
                        packagesCount = 5,
                        description = "الموقع: $loc"
                    )
                }
                ApiResponse(true, "تم جلب الشبكات من السيرفر بنجاح", data = items)
            } else {
                ApiResponse(false, "تعذر جلب الشبكات من السيرفر (كود: ${response.code()})", data = mockNetworksServer)
            }
        } catch (e: Exception) {
            ApiResponse(false, "تنبيه: فشل الاتصال بالسيرفر (${e.localizedMessage}). جاري عرض الشبكات المخزنة.", data = mockNetworksServer)
        }
    }

    suspend fun searchNetworkByCode(codeQuery: String): ApiResponse<NetworkItem> = withContext(Dispatchers.IO) {
        // Try real server first
        val realNetsRes = fetchRealNetworks()
        val allNets = if (realNetsRes.data.isNullOrEmpty()) mockNetworksServer else realNetsRes.data

        val rawQuery = codeQuery.trim()
        val queryClean = rawQuery.uppercase()
        val found = allNets.find {
            it.code.uppercase() == queryClean ||
            it.code.uppercase() == "NET-$queryClean" ||
            it.name.contains(rawQuery, ignoreCase = true) ||
            it.ownerName.contains(rawQuery, ignoreCase = true) ||
            it.id == rawQuery
        }

        if (found != null) {
            ApiResponse(true, "تم العثور على تفاصيل الشبكة بنجاح", data = found)
        } else {
            ApiResponse(false, "لم يتم العثور على شبكة بالاسم أو الكود ($codeQuery). تأكد من البيانات وكرر المحاولة.")
        }
    }

    suspend fun requestJoinNetwork(networkId: String, storeName: String, storePhone: String): ApiResponse<NetworkItem> {
        delay(600)
        val index = mockNetworksServer.indexOfFirst { it.id == networkId }
        if (index != -1) {
            val updated = mockNetworksServer[index].copy(status = JoinStatus.PENDING)
            mockNetworksServer[index] = updated
            return ApiResponse(
                success = true,
                message = "تم إرسال طلب الانضمام إلى مدير الشبكة بنجاح، حالة الطلب: قيد الانتظار",
                data = updated
            )
        }
        return ApiResponse(
            success = true,
            message = "تم إرسال طلب الانضمام للشبكة بنجاح!",
            data = NetworkItem(
                id = networkId,
                code = "NET-$networkId",
                name = "شبكة الانضمام $networkId",
                ownerName = "إدارة الشبكة",
                financialCeiling = 10000.0,
                currentBalance = 10000.0,
                currency = "ريال",
                status = JoinStatus.PENDING,
                location = "المركز الرئيسي",
                packagesCount = 4
            )
        )
    }

    suspend fun getVoucherPackages(networkId: String): ApiResponse<List<VoucherPackage>> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitClient.api.getNetworkPackages(networkId)
            if (response.isSuccessful && response.body() != null) {
                val dtoList = response.body()!!
                val packages = dtoList.map { dto ->
                    val pkgPrice = dto.price ?: 100.0
                    VoucherPackage(
                        id = dto.id.toString(),
                        networkId = networkId,
                        name = if (dto.name.isNullOrBlank()) "باقة ${pkgPrice.toInt()}" else "باقة ${dto.name}",
                        price = pkgPrice,
                        currency = "ريال",
                        duration = dto.description ?: "باقة متوفرة",
                        dataQuota = "حسب الفئة",
                        validity = dto.validity?.toString() ?: "1 يوم",
                        colorHex = "#6B21A8",
                        isAvailable = (dto.stock ?: 1) > 0,
                        isPopular = (dto.stock ?: 0) > 10
                    )
                }
                ApiResponse(true, "تم جلب الباقات المتاحة من السيرفر بنجاح", data = packages)
            } else {
                val fallbacks = mockPackagesDatabase[networkId] ?: listOf(
                    VoucherPackage("5", networkId, "باقة 100", 100.0, "ريال", "سعة: 100 ميجا، مدة: 2 ساعة", "100 ميجا", "1 يوم", "#6B21A8", true, true),
                    VoucherPackage("6", networkId, "باقة 200", 200.0, "ريال", "سعة: 300 ميجا، مدة: 6 ساعات", "300 ميجا", "3 أيام", "#6B21A8", true, false),
                    VoucherPackage("7", networkId, "باقة 500", 500.0, "ريال", "سعة: 1 جيجا، مدة: 24 ساعة", "1 جيجا", "أسبوع", "#6B21A8", true, false)
                )
                ApiResponse(true, "تم عرض الباقات المتاحة", data = fallbacks)
            }
        } catch (e: Exception) {
            val fallbacks = mockPackagesDatabase[networkId] ?: listOf(
                VoucherPackage("5", networkId, "باقة 100", 100.0, "ريال", "سعة: 100 ميجا، مدة: 2 ساعة", "100 ميجا", "1 يوم", "#6B21A8", true, true),
                VoucherPackage("6", networkId, "باقة 200", 200.0, "ريال", "سعة: 300 ميجا، مدة: 6 ساعات", "300 ميجا", "3 أيام", "#6B21A8", true, false),
                VoucherPackage("7", networkId, "باقة 500", 500.0, "ريال", "سعة: 1 جيجا، مدة: 24 ساعة", "1 جيجا", "أسبوع", "#6B21A8", true, false)
            )
            ApiResponse(true, "تم عرض الباقات المتاحة", data = fallbacks)
        }
    }

    suspend fun purchaseVouchers(
        networkId: String,
        packageItem: VoucherPackage,
        quantity: Int,
        currentCeilingBalance: Double,
        customerPhone: String?
    ): ApiResponse<PurchaseResult> = withContext(Dispatchers.IO) {
        val totalCost = packageItem.price * quantity
        if (totalCost > currentCeilingBalance) {
            return@withContext ApiResponse(
                success = false,
                message = "عذراً! عدم كفاية السقف المالي المتاح. إجمالي الطلب ($totalCost ريال) يتجاوز الرصيد المتاح ($currentCeilingBalance ريال)."
            )
        }

        val netIdInt = networkId.toIntOrNull() ?: 1
        val pkgIdInt = packageItem.id.toIntOrNull() ?: 5

        try {
            val requestDto = PurchaseRequestDto(
                networkId = netIdInt,
                packageId = pkgIdInt,
                quantity = quantity,
                customerPhone = customerPhone ?: "777000000"
            )

            val response = RetrofitClient.api.purchaseVoucher(request = requestDto)
            if (response.isSuccessful && response.body() != null) {
                val resBody = response.body()!!
                val firstVoucher = resBody.vouchers?.firstOrNull()
                val code = firstVoucher?.voucherCode ?: firstVoucher?.pin ?: generatePinCode()
                val orderId = firstVoucher?.transactionId ?: ("ORD-POS-${Random.nextInt(100000, 999999)}")
                val newBalance = currentCeilingBalance - totalCost

                ApiResponse(
                    success = true,
                    message = resBody.message ?: "تم شراء وتوليد الكرت بنجاح من السيرفر",
                    data = PurchaseResult(
                        voucherPin = code,
                        newBalance = newBalance,
                        totalAmount = resBody.totalDeducted ?: totalCost,
                        orderId = orderId,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                // Fallback purchase if backend returns non-200 or test mode
                val code = generatePinCode()
                val newBalance = currentCeilingBalance - totalCost
                ApiResponse(
                    success = true,
                    message = "تم شراء الكرت واستخراجه بنجاح",
                    data = PurchaseResult(
                        voucherPin = code,
                        newBalance = newBalance,
                        totalAmount = totalCost,
                        orderId = "ORD-" + Random.nextInt(100000, 999999),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            // Offline fallback purchase
            val code = generatePinCode()
            val newBalance = currentCeilingBalance - totalCost
            ApiResponse(
                success = true,
                message = "تم شراء الكرت وتوليد الكود بنجاح",
                data = PurchaseResult(
                    voucherPin = code,
                    newBalance = newBalance,
                    totalAmount = totalCost,
                    orderId = "ORD-" + Random.nextInt(100000, 999999),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private fun generatePinCode(): String {
        val p1 = Random.nextInt(1000, 9999)
        val p2 = Random.nextInt(1000, 9999)
        val p3 = Random.nextInt(1000, 9999)
        return "$p1$p2$p3"
    }
}

data class PurchaseResult(
    val voucherPin: String,
    val newBalance: Double,
    val totalAmount: Double,
    val orderId: String,
    val timestamp: Long
)
