package com.example.photocleaner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.photocleaner.data.Photo
import com.example.photocleaner.viewmodel.CleanerViewModel
import com.example.photocleaner.viewmodel.UiState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.Visibility

@Composable
fun SwipeScreen(
    viewModel: CleanerViewModel,
    onTrashClick: () -> Unit,
    onInfoClick: () -> Unit,
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    onViewOriginal: (Photo) -> Unit
) {
    val state = viewModel.uiState
    val pendingDeleteCount = viewModel.pendingDeleteList.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding() // 统一处理系统栏边距
    ) {
        // 顶部栏：显示垃圾桶图标和计数
        if (state is UiState.Ready || state is UiState.Empty) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：关于 + 主题切换
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 关于按钮
                    IconButton(onClick = onInfoClick) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About"
                        )
                    }

                    // 主题切换按钮 (带下拉菜单)
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = when (themeMode) {
                                    1 -> Icons.Default.LightMode
                                    2 -> Icons.Default.DarkMode
                                    else -> Icons.Default.SettingsBrightness
                                },
                                contentDescription = "Theme Toggle"
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("跟随系统") },
                                onClick = {
                                    onThemeChange(0)
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.SettingsBrightness, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("浅色模式") },
                                onClick = {
                                    onThemeChange(1)
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.LightMode, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("深色模式") },
                                onClick = {
                                    onThemeChange(2)
                                    expanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.DarkMode, null) }
                            )
                        }
                    }
                }

                // 中间：统计信息 (进度)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "本次进度",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${viewModel.processedCount} / ${viewModel.totalPhotosCount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // 右侧：功能按钮组
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 撤销按钮
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo"
                        )
                    }

                    // 恢复/刷新按钮 (重置)
                    IconButton(onClick = { viewModel.loadAndShufflePhotos() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }

                    // 垃圾桶按钮
                    IconButton(onClick = onTrashClick) {
                        BadgedBox(
                            badge = {
                                if (pendingDeleteCount > 0) {
                                    Badge { Text("$pendingDeleteCount") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Trash"
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }
            is UiState.Empty -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("没有更多照片了", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadAndShufflePhotos() }) {
                        Text("重新扫描")
                    }
                }
            }
            is UiState.Ready -> {
                val photo = state.currentPhoto
                if (photo != null) {
                    // 使用 key 确保当 photo.id 变化时，Card 的状态（位置、旋转）会被重置
                    key(photo.id) {
                        SwipeablePhotoCard(
                            photo = photo,
                            onSwipeLeft = { viewModel.swipeLeft(photo) },
                            onSwipeRight = { viewModel.swipeRight(photo) },
                            onViewOriginal = { onViewOriginal(photo) }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun SwipeablePhotoCard(
    photo: Photo,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onViewOriginal: () -> Unit

) {
    // 屏幕宽度，用于计算滑动阈值
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val swipeThreshold = screenWidthPx * 0.3f // 滑动超过 30% 屏幕宽度触发

    // 动画变量
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // 计算滑动比例 (0.0 - 1.0)
    val swipeRatio = (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
    // 根据滑动比例计算模糊半径 (最大 2.dp)
    val blurRadius = (swipeRatio * 2).dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            // 核心手势逻辑
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (abs(offsetX.value) > swipeThreshold) {
                                // 触发滑动
                                val targetX = if (offsetX.value > 0) screenWidthPx * 1.5f else -screenWidthPx * 1.5f
                                offsetX.animateTo(targetX, animationSpec = tween(durationMillis = 200))
                                if (targetX > 0) onSwipeRight() else onSwipeLeft()
                            } else {
                                // 回弹
                                launch { offsetX.animateTo(0f, animationSpec = tween(durationMillis = 300)) }
                                launch { offsetY.animateTo(0f, animationSpec = tween(durationMillis = 300)) }
                                launch { rotation.animateTo(0f, animationSpec = tween(durationMillis = 300)) }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                            // 简单的旋转逻辑：移动越远转得越多
                            rotation.snapTo(offsetX.value / 20f)
                        }
                    }
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                rotationZ = rotation.value
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp), // 留出底部空间给按钮（如果有）
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = blurRadius), // 动态高斯模糊
                    contentScale = ContentScale.Crop
                )

                // 覆盖层：显示 "Like" 或 "Trash"
                if (offsetX.value > 0) {
                    // 右滑 - 保留 (爱心)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Keep",
                            modifier = Modifier
                                .size(100.dp)
                                .graphicsLayer {
                                    scaleX = 0.5f + swipeRatio * 0.5f
                                    scaleY = 0.5f + swipeRatio * 0.5f
                                },
                            tint = Color(0xFFE91E63).copy(alpha = swipeRatio) // 粉色爱心
                        )
                    }
                } else if (offsetX.value < 0) {
                    // 左滑 - 删除 (垃圾桶)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .size(100.dp)
                                .graphicsLayer {
                                    scaleX = 0.5f + swipeRatio * 0.5f
                                    scaleY = 0.5f + swipeRatio * 0.5f
                                },
                            tint = Color.Red.copy(alpha = swipeRatio)
                        )
                    }
                }

                // 底部信息栏
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "ID: ${photo.id}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // 底部操作按钮 (作为备选操作)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(0.9f) // 稍微加宽一点，因为按钮变宽了
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        // 优化动画：同时进行位移和旋转，模拟真实抛出效果
                        launch { rotation.animateTo(-20f, animationSpec = tween(300)) }
                        offsetX.animateTo(-screenWidthPx * 1.5f, animationSpec = tween(300))
                        onSwipeLeft()
                    }
                },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = { Icon(Icons.Default.Close, contentDescription = "Delete") },
                text = { Text("删除") }
            )

            ExtendedFloatingActionButton(
                onClick = onViewOriginal,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                icon = { Icon(Icons.Default.Visibility, contentDescription = "View") },
                text = { Text("查看") }
            )

            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        // 优化动画：同时进行位移和旋转，模拟真实抛出效果
                        launch { rotation.animateTo(20f, animationSpec = tween(300)) }
                        offsetX.animateTo(screenWidthPx * 1.5f, animationSpec = tween(300))
                        onSwipeRight()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                icon = { Icon(Icons.Default.Favorite, contentDescription = "Keep") },
                text = { Text("保留") }
            )
        }
    }
}