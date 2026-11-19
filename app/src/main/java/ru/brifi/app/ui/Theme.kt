package ru.brifi.app.ui

import android.content.Context
import android.graphics.Typeface
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.brifi.app.R

object BrifiColors {
    val BgBlack = Color(0xFF000000)
    val SheetDark = Color(0xFF1F1F24)
    val SheetDarkBorder = Color(0xFF2E2E36)
    val CardDark = Color(0xFF141418)
    val CardDarkElevated = Color(0xFF1A1A20)

    // Новая палитра Брифи v2 (космический индиго + фиолетовый + бирюзовый)
    val AccentDeepBlue = Color(0xFF1518B8)
    val AccentIndigo = Color(0xFF5D5AF6)
    val AccentIndigoSoft = Color(0xFF7B78FF)
    val AccentCyan = Color(0xFF58B1D8)
    val AccentCyanLight = Color(0xFF83D4F2)
    val AccentSoftBlue = Color(0xFF9FB8FF)

    val DeckBackGray = Color(0xFF9A9AA2)
    val DeckFrontLight = Color(0xFFEBEBF0)
    val ScrollRail = Color(0xFFD8D8E0)
    val ScrollRailIcon = Color(0xFF82828E)
    val StatsCardLight = Color(0xFFF2F2F6)

    // Плитки Статистики по макету Брифи v2.pdf
    val KpiTile1 = Color(0xFF1518B8)
    val KpiTile2 = Color(0xFF5D5AF6)
    val KpiTile3 = Color(0xFF5AAED6)
    val KpiTile4 = Color(0xFFF0F0F5)

    val ProfileCardLower = Color(0xFF16161B)
    val ProfileTileDark = Color(0xFF09090C)
    val ProfileDashedBorder = Color(0xFF3A3A46)

    val NavPillBg = Color(0xEB151519)
    val NavActive = Color(0xFFFFFFFF)
    val NavInactive = Color(0xFF7C7C88)
    val SegmentDark = Color(0xFF151519)

    val TextWhite = Color(0xFFFFFFFF)
    val TextBlack = Color(0xFF111114)
    val TextMutedLight = Color(0xFFC8C8D2)
    val TextMutedDark = Color(0xFF5A5A66)

    // Подложки срочности дедлайна
    val UrgencyRed = Color(0xFFE53935)
    val UrgencyYellow = Color(0xFFFFC107)
    val UrgencyGreen = Color(0xFF2E9E5B)
}

fun urgencyBadgeColors(urgency: String): Pair<Color, Color> {
    return when (urgency.trim()) {
        "!!!" -> BrifiColors.UrgencyRed to BrifiColors.TextWhite
        "!!" -> BrifiColors.UrgencyYellow to BrifiColors.TextBlack
        else -> BrifiColors.UrgencyGreen to BrifiColors.TextWhite
    }
}

val GoogleSansFamily: FontFamily = runCatching {
    FontFamily(Typeface.create("google-sans", Typeface.NORMAL))
}.getOrDefault(FontFamily.SansSerif)

val A24FontFamily: FontFamily = FontFamily(
    Font(R.font.a24_regular)
)

private val BaseTypography = Typography()

val BrifiTypography = Typography(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = GoogleSansFamily),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = GoogleSansFamily),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = GoogleSansFamily),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = GoogleSansFamily),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = GoogleSansFamily),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = GoogleSansFamily),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = GoogleSansFamily),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = GoogleSansFamily),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = GoogleSansFamily),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = GoogleSansFamily),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = GoogleSansFamily),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = GoogleSansFamily),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = GoogleSansFamily),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = GoogleSansFamily),
    labelSmall = BaseTypography.labelSmall.copy(fontFamily = GoogleSansFamily)
)

@Composable
fun BrifiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BrifiColors.AccentIndigo,
            secondary = BrifiColors.AccentCyan,
            background = BrifiColors.BgBlack,
            surface = BrifiColors.SheetDark,
            onPrimary = BrifiColors.TextWhite,
            onBackground = BrifiColors.TextWhite,
            onSurface = BrifiColors.TextWhite
        ),
        typography = BrifiTypography,
        content = content
    )
}

fun triggerBrifiHaptic(context: Context) {
    runCatching {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vm?.defaultVibrator ?: return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        }
    }
}

fun triggerBrifiConfirmHaptic(context: Context) {
    runCatching {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vm?.defaultVibrator ?: return
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        }
    }
}

/**
 * Плавный, медленный «дышащий» градиент (аврора) — работает на GPU в 60fps без рывков.
 */
