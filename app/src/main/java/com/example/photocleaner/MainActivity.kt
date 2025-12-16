package com.example.photocleaner

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels // 这一行如果爆红，看下面的“排错指南”
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.photocleaner.ui.SwipeScreen
import com.example.photocleaner.ui.theme.PhotoCleanerTheme
import com.example.photocleaner.viewmodel.CleanerViewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.photocleaner.ui.SystemTrashScreen
import com.example.photocleaner.ui.TrashReviewScreen
import com.example.photocleaner.ui.AboutScreen

import com.example.photocleaner.ui.FullScreenPhotoScreen
import com.example.photocleaner.data.Photo

import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {

    // 1. 初始化 ViewModel (我们的逻辑大脑)
    private val viewModel: CleanerViewModel by viewModels()

    // 2. 注册权限请求回调
    // 当用户点完“允许”或“拒绝”后，系统会调用这里
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 权限拿到了！通知 ViewModel 重新尝试加载照片
            viewModel.loadAndShufflePhotos()
            // 或者简单点，直接重建 Activity：
            // recreate()
        } else {
            // 用户拒绝了权限...
            // 实际开发中应该弹窗解释为什么需要权限，这里暂时不做处理
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 3. 开启全面屏模式 (保留模板自带的)
        enableEdgeToEdge()

        // 4. 检查并申请权限
        checkPermissions()

        setContent {
            // 主题模式状态：0=跟随系统, 1=浅色, 2=深色
            var themeMode by remember { mutableStateOf(0) }
            val isDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            PhotoCleanerTheme(darkTheme = isDarkTheme) {
                // 简单的导航状态管理
                // 0: 主页面 (SwipeScreen)
                // 1: 待删除页面 (TrashReviewScreen)
                // 2: 系统回收站页面 (SystemTrashScreen)
                // 3: 关于页面 (AboutScreen)
                // 4: 全屏查看 (FullScreenPhotoScreen)
                var currentScreen by remember { mutableStateOf(0) }
                var viewingPhoto by remember { mutableStateOf<Photo?>(null) }

                // 移除外层 Scaffold，让每个屏幕自己处理系统栏边距 (Edge-to-Edge)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        0 -> SwipeScreen(
                            viewModel = viewModel,
                            onTrashClick = { currentScreen = 1 },
                            onInfoClick = { currentScreen = 3 },
                            themeMode = themeMode,
                            onThemeChange = { themeMode = it },
                            onViewOriginal = { photo ->
                                viewingPhoto = photo
                                currentScreen = 4
                            }
                        )
                        1 -> TrashReviewScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = 0 },
                            onOpenSystemTrash = { currentScreen = 2 }
                        )
                        2 -> SystemTrashScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = 1 }
                        )
                        3 -> AboutScreen(
                            onBack = { currentScreen = 0 }
                        )
                        4 -> {
                            viewingPhoto?.let { photo ->
                                FullScreenPhotoScreen(
                                    photo = photo,
                                    onDismiss = { currentScreen = 0 }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissions() {
        // 简单的版本适配逻辑
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Android 12 及以下
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}