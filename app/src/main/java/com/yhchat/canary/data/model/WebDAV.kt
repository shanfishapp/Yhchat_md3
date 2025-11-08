package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * WebDAV 挂载设置请求
 */
data class MountSettingRequest(
    @SerializedName("groupId")
    val groupId: String,
    
    @SerializedName("encryptKey")
    val encryptKey: String,  // Base64 编码的 RSA 加密后的密钥
    
    @SerializedName("encryptIv")
    val encryptIv: String   // Base64 编码的 RSA 加密后的 IV
)

/**
 * WebDAV 挂载设置响应
 */
data class MountSettingResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: MountSettingData?,
    
    @SerializedName("msg")
    val msg: String
)

data class MountSettingData(
    @SerializedName("list")
    val list: List<MountSetting>
)

/**
 * WebDAV 挂载设置
 */
data class MountSetting(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("groupId")
    val groupId: String,
    
    @SerializedName("mountName")
    val mountName: String,  // 挂载名称，用于分类栏显示
    
    @SerializedName("webdavUrl")
    val webdavUrl: String,  // WebDAV URL
    
    @SerializedName("webdavUserName")
    val webdavUserName: String,  // WebDAV 用户名
    
    @SerializedName("webdavPassword")
    val webdavPassword: String,  // WebDAV 密码（可能为空，需要解密）
    
    @SerializedName("webdavRootPath")
    val webdavRootPath: String,  // WebDAV 根路径
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("delFlag")
    val delFlag: Int,
    
    @SerializedName("userId")
    val userId: String
)

/**
 * WebDAV 文件/文件夹（用于显示）
 */
data class WebDAVFile(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val mountSetting: MountSetting? = null
) {
    fun getFormattedSize(): String {
        if (isDirectory) return "-"
        
        val kb = size / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$size B"
        }
    }
}

