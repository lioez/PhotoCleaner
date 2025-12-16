package com.example.photocleaner.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据仓库类，负责与 Android 系统的 MediaStore 进行交互，获取照片数据。
 */
class PhotoRepository(private val context: Context) {

    /**
     * 挂起函数，从设备的外部存储中查询所有图片的 ID。
     * 使用 Dispatchers.IO 在后台线程执行，避免阻塞主线程。
     *
     * @return 包含所有图片 ID 的列表，按添加时间降序排列。
     */
    suspend fun getAllImageIds(): List<Long> = withContext(Dispatchers.IO) {
        val idList = mutableListOf<Long>()
        // 定义查询的 URI，这里是外部存储的图片
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        // 定义需要查询的列，这里只需要 ID
        val projection = arrayOf(MediaStore.Images.Media._ID)
        // 定义排序方式，按添加时间降序（最新的在前）
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection, projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                idList.add(cursor.getLong(idColumn))
            }
        }
        return@withContext idList
    }

    /**
     * 根据图片的 ID 构建其完整的 URI。
     *
     * @param id 图片的 ID。
     * @return 图片的 URI。
     */
    fun getUriForId(id: Long): android.net.Uri {
        return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
    }
}