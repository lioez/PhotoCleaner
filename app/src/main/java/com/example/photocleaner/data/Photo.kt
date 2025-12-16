package com.example.photocleaner.data
import android.net.Uri

/**
 * 数据模型类，代表一张照片。
 *
 * @property id 照片在 MediaStore 中的唯一 ID。
 * @property uri 照片的访问 URI，用于加载图片。
 * @property dateTaken 照片拍摄时间的时间戳（可选）。
 * @property name 照片的文件名（可选）。
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val dateTaken: Long? = null,
    val name: String? = null
)