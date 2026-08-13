package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    savedPhone: String = "",
    savedPassword: String = "",
    initialRememberMe: Boolean = true,
    onToggleThemeMode: () -> Unit = {},
    onRegister: (String, String, String, String, Boolean, (Boolean, String) -> Unit) -> Unit,
    onLogin: (String, String, Boolean, (Boolean, String) -> Unit) -> Unit,
    onResetPassword: (String, String, (Boolean, String) -> Unit) -> Unit = { _, _, cb -> cb(true, "") }
) {
    val context = LocalContext.current
    var isRegisterMode by remember { mutableStateOf(false) }

    // Form fields
    var storeName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf(savedPhone) }
    var location by remember { mutableStateOf("") }
    var password by remember { mutableStateOf(savedPassword) }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Remember Me
    var rememberMe by remember { mutableStateOf(initialRememberMe) }

    LaunchedEffect(savedPhone, savedPassword) {
        if (savedPhone.isNotBlank()) phone = savedPhone
        if (savedPassword.isNotBlank()) password = savedPassword
    }

    // OTP Modal states
    var showOtpModal by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("") }
    var isOtpVerifying by remember { mutableStateOf(false) }

    // Forgot password states
    var showForgotPasswordModal by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = onToggleThemeMode) {
                    val icon = when (themeMode) {
                        ThemeMode.LIGHT -> Icons.Outlined.LightMode
                        ThemeMode.DARK -> Icons.Outlined.DarkMode
                        ThemeMode.SYSTEM -> Icons.Outlined.SettingsBrightness
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "تبديل المظهر",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Logo Banner
                Surface(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = "Card Box POS Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Card Box POS",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isRegisterMode) "قم بإدخال بيانات متجرك للانضمام كنقطة بيع معتمدة في Card Box POS" else "نظام Card Box POS لبيع وتوزيع كروت الإنترنت والطباعة الحرارية",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Tab Switcher Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Button(
                            onClick = {
                                isRegisterMode = false
                                errorMessage = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_login"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (!isRegisterMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = null
                        ) {
                            Text("تسجيل الدخول", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                isRegisterMode = true
                                errorMessage = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_register"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRegisterMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (isRegisterMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = null
                        ) {
                            Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Main Form Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isRegisterMode) {
                            // Store Name
                            OutlinedTextField(
                                value = storeName,
                                onValueChange = { storeName = it },
                                label = { Text("اسم البقالة / المتجر") },
                                placeholder = { Text("مثال: سوبرماركت البركة") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Store, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_store_name"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            // Location
                            OutlinedTextField(
                                value = location,
                                onValueChange = { location = it },
                                label = { Text("الموقع / العنوان التفصيلي") },
                                placeholder = { Text("مثال: صنعاء - شارع الحصبة") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_location"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Phone Number
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("رقم الجوال") },
                            placeholder = { Text("مثال: 770000000") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Phone, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_phone"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Password
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("كلمة المرور") },
                            placeholder = { Text("******") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = "عرض/إخفاء كلمة المرور"
                                    )
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_password"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Confirm Password (Only in Register Mode)
                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("تأكيد كلمة المرور") },
                                placeholder = { Text("أعد كتابة كلمة المرور") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.LockReset, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isConfirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                            contentDescription = "عرض/إخفاء"
                                        )
                                    }
                                },
                                visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_confirm_password"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Remember Me & Forgot Password in Login Mode
                        if (!isRegisterMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                                ) {
                                    Checkbox(
                                        checked = rememberMe,
                                        onCheckedChange = { rememberMe = it },
                                        modifier = Modifier.testTag("checkbox_remember_me")
                                    )
                                    Text(
                                        text = "تذكرني للدخول السريع",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                TextButton(
                                    onClick = { showForgotPasswordModal = true },
                                    modifier = Modifier.testTag("btn_forgot_password")
                                ) {
                                    Text(
                                        text = "نسيت كلمة المرور؟",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = PosRedError,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                errorMessage = null
                                if (isRegisterMode) {
                                    if (storeName.isBlank() || phone.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                        errorMessage = "يرجى تعبئة جميع الحقول المطلوبة"
                                        return@Button
                                    }
                                    if (password != confirmPassword) {
                                        errorMessage = "كلمتا المرور غير متطابقتين، يرجى التأكد وإعادة الكتابة"
                                        return@Button
                                    }
                                    // Open OTP verification step for phone verification
                                    otpInput = ""
                                    showOtpModal = true
                                } else {
                                    if (phone.isBlank() || password.isBlank()) {
                                        errorMessage = "يرجى إدخال رقم الجوال وكلمة المرور"
                                        return@Button
                                    }
                                    isLoading = true
                                    onLogin(phone, password, rememberMe) { success, msg ->
                                        isLoading = false
                                        if (!success) errorMessage = msg
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_auth_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (isRegisterMode) "متابعة والتحقق برمز OTP" else "تسجيل الدخول",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Demo account help hint
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "حساب تجريبي جاهز للتجربة السريعة:\nرقم الجوال: 770000000 | كلمة المرور: 123456",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Modal 1: Phone Verification OTP Dialog
    if (showOtpModal) {
        AlertDialog(
            onDismissRequest = { if (!isOtpVerifying) showOtpModal = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = "رمز تحقق الجوال (OTP)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تم إرسال رمز التحقق المكون من 4 أرقام عبر رسالة SMS إلى رقمك ($phone).",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PosTealSecondary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "رمز التحقق الافتراضي للاختبار: 1234",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PosTealSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 4) otpInput = it },
                        label = { Text("رمز التحقق OTP") },
                        placeholder = { Text("1234") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                Toast.makeText(context, "تم إعادة إرسال رمز OTP إلى $phone", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("إعادة إرسال الرمز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "ينتهي خلال 01:59",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (otpInput.trim() != "1234" && otpInput.trim().length != 4) {
                            Toast.makeText(context, "رمز OTP غير صحيح. جرب 1234", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isOtpVerifying = true
                        onRegister(storeName, phone, location, password, rememberMe) { success, msg ->
                            isOtpVerifying = false
                            if (success) {
                                showOtpModal = false
                                Toast.makeText(context, "تم التحقق وإنشاء الحساب بنجاح!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isOtpVerifying && otpInput.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isOtpVerifying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Text("تأكيد وإنشاء الحساب", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpModal = false }) {
                    Text("تعديل الرقم")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Modal 2: Forgot Password Reset Dialog
    if (showForgotPasswordModal) {
        var resetPhone by remember { mutableStateOf(phone) }
        var resetOtp by remember { mutableStateOf("") }
        var newPass by remember { mutableStateOf("") }
        var isResetting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showForgotPasswordModal = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.LockReset,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("استعادة كلمة المرور", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "أدخل رقم الجوال المسجل ورمز التحقق OTP لاختيار كلمة مرور جديدة:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = resetPhone,
                        onValueChange = { resetPhone = it },
                        label = { Text("رقم الجوال") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = resetOtp,
                        onValueChange = { resetOtp = it },
                        label = { Text("رمز التحقق (1234)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("كلمة المرور الجديدة") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetPhone.isBlank() || resetOtp.isBlank() || newPass.isBlank()) {
                            Toast.makeText(context, "يرجى ملء جميع الحقول", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (resetOtp.trim() != "1234" && resetOtp.trim().length != 4) {
                            Toast.makeText(context, "رمز OTP غير صحيح. جرب 1234", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isResetting = true
                        onResetPassword(resetPhone, newPass) { success, msg ->
                            isResetting = false
                            if (success) {
                                showForgotPasswordModal = false
                                Toast.makeText(context, "تم تغيير كلمة المرور بنجاح! يمكنك الدخول الآن", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ كلمة المرور الجديدة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordModal = false }) {
                    Text("إلغاء")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }
}
