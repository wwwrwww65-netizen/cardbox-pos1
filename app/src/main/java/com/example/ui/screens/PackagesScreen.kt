package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.JoinStatus
import com.example.data.model.NetworkItem
import com.example.data.model.OrderTransaction
import com.example.data.model.VoucherPackage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesScreen(
    network: NetworkItem,
    packages: List<VoucherPackage>,
    selectedQuantities: Map<String, Int>,
    customerPhone: String,
    isPurchasing: Boolean,
    orders: List<OrderTransaction> = emptyList(),
    showUnjoinedTopUpDialog: Boolean = false,
    showCeilingExhaustedDialog: Boolean = false,
    showInsufficientWalletDialog: Boolean = false,
    onDismissUnjoinedDialog: () -> Unit = {},
    onDismissCeilingDialog: () -> Unit = {},
    onDismissInsufficientWalletDialog: () -> Unit = {},
    onQuantityChange: (String, Int) -> Unit,
    onClearAll: () -> Unit = {},
    onCustomerPhoneChange: (String) -> Unit,
    onProcessPurchase: () -> Unit,
    onProcessPurchaseViaWallet: () -> Unit = {},
    onRequestJoin: (NetworkItem) -> Unit,
    onNavigateToWallet: () -> Unit = {},
    onBack: () -> Unit
) {
    val selectedEntries = selectedQuantities.filterValues { it > 0 }
    val totalQuantity = selectedEntries.values.sum()


    // Calculate total price
    val totalPrice = selectedEntries.entries.sumOf { entry ->
        val pkg = packages.find { it.id == entry.key }
        (pkg?.price ?: 0.0) * entry.value
    }

    // Network stats metrics calculation
    val networkOrders = remember(orders, network.id) {
        orders.filter { it.networkId == network.id }
    }
    val totalSoldCards = networkOrders.sumOf { it.quantity }
    val totalSoldAmount = networkOrders.sumOf { it.totalAmount }
    val consumedDebt = (network.financialCeiling - network.currentBalance).coerceAtLeast(0.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                network.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "السقف: ${network.currentBalance.toInt()} ريال",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Join request button or status badge in header
                        when (network.status) {
                            JoinStatus.APPROVED -> {
                                Surface(
                                    color = PosEmeraldSuccess.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = PosEmeraldSuccess,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "معتمدة",
                                            fontSize = 11.sp,
                                            color = PosEmeraldSuccess,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            JoinStatus.PENDING -> {
                                Surface(
                                    color = PosAmberWarning.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.HourglassTop,
                                            contentDescription = null,
                                            tint = PosAmberWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "قيد الانتظار",
                                            fontSize = 11.sp,
                                            color = PosAmberWarning,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            else -> {
                                Button(
                                    onClick = { onRequestJoin(network) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("btn_request_join_header")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Storefront,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "طلب انضمام",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Stats cards overview at top (Only for approved/joined networks)
            if (network.status == JoinStatus.APPROVED) {
                NetworkStatsOverview(
                    availableBalance = network.currentBalance,
                    financialCeiling = network.financialCeiling,
                    soldCardsCount = totalSoldCards,
                    soldTotalAmount = totalSoldAmount,
                    consumedDebt = consumedDebt
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "فئات كروت الإنترنت المتاحة للبيع الفوري",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (packages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 140.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(packages, key = { it.id }) { pkg ->
                        val qty = selectedQuantities[pkg.id] ?: 0
                        PackageGridCard(
                            packageItem = pkg,
                            quantity = qty,
                            onIncrement = { onQuantityChange(pkg.id, qty + 1) },
                            onDecrement = { onQuantityChange(pkg.id, (qty - 1).coerceAtLeast(0)) },
                            onCardClick = {
                                if (qty == 0) {
                                    onQuantityChange(pkg.id, 1)
                                } else {
                                    onQuantityChange(pkg.id, 0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog 1: Unjoined Network Top Up Alert
    if (showUnjoinedTopUpDialog) {
        AlertDialog(
            onDismissRequest = onDismissUnjoinedDialog,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "يرجى تغذية رصيد حسابك لإكمال الشراء",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "يرجى تغذية رصيد حسابك لإكمال الشراء. قم بتغذية رصيد حسابك الآن عبر المحافظ المالية لتتمكن من إصدار الكروت فوراً.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissUnjoinedDialog()
                        onNavigateToWallet()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_dialog_topup_now")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغذية الحساب الآن", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDismissUnjoinedDialog()
                        onRequestJoin(network)
                    }
                ) {
                    Text("طلب انضمام للشبكة")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Dialog 2: Financial Ceiling Exhausted Alert
    if (showCeilingExhaustedDialog) {
        AlertDialog(
            onDismissRequest = onDismissCeilingDialog,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "انتهى السقف المالي المتاح",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "انتهى رصيد السقف المالي المتاح من صاحب الشبكة (${network.ownerName}). يرجى تسديد صاحب الشبكة المبلغ الذي عليك وتجديد رصيد حسابك.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "أو يمكنك الاستمرار فوراً وإكمال عملية الشراء عبر رصيد محفظة CardBox الخاص بك!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissCeilingDialog()
                        onProcessPurchaseViaWallet()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_dialog_buy_via_cardbox")
                ) {
                    Text("إكمال عملية الشراء عبر كارد بوكس", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        onDismissCeilingDialog()
                        onNavigateToWallet()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تغذية المحفظة")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Dialog 3: Insufficient Wallet Balance Alert
    if (showInsufficientWalletDialog) {
        AlertDialog(
            onDismissRequest = onDismissInsufficientWalletDialog,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "لا يوجد رصيد كافٍ في محفظتك",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "لا يوجد رصيد كافٍ في محفظة كارد بوكس لإكمال العملية. قم بتغذية رصيد حسابك الآن عبر المحافظ المالية (جيب، كريمي، ون كاش، جوالي...).",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissInsufficientWalletDialog()
                        onNavigateToWallet()
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_dialog_insufficient_topup")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تغذية الحساب الآن", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissInsufficientWalletDialog) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Modal 4: Purchase Checkout Sheet (Appears over bottom nav bar)
    if (totalQuantity > 0) {
        ModalBottomSheet(
            onDismissRequest = onClearAll,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null
        ) {
            CheckoutBottomSheet(
                totalQuantity = totalQuantity,
                totalPrice = totalPrice,
                availableBalance = network.currentBalance,
                customerPhone = customerPhone,
                isPurchasing = isPurchasing,
                onCustomerPhoneChange = onCustomerPhoneChange,
                onClearAll = onClearAll,
                onContinuePurchase = onProcessPurchase
            )
        }
    }
}


@Composable
fun PackageGridCard(
    packageItem: VoucherPackage,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onCardClick: () -> Unit
) {
    val isSelected = quantity > 0

    val cardBackground = Color(0xFF131826)
    val borderColor = if (isSelected) Color(0xFFA855F7) else Color(0xFF222B40)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = cardBackground,
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            borderColor
        ),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onCardClick() }
            .testTag("package_card_${packageItem.id}")
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Badges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF059669),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "متوفر",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                if (packageItem.isPopular) {
                    Surface(
                        color = Color(0xFF7C2D12).copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "الأكثر طلباً",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Center Wifi Icon
            Surface(
                shape = CircleShape,
                color = Color(0xFF581C87),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Package Title
            Text(
                text = packageItem.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Price Pill
            Surface(
                color = Color(0xFF4C1D95),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "${packageItem.price.toInt()} ${packageItem.currency}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Specs Table Rows
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الحجم:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = packageItem.dataQuota,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المدة:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = packageItem.duration,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الصلاحية:",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = packageItem.validity,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Quantity Counter Bar
            Surface(
                color = Color(0xFF1B2236),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF28324A))
                            .clickable(enabled = quantity > 0) { onDecrement() }
                            .testTag("btn_dec_${packageItem.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = "إنقاص",
                            tint = if (quantity > 0) Color.White else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "$quantity",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF9333EA))
                            .clickable { onIncrement() }
                            .testTag("btn_inc_${packageItem.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "زيادة",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutBottomSheet(
    totalQuantity: Int,
    totalPrice: Double,
    availableBalance: Double,
    customerPhone: String,
    isPurchasing: Boolean,
    onCustomerPhoneChange: (String) -> Unit,
    onClearAll: () -> Unit,
    onContinuePurchase: () -> Unit
) {
    val isBalanceSufficient = totalPrice <= availableBalance

    Surface(
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Drag handle pill and close icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(28.dp))

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline)
                )

                // Quick Close X Button
                IconButton(
                    onClick = onClearAll,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("btn_close_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "إلغاء الاختيار",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Summary Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "ملخص الطلب ($totalQuantity)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Clear All Button
                    Surface(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(8.dp),
                        color = PosRedError.copy(alpha = 0.1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PosRedError.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("btn_clear_selection")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = PosRedError,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إلغاء الاختيار",
                                fontSize = 11.sp,
                                color = PosRedError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "الإجمالي: ${totalPrice.toInt()} ريال",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isBalanceSufficient) MaterialTheme.colorScheme.primary else PosRedError
                )
            }

            // Customer Phone Number (Optional)
            OutlinedTextField(
                value = customerPhone,
                onValueChange = onCustomerPhoneChange,
                label = { Text("رقم هاتف العميل (اختياري للإرسال عبر SMS)") },
                placeholder = { Text("مثال: 771234567") },
                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_customer_phone"),
                shape = RoundedCornerShape(14.dp)
            )

            if (!isBalanceSufficient) {
                Text(
                    text = "تنبيه: عدم كفاية السقف المالي المتاح (${availableBalance.toInt()} ريال)",
                    color = PosRedError,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onContinuePurchase,
                enabled = !isPurchasing && isBalanceSufficient && totalQuantity > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_continue_checkout"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = PosOutline
                )
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.ShoppingCartCheckout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "متابعة الشراء واستخراج الكرت",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkStatsOverview(
    availableBalance: Double,
    financialCeiling: Double,
    soldCardsCount: Int,
    soldTotalAmount: Double,
    consumedDebt: Double,
    currency: String = "ريال"
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: الرصيد المتاح
            StatMiniCard(
                title = "الرصيد المتاح",
                value = "${availableBalance.toInt()} $currency",
                subtitle = "جاهز للبيع الأن",
                icon = Icons.Outlined.AccountBalanceWallet,
                iconTint = PosEmeraldSuccess,
                bgColor = PosEmeraldSuccess.copy(alpha = 0.12f),
                borderColor = PosEmeraldSuccess.copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )

            // Card 2: السقف المالي / الرصيد الكلي
            StatMiniCard(
                title = "السقف المالي",
                value = "${financialCeiling.toInt()} $currency",
                subtitle = "حد الائتمان الكلي",
                icon = Icons.Outlined.PieChart,
                iconTint = MaterialTheme.colorScheme.primary,
                bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: المبيعات (كم بعت كروت)
            StatMiniCard(
                title = "الكروت المباعة",
                value = if (soldCardsCount > 0) "$soldCardsCount كرت" else "0 كرت",
                subtitle = if (soldTotalAmount > 0) "إجمالي: ${soldTotalAmount.toInt()} $currency" else "لا توجد مبيعات بعد",
                icon = Icons.Outlined.Sell,
                iconTint = Color(0xFFA855F7),
                bgColor = Color(0xFFA855F7).copy(alpha = 0.12f),
                borderColor = Color(0xFFA855F7).copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )

            // Card 4: الرصيد الآجل الذي فوقي لكي أحاسبه
            StatMiniCard(
                title = "المستحق للمالك (آجل)",
                value = "${consumedDebt.toInt()} $currency",
                subtitle = "واجب المحاسبة والتعزيز",
                icon = Icons.Outlined.ReceiptLong,
                iconTint = PosAmberWarning,
                bgColor = PosAmberWarning.copy(alpha = 0.12f),
                borderColor = PosAmberWarning.copy(alpha = 0.3f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatMiniCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = CircleShape,
                    color = bgColor,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