@Composable
fun BreathingAuroraCanvas(
    primaryGlow: Color,
    secondaryGlow: Color,
    showBottomGlow: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BreathingAurora")
    val breathA by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathA"
    )
    val breathB by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathB"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Верхнее левое/центральное свечение
        val topLeftCenter = Offset(
            x = w * (0.24f + breathA * 0.16f),
            y = h * (0.04f + breathB * 0.06f)
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryGlow.copy(alpha = 0.68f + breathA * 0.18f),
                    primaryGlow.copy(alpha = 0.24f + breathB * 0.10f),
                    Color.Transparent
                ),
                center = topLeftCenter,
                radius = w * (0.78f + breathA * 0.14f)
            )
        )

        // Верхнее правое свечение
        val topRightCenter = Offset(
            x = w * (0.82f - breathB * 0.14f),
            y = h * (0.08f + breathA * 0.05f)
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondaryGlow.copy(alpha = 0.56f + breathB * 0.18f),
                    secondaryGlow.copy(alpha = 0.18f + breathA * 0.08f),
                    Color.Transparent
                ),
                center = topRightCenter,
                radius = w * (0.72f + breathB * 0.14f)
            )
        )

        if (showBottomGlow) {
            val bottomCenter = Offset(
                x = w * (0.48f + (breathA - 0.5f) * 0.18f),
                y = h * (0.94f - breathB * 0.04f)
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.62f + breathB * 0.16f),
                        secondaryGlow.copy(alpha = 0.26f + breathA * 0.12f),
                        Color.Transparent
                    ),
                    center = bottomCenter,
                    radius = w * (0.88f + breathA * 0.14f)
                )
            )
        }
    }
}

@Composable
fun BrifiLogoImage(
    height: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.brifi_logo_v2),
        contentDescription = "Брифи",
        contentScale = ContentScale.FillHeight,
        modifier = modifier.height(height)
    )
}

/**
 * Верхняя «дышащая» градиентная шапка с логотипом БРИФИ
 */
@Composable
fun BrifiBreathingHeader(
    centered: Boolean,
    primaryGlow: Color = BrifiColors.AccentIndigo,
    secondaryGlow: Color = BrifiColors.AccentCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .background(BrifiColors.BgBlack)
    ) {
        BreathingAuroraCanvas(
            primaryGlow = primaryGlow,
            secondaryGlow = secondaryGlow,
            showBottomGlow = false
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart
        ) {
            BrifiLogoImage(height = 24.dp)
        }
    }
}

@Composable
fun DashedBorderBox(
    cornerRadius: Dp = 24.dp,
    borderColor: Color = BrifiColors.ProfileDashedBorder,
    backgroundColor: Color = BrifiColors.BgBlack,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .drawBehind {
                val strokePx = 1.4.dp.toPx()
                val radiusPx = cornerRadius.toPx()
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(strokePx / 2f, strokePx / 2f),
                    size = Size(size.width - strokePx, size.height - strokePx),
                    cornerRadius = CornerRadius(radiusPx, radiusPx),
                    style = Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 9f), 0f)
                    )
                )
            },
        content = content
    )
}

@Composable
fun NavHomeIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val housePath = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.44f)
            lineTo(w * 0.80f, h * 0.44f)
            lineTo(w * 0.80f, h * 0.86f)
            lineTo(w * 0.20f, h * 0.86f)
            lineTo(w * 0.20f, h * 0.44f)
            lineTo(w * 0.12f, h * 0.44f)
            close()
        }
        drawPath(housePath, color = color)
        drawCircle(
            color = BrifiColors.NavPillBg,
            radius = w * 0.14f,
            center = Offset(w * 0.50f, h * 0.62f)
        )
    }
}

@Composable
fun NavStatsIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val barW = w * 0.18f
        val r = CornerRadius(barW / 2f, barW / 2f)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.52f),
            size = Size(barW, h * 0.34f),
            cornerRadius = r
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.41f, h * 0.22f),
            size = Size(barW, h * 0.64f),
            cornerRadius = r
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.68f, h * 0.38f),
            size = Size(barW, h * 0.48f),
            cornerRadius = r
        )
    }
}

@Composable
fun NavProfileIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = color,
            radius = w * 0.19f,
            center = Offset(w * 0.50f, h * 0.31f)
        )
        val bodyPath = Path().apply {
            moveTo(w * 0.16f, h * 0.86f)
            cubicTo(
                w * 0.16f, h * 0.54f,
                w * 0.84f, h * 0.54f,
                w * 0.84f, h * 0.86f
            )
            close()
        }
        drawPath(bodyPath, color = color)
    }
}

@Composable
fun PlusIcon(color: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.13f
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.18f),
            end = Offset(w * 0.5f, h * 0.82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.18f, h * 0.5f),
            end = Offset(w * 0.82f, h * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}
