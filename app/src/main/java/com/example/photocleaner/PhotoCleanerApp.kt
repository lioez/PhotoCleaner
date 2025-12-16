package com.example.photocleaner

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.photocleaner.coil.MediaStoreThumbnailFetcher

/**
 * 自定义 Application 类，用于全局配置 Coil 图片加载库。
 */
class PhotoCleanerApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                // 将我们自定义的 MediaStoreThumbnailFetcher 添加到最前面
                // 这样 Coil 在加载 content:// 图片时会优先尝试使用系统缩略图 API
                add(MediaStoreThumbnailFetcher.Factory(this@PhotoCleanerApp))
            }
            // 开启淡入淡出效果
            .crossfade(true)
            .build()
    }
}