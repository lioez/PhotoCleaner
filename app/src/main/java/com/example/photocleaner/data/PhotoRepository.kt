package com.example.photocleaner.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 数据仓库类，负责与 Android 系统的 MediaStore 进行交互，获取照片数据。
 */
class PhotoRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("app_trash_prefs", Context.MODE_PRIVATE)
    private val TRASH_KEY = "trashed_photo_ids"

    /**
     * 获取本地回收站中的所有照片 ID。
     */
    fun getLocalTrashedIds(): Set<Long> {
        val stringSet = prefs.getStringSet(TRASH_KEY, emptySet()) ?: emptySet()
        return stringSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    /**
     * 将照片 ID 加入本地回收站。
     */
    fun addToLocalTrash(id: Long) {
        val current = getLocalTrashedIds().toMutableSet()
        current.add(id)
        prefs.edit().putStringSet(TRASH_KEY, current.map { it.toString() }.toSet()).apply()
    }

    /**
     * 从本地回收站移除照片 ID（恢复）。
     */
    fun removeFromLocalTrash(id: Long) {
        val current = getLocalTrashedIds().toMutableSet()
        current.remove(id)
        prefs.edit().putStringSet(TRASH_KEY, current.map { it.toString() }.toSet()).apply()
    }

    /**
     * 清空本地回收站记录（通常在物理删除成功后调用）。
     */
    fun clearLocalTrash(ids: List<Long>) {
        val current = getLocalTrashedIds().toMutableSet()
        current.removeAll(ids.toSet())
        prefs.edit().putStringSet(TRASH_KEY, current.map { it.toString() }.toSet()).apply()
    }

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

    /**
     * 尝试将照片移入系统回收站（Android 11+）。
     * Android 10 及以下直接物理删除。
     */
    suspend fun trashPhotos(photos: List<Photo>): IntentSender? = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) return@withContext null
        val uris = photos.map { it.uri }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // true = 移入回收站
            val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, true)
            return@withContext pendingIntent.intentSender
        } else {
            // Android 10- 只能直接删除
            deletePhotosDirectly(uris)
            return@withContext null
        }
    }

    /**
     * 尝试物理删除照片（不进回收站）。
     */
    suspend fun deletePhotos(photos: List<Photo>): IntentSender? = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) return@withContext null
        val uris = photos.map { it.uri }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // createDeleteRequest = 物理删除
            val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
            return@withContext pendingIntent.intentSender
        } else {
            deletePhotosDirectly(uris)
            return@withContext null
        }
    }

    /**
     * 恢复系统回收站中的照片（Android 11+）。
     */
    suspend fun restoreFromSystemTrash(photos: List<Photo>): IntentSender? = withContext(Dispatchers.IO) {
        if (photos.isEmpty()) return@withContext null
        val uris = photos.map { it.uri }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // false = 从回收站恢复
            val pendingIntent = MediaStore.createTrashRequest(context.contentResolver, uris, false)
            return@withContext pendingIntent.intentSender
        }
        return@withContext null
    }

    private fun deletePhotosDirectly(uris: List<android.net.Uri>) {
        for (uri in uris) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取系统回收站中的照片（Android 11+）。
     * 包含过期时间信息。
     */
    suspend fun getSystemTrashPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()

        val trashList = mutableListOf<Photo>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_EXPIRES // 回收站过期时间
        )

        // 关键：告诉 MediaStore 我们要查询回收站里的东西
        val bundle = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
        }

        try {
            context.contentResolver.query(
                collection, projection, bundle, null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val expiresColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_EXPIRES)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    // DATE_EXPIRES 是秒级时间戳，表示何时会被永久删除
                    val dateExpires = cursor.getLong(expiresColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    // 我们复用 Photo 对象，把过期时间暂存在 dateTaken 字段里（或者新建一个字段，这里为了省事复用）
                    // 注意：DATE_EXPIRES 是“过期那一刻的时间戳”，不是“剩余时间”
                    trashList.add(Photo(id = id, uri = uri, dateTaken = dateExpires))
                }
            }
        } catch (e: SecurityException) {
            // 如果没有权限查看回收站（通常只能看自己 App 删的），可能会抛异常或返回空
            e.printStackTrace()
        }
        return@withContext trashList
    }
}