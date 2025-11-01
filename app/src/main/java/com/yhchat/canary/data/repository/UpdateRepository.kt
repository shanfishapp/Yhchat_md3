package com.yhchat.canary.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.yhchat.canary.data.api.UpdateService
import com.yhchat.canary.data.model.UpdateInfo
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 更新检查仓库
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "update_prefs"
        private const val KEY_CURRENT_VERSION = "current_version"
        private const val GITHUB_API_BASE_URL = "https://api.github.com/"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val updateService: UpdateService by lazy {
        Retrofit.Builder()
            .baseUrl(GITHUB_API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateService::class.java)
    }
    
    /**
     * 获取当前版本
     */
    fun getCurrentVersion(defaultVersion: String): String {
        return prefs.getString(KEY_CURRENT_VERSION, defaultVersion) ?: defaultVersion
    }
    
    /**
     * 设置当前版本
     */
    fun setCurrentVersion(version: String) {
        prefs.edit().putString(KEY_CURRENT_VERSION, version).apply()
    }
    
    /**
     * 检查更新
     */
    suspend fun checkForUpdate(isLatestBuildPreview: Boolean = false): Result<UpdateInfo> {
        return try {
            val response = updateService.getReleases()
            
            if (response.isSuccessful) {
                val releases = response.body()
                if (releases.isNullOrEmpty()) {
                    return Result.failure(Exception("没有找到发布版本"))
                }
                
                val latestRelease = releases.first()
                // 这里需要传入默认版本，但在这个方法中我们无法直接访问 AppInfoActivity
                // 所以我们需要通过参数传递或者其他方式获取
                val currentVersion = getCurrentVersion(com.yhchat.canary.ui.settings.AppInfoActivity.DEFAULT_VERSION_TAG)
                
                // 检查是否有更新
                val hasUpdate = if (isLatestBuildPreview) {
                    // 如果是最新构建预览版，总是显示有更新
                    true
                } else {
                    // 正常检查更新逻辑
                    latestRelease.tagName != currentVersion
                }
                
                // 查找 APK 文件
                val apkAsset = latestRelease.assets.find { asset ->
                    asset.name.endsWith(".apk") && asset.contentType == "application/vnd.android.package-archive"
                }
                
                if (apkAsset == null) {
                    return Result.failure(Exception("没有找到 APK 文件"))
                }
                
                // 格式化发布时间
                val publishTime = formatPublishTime(latestRelease.publishedAt)
                
                val updateInfo = UpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersion = latestRelease.tagName,
                    updateTitle = latestRelease.name,
                    updateContent = latestRelease.body,
                    publishTime = publishTime,
                    downloadUrl = apkAsset.browserDownloadUrl
                )
                
                Result.success(updateInfo)
            } else {
                Result.failure(Exception("请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 格式化发布时间
     */
    private fun formatPublishTime(publishedAt: String): String {
        return try {
            // GitHub API 返回的时间格式: 2025-10-29T15:02:29Z
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            val date = inputFormat.parse(publishedAt)
            date?.let { outputFormat.format(it) } ?: publishedAt
        } catch (e: Exception) {
            publishedAt
        }
    }
}