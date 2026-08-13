package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.PosUser
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class PendingAccountAction {
    UPDATE_PROFILE,
    CHANGE_PASSWORD,
    DELETE_ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsScreen(
    user: PosUser?,
    walletBalance: Double = 0.0,
    onUpdateProfile: (String, String, (Boolean, String) -> Unit) -> Unit,
    onChangePassword: (String, String, (Boolean, String) -> Unit) -> Unit,
    onDeleteAccount: ((Boolean, String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Form states
    var storeName by remember(user) { mutableStateOf(user?.storeName ?: "سوبرماركت التوفير والبركة") }
    var location by remember(user) { mutableStateOf(user?.location ?: "المركز الرئيسي - شارع الجامعة") }
    val phone = user?.phone ?: "770000000"

    // Password states
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isOldPassVisible by remember { mutableStateOf(false) }
    var isNewPassVisible by remember { mutableStateOf(false) }

    // OTP Modal states
    var pendingAction by remember { mutableStateOf<PendingAccountAction?>(null) }
    var showOtpModal by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var isOtpVerifying by remember { mutableStateOf(false) }
    var otpErrorMsg by remember { mutableStateOf<String?>(null) }

    // Delete confirmation dialog
    var showDeleteWarningDialog by remember { mutableStateOf(false) }

    fun requestOtpForAction(action: PendingAccountAction) {
        pendingAction = action
        otpCode = ""
        otpErrorMsg = null
        showOtpModal = true
        Toast.makeText(context, "تم إرسال رمز التأكيد (OTP) إلى الرقم $phone", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "معلومات حسابي ونقطة البيع",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
        ) {
            // Card 1: Account Header Summary
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Surface(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .border(
                                        2.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        CircleShape
                                    ),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_app_logo),
                                    contentDescription = "Store Avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(PosEmeraldSuccess)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = storeName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "مالك نقطة البيع: العميل المسجل",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = phone,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                color = PosEmeraldSuccess.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = PosEmeraldSuccess,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "حساب موثق ومفعل",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PosEmeraldSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Edit Store & Profile Information
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تحديث بيانات المتجر والموقع",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("اسم البقالة / نقطة البيع") },
                            leadingIcon = { Icon(Icons.Outlined.Store, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text("الموقع والعنوان التفصيلي") },
                            leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = {},
                            enabled = false,
                            label = { Text("رقم الجوال (موثق بالنظام)") },
                            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null) },
                            trailingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (storeName.isBlank() || location.isBlank()) {
                                    Toast.makeText(context, "يرجى ملء جميع الحقول المطلوب تعديلها", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                requestOtpForAction(PendingAccountAction.UPDATE_PROFILE)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ وتحديث معلومات الحساب", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 3: Change Password Section
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.LockReset,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تغيير كلمة المرور الأمني",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text("كلمة المرور الحالية") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isOldPassVisible = !isOldPassVisible }) {
                                    Icon(
                                        imageVector = if (isOldPassVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (isOldPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("كلمة المرور الجديدة") },
                            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { isNewPassVisible = !isNewPassVisible }) {
                                    Icon(
                                        imageVector = if (isNewPassVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (isNewPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("تأكيد كلمة المرور الجديدة") },
                            leadingIcon = { Icon(Icons.Outlined.Key, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (oldPassword.isBlank() || newPassword.isBlank()) {
                                    Toast.makeText(context, "يرجى كتابة كلمة المرور الحالية والجديدة", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    Toast.makeText(context, "كلمة المرور الجديدة غير متطابقة مع التأكيد", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                requestOtpForAction(PendingAccountAction.CHANGE_PASSWORD)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تحديث كلمة المرور بحماية OTP", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 4: Danger Zone - Delete Account Button
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = PosRedError.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PosRedError.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = PosRedError,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "منطقة حذف الحساب النهائي",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PosRedError
                            )
                        }

                        Text(
                            text = "حذف حساب نقطة البيع نهائياً سيؤدي لإلغاء الارتباط بجميع شبكات المايكروتك المسجلة ومحو سجل السندات بصفة دائمة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = { showDeleteWarningDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PosRedError),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("btn_delete_account_in_details")
                        ) {
                            Icon(Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حذف حساب نقطة البيع نهائياً", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS & OTP MODAL ---

    // Delete Warning Confirmation Dialog
    if (showDeleteWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ReportProblem, contentDescription = null, tint = PosRedError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("هل أنت أصل متأكد من حذف الحساب؟", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = "سيصلك رمز تأكيد OTP إلى جوالك $phone لإتمام عملية الحذف النهائية.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteWarningDialog = false
                        requestOtpForAction(PendingAccountAction.DELETE_ACCOUNT)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PosRedError)
                ) {
                    Text("نعم، أرسل رمز OTP للحذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteWarningDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // OTP Modal Dialog
    if (showOtpModal && pendingAction != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isOtpVerifying) showOtpModal = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "رمز التأكيد الأمني (OTP)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "تم إرسال رمز أمان إلى رقم جوالك $phone لتأكيد عملية (${
                            when (pendingAction) {
                                PendingAccountAction.UPDATE_PROFILE -> "تحديث بيانات الحساب"
                                PendingAccountAction.CHANGE_PASSWORD -> "تغيير كلمة المرور"
                                PendingAccountAction.DELETE_ACCOUNT -> "حذف الحساب نهائياً"
                                null -> ""
                            }
                        }):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label = { Text("أدخل رمز OTP (أمثلة: 1234)") },
                        leadingIcon = { Icon(Icons.Outlined.VpnKey, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    otpErrorMsg?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosRedError
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (otpCode.length < 4) {
                            otpErrorMsg = "يرجى كتابة رمز OTP المكون من 4 أرقام"
                            return@Button
                        }
                        isOtpVerifying = true
                        otpErrorMsg = null

                        when (pendingAction) {
                            PendingAccountAction.UPDATE_PROFILE -> {
                                onUpdateProfile(storeName, location) { success, msg ->
                                    isOtpVerifying = false
                                    if (success) {
                                        showOtpModal = false
                                        Toast.makeText(context, "تم تحديث بيانات الحساب بنجاح", Toast.LENGTH_SHORT).show()
                                    } else {
                                        otpErrorMsg = msg
                                    }
                                }
                            }
                            PendingAccountAction.CHANGE_PASSWORD -> {
                                onChangePassword(oldPassword, newPassword) { success, msg ->
                                    isOtpVerifying = false
                                    if (success) {
                                        showOtpModal = false
                                        oldPassword = ""
                                        newPassword = ""
                                        confirmPassword = ""
                                        Toast.makeText(context, "تم تغيير كلمة المرور بنجاح", Toast.LENGTH_SHORT).show()
                                    } else {
                                        otpErrorMsg = msg
                                    }
                                }
                            }
                            PendingAccountAction.DELETE_ACCOUNT -> {
                                onDeleteAccount { success, msg ->
                                    isOtpVerifying = false
                                    if (success) {
                                        showOtpModal = false
                                        Toast.makeText(context, "تم حذف الحساب بنجاح", Toast.LENGTH_SHORT).show()
                                    } else {
                                        otpErrorMsg = msg
                                    }
                                }
                            }
                            null -> {}
                        }
                    },
                    enabled = !isOtpVerifying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (pendingAction == PendingAccountAction.DELETE_ACCOUNT) PosRedError else MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isOtpVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("تأكيد العملية الآن")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showOtpModal = false },
                    enabled = !isOtpVerifying
                ) {
                    Text("إلغاء")
                }
            }
        )
    }
}
