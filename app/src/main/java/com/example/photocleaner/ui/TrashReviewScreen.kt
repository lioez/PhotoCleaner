package com.example.photocleaner.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photocleaner.viewmodel.CleanerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashReviewScreen(
    viewModel: CleanerViewModel,
    onBack: () -> Unit,
    onOpenSystemTrash: () -> Unit
) {
    // 每次进入页面时刷新数据
    LaunchedEffect(Unit) {
        viewModel.refreshTrashList()
    }

    val pendingList = viewModel.pendingDeleteList
    val context = LocalContext.current

    // 控制自定义确认弹窗的显示
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isPermanentDeleteChecked by remember { mutableStateOf(false) }

    // 处理删除确认弹窗
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onDeleteSuccess()
            val msg = if (isPermanentDeleteChecked) "已永久删除" else "已移入系统回收站"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else {
            viewModel.resetDeleteState()
        }
    }

    val intentSender = viewModel.deleteIntentSender
    LaunchedEffect(intentSender) {
        if (intentSender != null) {
            val request = IntentSenderRequest.Builder(intentSender).build()
            deleteLauncher.launch(request)
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认删除") },
            text = {
                Column {
                    Text("确定要删除这 ${pendingList.size} 张照片吗？")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isPermanentDeleteChecked = !isPermanentDeleteChecked }
                    ) {
                        Checkbox(
                            checked = isPermanentDeleteChecked,
                            onCheckedChange = { isPermanentDeleteChecked = it }
                        )
                        Text("永久删除 (不进入回收站)")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.confirmDelete(isPermanentDeleteChecked)
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("待删除 (${pendingList.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 系统回收站入口
                    TextButton(onClick = onOpenSystemTrash) {
                        Text("系统回收站", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            if (pendingList.isNotEmpty()) {
                Button(
                    onClick = {
                        // 打开自定义确认弹窗
                        isPermanentDeleteChecked = false // 默认不勾选
                        showConfirmDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除全部")
                }
            }
        }
    ) { innerPadding ->
        if (pendingList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("垃圾桶是空的", style = MaterialTheme.typography.bodyLarge)
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
                items(pendingList, key = { it.id }) { photo ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(Color.LightGray)
                            .clickable {
                                // 点击照片将其移出删除列表（恢复）
                                viewModel.restorePhoto(photo)
                            }
                    ) {
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // 覆盖一个“恢复”提示
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("点击恢复", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
