package com.example.photocleaner.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
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

@Composable
fun SwipeScreen(
    viewModel: CleanerViewModel,
    onTrashClick: () -> Unit
) {
    val state = viewModel.uiState
    val pendingDeleteCount = viewModel.pendingDeleteList.size

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(), // 统一处理系统栏边距
        contentAlignment = Alignment.Center
    ) {
        // 顶部栏：显示垃圾桶图标和计数
        if (state is UiState.Ready || state is UiState.Empty) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    // .statusBarsPadding() // 父容器已经处理了，这里不需要了
                    .zIndex(1f) // 确保显示在最上层
            ) {
                FilledTonalButton(
                    onClick = onTrashClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "$pendingDeleteCount")
                }
            }
        }

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
                            onSwipeRight = { viewModel.swipeRight(photo) }
                        )
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
    onSwipeRight: () -> Unit
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
                        offsetX.animateTo(-screenWidthPx * 1.5f)
                        onSwipeLeft()
                    }
                },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                icon = { Icon(Icons.Default.Close, contentDescription = "Delete") },
                text = { Text("删除") }
            )

            ExtendedFloatingActionButton(
                onClick = {
                    scope.launch {
                        offsetX.animateTo(screenWidthPx * 1.5f)
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