package com.example.photocleaner.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    if (showPrivacyPolicy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicy = false })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 顶部 Header ---
            Spacer(modifier = Modifier.height(24.dp))
            // App 图标 (占位)
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 6.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "清图",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            // --- 内容区域 ---

            // 卡片 1：应用信息
            InfoCard(title = "简介") {
                ListItem(
                    headlineContent = { Text("让相册回归清爽") },
                    supportingContent = { Text("高效的照片清理工具") },
                    leadingContent = { Icon(Icons.Filled.Image, contentDescription = null) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 卡片 2：开发者与反馈
            InfoCard(title = "开发者") {
                // GitHub
                ListItem(
                    headlineContent = { Text("GitHub") },
                    supportingContent = { Text("https://github.com/lioez") },
                    leadingContent = { Icon(Icons.Filled.Code, contentDescription = null) },
                    modifier = Modifier.clickable {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/lioez"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // 防止没有浏览器的情况
                        }
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                // 分享
                ListItem(
                    headlineContent = { Text("分享给朋友") },
                    leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "发现一个好用的照片清理 App：清图，快来试试吧！")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 卡片 3：法律信息
            InfoCard(title = "其他") {
                ListItem(
                    headlineContent = { Text("隐私政策") },
                    leadingContent = { Icon(Icons.Filled.Shield, contentDescription = null) },
                    modifier = Modifier.clickable {
                        showPrivacyPolicy = true
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(32.dp))

            // --- 底部 Footer ---
            Text(
                text = "Made with ❤️ by loez",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "© 2025",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PrivacyPolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私政策") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = """
                        生效日期：2025年12月16日

                        1. 引言
                        清图（以下简称“本应用”）非常重视您的隐私。本应用由独立开发者 loez 开发。

                        2. 数据收集与使用
                        本应用是一款本地工具类应用。
                        • 照片数据：本应用仅在您的设备本地读取和处理您的照片。我们不会上传、存储或分享您的任何照片至任何服务器。
                        • 个人信息：我们不收集任何个人身份信息。

                        3. 权限说明
                        为了提供核心功能，本应用需要以下权限：
                        • 存储权限：用于读取相册列表以及执行您确认的删除操作。

                        4. 第三方服务
                        本应用目前不包含任何第三方广告 SDK 或数据分析 SDK。

                        5. 联系我们
                        如有疑问，请通过 GitHub (https://github.com/lioez) 联系开发者。
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(content = content)
        }
    }
}
