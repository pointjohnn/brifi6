package ru.brifi.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.brifi.app.R
import ru.brifi.app.data.BrifiRepository
import ru.brifi.app.data.SheetsParser
import ru.brifi.app.model.BrifiStory
import ru.brifi.app.model.BrifiTab
import ru.brifi.app.model.FinanceSummary
import ru.brifi.app.model.OrderTask
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

/**
 * Экран входа (Splash) с плавным «дышащим» градиентом — точно по Брифи v2.pdf
 */
@Composable
fun SplashScreen(lastUpdated: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrifiColors.BgBlack)
    ) {
        BreathingAuroraCanvas(
            primaryGlow = BrifiColors.AccentIndigo,
            secondaryGlow = BrifiColors.AccentCyan,
            showBottomGlow = true
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 42.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Разработано",
                color = Color(0xFFB4B4D0),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Image(
                painter = painterResource(id = R.drawable.point_logo),
                contentDescription = "ПОИНТ.",
                contentScale = ContentScale.FillHeight,
                modifier = Modifier.height(14.dp)
            )
        }

        BrifiLogoImage(
            height = 34.dp,
            modifier = Modifier.align(Alignment.Center)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Дата обновления данных",
                color = Color(0xFFC4C8E0),
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = lastUpdated,
                color = BrifiColors.TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Цветная подложка под датой дедлайна (красный / жёлтый / зелёный по срочности)
 */
@Composable
private fun UrgencyDeadlinePill(task: OrderTask) {
    val (bg, fg) = urgencyBadgeColors(task.urgency)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "📅 ${task.deadlineRaw.ifBlank { SheetsParser.formatDate(task.deadline) }}",
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Плавный интерактивный слайдер-кнопка («Готово ›››» / «Взять в работу ›››») с прогрессивной вибрацией
 */
@Composable
private fun BrifiSlideActionBar(
    thumbLabel: String,
    thumbColor: Color,
    thumbTextColor: Color,
    rightActionLabel: String,
    rightActionUnderline: Boolean,
    enabled: Boolean,
    onSlideComplete: () -> Unit,
    onRightActionClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dragX = remember { Animatable(0f) }

    var trackWidthPx by remember { mutableIntStateOf(0) }
    var thumbWidthPx by remember { mutableIntStateOf(0) }
    var ticked35 by remember { mutableStateOf(false) }
    var ticked70 by remember { mutableStateOf(false) }

    val maxDragPx = (trackWidthPx - thumbWidthPx - 14).coerceAtLeast(1).toFloat()
    val progress = (dragX.value / maxDragPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BrifiColors.TextWhite)
            .onSizeChanged { trackWidthPx = it.width }
            .padding(horizontal = 5.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Правая кнопка / статус
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .graphicsLayer { alpha = (1f - progress * 1.5f).coerceIn(0f, 1f) }
                .clickable(enabled = enabled && progress < 0.08f) { onRightActionClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = rightActionLabel,
                color = BrifiColors.TextBlack,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (rightActionUnderline) TextDecoration.Underline else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Тонкие стрелочки-подсказки ››› рядом с ползунком (как на макете Брифи v2.pdf)
        Text(
            text = if (progress > 0.72f) "Отпусти!" else "›››",
            color = thumbColor.copy(alpha = (0.55f + progress * 0.45f).coerceIn(0.4f, 1f)),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer { translationX = if (progress > 0.72f) 0f else -10f }
        )

        // Ползунок слайдера
        Box(
            modifier = Modifier
                .offset { IntOffset(dragX.value.roundToInt(), 0) }
                .onSizeChanged { thumbWidthPx = it.width }
                .clip(RoundedCornerShape(14.dp))
                .background(thumbColor)
                .pointerInput(enabled, maxDragPx) {
                    if (!enabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            ticked35 = false
                            ticked70 = false
                            triggerBrifiHaptic(context)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val next = (dragX.value + dragAmount).coerceIn(0f, maxDragPx)
                            val p = next / maxDragPx
                            if (p >= 0.35f && !ticked35) {
                                ticked35 = true
                                triggerBrifiHaptic(context)
                            }
                            if (p >= 0.70f && !ticked70) {
                                ticked70 = true
                                triggerBrifiHaptic(context)
                            }
                            scope.launch { dragX.snapTo(next) }
                        },
                        onDragEnd = {
                            if (dragX.value >= maxDragPx * 0.72f) {
                                scope.launch {
                                    dragX.animateTo(
                                        maxDragPx,
                                        animationSpec = tween(140, easing = FastOutSlowInEasing)
                                    )
                                    triggerBrifiConfirmHaptic(context)
                                    onSlideComplete()
                                    dragX.snapTo(0f)
                                }
                            } else {
                                scope.launch {
                                    dragX.animateTo(
                                        0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.80f,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                dragX.animateTo(
                                    0f,
                                    animationSpec = spring(dampingRatio = 0.80f, stiffness = Spring.StiffnessMediumLow)
                                )
                            }
                        }
                    )
                }
                .padding(horizontal = 13.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = thumbLabel,
                color = thumbTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

/**
 * Функциональная боковая полоса навигации справа от стопки «Другие задачи»
 */
@Composable
private fun InteractiveSideRail(
    totalCount: Int,
    currentIndex: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpTo: (Int) -> Unit
) {
    val dotSlots = minOf(totalCount, 4)
    val activeSlot = if (totalCount <= 1) 0 else ((currentIndex.toFloat() / (totalCount - 1)) * (dotSlots - 1)).roundToInt()

    Column(
        modifier = Modifier
            .width(24.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrifiColors.ScrollRail)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "▲",
            color = BrifiColors.ScrollRailIcon,
            fontSize = 9.sp,
            modifier = Modifier
                .clickable { onPrev() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
        for (slot in 0 until dotSlots) {
            val isActive = slot == activeSlot
            val targetIdx = if (dotSlots <= 1) 0 else ((slot.toFloat() / (dotSlots - 1)) * (totalCount - 1)).roundToInt()
            Box(
                modifier = Modifier
                    .width(if (isActive) 6.dp else 5.dp)
                    .height(if (isActive) 22.dp else 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isActive) BrifiColors.AccentIndigo else BrifiColors.ScrollRailIcon.copy(alpha = 0.65f))
                    .clickable { onJumpTo(targetIdx) }
            )
        }
        Text(
            text = "▼",
            color = BrifiColors.ScrollRailIcon,
            fontSize = 9.sp,
            modifier = Modifier
                .clickable { onNext() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Плавная вертикальная стопка карточек для «Другие задачи» (без дёрганий)
 */
@Composable
private fun BrifiVerticalCardDeck(
    items: List<OrderTask>,
    autoAdvanceDelayMs: Long = 10_000L,
    cardContent: @Composable (task: OrderTask, depth: Int) -> Unit
) {
    if (items.isEmpty()) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var currentIndex by remember(items.size) { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, items.lastIndex)

    val dragOffsetY = remember { Animatable(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    val flingDistancePx = with(density) { 190.dp.toPx() }
    val thresholdPx = with(density) { 54.dp.toPx() }
    val stepOffsetPx = with(density) { 14.dp.toPx() }

    fun animateToIndex(targetIndex: Int, direction: Int) {
        if (items.size <= 1 || isAnimating || targetIndex == safeIndex) return
        scope.launch {
            isAnimating = true
            val targetY = if (direction > 0) -flingDistancePx else flingDistancePx
            dragOffsetY.animateTo(
                targetValue = targetY,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
            )
            currentIndex = targetIndex.coerceIn(0, items.lastIndex)
            triggerBrifiHaptic(context)
            dragOffsetY.snapTo(if (direction > 0) stepOffsetPx * 1.1f else -stepOffsetPx * 1.1f)
            dragOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            isAnimating = false
        }
    }

    fun step(direction: Int) {
        if (items.size <= 1) return
        val next = if (direction > 0) (safeIndex + 1) % items.size
        else if (safeIndex == 0) items.lastIndex else safeIndex - 1
        animateToIndex(next, direction)
    }

    LaunchedEffect(items.size, safeIndex, isAnimating) {
        if (items.size > 1 && !isAnimating) {
            delay(autoAdvanceDelayMs)
            step(direction = 1)
        }
    }

    val visibleDepthCount = minOf(3, items.size)
    val progress = (abs(dragOffsetY.value) / flingDistancePx).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (items.size > 1) 24.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .pointerInput(items.size, safeIndex) {
                    if (items.size <= 1) return@pointerInput
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (!isAnimating) {
                                scope.launch {
                                    dragOffsetY.snapTo(
                                        (dragOffsetY.value + dragAmount).coerceIn(-flingDistancePx, flingDistancePx)
                                    )
                                }
                            }
                        },
                        onDragEnd = {
                            if (!isAnimating) {
                                val offset = dragOffsetY.value
                                when {
                                    offset < -thresholdPx -> step(direction = 1)
                                    offset > thresholdPx -> step(direction = -1)
                                    else -> scope.launch {
                                        dragOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.84f,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            if (!isAnimating) {
                                scope.launch {
                                    dragOffsetY.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow)
                                    )
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.TopCenter
        ) {
            for (depth in (visibleDepthCount - 1) downTo 0) {
                val itemIdx = (safeIndex + depth) % items.size
                val task = items[itemIdx]

                val effectiveDepth = (depth - progress).coerceAtLeast(0f)
                val scale = if (depth == 0) 1f - progress * 0.04f else 1f - effectiveDepth * 0.055f
                val translationY = if (depth == 0) dragOffsetY.value else -effectiveDepth * stepOffsetPx
                val alpha = if (depth == 0) (1f - progress * 0.55f).coerceIn(0.2f, 1f)
                else (1f - effectiveDepth * 0.16f).coerceIn(0.45f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex((visibleDepthCount - depth).toFloat())
                        .graphicsLayer {
                            this.scaleX = scale
                            this.scaleY = scale
                            this.translationY = translationY
                            this.alpha = alpha
                        }
                ) {
                    cardContent(task, depth)
                }
            }
        }

        if (items.size > 1) {
            Spacer(modifier = Modifier.width(8.dp))
            InteractiveSideRail(
                totalCount = items.size,
                currentIndex = safeIndex,
                onPrev = { step(-1) },
                onNext = { step(1) },
                onJumpTo = { idx -> animateToIndex(idx, if (idx > safeIndex) 1 else -1) }
            )
        }
    }
}

/**
 * Вкладка 1 — Главный экран (точно по Брифи v2.pdf)
 */
@Composable
fun HomeScreen(
    allTasks: List<OrderTask>,
    inProgressTasks: List<OrderTask>,
    otherTasks: List<OrderTask>,
    lastUpdatedText: String,
    isRefreshing: Boolean,
    statusBanner: String?,
    onOpenStory: (BrifiStory) -> Unit,
    onCreateTaskClick: () -> Unit,
    onSaveLayoutClick: () -> Unit,
    onOpenDetails: (OrderTask) -> Unit,
    onChangeStatus: (OrderTask) -> Unit,
    onMarkDone: (OrderTask) -> Unit,
    onTakeIntoWork: (OrderTask) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val activeCount = remember(allTasks) { allTasks.count { it.isActive } }
    val deadlineTodayCount = remember(allTasks, today) { allTasks.count { it.isActive && it.deadline == today } }

    val sheetShape = RoundedCornerShape(28.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 112.dp)
    ) {
        // Верхняя дышащая шапка с логотипом слева (как на макете Главного экрана)
        BrifiBreathingHeader(
            centered = false,
            primaryGlow = BrifiColors.AccentIndigo,
            secondaryGlow = BrifiColors.AccentCyan
        )

        // 4 верхние плитки: 12 Активных задач | 2 Дедлайн сегодня! | Справка | Чек-лист
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TopInfoTile(
                modifier = Modifier.weight(1f),
                label = "Активных\nзадач",
                onClick = null
            ) {
                Text(
                    text = activeCount.toString(),
                    color = BrifiColors.AccentIndigoSoft,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
            }
            TopInfoTile(
                modifier = Modifier.weight(1f),
                label = "Дедлайн\nсегодня!",
                onClick = null
            ) {
                Text(
                    text = deadlineTodayCount.toString(),
                    color = BrifiColors.AccentCyanLight,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
            }
            TopInfoTile(
                modifier = Modifier.weight(1f),
                label = "Справка по\nпроизводству",
                onClick = {
                    triggerBrifiHaptic(context)
                    onOpenStory(BrifiStory.PRODUCTION)
                }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_story_printer),
                    contentDescription = "Справка по производству",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp)
                )
            }
            TopInfoTile(
                modifier = Modifier.weight(1f),
                label = "Чек-лист\nперед загрузкой",
                onClick = {
                    triggerBrifiHaptic(context)
                    onOpenStory(BrifiStory.CHECKLIST)
                }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_story_checklist),
                    contentDescription = "Чек-лист",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // 2 кнопки: «Создать задачу» и «Сохранить макет» (URL)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BrifiColors.CardDarkElevated)
                    .border(1.dp, BrifiColors.SheetDarkBorder, RoundedCornerShape(22.dp))
                    .clickable {
                        triggerBrifiHaptic(context)
                        onCreateTaskClick()
                    }
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.8.dp, BrifiColors.TextWhite, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    PlusIcon(color = BrifiColors.AccentIndigoSoft, modifier = Modifier.size(13.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Создать задачу",
                    color = BrifiColors.TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BrifiColors.CardDarkElevated)
                    .border(1.dp, BrifiColors.SheetDarkBorder, RoundedCornerShape(22.dp))
                    .clickable {
                        triggerBrifiHaptic(context)
                        onSaveLayoutClick()
                    }
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "URL",
                    color = BrifiColors.TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Сохранить макет",
                    color = BrifiColors.TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (!statusBanner.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrifiColors.CardDarkElevated)
                    .border(1.dp, BrifiColors.AccentIndigo.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = statusBanner,
                    color = BrifiColors.AccentCyanLight,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Блок «В работе:» — плавный нативный HorizontalPager (карточки рядом, следующая выглядывает справа!)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(sheetShape)
                .background(BrifiColors.SheetDark)
                .border(1.dp, BrifiColors.SheetDarkBorder, sheetShape)
                .padding(vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "В работе:",
                    color = BrifiColors.TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (inProgressTasks.size > 1) {
                    Text(
                        text = "${inProgressTasks.size} в работе",
                        color = BrifiColors.AccentIndigoSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (inProgressTasks.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { inProgressTasks.size })

                // Плавное автопереключение каждые 10 секунд (ставится на паузу во время касания)
                LaunchedEffect(inProgressTasks.size, pagerState.currentPage, pagerState.isScrollInProgress) {
                    if (inProgressTasks.size > 1 && !pagerState.isScrollInProgress) {
                        delay(10_000L)
                        val nextPage = (pagerState.currentPage + 1) % inProgressTasks.size
                        pagerState.animateScrollToPage(
                            page = nextPage,
                            animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
                        )
                        triggerBrifiHaptic(context)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = if (inProgressTasks.size > 1) 42.dp else 16.dp
                    ),
                    pageSpacing = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val task = inProgressTasks[page]
                    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    val scale = (1f - abs(pageOffset).coerceIn(0f, 1f) * 0.045f)
                    val alpha = (1f - abs(pageOffset).coerceIn(0f, 1f) * 0.25f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            }
                    ) {
                        ActiveWorkDarkCard(
                            task = task,
                            onOpenDetails = { onOpenDetails(task) },
                            onChangeStatus = { onChangeStatus(task) },
                            onMarkDone = { onMarkDone(task) }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(BrifiColors.CardDark)
                        .padding(22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет активных задач — всё выполнено!",
                        color = BrifiColors.TextMutedLight,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Дата обновления данных: $lastUpdatedText",
                color = Color(0xFF888896),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Блок «Другие задачи:»
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(sheetShape)
                .background(BrifiColors.SheetDark)
                .border(1.dp, BrifiColors.SheetDarkBorder, sheetShape)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Другие задачи:",
                    color = BrifiColors.TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (otherTasks.isNotEmpty()) {
                    Text(
                        text = "${otherTasks.size} шт.",
                        color = Color(0xFFA0A0B0),
                        fontSize = 12.sp
                    )
                }
            }

            if (otherTasks.isNotEmpty()) {
                BrifiVerticalCardDeck(
                    items = otherTasks,
                    autoAdvanceDelayMs = 10_000L
                ) { task, depth ->
                    OtherTaskCardBody(
                        task = task,
                        isBackCard = depth > 0,
                        onOpenDetails = { onOpenDetails(task) },
                        onTakeIntoWork = { onTakeIntoWork(task) },
                        onChangeStatus = { onChangeStatus(task) }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BrifiColors.CardDark)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "В очереди других задач нет",
                        color = Color(0xFF90909E),
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Блок «Last but not least» — 6 быстрых иконок точно как на макете
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Last but not least",
                color = BrifiColors.TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_tg,
                    label = "Чат тг",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_TG_CHAT) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_sheets,
                    label = "Таблица",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.SHEET_URL) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_print,
                    label = "Принт",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_PRINTOFFICE) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_mail,
                    label = "Mail",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_MAIL_RU) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_disk,
                    label = "Диск",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_YANDEX_DISK) }
                )
                QuickLinkItem(
                    iconRes = R.drawable.ic_quick_max,
                    label = "Макс",
                    modifier = Modifier.weight(1f),
                    onClick = { openUrl(context, BrifiRepository.URL_MAX) }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        RefreshFooter(isRefreshing = isRefreshing, onRefresh = onRefresh)
    }
}

@Composable
private fun TopInfoTile(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BrifiColors.CardDarkElevated)
                .border(1.dp, BrifiColors.SheetDarkBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color(0xFFB4B4C2),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun QuickLinkItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BrifiColors.CardDarkElevated)
            .border(1.dp, BrifiColors.SheetDarkBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = Color(0xFFCCCCDA),
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * Тёмная карточка заказа в блоке «В работе» (точно по Брифи v2.pdf)
 */
@Composable
private fun ActiveWorkDarkCard(
    task: OrderTask,
    onOpenDetails: () -> Unit,
    onChangeStatus: () -> Unit,
    onMarkDone: () -> Unit
) {
    val cardShape = RoundedCornerShape(22.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(BrifiColors.CardDark)
            .border(1.dp, Color(0xFF343442), cardShape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = BrifiColors.TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(task.orderNumber)
                        if (task.urgency.isNotBlank()) append("   ${task.urgency}")
                    },
                    color = Color(0xFF9C9CAE),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262630))
                    .clickable { onOpenDetails() },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = BrifiColors.TextWhite, modifier = Modifier.size(15.dp))
            }
        }

        if (task.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = task.note,
                color = Color(0xFFC2C2D4),
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF22222C))
                    .clickable { onOpenDetails() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgencyDeadlinePill(task = task)
            Text(
                text = "🖥️ ${task.source}",
                color = Color(0xFFC8C8D6),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BrifiSlideActionBar(
            thumbLabel = "Готово",
            thumbColor = BrifiColors.AccentIndigo,
            thumbTextColor = BrifiColors.TextWhite,
            rightActionLabel = "Изменить статус",
            rightActionUnderline = true,
            enabled = true,
            onSlideComplete = onMarkDone,
            onRightActionClick = onChangeStatus
        )
    }
}

/**
 * Светлая карточка заказа в стопке «Другие задачи» (точно по Брифи v2.pdf)
 */
@Composable
private fun OtherTaskCardBody(
    task: OrderTask,
    isBackCard: Boolean,
    onOpenDetails: () -> Unit,
    onTakeIntoWork: () -> Unit,
    onChangeStatus: () -> Unit
) {
    val bgColor = if (isBackCard) BrifiColors.DeckBackGray else BrifiColors.DeckFrontLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = BrifiColors.TextBlack,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(task.orderNumber)
                        if (task.urgency.isNotBlank()) append("   ${task.urgency}")
                    },
                    color = BrifiColors.TextMutedDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(BrifiColors.TextBlack)
                    .clickable(enabled = !isBackCard) { onOpenDetails() },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = BrifiColors.TextWhite, modifier = Modifier.size(15.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BrifiColors.TextWhite)
                .clickable(enabled = !isBackCard) { onOpenDetails() }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.TopStart
        ) {
            Text(
                text = task.note.ifBlank { "Поле для заметок" },
                color = if (task.note.isNotBlank()) BrifiColors.TextBlack else Color(0xFF9898A4),
                fontSize = 12.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgencyDeadlinePill(task = task)
            Text(
                text = "🖥️ ${task.source}",
                color = BrifiColors.TextBlack,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BrifiSlideActionBar(
            thumbLabel = "Взять в работу",
            thumbColor = BrifiColors.AccentIndigo,
            thumbTextColor = BrifiColors.TextWhite,
            rightActionLabel = task.status,
            rightActionUnderline = false,
            enabled = !isBackCard,
            onSlideComplete = onTakeIntoWork,
            onRightActionClick = onChangeStatus
        )
    }
}

/**
 * Вкладка 2 — Статистика (точно по Брифи v2.pdf)
 */
@Composable
fun StatsScreen(
    allTasks: List<OrderTask>,
    totalOrdersCount: Int,
    weeksAgo: Int,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenDetails: (OrderTask) -> Unit,
    onChangeStatus: (OrderTask) -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val tomorrow = remember(today) { today.plusDays(1) }

    val tasksForToday = remember(allTasks, today) {
        allTasks.count { it.isActive && it.deadline == today }
    }
    val doneToday = remember(allTasks, today) {
        allTasks.count { it.isCompleted && (it.deadline == today || it.createdDate == today) }
    }
    val tasksForTomorrow = remember(allTasks, tomorrow) {
        allTasks.count { it.isActive && it.deadline == tomorrow }
    }

    val (weekStart, weekEnd) = remember(today, weeksAgo) {
        SheetsParser.weekBounds(today, weeksAgo)
    }
    val weekTasks = remember(allTasks, weekStart, weekEnd) {
        SheetsParser.filterTasksForWeek(allTasks, weekStart, weekEnd)
    }

    val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 28.dp, bottomEnd = 28.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 112.dp)
    ) {
        BrifiBreathingHeader(
            centered = true,
            primaryGlow = BrifiColors.AccentCyan,
            secondaryGlow = BrifiColors.AccentDeepBlue
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(BrifiColors.SheetDark)
                .border(1.dp, BrifiColors.SheetDarkBorder, sheetShape)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Статистика",
                color = BrifiColors.TextWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiTile(
                    value = totalOrdersCount.toString(),
                    title = "Задач всего",
                    bgColor = BrifiColors.KpiTile1,
                    textColor = BrifiColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    value = tasksForToday.toString(),
                    title = "Задачи на сегодня",
                    bgColor = BrifiColors.KpiTile2,
                    textColor = BrifiColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiTile(
                    value = doneToday.toString(),
                    title = "Задача выполнена\nсегодня",
                    bgColor = BrifiColors.KpiTile3,
                    textColor = BrifiColors.TextWhite,
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    value = tasksForTomorrow.toString(),
                    title = "Задач на завтра",
                    bgColor = BrifiColors.KpiTile4,
                    textColor = BrifiColors.AccentDeepBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Заказы за неделю",
                color = BrifiColors.TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
            )

            if (weekTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(BrifiColors.CardDark)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "За эту неделю заказов не найдено",
                        color = Color(0xFF90909E),
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    weekTasks.forEach { task ->
                        StatsWeekTaskCard(
                            task = task,
                            onOpenDetails = { onOpenDetails(task) },
                            onChangeStatus = { onChangeStatus(task) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Переключатель недель `< Неделя от ДД.ММ.ГГГГ >` точно как на макете
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    color = BrifiColors.AccentIndigoSoft,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            triggerBrifiHaptic(context)
                            onPrevWeek()
                        }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
                Text(
                    text = SheetsParser.formatWeekStart(weekStart),
                    color = Color(0xFFB8B8C8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = TextDecoration.Underline
                )
                Text(
                    text = "›",
                    color = if (weeksAgo > 0) BrifiColors.AccentIndigoSoft else Color(0xFF4A4A58),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = weeksAgo > 0) {
                            triggerBrifiHaptic(context)
                            onNextWeek()
                        }
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun KpiTile(
    value: String,
    title: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(114.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(bgColor)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = value,
            color = textColor,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun StatsWeekTaskCard(
    task: OrderTask,
    onOpenDetails: () -> Unit,
    onChangeStatus: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrifiColors.StatsCardLight)
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    color = BrifiColors.TextBlack,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(task.orderNumber)
                        if (task.urgency.isNotBlank()) append("   ${task.urgency}")
                    },
                    color = BrifiColors.TextMutedDark,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onOpenDetails() },
                contentAlignment = Alignment.Center
            ) {
                PlusIcon(color = BrifiColors.TextBlack, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UrgencyDeadlinePill(task = task)
            Text(
                text = "🖥️ ${task.source}",
                color = BrifiColors.TextBlack,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(BrifiColors.BgBlack)
                .padding(horizontal = 7.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrifiColors.TextWhite)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = task.status,
                    color = BrifiColors.TextBlack,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Text(
                text = "Изменить статус заказа",
                color = BrifiColors.TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onChangeStatus() }
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

/**
 * Вкладка 3 — Профиль / Финансовая аналитика (точно по Брифи v2.pdf с 5 плитками и круговой диаграммой)
 */
@Composable
fun ProfileScreen(
    finance: FinanceSummary,
    isRefreshing: Boolean,
    hasWebhookConfigured: Boolean,
    onConfirmPayoutClick: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 28.dp, bottomEnd = 28.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 112.dp)
    ) {
        BrifiBreathingHeader(
            centered = true,
            primaryGlow = BrifiColors.AccentIndigo,
            secondaryGlow = BrifiColors.AccentDeepBlue
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(BrifiColors.SheetDark)
                .border(1.dp, BrifiColors.SheetDarkBorder, sheetShape)
                .padding(horizontal = 16.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Привет,",
                color = BrifiColors.TextWhite,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Поинт Джон",
                color = BrifiColors.AccentIndigoSoft,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "За неделю от ${finance.latestWeek.startDateLabel}",
                color = Color(0xFF9E9EAC),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Ты нахастлил",
                color = BrifiColors.TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Главная чёрная карточка суммы с пунктирным низом
            DashedBorderBox(
                cornerRadius = 26.dp,
                borderColor = BrifiColors.ProfileDashedBorder,
                backgroundColor = BrifiColors.BgBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 26.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = SheetsParser.formatMoney(finance.latestWeek.amount),
                        color = BrifiColors.TextWhite,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Строка статуса оплаты + фиолетовая кнопка «Подтвердить оплату»
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrifiColors.BgBlack)
                        .border(1.dp, Color(0xFF2B2B36), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = finance.latestWeek.payoutStatus,
                        color = Color(0xFFC0C0CE),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BrifiColors.AccentIndigo)
                        .clickable {
                            triggerBrifiHaptic(context)
                            onConfirmPayoutClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Подтвердить оплату",
                        color = BrifiColors.TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ряд 1: Заработал за все время | Заработал за этот месяц
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileIncomeTile(
                    amount = SheetsParser.formatMoney(finance.allTimeTotalIncome),
                    label = "Заработал за все время",
                    modifier = Modifier.weight(1f)
                )
                ProfileIncomeTile(
                    amount = SheetsParser.formatMoney(finance.currentMonthTotalIncome),
                    label = "Заработал за этот месяц",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ряд 2: Слева 2 плитки (Средний процент + Средний доход), справа высокая плитка с круговой диаграммой
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileIncomeTile(
                        amount = SheetsParser.formatMoney(finance.avgMonthlyCommission50),
                        label = "Средний процент в месяц",
                        heightDp = 82
                    )
                    ProfileIncomeTile(
                        amount = SheetsParser.formatMoney(finance.avgMonthlyTotalIncome),
                        label = "Средний доход в месяц",
                        heightDp = 82
                    )
                }

                PaidFreePieTile(
                    paid = finance.paidOrdersCount,
                    free = finance.freeOrdersCount,
                    modifier = Modifier
                        .weight(1f)
                        .height(174.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Настройка двусторонней синхронизации с Google Таблицей
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(BrifiColors.ProfileTileDark)
                    .border(1.dp, BrifiColors.SheetDarkBorder, RoundedCornerShape(18.dp))
                    .clickable { onOpenSyncSettings() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Синхронизация с таблицей",
                        color = BrifiColors.TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (hasWebhookConfigured) "Активна — правки пишутся в таблицу" else "Вставь URL скрипта для обратной записи",
                        color = if (hasWebhookConfigured) Color(0xFF66D17A) else Color(0xFF9090A0),
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "Настроить",
                    color = BrifiColors.AccentIndigoSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(22.dp))
            RefreshFooter(isRefreshing = isRefreshing, onRefresh = onRefresh)
        }
    }
}

@Composable
private fun ProfileIncomeTile(
    amount: String,
    label: String,
    heightDp: Int = 88,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(BrifiColors.ProfileTileDark)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = amount,
            color = BrifiColors.TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color(0xFF9898A6),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Круговая диаграмма «Платные/бесплатные» (точно по макету Брифи v2.pdf)
 */
@Composable
private fun PaidFreePieTile(
    paid: Int,
    free: Int,
    modifier: Modifier = Modifier
) {
    val total = (paid + free).coerceAtLeast(1)
    val paidRatio = if (paid + free == 0) 0.71f else (paid.toFloat() / total.toFloat())
    val paidSweep = 360f * paidRatio
    val freeSweep = 360f - paidSweep
    val paidPct = (paidRatio * 100f).roundToInt()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(BrifiColors.ProfileTileDark)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(modifier = Modifier.size(96.dp)) {
            val d = size.minDimension
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arcSize = Size(d, d)
            // Платные — фиолетово-индиго сектор
            drawArc(
                color = BrifiColors.AccentIndigo,
                startAngle = -90f,
                sweepAngle = paidSweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
            // Бесплатные — светло-голубой сектор
            drawArc(
                color = BrifiColors.AccentSoftBlue,
                startAngle = -90f + paidSweep,
                sweepAngle = freeSweep,
                useCenter = true,
                topLeft = topLeft,
                size = arcSize
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Платные/бесплатные",
                color = Color(0xFF9898A6),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "$paidPct% / ${100 - paidPct}%",
                color = BrifiColors.TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RefreshFooter(
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !isRefreshing) { onRefresh() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    color = BrifiColors.AccentIndigoSoft,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = if (isRefreshing) "Обновление..." else "Обновить данные",
                color = Color(0xFFC8C8D6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        BrifiLogoImage(height = 19.dp)
    }
}

@Composable
fun FloatingBottomBar(
    currentTab: BrifiTab,
    onSelectTab: (BrifiTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(58.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(BrifiColors.NavPillBg)
                .border(1.dp, Color(0xFF30303C), RoundedCornerShape(30.dp))
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelectTab(BrifiTab.HOME) }
                    .padding(10.dp)
            ) {
                NavHomeIcon(
                    color = if (currentTab == BrifiTab.HOME) BrifiColors.NavActive else BrifiColors.NavInactive
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelectTab(BrifiTab.STATS) }
                    .padding(10.dp)
            ) {
                NavStatsIcon(
                    color = if (currentTab == BrifiTab.STATS) BrifiColors.NavActive else BrifiColors.NavInactive
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSelectTab(BrifiTab.PROFILE) }
                    .padding(10.dp)
            ) {
                NavProfileIcon(
                    color = if (currentTab == BrifiTab.PROFILE) BrifiColors.NavActive else BrifiColors.NavInactive
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(BrifiColors.NavPillBg)
                .border(1.dp, Color(0xFF30303C), CircleShape)
                .clickable { onAddClick() },
            contentAlignment = Alignment.Center
        ) {
            PlusIcon(color = BrifiColors.TextWhite, modifier = Modifier.size(22.dp))
        }
    }
}
