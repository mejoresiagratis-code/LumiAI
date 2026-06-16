package com.lumiai.flashlight.feature.onboarding

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lumiai.flashlight.R
import com.lumiai.flashlight.ui.theme.LumiColor

private data class OnboardingPage(
    val symbol: String,
    val color: Color,
    val title: String,
    val desc: String,
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onMarkSeen: (() -> Unit)? = null,
    onEnableNotifications: () -> Unit = {},
) {
    // Built inside the composable so all copy is localized (values / values-es).
    val pages = listOf(
        OnboardingPage("⚡", LumiColor.Amber400,  stringResource(R.string.onb_p1_title), stringResource(R.string.onb_p1_desc)),
        OnboardingPage("◎", LumiColor.Amber500,  stringResource(R.string.onb_p2_title), stringResource(R.string.onb_p2_desc)),
        OnboardingPage("✦", LumiColor.Purple400, stringResource(R.string.onb_p3_title), stringResource(R.string.onb_p3_desc)),
        OnboardingPage("🔔", LumiColor.Amber500, stringResource(R.string.onb_p4_title), stringResource(R.string.onb_p4_desc)),
    )

    var page by remember { mutableIntStateOf(0) }
    val current = pages[page]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LumiColor.Navy950),
    ) {
        // Background glow
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            current.color.copy(alpha = 0.08f),
                            Color.Transparent,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Skip
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    stringResource(R.string.onb_skip),
                    fontSize = 14.sp,
                    color = LumiColor.Gray500,
                    modifier = Modifier.clickable(onClick = { onMarkSeen?.invoke(); onFinished() }).padding(8.dp),
                )
            }

            // Content
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                label = "page",
            ) { idx ->
                val p = pages[idx]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // Symbol
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(p.color.copy(alpha = 0.12f))
                            .clip(CircleShape),
                    ) {
                        Text(p.symbol, fontSize = 44.sp)
                    }
                    // Text
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            p.title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.W700,
                            color = LumiColor.White,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.3).sp,
                        )
                        Text(
                            p.desc,
                            fontSize = 15.sp,
                            color = LumiColor.Gray400,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            // Bottom nav
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.indices.forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == page) 20.dp else 6.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == page) LumiColor.Amber400
                                    else LumiColor.Navy600
                                ),
                        )
                    }
                }

                // Optional: enable notification flash (last page only). Non-coercive —
                // opens the system Notification-access screen; the user can skip it.
                if (page == pages.lastIndex) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(LumiColor.Navy700)
                            .clickable { onEnableNotifications() },
                    ) {
                        Text(
                            text = stringResource(R.string.onb_enable_notif),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.W600,
                            color = LumiColor.Amber400,
                        )
                    }
                }

                // CTA button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (page == pages.lastIndex)
                                Brush.horizontalGradient(listOf(LumiColor.Amber400, LumiColor.Amber600))
                            else
                                Brush.horizontalGradient(listOf(LumiColor.Navy700, LumiColor.Navy700))
                        )
                        .clickable {
                            if (page < pages.lastIndex) page++
                            else { onMarkSeen?.invoke(); onFinished() }
                        },
                ) {
                    Text(
                        text = if (page == pages.lastIndex) stringResource(R.string.onb_start) else stringResource(R.string.onb_next),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.W600,
                        color = if (page == pages.lastIndex) LumiColor.Navy900 else LumiColor.White,
                    )
                }
            }
        }
    }
}
