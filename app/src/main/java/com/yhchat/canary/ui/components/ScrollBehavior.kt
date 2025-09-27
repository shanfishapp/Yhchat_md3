package com.yhchat.canary.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 滚动行为检测工具类
 * 用于检测用户滚动行为并控制底部导航栏的显示/隐藏
 */
class ScrollBehavior {
    private var _isVisible = mutableStateOf(true)
    val isVisible: State<Boolean> = _isVisible
    
    private var lastScrollOffset = 0
    private var hideTimer: kotlinx.coroutines.Job? = null
    
    /**
     * 处理滚动偏移变化
     * @param scrollOffset 当前滚动偏移量
     * @param coroutineScope 协程作用域，用于延迟显示导航栏
     */
    fun handleScrollOffsetChange(
        scrollOffset: Int,
        coroutineScope: kotlinx.coroutines.CoroutineScope
    ) {
        val scrollDelta = scrollOffset - lastScrollOffset
        lastScrollOffset = scrollOffset
        
        // 如果滚动距离太小，不做处理
        if (kotlin.math.abs(scrollDelta) < 5) return
        
        // 向下滚动时隐藏导航栏
        if (scrollDelta > 0 && _isVisible.value) {
            _isVisible.value = false
            cancelHideTimer()
        }
        // 向上滚动时立即显示导航栏
        else if (scrollDelta < 0 && !_isVisible.value) {
            _isVisible.value = true
            cancelHideTimer()
        }
        
        // 停止滚动后延迟显示导航栏
        scheduleShowNavigation(coroutineScope)
    }
    
    /**
     * 强制显示导航栏
     */
    fun show() {
        _isVisible.value = true
        cancelHideTimer()
    }
    
    /**
     * 强制隐藏导航栏
     */
    fun hide() {
        _isVisible.value = false
        cancelHideTimer()
    }
    
    /**
     * 安排延迟显示导航栏
     */
    private fun scheduleShowNavigation(coroutineScope: kotlinx.coroutines.CoroutineScope) {
        cancelHideTimer()
        hideTimer = coroutineScope.launch {
            delay(2000) // 2秒后自动显示导航栏
            _isVisible.value = true
        }
    }
    
    /**
     * 取消延迟显示定时器
     */
    private fun cancelHideTimer() {
        hideTimer?.cancel()
        hideTimer = null
    }
}

/**
 * 创建滚动行为管理器的组合函数
 */
@Composable
fun rememberScrollBehavior(): ScrollBehavior {
    return remember { ScrollBehavior() }
}

/**
 * LazyListState 扩展函数，用于自动处理滚动行为
 */
@Composable
fun LazyListState.HandleScrollBehavior(
    scrollBehavior: ScrollBehavior,
    coroutineScope: kotlinx.coroutines.CoroutineScope = rememberCoroutineScope()
) {
    LaunchedEffect(this.firstVisibleItemScrollOffset, this.firstVisibleItemIndex) {
        val scrollOffset = this@HandleScrollBehavior.firstVisibleItemIndex * 1000 + 
                          this@HandleScrollBehavior.firstVisibleItemScrollOffset
        scrollBehavior.handleScrollOffsetChange(scrollOffset, coroutineScope)
    }
}