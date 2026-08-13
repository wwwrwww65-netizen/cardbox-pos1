package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.MikroTikApiService
import com.example.data.repository.PosRepository
import com.example.printer.ThermalPrinterManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val apiService = MikroTikApiService()
    val repository = PosRepository(db, apiService)
    val printerManager = ThermalPrinterManager(application)

    private val prefs = application.getSharedPreferences("cardbox_pos_prefs", android.content.Context.MODE_PRIVATE)

    fun isFirstRun(): Boolean = prefs.getBoolean("is_first_run", true)

    fun setFirstRunCompleted() {
        prefs.edit().putBoolean("is_first_run", false).apply()
    }

    private val _themeMode = MutableStateFlow(
        runCatching {
            com.example.ui.theme.ThemeMode.valueOf(prefs.getString("theme_mode", com.example.ui.theme.ThemeMode.SYSTEM.name) ?: com.example.ui.theme.ThemeMode.SYSTEM.name)
        }.getOrDefault(com.example.ui.theme.ThemeMode.SYSTEM)
    )
    val themeMode: StateFlow<com.example.ui.theme.ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: com.example.ui.theme.ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun toggleThemeMode() {
        val nextMode = when (_themeMode.value) {
            com.example.ui.theme.ThemeMode.LIGHT -> com.example.ui.theme.ThemeMode.DARK
            com.example.ui.theme.ThemeMode.DARK -> com.example.ui.theme.ThemeMode.SYSTEM
            com.example.ui.theme.ThemeMode.SYSTEM -> com.example.ui.theme.ThemeMode.LIGHT
        }
        setThemeMode(nextMode)
    }

    val currentUser = repository.loggedInUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val joinedNetworks = repository.joinedNetworks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _pinnedNetworkIds = MutableStateFlow<Set<String>>(setOf("net-101"))
    val pinnedNetworkIds: StateFlow<Set<String>> = _pinnedNetworkIds.asStateFlow()

    private val _networkCustomOrder = MutableStateFlow<List<String>>(emptyList())
    val networkCustomOrder: StateFlow<List<String>> = _networkCustomOrder.asStateFlow()

    val sortedJoinedNetworks: StateFlow<List<NetworkItem>> = combine(
        joinedNetworks,
        _pinnedNetworkIds,
        _networkCustomOrder
    ) { list, pinnedSet, customOrder ->
        list.map { net ->
            net.copy(isPinned = pinnedSet.contains(net.id))
        }.sortedWith(
            compareByDescending<NetworkItem> { it.isPinned }
                .thenBy { net ->
                    val idx = customOrder.indexOf(net.id)
                    if (idx != -1) idx else Int.MAX_VALUE
                }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun togglePinNetwork(networkId: String) {
        viewModelScope.launch {
            val current = _pinnedNetworkIds.value.toMutableSet()
            if (current.contains(networkId)) {
                current.remove(networkId)
                setToast("تم إلغاء تثبيت الشبكة")
            } else {
                current.add(networkId)
                val searched = _searchedNetwork.value
                if (searched != null && searched.id == networkId) {
                    repository.trackNetworkInDb(searched)
                }
                setToast("تم تثبيت الشبكة في الأعلى")
            }
            _pinnedNetworkIds.value = current
        }
    }

    fun moveNetworkOrder(networkId: String, moveUp: Boolean) {
        val currentList = sortedJoinedNetworks.value.map { it.id }.toMutableList()
        val index = currentList.indexOf(networkId)
        if (index != -1) {
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in 0 until currentList.size) {
                val item = currentList.removeAt(index)
                currentList.add(targetIndex, item)
                _networkCustomOrder.value = currentList
                setToast("تم تعديل ترتيب الشبكة")
            }
        }
    }

    val orderHistory = repository.orderHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activePrinter = repository.printerSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrinterDevice("طابعة افتراضية 58mm", "00:11:22:33:44:55", true, true)
    )

    val walletBalance = repository.walletBalance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 35000.0
    )

    val walletTransactions = repository.walletTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Dialog state management for unjoined network purchase & ceiling exhausted flows
    private val _showUnjoinedTopUpDialog = MutableStateFlow(false)
    val showUnjoinedTopUpDialog: StateFlow<Boolean> = _showUnjoinedTopUpDialog.asStateFlow()

    private val _showCeilingExhaustedDialog = MutableStateFlow(false)
    val showCeilingExhaustedDialog: StateFlow<Boolean> = _showCeilingExhaustedDialog.asStateFlow()

    private val _showInsufficientWalletDialog = MutableStateFlow(false)
    val showInsufficientWalletDialog: StateFlow<Boolean> = _showInsufficientWalletDialog.asStateFlow()

    private val _isTopUpProcessing = MutableStateFlow(false)
    val isTopUpProcessing: StateFlow<Boolean> = _isTopUpProcessing.asStateFlow()

    fun dismissUnjoinedTopUpDialog() {
        _showUnjoinedTopUpDialog.value = false
    }

    fun dismissCeilingExhaustedDialog() {
        _showCeilingExhaustedDialog.value = false
    }

    fun dismissInsufficientWalletDialog() {
        _showInsufficientWalletDialog.value = false
    }


    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchedNetwork = MutableStateFlow<NetworkItem?>(null)
    val searchedNetwork: StateFlow<NetworkItem?> = _searchedNetwork.asStateFlow()

    private val _selectedNetwork = MutableStateFlow<NetworkItem?>(null)
    val selectedNetwork: StateFlow<NetworkItem?> = _selectedNetwork.asStateFlow()

    private val _availablePackages = MutableStateFlow<List<VoucherPackage>>(emptyList())
    val availablePackages: StateFlow<List<VoucherPackage>> = _availablePackages.asStateFlow()

    // Map of packageId to quantity selected
    private val _selectedQuantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val selectedQuantities: StateFlow<Map<String, Int>> = _selectedQuantities.asStateFlow()

    private val _customerPhone = MutableStateFlow("")
    val customerPhone: StateFlow<String> = _customerPhone.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _latestOrder = MutableStateFlow<OrderTransaction?>(null)
    val latestOrder: StateFlow<OrderTransaction?> = _latestOrder.asStateFlow()

    private val _showReceiptModal = MutableStateFlow(false)
    val showReceiptModal: StateFlow<Boolean> = _showReceiptModal.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initDefaultDataIfNeeded()
            repository.seedDefaultNotificationsIfEmpty(getApplication())
        }
    }

    val notifications = repository.notifications.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadNotificationsCount = repository.unreadNotificationsCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            setToast("تم تحديد جميع التنبيهات كمقروءة")
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            setToast("تم مسح جميع التنبيهات")
        }
    }

    fun sendNotification(title: String, message: String, type: NotificationType, amount: Double? = null) {
        viewModelScope.launch {
            repository.addNotification(
                title = title,
                message = message,
                type = type,
                amount = amount,
                context = getApplication()
            )
        }
    }


    fun setToast(message: String?) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchNetwork() {
        val q = _searchQuery.value.trim()
        if (q.isEmpty()) {
            _toastMessage.value = "يرجى إدخال كود الشبكة للبحث (مثال: NET-101)"
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            val res = repository.searchNetwork(q)
            _isSearching.value = false
            if (res.success && res.data != null) {
                _searchedNetwork.value = res.data
                _toastMessage.value = res.message
            } else {
                _searchedNetwork.value = null
                _toastMessage.value = res.message
            }
        }
    }

    fun requestJoinNetwork(network: NetworkItem) {
        val user = currentUser.value
        val storeName = user?.storeName ?: "نقطة بيع البركة"
        val storePhone = user?.phone ?: "770000000"

        viewModelScope.launch {
            val res = repository.requestJoinNetwork(network, storeName, storePhone)
            if (res.success && res.data != null) {
                _searchedNetwork.value = res.data
                _toastMessage.value = res.message
                repository.addNotification(
                    title = "تم قبول انضمامك في شبكة ${network.name}",
                    message = "تهانينا! تم قبول انضمام نقطة بيعك بنجاح في شبكة ${network.name}. يمكنك الآن بيع واستخراج جميع باقات الشبكة مباشرة.",
                    type = NotificationType.NETWORK_JOIN_APPROVED,
                    relatedEntityId = network.id,
                    context = getApplication()
                )
            } else {
                _toastMessage.value = res.message
            }
        }
    }


    fun selectNetworkForStore(network: NetworkItem) {
        _selectedNetwork.value = network
        _selectedQuantities.value = emptyMap()
        _customerPhone.value = ""
        fetchPackagesForNetwork(network.id)
    }

    fun fetchPackagesForNetwork(networkId: String) {
        viewModelScope.launch {
            val res = repository.getVoucherPackages(networkId)
            if (res.success && res.data != null) {
                _availablePackages.value = res.data
            } else {
                _availablePackages.value = emptyList()
            }
        }
    }

    fun setQuantity(packageId: String, qty: Int) {
        val map = _selectedQuantities.value.toMutableMap()
        if (qty <= 0) {
            map.remove(packageId)
        } else {
            map[packageId] = qty
        }
        _selectedQuantities.value = map
    }

    fun incrementQuantity(packageId: String) {
        val current = _selectedQuantities.value[packageId] ?: 0
        setQuantity(packageId, current + 1)
    }

    fun decrementQuantity(packageId: String) {
        val current = _selectedQuantities.value[packageId] ?: 0
        setQuantity(packageId, current - 1)
    }

    fun setCustomerPhone(phone: String) {
        _customerPhone.value = phone
    }

    fun clearAllSelections() {
        _selectedQuantities.value = emptyMap()
        _customerPhone.value = ""
    }

    fun processPurchase() {
        val net = _selectedNetwork.value
        if (net == null) {
            _toastMessage.value = "يرجى تحديد الشبكة أولاً"
            return
        }

        val entries = _selectedQuantities.value.filterValues { it > 0 }
        if (entries.isEmpty()) {
            _toastMessage.value = "يرجى تحديد كمية كرت واحد على الأقل للمتابعة"
            return
        }

        val packageId = entries.keys.first()
        val quantity = entries.values.first()
        val pkg = _availablePackages.value.find { it.id == packageId }
        if (pkg == null) {
            _toastMessage.value = "الباقة غير متاحة حالياً"
            return
        }

        val totalCost = pkg.price * quantity

        // Flow 1: Unjoined network
        if (net.status != JoinStatus.APPROVED) {
            _showUnjoinedTopUpDialog.value = true
            return
        }

        // Flow 2: Joined network but financial ceiling exceeded
        if (totalCost > net.currentBalance) {
            _showCeilingExhaustedDialog.value = true
            return
        }

        // Normal Flow: Purchase from network ceiling balance
        val storeName = currentUser.value?.storeName ?: "نقطة بيع معتمدة"

        viewModelScope.launch {
            _isPurchasing.value = true
            val res = repository.purchaseVouchers(
                network = net,
                packageItem = pkg,
                quantity = quantity,
                customerPhone = _customerPhone.value.ifBlank { null },
                storeName = storeName
            )
            _isPurchasing.value = false

            if (res.success && res.data != null) {
                _latestOrder.value = res.data
                _showReceiptModal.value = true
                // Update selected network balance locally
                _selectedNetwork.value = net.copy(currentBalance = net.currentBalance - totalCost)
                _selectedQuantities.value = emptyMap()
                _toastMessage.value = res.message
            } else {
                _toastMessage.value = res.message
            }
        }
    }

    fun processPurchaseViaWallet() {
        _showCeilingExhaustedDialog.value = false
        _showUnjoinedTopUpDialog.value = false

        val net = _selectedNetwork.value ?: return
        val entries = _selectedQuantities.value.filterValues { it > 0 }
        if (entries.isEmpty()) return

        val packageId = entries.keys.first()
        val quantity = entries.values.first()
        val pkg = _availablePackages.value.find { it.id == packageId } ?: return
        val totalCost = pkg.price * quantity
        val currentWallet = walletBalance.value

        if (currentWallet < totalCost) {
            _showInsufficientWalletDialog.value = true
            return
        }

        val storeName = currentUser.value?.storeName ?: "نقطة بيع معتمدة"

        viewModelScope.launch {
            _isPurchasing.value = true
            val res = repository.purchaseVouchersWithWallet(
                network = net,
                packageItem = pkg,
                quantity = quantity,
                customerPhone = _customerPhone.value.ifBlank { null },
                storeName = storeName,
                currentWalletBalance = currentWallet
            )
            _isPurchasing.value = false

            if (res.success && res.data != null) {
                _latestOrder.value = res.data
                _showReceiptModal.value = true
                _selectedQuantities.value = emptyMap()
                _toastMessage.value = res.message
            } else {
                _toastMessage.value = res.message
            }
        }
    }

    fun topUpWallet(amount: Double, paymentMethodName: String, referenceNumber: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isTopUpProcessing.value = true
            val res = repository.topUpWalletBalance(amount, paymentMethodName, referenceNumber)
            _isTopUpProcessing.value = false
            onResult(res.success, res.message)
            if (res.success) {
                _toastMessage.value = res.message
                repository.addNotification(
                    title = "شحن المحفظة بنجاح",
                    message = "تمت إضافة مبلغ ${amount.toInt()} ريال لحساب محفظتك بنجاح عبر $paymentMethodName (المرجع: $referenceNumber).",
                    type = NotificationType.WALLET_TOPUP_SUCCESS,
                    amount = amount,
                    context = getApplication()
                )
            }
        }
    }



    fun dismissReceiptModal() {
        _showReceiptModal.value = false
    }

    fun printCurrentOrderReceipt() {
        val order = _latestOrder.value
        val printer = activePrinter.value
        if (order == null || printer == null) {
            _toastMessage.value = "لا يوجد كرت جاهز للطباعة"
            return
        }

        viewModelScope.launch {
            _isPrinting.value = true
            val printRes = printerManager.printOrder(order, printer)
            _isPrinting.value = false
            if (printRes.success) {
                repository.markOrderPrinted(order.id)
                _latestOrder.value = order.copy(isPrinted = true)
                _toastMessage.value = printRes.message
            } else {
                _toastMessage.value = printRes.message
            }
        }
    }

    fun saveSelectedPrinter(device: PrinterDevice) {
        viewModelScope.launch {
            repository.savePrinterSettings(device)
            _toastMessage.value = "تم حفظ إعدادات الطابعة (${device.name}) بنجاح"
        }
    }

    fun getSavedPhone(): String = prefs.getString("saved_phone", "770000000") ?: "770000000"
    fun getSavedPassword(): String = prefs.getString("saved_password", "123456") ?: "123456"
    fun isRememberMeEnabled(): Boolean = prefs.getBoolean("remember_me", true)

    fun saveAuthCredentials(phone: String, pass: String, remember: Boolean) {
        if (remember) {
            prefs.edit()
                .putString("saved_phone", phone)
                .putString("saved_password", pass)
                .putBoolean("remember_me", true)
                .apply()
        } else {
            prefs.edit()
                .remove("saved_phone")
                .remove("saved_password")
                .putBoolean("remember_me", false)
                .apply()
        }
    }

    fun registerAccount(storeName: String, phone: String, location: String, password: String, rememberMe: Boolean = true, onResult: (Boolean, String) -> Unit) {
        saveAuthCredentials(phone, password, rememberMe)
        viewModelScope.launch {
            val res = repository.registerAccount(storeName, phone, location, password)
            onResult(res.success, res.message)
            if (res.success) {
                _toastMessage.value = res.message
            }
        }
    }

    fun login(phone: String, password: String, rememberMe: Boolean = true, onResult: (Boolean, String) -> Unit) {
        saveAuthCredentials(phone, password, rememberMe)
        viewModelScope.launch {
            val res = repository.login(phone, password)
            onResult(res.success, res.message)
            if (res.success) {
                _toastMessage.value = res.message
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _toastMessage.value = "تم تسجيل الخروج بنجاح"
        }
    }

    fun updateProfile(newStoreName: String, newLocation: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.updateProfileInfo(user.phone, newStoreName, newLocation)
            if (res.success) {
                _toastMessage.value = res.message
            }
            onResult(res.success, res.message)
        }
    }

    fun changePassword(oldPass: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.changePassword(user.phone, oldPass, newPass)
            if (res.success) {
                _toastMessage.value = res.message
            }
            onResult(res.success, res.message)
        }
    }

    fun resetPasswordWithOtp(phone: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = repository.resetPasswordWithOtp(phone, newPass)
            if (res.success) {
                _toastMessage.value = res.message
            }
            onResult(res.success, res.message)
        }
    }

    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val res = repository.deleteAccount(user.phone)
            if (res.success) {
                _toastMessage.value = res.message
            }
            onResult(res.success, res.message)
        }
    }
}
