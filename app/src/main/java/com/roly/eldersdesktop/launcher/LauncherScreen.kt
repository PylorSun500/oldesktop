package com.roly.eldersdesktop.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.roly.eldersdesktop.ui.theme.EldersdesktopTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SETTINGS_HOLD_DURATION_MS = 1_000L
private const val SETTINGS_HOLD_PREVIEW_MS = 300L
private const val SETTINGS_HOLD_PROGRESS_DELAY_MS = 180L

@Composable
fun EldersLauncherApp() {
    EldersLauncherScreen()
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EldersLauncherScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val launcherController = remember { LauncherController(context) }
    val pagerState = rememberPagerState(pageCount = { launcherController.cards.size })
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    var isManageMode by rememberSaveable { mutableStateOf(false) }
    var settingsHoldProgress by remember { mutableFloatStateOf(0f) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    val now by rememberCurrentTime()
    val timeText = remember(now) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    }
    val dateText = remember(now) {
        SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date(now))
    }
    val currentCard by remember(launcherController.cards, pagerState.currentPage) {
        derivedStateOf {
            launcherController.cards.getOrNull(pagerState.currentPage)
        }
    }

    LaunchedEffect(Unit) {
        launcherController.refreshInstalledApps()
    }

    LaunchedEffect(launcherController.cards.size) {
        if (launcherController.cards.isEmpty()) {
            return@LaunchedEffect
        }

        val targetPage = pagerState.currentPage.coerceIn(0, launcherController.cards.lastIndex)
        if (targetPage != pagerState.currentPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            AnimatedContent(
                targetState = isManageMode,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "launcher_mode_bottom_bar"
            ) { manageMode ->
                if (manageMode) {
                    ManageModePanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        canMoveLeft = pagerState.currentPage > 0,
                        canMoveRight = pagerState.currentPage < launcherController.cards.lastIndex,
                        onAdd = { showAddSheet = true },
                        onMoveLeft = {
                            val target = launcherController.moveLeft(pagerState.currentPage)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(target)
                            }
                        },
                        onMoveRight = {
                            val target = launcherController.moveRight(pagerState.currentPage)
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(target)
                            }
                        },
                        onDelete = {
                            val nextPage = launcherController.removeAt(pagerState.currentPage)
                            if (nextPage == null) {
                                Toast.makeText(
                                    context,
                                    "至少要保留一张卡片",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@ManageModePanel
                            }

                            coroutineScope.launch {
                                pagerState.animateScrollToPage(nextPage)
                            }
                        },
                        onDone = {
                            isManageMode = false
                            settingsHoldProgress = 0f
                        }
                    )
                } else {
                    NavigationPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        canGoLeft = pagerState.currentPage > 0,
                        canGoRight = pagerState.currentPage < launcherController.cards.lastIndex,
                        onNavigateLeft = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        onNavigateRight = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            StatusSummaryCard(
                timeText = timeText,
                dateText = dateText,
                isManageMode = isManageMode,
                holdProgress = settingsHoldProgress,
                touchSlop = touchSlop,
                onHoldPreview = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onHoldProgress = { progress ->
                    settingsHoldProgress = progress
                },
                onEnterManageMode = {
                    if (!isManageMode) {
                        isManageMode = true
                        settingsHoldProgress = 0f
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                pageSpacing = 16.dp,
                beyondViewportPageCount = 1
            ) { page ->
                val card = launcherController.cards[page]
                LauncherCardPage(
                    card = card,
                    appLabel = launcherController.labelFor(card.packageName),
                    isManageMode = isManageMode,
                    onTap = {
                        if (!isManageMode) {
                            val launched = launchCard(context, card)
                            if (!launched) {
                                Toast.makeText(
                                    context,
                                    "这个功能暂时打不开",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PagerIndicator(
                pageCount = launcherController.cards.size,
                selectedPage = pagerState.currentPage
            )
        }
    }

    if (showAddSheet && currentCard != null) {
        AddCardSheet(
            builtInTemplates = launcherController.availableTemplates(),
            availableApps = launcherController.availableApps(),
            onDismiss = { showAddSheet = false },
            onAddBuiltIn = { type ->
                val target = launcherController.addBuiltInAfter(
                    currentIndex = pagerState.currentPage,
                    type = type
                )
                showAddSheet = false
                coroutineScope.launch {
                    pagerState.animateScrollToPage(target)
                }
            },
            onAddApp = { packageName ->
                val target = launcherController.addAppAfter(
                    currentIndex = pagerState.currentPage,
                    packageName = packageName
                )
                showAddSheet = false
                coroutineScope.launch {
                    pagerState.animateScrollToPage(target)
                }
            }
        )
    }
}

@Composable
private fun StatusSummaryCard(
    timeText: String,
    dateText: String,
    isManageMode: Boolean,
    holdProgress: Float,
    touchSlop: Float,
    onHoldPreview: () -> Unit,
    onHoldProgress: (Float) -> Unit,
    onEnterManageMode: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HoldToManageButton(
                modifier = Modifier
                    .height(64.dp)
                    .widthIn(min = 82.dp),
                isManageMode = isManageMode,
                holdProgress = holdProgress,
                touchSlop = touchSlop,
                onHoldPreview = onHoldPreview,
                onHoldProgress = onHoldProgress,
                onEnterManageMode = onEnterManageMode
            )
        }
    }
}

@Composable
private fun LauncherCardPage(
    card: LauncherCard,
    appLabel: String,
    isManageMode: Boolean,
    onTap: () -> Unit
) {
    val palette = cardPalette(card.type)
    val borderColor by animateColorAsState(
        targetValue = if (isManageMode) palette.outline else MaterialTheme.colorScheme.outlineVariant,
        label = "card_border_color"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isManageMode) 2.dp else 1.dp,
        label = "card_border_width"
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = !isManageMode,
                onClick = onTap
            ),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = borderWidth,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isManageMode) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(text = "正在管理当前卡片")
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = palette.container,
                            labelColor = palette.onContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            HeroCard(
                card = card,
                appLabel = appLabel,
                palette = palette
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = cardTitle(card, appLabel),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = cardSubtitle(card),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroCard(
    card: LauncherCard,
    appLabel: String,
    palette: CardPalette
) {
    if (card.type == LauncherCardType.CLOCK) {
        val now by rememberCurrentTime()
        val timeText = remember(now) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
        }

        ElevatedCard(
            modifier = Modifier
                .size(220.dp),
            shape = CircleShape,
            colors = CardDefaults.elevatedCardColors(
                containerColor = palette.container
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = palette.onContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayLarge,
                        color = palette.onContainer,
                        fontSize = 56.sp
                    )
                }
            }
        }
        return
    }

    ElevatedCard(
        modifier = Modifier
            .size(188.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = palette.container
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (card.type == LauncherCardType.APP) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    AppIcon(
                        packageName = card.packageName,
                        contentDescription = appLabel,
                        tint = palette.onContainer,
                        modifier = Modifier
                            .padding(20.dp)
                            .size(88.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = cardHeroIcon(card.type),
                    contentDescription = null,
                    modifier = Modifier.size(84.dp),
                    tint = palette.onContainer
                )
            }
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String?,
    contentDescription: String,
    tint: Color,
    modifier: Modifier = Modifier.size(120.dp)
) {
    val bitmap = rememberAppIconBitmap(packageName)

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Apps,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

@Composable
private fun PagerIndicator(
    pageCount: Int,
    selectedPage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            Icon(
                imageVector = Icons.Filled.Lens,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == selectedPage) 14.dp else 10.dp),
                tint = if (index == selectedPage) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }
            )
        }
    }
}

