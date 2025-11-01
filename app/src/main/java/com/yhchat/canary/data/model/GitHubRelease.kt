package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * GitHub Release 数据模型
 */
data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("body")
    val body: String,
    
    @SerializedName("published_at")
    val publishedAt: String,
    
    @SerializedName("assets")
    val assets: List<GitHubAsset>
)

/**
 * GitHub Release Asset 数据模型
 */
data class GitHubAsset(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    
    @SerializedName("content_type")
    val contentType: String,
    
    @SerializedName("size")
    val size: Long
)

/**
 * 更新信息
 */
data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val updateTitle: String,
    val updateContent: String,
    val publishTime: String,
    val downloadUrl: String
)