package com.yhchat.canary.data.model

/**
 * 草稿数据类
 */
data class Draft(
    val id: String,
    val title: String,
    val content: String,
    val boardId: Int,
    val boardName: String,
    val isMarkdownMode: Boolean,
    val createTime: Long,
    val updateTime: Long
)
