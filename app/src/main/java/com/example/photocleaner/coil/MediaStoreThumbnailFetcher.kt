package com.example.photocleaner.coil

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Dimension
import coil.size.Size as CoilSize

/**
 * 自定义 Coil Fetcher，用于使用 Android 系统 API (loadThumbnail) 高效加载 MediaStore 图片缩略图。
 * 这可以显著减少加载大图时的内存占用和卡顿。
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        // 只处理 content:// 类型的 URI
        if (data.scheme != ContentResolver.SCHEME_CONTENT) return null

        // 1. 计算需要的大小
        val width: Int
        val height: Int

        val size = options.size
        if (size == CoilSize.ORIGINAL) {
             // 如果请求的是原始大小（通常是全屏查看），则不使用缩略图加载器，
             // 而是返回 null，让 Coil 使用默认的 ContentUriFetcher 加载原图。
             return null
        } else {
             // 这里的 size 是 CoilSize (Pixel or Original)
             // 我们需要安全地提取像素值
             width = (size.width as? Dimension.Pixels)?.px ?: 0
             height = (size.height as? Dimension.Pixels)?.px ?: 0

             // 如果请求的尺寸非常大（例如超过 1000px），也建议直接加载原图，
             // 因为 loadThumbnail 生成大图的效率优势不明显，且质量可能不如原图解码。
             if (width > 1000 || height > 1000) {
                 return null
             }

             // 如果无法确定尺寸，也回退到默认加载器
             if (width <= 0 || height <= 0) {
                 return null
             }
        }

        return try {
            // API 29+ (Android 10+): 使用 loadThumbnail
            // 这是一个非常高效的系统 API，直接返回缩略图，无需解码原图
            val thumbnail = context.contentResolver.loadThumbnail(
                data,
                Size(width, height),
                null
            )

            DrawableResult(
                drawable = thumbnail.toDrawable(context.resources),
                isSampled = true,
                dataSource = DataSource.DISK
            )
        } catch (_: Exception) {
            // 如果加载失败（例如文件不存在），返回 null 让 Coil 尝试默认方式
            null
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // 只有 content:// URI 才使用此 Fetcher
            if (data.scheme == ContentResolver.SCHEME_CONTENT) {
                return MediaStoreThumbnailFetcher(context, data, options)
            }
            return null
        }
    }
}