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
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.photocleaner.ui.SwipeScreen
import com.example.photocleaner.ui.theme.PhotoCleanerTheme
import com.example.photocleaner.viewmodel.CleanerViewModel

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
            PhotoCleanerTheme {
                // Scaffold 是标准的 Material3 页面骨架
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    // Box 是一个容器，用来应用 innerPadding
                    // 这样你的内容就不会被状态栏(电池图标)或底部导航条挡住
                    Box(modifier = Modifier.padding(innerPadding)) {

                        // 5. 这里原来是 Greeting(...)，现在换成我们的界面！
                        SwipeScreen(viewModel = viewModel)
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