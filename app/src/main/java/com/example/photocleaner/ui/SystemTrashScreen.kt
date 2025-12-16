package com.example.photocleaner.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.photocleaner.data.Photo
import com.example.photocleaner.viewmodel.CleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemTrashScreen(
    viewModel: CleanerViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadSystemTrash()
    }

    val trashList = viewModel.systemTrashList
    val context = LocalContext.current

    // 多选状态
    val selectedPhotos = remember { mutableStateListOf<Photo>() }

    // 处理系统弹窗
    val actionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeleteSuccess() // 复用成功回调来刷新列表
            selectedPhotos.clear()
            Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.resetDeleteState()
        }
    }

    val intentSender = viewModel.deleteIntentSender
    LaunchedEffect(intentSender) {
        if (intentSender != null) {
            val request = IntentSenderRequest.Builder(intentSender).build()
            actionLauncher.launch(request)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统回收站 (${trashList.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            if (selectedPhotos.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.restoreFromSystemTrash(selectedPhotos) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("恢复")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { viewModel.deleteFromSystemTrash(selectedPhotos) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("彻底删除")
                    }
                }
            }
        }
    ) { innerPadding ->
        if (trashList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("系统回收站是空的", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashList, key = { it.id }) { photo ->
                    val isSelected = selectedPhotos.contains(photo)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.LightGray)
                            .clickable {
                                if (isSelected) selectedPhotos.remove(photo) else selectedPhotos.add(photo)
                            }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(photo.uri)
                                .crossfade(true)
                                // 限制加载尺寸，避免加载原图导致卡顿
                                .size(300, 300)
                                .scale(Scale.FILL)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = if (isSelected) 0.5f else 1f
                        )

                        // 显示过期时间 (dateTaken 字段被复用为 dateExpires)
                        photo.dateTaken?.let { expiresTimestamp ->
                            // 计算剩余天数
                            val now = System.currentTimeMillis() / 1000
                            val daysLeft = ((expiresTimestamp - now) / (60 * 60 * 24)).coerceAtLeast(0)

                            Text(
                                text = "${daysLeft}天后删除",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(4.dp)
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Refresh, // 勾选图标
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}