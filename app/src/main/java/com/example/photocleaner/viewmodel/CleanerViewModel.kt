package com.example.photocleaner.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import coil.request.ImageRequest
import com.example.photocleaner.data.Photo
import com.example.photocleaner.data.PhotoRepository
import kotlinx.coroutines.launch

/**
 * UI 状态接口，定义了屏幕可能处于的不同状态。
 */
sealed interface UiState {
    /** 正在加载数据 */
    object Loading : UiState
    /** 没有照片数据 */
    object Empty : UiState
    /** 数据准备就绪，显示当前照片 */
    data class Ready(val currentPhoto: Photo?) : UiState
}

/**
 * 清理功能的 ViewModel，负责管理照片数据和业务逻辑。
 * 继承自 AndroidViewModel 以获取 Application 上下文。
 */
class CleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PhotoRepository(application)
    // 用于存储待处理照片 ID 的队列
    private val globalIdPool = ArrayDeque<Long>()

    // 预加载配置
    private val PRELOAD_BUFFER_SIZE = 20 // 缓存池大小
    private val REFILL_THRESHOLD = 5     // 触发补充的阈值
    private val PRELOAD_BITMAP_COUNT = 8 // 提前渲染图片的数量

    // 待显示照片缓冲队列
    private val preloadBuffer = ArrayDeque<Photo>()

    // 待删除列表 (使用 mutableStateListOf 以便 UI 自动更新)
    private val _pendingDeleteList = androidx.compose.runtime.mutableStateListOf<Photo>()
    val pendingDeleteList: List<Photo> = _pendingDeleteList

    // 使用 Compose 的 State 来持有 UI 状态，以便 UI 自动重组
    var uiState: UiState by mutableStateOf(UiState.Loading)
        private set

    // 用于触发删除确认弹窗的 IntentSender
    var deleteIntentSender: IntentSender? by mutableStateOf(null)
        private set

    // 系统回收站列表
    private val _systemTrashList = androidx.compose.runtime.mutableStateListOf<Photo>()
    val systemTrashList: List<Photo> = _systemTrashList

    // 操作历史栈 (用于撤销)
    private val actionHistory = ArrayDeque<Pair<Photo, Boolean>>() // Pair<Photo, isLeftSwipe>

    // 统计数据
    var totalPhotosCount by mutableStateOf(0)
        private set
    var processedCount by mutableStateOf(0)
        private set

    init {
        loadAndShufflePhotos()
    }

    /**
     * 初始化加载照片 ID，并进行随机打乱。
     */
    fun loadAndShufflePhotos() {
        viewModelScope.launch {
            uiState = UiState.Loading
            actionHistory.clear()
            processedCount = 0
            preloadBuffer.clear()

            // 从仓库获取所有图片 ID
            val allIds = repository.getAllImageIds()
            // 获取本地回收站的 ID
            val trashedIds = repository.getLocalTrashedIds()

            // 过滤掉已经在回收站的照片
            val availableIds = allIds.filter { !trashedIds.contains(it) }

            totalPhotosCount = availableIds.size

            if (availableIds.isEmpty()) {
                uiState = UiState.Empty
                return@launch
            }

            // 打乱顺序，增加趣味性
            val shuffledIds = availableIds.toMutableList()
            shuffledIds.shuffle()
            globalIdPool.clear()
            globalIdPool.addAll(shuffledIds)

            // 初始填充缓冲区
            fillPreloadBuffer()

            // 加载第一张照片
            loadNextPhoto()
        }
    }

    /**
     * 填充预加载缓冲区，并触发图片预加载
     */
    private fun fillPreloadBuffer() {
        // 1. 补充 Photo 对象到缓冲区
        while (preloadBuffer.size < PRELOAD_BUFFER_SIZE && globalIdPool.isNotEmpty()) {
            val nextId = globalIdPool.removeFirst()
            val uri = repository.getUriForId(nextId)
            preloadBuffer.addLast(Photo(id = nextId, uri = uri))
        }

        // 2. 对缓冲区前几张进行图片预加载 (Pre-render)
        preloadBuffer.take(PRELOAD_BITMAP_COUNT).forEach { photo ->
            val request = ImageRequest.Builder(getApplication())
                .data(photo.uri)
                .build()
            getApplication<Application>().imageLoader.enqueue(request)
        }
    }

    /**
     * 刷新待删除列表（从本地存储加载）。
     * 每次进入垃圾桶页面时调用。
     */
    fun refreshTrashList() {
        _pendingDeleteList.clear()
        val trashedIds = repository.getLocalTrashedIds()
        trashedIds.forEach { id ->
            val uri = repository.getUriForId(id)
            _pendingDeleteList.add(Photo(id = id, uri = uri))
        }
    }

    /**
     * 从队列中取出下一张照片并更新 UI 状态。
     */
    fun loadNextPhoto() {
        // 检查是否需要补充
        if (preloadBuffer.size < REFILL_THRESHOLD) {
            fillPreloadBuffer()
        }

        if (preloadBuffer.isEmpty()) {
             // 尝试最后一次补充（处理边界情况）
             fillPreloadBuffer()
             if (preloadBuffer.isEmpty()) {
                 uiState = UiState.Empty
                 return
             }
        }

        val nextPhoto = preloadBuffer.removeFirst()
        uiState = UiState.Ready(currentPhoto = nextPhoto)

        // 再次检查补充，确保后台始终有存货
        if (preloadBuffer.size < REFILL_THRESHOLD) {
            fillPreloadBuffer()
        }
    }

    /**
     * 处理左滑操作（例如：删除/跳过）。
     */
    fun swipeLeft(photo: Photo) {
        // 记录历史
        actionHistory.addLast(photo to true)
        processedCount++
        // 存入本地回收站
        repository.addToLocalTrash(photo.id)
        // 同时也更新内存中的列表（虽然 refreshTrashList 会重载，但保持同步是个好习惯）
        _pendingDeleteList.add(photo)
        loadNextPhoto()
    }

    /**
     * 处理右滑操作（例如：保留）。
     */
    fun swipeRight(photo: Photo) {
        // 记录历史
        actionHistory.addLast(photo to false)
        processedCount++
        loadNextPhoto()
    }

    /**
     * 撤销上一次操作
     */
    fun undo() {
        if (actionHistory.isEmpty()) return

        val (lastPhoto, wasLeftSwipe) = actionHistory.removeLast()
        processedCount--

        // 如果上一步是左滑（删除），需要从回收站恢复
        if (wasLeftSwipe) {
            repository.removeFromLocalTrash(lastPhoto.id)
            _pendingDeleteList.remove(lastPhoto)
        }

        // 将当前正在显示的照片（如果有）放回池子头部
        if (uiState is UiState.Ready) {
            val current = (uiState as UiState.Ready).currentPhoto
            if (current != null) {
                // 放回缓冲区头部，而不是 globalIdPool
                preloadBuffer.addFirst(current)
            }
        }

        // 恢复上一步的照片
        uiState = UiState.Ready(lastPhoto)
    }

    /**
     * 从待删除列表中移除照片（撤销删除）。
     */
    fun restorePhoto(photo: Photo) {
        repository.removeFromLocalTrash(photo.id)
        _pendingDeleteList.remove(photo)
    }

    /**
     * 确认删除所有待删除的照片。
     * @param permanentDelete 是否永久删除（true=物理删除，false=移入系统回收站）
     */
    fun confirmDelete(permanentDelete: Boolean) {
        viewModelScope.launch {
            val intentSender = if (permanentDelete) {
                repository.deletePhotos(pendingDeleteList)
            } else {
                repository.trashPhotos(pendingDeleteList)
            }

            if (intentSender != null) {
                deleteIntentSender = intentSender
            } else {
                onDeleteSuccess()
            }
        }
    }

    /**
     * 加载系统回收站内容
     */
    fun loadSystemTrash() {
        viewModelScope.launch {
            _systemTrashList.clear()
            _systemTrashList.addAll(repository.getSystemTrashPhotos())
        }
    }

    /**
     * 从系统回收站恢复照片
     */
    fun restoreFromSystemTrash(photos: List<Photo>) {
        viewModelScope.launch {
            val intentSender = repository.restoreFromSystemTrash(photos)
            if (intentSender != null) {
                deleteIntentSender = intentSender
            } else {
                loadSystemTrash() // 刷新列表
            }
        }
    }

    /**
     * 从系统回收站永久删除照片
     */
    fun deleteFromSystemTrash(photos: List<Photo>) {
        viewModelScope.launch {
            val intentSender = repository.deletePhotos(photos)
            if (intentSender != null) {
                deleteIntentSender = intentSender
            } else {
                loadSystemTrash() // 刷新列表
            }
        }
    }

    /**
     * 当用户在系统弹窗中确认删除后调用。
     */
    fun onDeleteSuccess() {
        // 从本地回收站记录中清除这些 ID
        val idsToDelete = pendingDeleteList.map { it.id }
        repository.clearLocalTrash(idsToDelete)

        _pendingDeleteList.clear()
        deleteIntentSender = null

        // 如果是在系统回收站页面操作成功，也刷新一下
        loadSystemTrash()
    }

    /**
     * 重置删除请求状态（例如用户取消了弹窗）。
     */
    fun resetDeleteState() {
        deleteIntentSender = null
    }
}
