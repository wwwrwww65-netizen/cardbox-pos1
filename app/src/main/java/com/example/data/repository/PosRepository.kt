package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.ApiResponse
import com.example.data.remote.MikroTikApiService
import com.example.data.remote.PurchaseResult
import com.example.notification.AppNotificationManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext



class PosRepository(
    private val db: AppDatabase,
    private val apiService: MikroTikApiService
) {
    val loggedInUser: Flow<PosUser?> = db.posAccountDao().getLoggedInAccount().map { entity ->
        entity?.let {
            PosUser(
                storeName = it.storeName,
                phone = it.phone,
                location = it.location,
                isLoggedIn = it.isLoggedIn
            )
        }
    }

    val joinedNetworks: Flow<List<NetworkItem>> = db.joinedNetworkDao().getAllJoinedNetworks().map { list ->
        list.map {
            NetworkItem(
                id = it.id,
                code = it.code,
                name = it.name,
                ownerName = it.ownerName,
                financialCeiling = it.financialCeiling,
                currentBalance = it.currentBalance,
                currency = it.currency,
                status = try { JoinStatus.valueOf(it.status) } catch (e: Exception) { JoinStatus.APPROVED },
                location = it.location,
                packagesCount = it.packagesCount
            )
        }
    }

    val orderHistory: Flow<List<OrderTransaction>> = db.orderDao().getAllOrders().map { list ->
        list.map {
            OrderTransaction(
                id = it.id,
                networkId = it.networkId,
                networkName = it.networkName,
                packageName = it.packageName,
                packagePrice = it.packagePrice,
                quantity = it.quantity,
                totalAmount = it.totalAmount,
                customerPhone = it.customerPhone,
                voucherPin = it.voucherPin,
                timestamp = it.timestamp,
                posStoreName = it.posStoreName,
                isPrinted = it.isPrinted,
                duration = it.duration,
                dataQuota = it.dataQuota,
                validity = it.validity
            )
        }
    }

    val printerSettings: Flow<PrinterDevice?> = db.printerDao().getPrinterSettings().map { entity ->
        if (entity != null) {
            PrinterDevice(
                name = entity.printerName,
                address = entity.macAddress,
                isConnected = entity.isConnected,
                isSimulationMode = entity.isSimulationMode
            )
        } else {
            PrinterDevice(
                name = "طابعة حرارية افتراضية (Simulation Mode)",
                address = "00:11:22:33:44:55",
                isConnected = true,
                isSimulationMode = true
            )
        }
    }

    val walletBalance: Flow<Double> = db.walletDao().getWalletAccount().map { entity ->
        entity?.balance ?: 35000.0 // Default initial wallet balance for demo
    }

    val walletTransactions: Flow<List<WalletTransaction>> = db.walletDao().getAllWalletTransactions().map { list ->
        list.map {
            WalletTransaction(
                id = it.id,
                title = it.title,
                type = try { WalletTxType.valueOf(it.type) } catch (e: Exception) { WalletTxType.DEPOSIT },
                amount = it.amount,
                currency = it.currency,
                referenceNumber = it.referenceNumber,
                paymentMethod = it.paymentMethod,
                status = try { WalletTxStatus.valueOf(it.status) } catch (e: Exception) { WalletTxStatus.COMPLETED },
                timestamp = it.timestamp,
                networkName = it.networkName
            )
        }
    }

    suspend fun initDefaultDataIfNeeded() = withContext(Dispatchers.IO) {
        // Pre-populate demo user and accepted networks if empty
        val existingAccount = db.posAccountDao().getAccountByPhone("770000000")
        if (existingAccount == null) {
            val demoAccount = PosAccountEntity(
                phone = "770000000",
                storeName = "سوبرماركت البركة والتوفير",
                location = "الأمانة - شارع التحرير - جولة العلفي",
                passwordHash = "123456",
                isLoggedIn = true
            )
            db.posAccountDao().insertAccount(demoAccount)
        }

        val wallet = db.walletDao().getWalletAccount()
        if (wallet == null) {
            db.walletDao().saveWalletAccount(WalletAccountEntity(id = 1, balance = 35000.0))
            // Pre-populate initial sample wallet transactions
            val initialTxs = listOf(
                WalletTransactionEntity(
                    id = "tx-1001",
                    title = "تغذية حساب عبر محفظة جيب",
                    type = WalletTxType.DEPOSIT.name,
                    amount = 25000.0,
                    currency = "ريال",
                    referenceNumber = "JAIB-981240",
                    paymentMethod = "جيب (Jaib)",
                    status = WalletTxStatus.COMPLETED.name,
                    timestamp = System.currentTimeMillis() - 86400000L,
                    networkName = null
                ),
                WalletTransactionEntity(
                    id = "tx-1002",
                    title = "تغذية حساب عبر بنك الكريمي",
                    type = WalletTxType.DEPOSIT.name,
                    amount = 10000.0,
                    currency = "ريال",
                    referenceNumber = "KRM-554129",
                    paymentMethod = "الكريمي (Kuraimi)",
                    status = WalletTxStatus.COMPLETED.name,
                    timestamp = System.currentTimeMillis() - 172800000L,
                    networkName = null
                )
            )
            initialTxs.forEach { db.walletDao().insertTransaction(it) }
        }

        val joined = db.joinedNetworkDao().getNetworkById("1")

        if (joined == null) {
            val realRes = apiService.fetchRealNetworks()
            if (realRes.success && !realRes.data.isNullOrEmpty()) {
                val realEntities = realRes.data.map { net ->
                    JoinedNetworkEntity(
                        id = net.id,
                        code = net.code,
                        name = net.name,
                        ownerName = net.ownerName,
                        financialCeiling = net.financialCeiling,
                        currentBalance = net.currentBalance,
                        currency = net.currency,
                        status = net.status.name,
                        location = net.location,
                        packagesCount = net.packagesCount
                    )
                }
                db.joinedNetworkDao().insertAll(realEntities)
            } else {
                val defaultJoined = listOf(
                    JoinedNetworkEntity(
                        id = "1",
                        code = "81234",
                        name = "شبكة التميز",
                        ownerName = "إدارة شبكة التميز",
                        financialCeiling = 20000.0,
                        currentBalance = 15500.0,
                        currency = "ريال",
                        status = JoinStatus.APPROVED.name,
                        location = "صنعاء - حدة",
                        packagesCount = 5
                    ),
                    JoinedNetworkEntity(
                        id = "2",
                        code = "NET-102",
                        name = "شبكة الأمل مايكروتك",
                        ownerName = "أبو طارق السنيد",
                        financialCeiling = 10000.0,
                        currentBalance = 8200.0,
                        currency = "ريال",
                        status = JoinStatus.APPROVED.name,
                        location = "الحصبة - الجولة الرئيسية",
                        packagesCount = 4
                    )
                )
                db.joinedNetworkDao().insertAll(defaultJoined)
            }
        }
    }

    suspend fun registerAccount(storeName: String, phone: String, location: String, password: String): ApiResponse<PosUser> = withContext(Dispatchers.IO) {
        if (storeName.isBlank() || phone.isBlank() || password.isBlank()) {
            return@withContext ApiResponse(false, "يرجى تعبئة جميع الحقول المطلوبة بشكل صحيح.")
        }
        val existing = db.posAccountDao().getAccountByPhone(phone)
        if (existing != null) {
            return@withContext ApiResponse(false, "رقم الجوال هذا مسجل مسبقاً كنقطة بيع. يمكنك تسجيل الدخول مباشرة.")
        }

        db.posAccountDao().logoutAll()
        val newAccount = PosAccountEntity(
            phone = phone,
            storeName = storeName,
            location = location,
            passwordHash = password,
            isLoggedIn = true
        )
        db.posAccountDao().insertAccount(newAccount)

        ApiResponse(
            success = true,
            message = "تم إنشاء حساب نقطة البيع بنجاح!",
            data = PosUser(storeName, phone, location, true)
        )
    }

    suspend fun login(phone: String, password: String): ApiResponse<PosUser> = withContext(Dispatchers.IO) {
        // Authenticate with real server if possible
        apiService.loginApi(phone, password)

        var account = db.posAccountDao().getAccountByPhone(phone)
        if (account == null) {
            // Auto-create account for direct seamless login
            val newAccount = PosAccountEntity(
                phone = phone,
                storeName = "متجر $phone",
                location = "المركز الرئيسي",
                passwordHash = password,
                isLoggedIn = true
            )
            db.posAccountDao().insertAccount(newAccount)
            account = newAccount
        } else if (account.passwordHash != password) {
            return@withContext ApiResponse(false, "كلمة المرور غير صحيحة. حاول مرة أخرى.")
        }

        db.posAccountDao().logoutAll()
        db.posAccountDao().setLoggedIn(phone)

        // Fetch real networks from server into local database
        val realNetsRes = apiService.fetchRealNetworks()
        if (realNetsRes.success && !realNetsRes.data.isNullOrEmpty()) {
            val entities = realNetsRes.data.map { net ->
                JoinedNetworkEntity(
                    id = net.id,
                    code = net.code,
                    name = net.name,
                    ownerName = net.ownerName,
                    financialCeiling = net.financialCeiling,
                    currentBalance = net.currentBalance,
                    currency = net.currency,
                    status = net.status.name,
                    location = net.location,
                    packagesCount = net.packagesCount
                )
            }
            db.joinedNetworkDao().insertAll(entities)
        }

        ApiResponse(
            success = true,
            message = "تم تسجيل الدخول بنجاح مع سيرفر POS!",
            data = PosUser(account.storeName, account.phone, account.location, true)
        )
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        db.posAccountDao().logoutAll()
    }

    suspend fun updateProfileInfo(phone: String, newStoreName: String, newLocation: String): ApiResponse<PosUser> = withContext(Dispatchers.IO) {
        if (newStoreName.isBlank()) {
            return@withContext ApiResponse(false, "يرجى كتابة اسم متجر صحيح.")
        }
        db.posAccountDao().updateAccountInfo(phone, newStoreName, newLocation)
        ApiResponse(
            success = true,
            message = "تم تحديث بيانات الحساب والمتجر بنجاح!",
            data = PosUser(newStoreName, phone, newLocation, true)
        )
    }

    suspend fun changePassword(phone: String, oldPass: String, newPass: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val account = db.posAccountDao().getAccountByPhone(phone)
            ?: return@withContext ApiResponse(false, "الحساب غير موجود.")
        if (account.passwordHash != oldPass) {
            return@withContext ApiResponse(false, "كلمة المرور الحالية غير صحيحة.")
        }
        if (newPass.length < 4) {
            return@withContext ApiResponse(false, "كلمة المرور الجديدة يجب ألا تقل عن 4 خانات.")
        }
        db.posAccountDao().updatePassword(phone, newPass)
        ApiResponse(true, "تم تغيير كلمة المرور بنجاح!")
    }

    suspend fun resetPasswordWithOtp(phone: String, newPass: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        val account = db.posAccountDao().getAccountByPhone(phone)
            ?: return@withContext ApiResponse(false, "رقم الجوال غير مسجل لدينا.")
        if (newPass.length < 4) {
            return@withContext ApiResponse(false, "كلمة المرور الجديدة يجب ألا تقل عن 4 خانات.")
        }
        db.posAccountDao().updatePassword(phone, newPass)
        ApiResponse(true, "تم إعادة تعيين كلمة المرور بنجاح!")
    }

    suspend fun deleteAccount(phone: String): ApiResponse<Boolean> = withContext(Dispatchers.IO) {
        db.posAccountDao().deleteAccountByPhone(phone)
        db.posAccountDao().logoutAll()
        ApiResponse(true, "تم حذف حساب نقطة البيع وجميع البيانات بنجاح.")
    }

    suspend fun trackNetworkInDb(network: NetworkItem) = withContext(Dispatchers.IO) {
        val entity = JoinedNetworkEntity(
            id = network.id,
            code = network.code,
            name = network.name,
            ownerName = network.ownerName,
            financialCeiling = network.financialCeiling,
            currentBalance = network.currentBalance,
            currency = network.currency,
            status = network.status.name,
            location = network.location,
            packagesCount = network.packagesCount
        )
        db.joinedNetworkDao().insertOrUpdateNetwork(entity)
    }

    suspend fun searchNetwork(code: String): ApiResponse<NetworkItem> {
        return apiService.searchNetworkByCode(code)
    }

    suspend fun requestJoinNetwork(network: NetworkItem, storeName: String, storePhone: String): ApiResponse<NetworkItem> = withContext(Dispatchers.IO) {
        val res = apiService.requestJoinNetwork(network.id, storeName, storePhone)
        if (res.success && res.data != null) {
            val net = res.data
            val entity = JoinedNetworkEntity(
                id = net.id,
                code = net.code,
                name = net.name,
                ownerName = net.ownerName,
                financialCeiling = net.financialCeiling,
                currentBalance = net.currentBalance,
                currency = net.currency,
                status = net.status.name,
                location = net.location,
                packagesCount = net.packagesCount
            )
            db.joinedNetworkDao().insertOrUpdateNetwork(entity)
        }
        res
    }

    suspend fun getVoucherPackages(networkId: String): ApiResponse<List<VoucherPackage>> {
        return apiService.getVoucherPackages(networkId)
    }

    suspend fun purchaseVouchers(
        network: NetworkItem,
        packageItem: VoucherPackage,
        quantity: Int,
        customerPhone: String?,
        storeName: String
    ): ApiResponse<OrderTransaction> = withContext(Dispatchers.IO) {

        val res = apiService.purchaseVouchers(
            networkId = network.id,
            packageItem = packageItem,
            quantity = quantity,
            currentCeilingBalance = network.currentBalance,
            customerPhone = customerPhone
        )

        if (!res.success || res.data == null) {
            return@withContext ApiResponse(false, res.message)
        }

        val result: PurchaseResult = res.data
        // Update Room local balance for this network
        db.joinedNetworkDao().updateBalance(network.id, result.newBalance)

        // Save Order transaction record
        val order = OrderTransaction(
            id = result.orderId,
            networkId = network.id,
            networkName = network.name,
            packageName = packageItem.name,
            packagePrice = packageItem.price,
            quantity = quantity,
            totalAmount = result.totalAmount,
            customerPhone = customerPhone,
            voucherPin = result.voucherPin,
            timestamp = result.timestamp,
            posStoreName = storeName,
            isPrinted = false,
            duration = packageItem.duration,
            dataQuota = packageItem.dataQuota,
            validity = packageItem.validity
        )

        db.orderDao().insertOrder(
            OrderTransactionEntity(
                id = order.id,
                networkId = order.networkId,
                networkName = order.networkName,
                packageName = order.packageName,
                packagePrice = order.packagePrice,
                quantity = order.quantity,
                totalAmount = order.totalAmount,
                customerPhone = order.customerPhone,
                voucherPin = order.voucherPin,
                timestamp = order.timestamp,
                posStoreName = order.posStoreName,
                isPrinted = order.isPrinted,
                duration = order.duration,
                dataQuota = order.dataQuota,
                validity = order.validity
            )
        )

        ApiResponse(
            success = true,
            message = "تمت عملية الشراء بنجاح وترحيل الرصيد من السقف المالي المتاح",
            data = order
        )
    }

    suspend fun savePrinterSettings(device: PrinterDevice) = withContext(Dispatchers.IO) {
        db.printerDao().savePrinterSettings(
            PrinterSettingsEntity(
                id = 1,
                printerName = device.name,
                macAddress = device.address,
                isConnected = device.isConnected,
                isSimulationMode = device.isSimulationMode
            )
        )
    }

    suspend fun markOrderPrinted(orderId: String) = withContext(Dispatchers.IO) {
        db.orderDao().markPrinted(orderId)
    }

    suspend fun topUpWalletBalance(
        amount: Double,
        paymentMethodName: String,
        referenceNumber: String
    ): ApiResponse<Double> = withContext(Dispatchers.IO) {
        if (amount <= 0) {
            return@withContext ApiResponse(false, "يرجى إدخال مبلغ إيداع صحيح.")
        }
        if (referenceNumber.isBlank()) {
            return@withContext ApiResponse(false, "يرجى إدخال الرقم المرجعي أو رقم العملية بشكل صحيح للتحقق.")
        }

        // Fetch current wallet account
        val currentAccount = db.walletDao().getWalletAccount().map { it?.balance ?: 35000.0 }.firstOrNull() ?: 35000.0
        val updatedBalance = currentAccount + amount

        // Save updated balance
        db.walletDao().saveWalletAccount(WalletAccountEntity(id = 1, balance = updatedBalance))

        // Record deposit transaction
        val txId = "tx-${System.currentTimeMillis()}"
        val tx = WalletTransactionEntity(
            id = txId,
            title = "تغذية حساب عبر $paymentMethodName",
            type = WalletTxType.DEPOSIT.name,
            amount = amount,
            currency = "ريال",
            referenceNumber = referenceNumber,
            paymentMethod = paymentMethodName,
            status = WalletTxStatus.COMPLETED.name,
            timestamp = System.currentTimeMillis(),
            networkName = null
        )
        db.walletDao().insertTransaction(tx)

        ApiResponse(
            success = true,
            message = "تم التحقق وتغذية محفظتك بنجاح بالمبلغ (${amount.toInt()} ريال)! رصيدك الجديد: ${updatedBalance.toInt()} ريال.",
            data = updatedBalance
        )
    }

    suspend fun purchaseVouchersWithWallet(
        network: NetworkItem,
        packageItem: VoucherPackage,
        quantity: Int,
        customerPhone: String?,
        storeName: String,
        currentWalletBalance: Double
    ): ApiResponse<OrderTransaction> = withContext(Dispatchers.IO) {
        val totalAmount = packageItem.price * quantity
        if (currentWalletBalance < totalAmount) {
            return@withContext ApiResponse(
                success = false,
                message = "لا يوجد رصيد كافٍ في محفظة كارد بوكس. رصيدك الحالي: ${currentWalletBalance.toInt()} ريال، المطلوب: ${totalAmount.toInt()} ريال. يرجى تغذية رصيد حسابك."
            )
        }

        // Deduct from wallet balance
        val newWalletBalance = currentWalletBalance - totalAmount
        db.walletDao().saveWalletAccount(WalletAccountEntity(id = 1, balance = newWalletBalance))

        // Generate Order Vouchers
        val pinCode = (100000000000..999999999999).random().toString()
        val orderId = "ORD-CB-${(10000..99999).random()}"
        val timestamp = System.currentTimeMillis()

        val order = OrderTransaction(
            id = orderId,
            networkId = network.id,
            networkName = network.name,
            packageName = packageItem.name,
            packagePrice = packageItem.price,
            quantity = quantity,
            totalAmount = totalAmount,
            customerPhone = customerPhone,
            voucherPin = pinCode,
            timestamp = timestamp,
            posStoreName = storeName,
            isPrinted = false,
            duration = packageItem.duration,
            dataQuota = packageItem.dataQuota,
            validity = packageItem.validity
        )

        db.orderDao().insertOrder(
            OrderTransactionEntity(
                id = order.id,
                networkId = order.networkId,
                networkName = order.networkName,
                packageName = order.packageName,
                packagePrice = order.packagePrice,
                quantity = order.quantity,
                totalAmount = order.totalAmount,
                customerPhone = order.customerPhone,
                voucherPin = order.voucherPin,
                timestamp = order.timestamp,
                posStoreName = order.posStoreName,
                isPrinted = order.isPrinted,
                duration = order.duration,
                dataQuota = order.dataQuota,
                validity = order.validity
            )
        )

        // Record Wallet Transaction record for this card purchase
        val txId = "tx-cb-${System.currentTimeMillis()}"
        db.walletDao().insertTransaction(
            WalletTransactionEntity(
                id = txId,
                title = "شراء ${quantity} كرت (${packageItem.name}) - ${network.name}",
                type = WalletTxType.VOUCHER_PURCHASE.name,
                amount = totalAmount,
                currency = "ريال",
                referenceNumber = orderId,
                paymentMethod = "محفظة كارد بوكس (CardBox Wallet)",
                status = WalletTxStatus.COMPLETED.name,
                timestamp = timestamp,
                networkName = network.name
            )
        )

        ApiResponse(
            success = true,
            message = "تم خصم الإجمالي (${totalAmount.toInt()} ريال) من محفظة كارد بوكس واستخراج الكرت بنجاح! رصيد محفظتك المتبقي: ${newWalletBalance.toInt()} ريال.",
            data = order
        )
    }

    // --- Notifications logic ---
    val notifications: Flow<List<AppNotification>> = db.notificationDao().getAllNotifications().map { list ->
        list.map {
            AppNotification(
                id = it.id,
                title = it.title,
                message = it.message,
                type = try { NotificationType.valueOf(it.type) } catch (e: Exception) { NotificationType.SYSTEM_ANNOUNCEMENT },
                timestamp = it.timestamp,
                isRead = it.isRead,
                relatedEntityId = it.relatedEntityId,
                amount = it.amount
            )
        }
    }

    val unreadNotificationsCount: Flow<Int> = db.notificationDao().getUnreadCount()

    suspend fun addNotification(
        title: String,
        message: String,
        type: NotificationType,
        relatedEntityId: String? = null,
        amount: Double? = null,
        context: Context? = null
    ) = withContext(Dispatchers.IO) {
        val id = "notif-${System.currentTimeMillis()}-${(1000..9999).random()}"
        val entity = AppNotificationEntity(
            id = id,
            title = title,
            message = message,
            type = type.name,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedEntityId = relatedEntityId,
            amount = amount
        )
        db.notificationDao().insertNotification(entity)

        context?.let { ctx ->
            AppNotificationManager.showSystemNotification(
                context = ctx,
                notificationId = (id.hashCode() and 0x7FFFFFFF),
                title = title,
                message = message,
                type = type
            )
        }
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        db.notificationDao().markAsRead(id)
    }

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        db.notificationDao().markAllAsRead()
    }

    suspend fun deleteNotification(id: String) = withContext(Dispatchers.IO) {
        db.notificationDao().deleteNotification(id)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        db.notificationDao().clearAllNotifications()
    }

    suspend fun seedDefaultNotificationsIfEmpty(context: Context? = null) = withContext(Dispatchers.IO) {
        val currentList = db.notificationDao().getAllNotifications().firstOrNull()
        if (currentList.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            val sampleNotifications = listOf(
                AppNotificationEntity(
                    id = "notif-1",
                    title = "قبول انضمام للشبكة",
                    message = "تم قبول انضمام نقطة بيعك بنجاح في شبكة النور سيتي! يمكنك الآن استخراج وتوزيع باقات الشبكة مباشرة.",
                    type = NotificationType.NETWORK_JOIN_APPROVED.name,
                    timestamp = now - (1000 * 60 * 15), // 15 mins ago
                    isRead = false,
                    relatedEntityId = "net-101"
                ),
                AppNotificationEntity(
                    id = "notif-2",
                    title = "شحن رصيد آجل من مالك الشبكة",
                    message = "تم منحك وتغذية سقف آجل بقيمة 15,000 ر.ي من مالك شبكة المستقبل بنجاح.",
                    type = NotificationType.NETWORK_CREDIT_GRANTED.name,
                    timestamp = now - (1000 * 60 * 60 * 2), // 2 hours ago
                    isRead = false,
                    relatedEntityId = "net-102",
                    amount = 15000.0
                ),
                AppNotificationEntity(
                    id = "notif-3",
                    title = "تنبيه: قرب نفاذ رصيد الشبكة",
                    message = "لقد قارب رصيدك المتاح في شبكة الواحة على النفاذ (المتبقي: 2,500 ر.ي)، يرجى التواصل مع مزود الخدمة لسداد الحساب وتغذية الرصيد.",
                    type = NotificationType.NETWORK_LOW_BALANCE.name,
                    timestamp = now - (1000 * 60 * 60 * 5), // 5 hours ago
                    isRead = false,
                    relatedEntityId = "net-103",
                    amount = 2500.0
                ),
                AppNotificationEntity(
                    id = "notif-4",
                    title = "شحن المحفظة بنجاح",
                    message = "تم شحن وتغذية محفظتك المالية بمبلغ 25,000 ر.ي بنجاح عبر تحويل الكريمي إكسبرس.",
                    type = NotificationType.WALLET_TOPUP_SUCCESS.name,
                    timestamp = now - (1000 * 60 * 60 * 24), // 1 day ago
                    isRead = true,
                    amount = 25000.0
                ),
                AppNotificationEntity(
                    id = "notif-5",
                    title = "تنبيه رصيد المحفظة",
                    message = "تنبيه: رصيد المحفظة المالية المتبقي يقترب من الحد الأدنى. يُوصى بإعادة الشحن لضمان الاستمرار دون انقطاع.",
                    type = NotificationType.WALLET_LOW_BALANCE.name,
                    timestamp = now - (1000 * 60 * 60 * 36), // 36 hours ago
                    isRead = true
                ),
                AppNotificationEntity(
                    id = "notif-6",
                    title = "تحديث جديد وعروض العمولات",
                    message = "تم إطلاق ميزة الطباعة المصغرة الفورية وتفعيل خصم 5% على عمولات باقات شبكات POS المتاحة!",
                    type = NotificationType.SYSTEM_ANNOUNCEMENT.name,
                    timestamp = now - (1000 * 60 * 60 * 48), // 2 days ago
                    isRead = true
                )
            )

            db.notificationDao().insertAll(sampleNotifications)
        }
    }
}