@Composable
private fun NavigationPanel(
    modifier: Modifier = Modifier,
    canGoLeft: Boolean,
    canGoRight: Boolean,
    onNavigateLeft: () -> Unit,
    onNavigateRight: () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(32.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateLeft,
                enabled = canGoLeft,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(
                    topStart = 32.dp,
                    bottomStart = 32.dp,
                    topEnd = 14.dp,
                    bottomEnd = 14.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "向左切换",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "向左",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Button(
                onClick = onNavigateRight,
                enabled = canGoRight,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    bottomStart = 14.dp,
                    topEnd = 32.dp,
                    bottomEnd = 32.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "向右切换",
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "向右",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ManageModePanel(
    modifier: Modifier = Modifier,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onAdd: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onDelete: () -> Unit,
    onDone: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "管理当前卡片",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "添加")
                }
                FilledTonalButton(
                    onClick = onMoveLeft,
                    enabled = canMoveLeft,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "左移")
                }
                FilledTonalButton(
                    onClick = onMoveRight,
                    enabled = canMoveRight,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "右移")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "删除")
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "完成")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCardSheet(
    builtInTemplates: List<BuiltInCardTemplate>,
    availableApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onAddBuiltIn: (LauncherCardType) -> Unit,
    onAddApp: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "添加卡片",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "选择常用功能或手机里的应用。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (builtInTemplates.isNotEmpty()) {
                    item {
                        Text(
                            text = "功能卡",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(builtInTemplates) { template ->
                        SheetItemCard(
                            title = template.title,
                            subtitle = template.subtitle,
                            leadingIcon = cardHeroIcon(template.type),
                            onClick = { onAddBuiltIn(template.type) }
                        )
                    }
                }

                if (availableApps.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "已安装应用",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    items(availableApps) { app ->
                        AppSheetItemCard(
                            app = app,
                            onClick = { onAddApp(app.packageName) }
                        )
                    }
                }

                if (builtInTemplates.isEmpty() && availableApps.isEmpty()) {
                    item {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(text = "当前没有可添加的卡片了")
                                },
                                supportingContent = {
                                    Text(text = "可以先退出管理模式继续使用桌面。")
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetItemCard(
    title: String,
    subtitle: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        ListItem(
            headlineContent = {
                Text(text = title)
            },
            supportingContent = {
                Text(text = subtitle)
            },
            leadingContent = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun AppSheetItemCard(
    app: InstalledApp,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        ListItem(
            headlineContent = {
                Text(text = app.label)
            },
            supportingContent = {
                Text(text = "添加为单独的应用卡片")
            },
            leadingContent = {
                AppIcon(
                    packageName = app.packageName,
                    contentDescription = app.label,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}

private fun Modifier.holdToTrigger(
    enabled: Boolean,
    touchSlop: Float,
    durationMs: Long,
    previewMs: Long,
    progressDelayMs: Long,
    onHoldPreview: () -> Unit,
    onHoldProgress: (Float) -> Unit,
    onHoldTriggered: () -> Unit
): Modifier {
    if (!enabled) {
        return this
    }

    return pointerInput(enabled, touchSlop) {
        kotlinx.coroutines.coroutineScope {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var movedTooFar = false
                var released = false
                var triggered = false
                var previewSent = false

                val holdJob = launch {
                    val startTime = SystemClock.uptimeMillis()
                    while (!triggered && !movedTooFar && !released) {
                        val elapsed = SystemClock.uptimeMillis() - startTime
                        val progress = if (elapsed < progressDelayMs) {
                            0f
                        } else {
                            (
                                (elapsed - progressDelayMs).toFloat() /
                                    (durationMs - progressDelayMs).toFloat()
                                ).coerceIn(0f, 1f)
                        }
                        onHoldProgress(progress)

                        if (!previewSent && elapsed >= previewMs) {
                            previewSent = true
                            onHoldPreview()
                        }

                        if (elapsed >= durationMs) {
                            triggered = true
                            onHoldProgress(1f)
                            onHoldTriggered()
                            break
                        }

                        delay(16)
                    }
                }

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    val movedDistance = (change.position - down.position).getDistance()

                    if (movedDistance > touchSlop) {
                        movedTooFar = true
                        break
                    }

                    if (!change.pressed) {
                        released = true
                        break
                    }

                    if (triggered) {
                        break
                    }
                }

                holdJob.cancel()
                onHoldProgress(0f)
            }
        }
    }
}

@Composable
private fun HoldToManageButton(
    modifier: Modifier = Modifier,
    isManageMode: Boolean,
    holdProgress: Float,
    touchSlop: Float,
    onHoldPreview: () -> Unit,
    onHoldProgress: (Float) -> Unit,
    onEnterManageMode: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isManageMode) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "manage_button_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isManageMode) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "manage_button_content_color"
    )

    ElevatedCard(
        modifier = modifier
            .holdToTrigger(
                enabled = !isManageMode,
                touchSlop = touchSlop,
                durationMs = SETTINGS_HOLD_DURATION_MS,
                previewMs = SETTINGS_HOLD_PREVIEW_MS,
                progressDelayMs = SETTINGS_HOLD_PROGRESS_DELAY_MS,
                onHoldPreview = onHoldPreview,
                onHoldProgress = onHoldProgress,
                onHoldTriggered = onEnterManageMode
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = contentColor
                    )
                    if (!isManageMode && holdProgress > 0f) {
                        CircularProgressIndicator(
                            progress = { holdProgress },
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.Transparent
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                AnimatedContent(
                    targetState = isManageMode,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "manage_button_label"
                ) { manageMode ->
                    Text(
                        text = if (manageMode) "管理中" else "设置",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }
            }
        }
    }
}

private fun launchCard(context: Context, card: LauncherCard): Boolean {
    val intent = when (card.type) {
        LauncherCardType.CLOCK -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        LauncherCardType.PHONE -> Intent(Intent.ACTION_DIAL)
        LauncherCardType.CONTACTS -> Intent(
            Intent.ACTION_VIEW,
            ContactsContract.Contacts.CONTENT_URI
        )

        LauncherCardType.CAMERA -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        LauncherCardType.APP -> {
            val packageName = card.packageName ?: return false
            context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        }
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

@Composable
private fun cardPalette(type: LauncherCardType): CardPalette {
    val scheme = MaterialTheme.colorScheme
    return when (type) {
        LauncherCardType.CLOCK -> CardPalette(
            container = scheme.primaryContainer.copy(alpha = 0.72f),
            onContainer = scheme.onPrimaryContainer,
            outline = scheme.primary
        )

        LauncherCardType.PHONE -> CardPalette(
            container = scheme.secondaryContainer.copy(alpha = 0.72f),
            onContainer = scheme.onSecondaryContainer,
            outline = scheme.secondary
        )

        LauncherCardType.CONTACTS -> CardPalette(
            container = scheme.tertiaryContainer.copy(alpha = 0.72f),
            onContainer = scheme.onTertiaryContainer,
            outline = scheme.tertiary
        )

        LauncherCardType.CAMERA -> CardPalette(
            container = scheme.errorContainer.copy(alpha = 0.72f),
            onContainer = scheme.onErrorContainer,
            outline = scheme.error
        )

        LauncherCardType.APP -> CardPalette(
            container = scheme.surfaceContainerHighest,
            onContainer = scheme.onSurface,
            outline = scheme.primary
        )
    }
}

private fun cardHeroIcon(type: LauncherCardType): ImageVector {
    return when (type) {
        LauncherCardType.CLOCK -> Icons.Filled.AccessTime
        LauncherCardType.PHONE -> Icons.Filled.Call
        LauncherCardType.CONTACTS -> Icons.Filled.Contacts
        LauncherCardType.CAMERA -> Icons.Filled.PhotoCamera
        LauncherCardType.APP -> Icons.Filled.Apps
    }
}

private fun cardTitle(card: LauncherCard, appLabel: String): String {
    return when (card.type) {
        LauncherCardType.CLOCK -> "现在时间"
        LauncherCardType.PHONE -> "拨号电话"
        LauncherCardType.CONTACTS -> "联系人"
        LauncherCardType.CAMERA -> "相机"
        LauncherCardType.APP -> appLabel
    }
}

private fun cardSubtitle(card: LauncherCard): String {
    return when (card.type) {
        LauncherCardType.CLOCK -> "看时间，也可以进入闹钟"
        LauncherCardType.PHONE -> "单击打开拨号盘"
        LauncherCardType.CONTACTS -> "查看通讯录并发起联系"
        LauncherCardType.CAMERA -> "快速拍照，记录生活"
        LauncherCardType.APP -> "单击直接打开应用"
    }
}

@Composable
private fun rememberAppIconBitmap(packageName: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        packageName
            ?.let { loadAppIcon(context, it) }
            ?.toBitmap(width = 256, height = 256)
            ?.asImageBitmap()
    }
}

private fun loadAppIcon(context: Context, packageName: String): Drawable? {
    return runCatching {
        context.packageManager.getApplicationIcon(packageName)
    }.getOrNull()
}

private data class CardPalette(
    val container: Color,
    val onContainer: Color,
    val outline: Color
)

@Composable
private fun rememberCurrentTime(): State<Long> {
    return produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1_000L)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EldersLauncherScreenPreview() {
    EldersdesktopTheme {
        EldersLauncherScreen()
    }
}
