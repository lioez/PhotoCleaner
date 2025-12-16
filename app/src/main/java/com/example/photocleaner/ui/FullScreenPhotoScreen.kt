package com.example.photocleaner.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.photocleaner.data.Photo
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage

@Composable
fun FullScreenPhotoScreen(
    photo: Photo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // 进入全屏时隐藏系统栏，退出时恢复
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            // 隐藏状态栏和导航栏
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            // 设置交互模式：滑动边缘时临时显示系统栏
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                // 恢复显示
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 使用 Telephoto 库实现高性能的缩放体验
        ZoomableAsyncImage(
            model = photo.uri,
            contentDescription = "Full Screen Image",
            modifier = Modifier.fillMaxSize(),
            onClick = { onDismiss() } // 点击图片也可以退出
        )
    }
}
