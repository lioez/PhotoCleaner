package com.example.photocleaner.viewmodel

import android.app.Application
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
            val ids = repository.getAllImageIds()

            if (ids.isEmpty()) {
                uiState = UiState.Empty
                return@launch
            }

            // 打乱顺序，增加趣味性
            val shuffledIds = ids.toMutableList()
            shuffledIds.shuffle()
            globalIdPool.addAll(shuffledIds)
            // 加载第一张照片
            loadNextPhoto()
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
    fun swipeLeft(photo: Photo) { loadNextPhoto() }

    /**
     * 处理右滑操作（例如：保留）。
     */
    fun swipeRight(photo: Photo) { loadNextPhoto() }
}
