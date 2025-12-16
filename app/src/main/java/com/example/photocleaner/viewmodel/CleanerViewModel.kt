package com.example.photocleaner.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        loadAndShufflePhotos()
    }

    /**
     * 初始化加载照片 ID，并进行随机打乱。
     */
    fun loadAndShufflePhotos() {
        viewModelScope.launch {
            uiState = UiState.Loading
            // 从仓库获取所有图片 ID
            val allIds = repository.getAllImageIds()
            // 获取本地回收站的 ID
            val trashedIds = repository.getLocalTrashedIds()

            // 过滤掉已经在回收站的照片
            val availableIds = allIds.filter { !trashedIds.contains(it) }

            if (availableIds.isEmpty()) {
                uiState = UiState.Empty
                return@launch
            }

            // 打乱顺序，增加趣味性
            val shuffledIds = availableIds.toMutableList()
            shuffledIds.shuffle()
            globalIdPool.clear()
            globalIdPool.addAll(shuffledIds)
            // 加载第一张照片
            loadNextPhoto()
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
        if (globalIdPool.isEmpty()) {
            uiState = UiState.Empty
            return
        }
        val nextId = globalIdPool.removeFirst()
        val uri = repository.getUriForId(nextId)
        uiState = UiState.Ready(currentPhoto = Photo(id = nextId, uri = uri))
    }

    /**
     * 处理左滑操作（例如：删除/跳过）。
     */
    fun swipeLeft(photo: Photo) {
        // 存入本地回收站
        repository.addToLocalTrash(photo.id)
        // 同时也更新内存中的列表（虽然 refreshTrashList 会重载，但保持同步是个好习惯）
        _pendingDeleteList.add(photo)
        loadNextPhoto()
    }

    /**
     * 处理右滑操作（例如：保留）。
     */
    fun swipeRight(photo: Photo) { loadNextPhoto() }

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
