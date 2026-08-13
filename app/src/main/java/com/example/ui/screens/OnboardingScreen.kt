package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PosAmberWarning
import com.example.ui.theme.PosEmeraldSuccess

data class OnboardingBadge(
    val label: String,
    val icon: ImageVector
)

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val badges: List<OnboardingBadge>,
    val highlightColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPageData(
            title = "طباعة وتوزيع كروت الشبكات",
            subtitle = "إدارة باقات شبكات الـ Wi-Fi والطباعة الحرارية السريعة",
            description = "أداة متكاملة لموزعي نقاط البيع لاستخراج وطباعة كروت الإنترنت لشبكات المايكروتك المحلية عبر البلوتوث بضغطة زر وبدون تعقيد.",
            icon = Icons.Outlined.Print,
            badges = listOf(
                OnboardingBadge("طباعة فورية", Icons.Outlined.FlashOn),
                OnboardingBadge("المايكروتك", Icons.Outlined.Wifi),
                OnboardingBadge("فواتير معتمدة", Icons.Outlined.Receipt)
            ),
            highlightColor = MaterialTheme.colorScheme.primary
        ),
        OnboardingPageData(
            title = "تغذية المحفظة وتسديد الحسابات",
            subtitle = "شحن الرصيد وسداد مستحقات الشبكات بأعلى أمان",
            description = "إمكانية إيداع وتغذية محفظتك الإلكترونية وسداد كروت الشبكات عبر البنوك والمحافظ اليمنية (جيب، الكريمي، ون كاش، جوالي) بسرعة فائقة.",
            icon = Icons.Outlined.AccountBalanceWallet,
            badges = listOf(
                OnboardingBadge("محفظة كارد بوكس", Icons.Outlined.CreditCard),
                OnboardingBadge("التحويل المباشر", Icons.Outlined.SyncAlt),
                OnboardingBadge("حماية مشفرة", Icons.Outlined.Shield)
            ),
            highlightColor = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPageData(
            title = "إدارة المبيعات والإشعارات الحية",
            subtitle = "تقارير يومية متكاملة وتنبيهات مستمرة لعملياتك",
            description = "متابعة دقيقة لكل فواتير المبيعات، إشعارات حية بقبول طلبات الانضمام للشبكات وتعبئة الرصيد، وسجل أرشيفي كامل بجميع المبيعات.",
            icon = Icons.Outlined.Analytics,
            badges = listOf(
                OnboardingBadge("تقارير تفصيلية", Icons.Outlined.BarChart),
                OnboardingBadge("تنبيهات فورية", Icons.Outlined.NotificationsActive),
                OnboardingBadge("أرشيف دائم", Icons.Outlined.Folder)
            ),
            highlightColor = PosEmeraldSuccess
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    if (currentPage < pages.size - 1) {
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.testTag("btn_onboarding_skip")
                        ) {
                            Text(
                                text = "تخطي",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Animated Slide Content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally(animationSpec = tween(400)) { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally(animationSpec = tween(400)) { width -> width } + fadeOut()
                    }
                },
                modifier = Modifier.weight(1f),
                label = "onboardingSlide"
            ) { pageIdx ->
                val page = pages[pageIdx]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Illustration Card Banner
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        shape = RoundedCornerShape(28.dp),
                        color = page.highlightColor.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            page.highlightColor.copy(alpha = 0.25f)
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Background decorative glow
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(CircleShape)
                                    .background(page.highlightColor.copy(alpha = 0.15f))
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.size(80.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = page.icon,
                                            contentDescription = null,
                                            tint = page.highlightColor,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Logo Brand Label
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_app_logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "CardBox POS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Title
                    Text(
                        text = page.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = page.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = page.highlightColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description Body
                    Text(
                        text = page.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Badges List
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        page.badges.forEach { badge ->
                            Surface(
                                color = page.highlightColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = badge.icon,
                                        contentDescription = null,
                                        tint = page.highlightColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = badge.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = page.highlightColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation & Dots Indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { index ->
                        val isSelected = index == currentPage
                        val targetWidth: Dp = if (isSelected) 28.dp else 8.dp
                        val widthAnim by animateDpAsState(
                            targetValue = targetWidth,
                            animationSpec = tween(300),
                            label = "dotWidth"
                        )
                        val color = if (isSelected) pages[currentPage].highlightColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(widthAnim)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Next / Previous / Finish Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    if (currentPage > 0) {
                        OutlinedButton(
                            onClick = { currentPage-- },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("btn_onboarding_prev")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                contentDescription = "Previous",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السابق", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // Next / Finish Button
                    Button(
                        onClick = {
                            if (currentPage < pages.size - 1) {
                                currentPage++
                            } else {
                                onFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pages[currentPage].highlightColor
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_onboarding_next")
                    ) {
                        Text(
                            text = if (currentPage == pages.size - 1) "متابعة وتفعيل الاذونات" else "التالي",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (currentPage == pages.size - 1) Icons.Outlined.CheckCircle else Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
