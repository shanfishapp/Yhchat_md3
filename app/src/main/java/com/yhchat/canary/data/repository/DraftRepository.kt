package com.yhchat.canary.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yhchat.canary.data.model.Draft
import java.util.*

/**
 * 草稿存储管理类
 */
class DraftRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("drafts", Context.MODE_PRIVATE)
    
    private val gson = Gson()
    
    companion object {
        private const val DRAFTS_KEY = "drafts_list"
    }
    
    /**
     * 保存草稿
     */
    fun saveDraft(
        title: String,
        content: String,
        boardId: Int,
        boardName: String,
        isMarkdownMode: Boolean
    ): String {
        val drafts = getDrafts().toMutableList()
        val draftId = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        
        val draft = Draft(
            id = draftId,
            title = title,
            content = content,
            boardId = boardId,
            boardName = boardName,
            isMarkdownMode = isMarkdownMode,
            createTime = currentTime,
            updateTime = currentTime
        )
        
        drafts.add(0, draft) // 添加到列表最前面
        saveDrafts(drafts)
        return draftId
    }
    
    /**
     * 更新草稿
     */
    fun updateDraft(
        draftId: String,
        title: String,
        content: String,
        isMarkdownMode: Boolean
    ): Boolean {
        val drafts = getDrafts().toMutableList()
        val index = drafts.indexOfFirst { it.id == draftId }
        
        if (index != -1) {
            val oldDraft = drafts[index]
            val updatedDraft = oldDraft.copy(
                title = title,
                content = content,
                isMarkdownMode = isMarkdownMode,
                updateTime = System.currentTimeMillis()
            )
            
            drafts[index] = updatedDraft
            // 将更新的草稿移到最前面
            drafts.removeAt(index)
            drafts.add(0, updatedDraft)
            
            saveDrafts(drafts)
            return true
        }
        return false
    }
    
    /**
     * 获取所有草稿
     */
    fun getDrafts(): List<Draft> {
        val draftsJson = sharedPreferences.getString(DRAFTS_KEY, null)
        return if (draftsJson != null) {
            try {
                val type = object : TypeToken<List<Draft>>() {}.type
                gson.fromJson(draftsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * 根据ID获取草稿
     */
    fun getDraftById(draftId: String): Draft? {
        return getDrafts().find { it.id == draftId }
    }
    
    /**
     * 删除草稿
     */
    fun deleteDraft(draftId: String): Boolean {
        val drafts = getDrafts().toMutableList()
        val removed = drafts.removeAll { it.id == draftId }
        if (removed) {
            saveDrafts(drafts)
        }
        return removed
    }
    
    /**
     * 清空所有草稿
     */
    fun clearAllDrafts() {
        sharedPreferences.edit().remove(DRAFTS_KEY).apply()
    }
    
    /**
     * 保存草稿列表到SharedPreferences
     */
    private fun saveDrafts(drafts: List<Draft>) {
        val draftsJson = gson.toJson(drafts)
        sharedPreferences.edit().putString(DRAFTS_KEY, draftsJson).apply()
    }
    
    /**
     * 获取草稿数量
     */
    fun getDraftCount(): Int {
        return getDrafts().size
    }
    
    /**
     * 检查是否有草稿
     */
    fun hasDrafts(): Boolean {
        return getDraftCount() > 0
    }
}
