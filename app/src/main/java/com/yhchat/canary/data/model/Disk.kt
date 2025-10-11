package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 创建文件夹请求
 */
data class CreateFolderRequest(
    @SerializedName("chatId")
    val chatId: String,  // 群聊id
    
    @SerializedName("chatType")
    val chatType: Int,  // 会话类型
    
    @SerializedName("folderName")
    val folderName: String,  // 文件夹名称
    
    @SerializedName("parentFolderId")
    val parentFolderId: Long = 0  // 父文件夹id，根目录为0
)

/**
 * 获取文件列表请求
 */
data class GetFileListRequest(
    @SerializedName("chatId")
    val chatId: String,
    
    @SerializedName("chatType")
    val chatType: Int,
    
    @SerializedName("folderId")
    val folderId: Long = 0,  // 文件夹id，根目录为0
    
    @SerializedName("sort")
    val sort: String = "name_asc"  // 排序方式
)

/**
 * 文件列表响应
 */
data class FileListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: FileListData,
    
    @SerializedName("msg")
    val msg: String
)

data class FileListData(
    @SerializedName("list")
    val list: List<DiskFile>
)

/**
 * 云盘文件/文件夹
 */
data class DiskFile(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,  // 文件/文件夹名称
    
    @SerializedName("fileSize")
    val fileSize: Long = 0,  // 文件大小
    
    @SerializedName("objectType")
    val objectType: Int,  // 对象类型：1-文件夹，2-文件
    
    @SerializedName("uploadTime")
    val uploadTime: Long = 0,  // 上传时间
    
    @SerializedName("uploadBy")
    val uploadBy: String = "",  // 上传者id
    
    @SerializedName("uploadByName")
    val uploadByName: String = "",  // 上传者名称
    
    @SerializedName("qiniuKey")
    val qiniuKey: String = ""  // 七牛云密钥
) {
    // 是否为文件夹
    fun isFolder(): Boolean = objectType == 1
    
    // 获取文件URL
    fun getFileUrl(): String {
        return if (qiniuKey.isNotEmpty()) {
            "https://chat-img.jwznb.com/$qiniuKey"
        } else {
            ""
        }
    }
    
    // 格式化文件大小
    fun getFormattedSize(): String {
        if (isFolder()) return "-"
        
        val kb = fileSize / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$fileSize B"
        }
    }
}

/**
 * 上传文件请求
 */
data class UploadFileRequest(
    @SerializedName("chatId")
    val chatId: String,
    
    @SerializedName("chatType")
    val chatType: Int,
    
    @SerializedName("fileSize")
    val fileSize: Long,  // 文件大小（KB）
    
    @SerializedName("fileName")
    val fileName: String,  // 文件名
    
    @SerializedName("fileMd5")
    val fileMd5: String,  // 文件MD5+扩展名
    
    @SerializedName("fileEtag")
    val fileEtag: String,  // Etag
    
    @SerializedName("qiniuKey")
    val qiniuKey: String,  // 七牛云key
    
    @SerializedName("folderId")
    val folderId: Long = 0  // 文件夹id，根目录为0
)

/**
 * 重命名文件请求
 */
data class RenameFileRequest(
    @SerializedName("id")
    val id: Long,  // 文件ID
    
    @SerializedName("objectType")
    val objectType: Int,  // 对象类型
    
    @SerializedName("name")
    val name: String  // 新文件名
)

/**
 * 删除文件请求
 */
data class RemoveFileRequest(
    @SerializedName("id")
    val id: Long,  // 文件ID
    
    @SerializedName("objectType")
    val objectType: Int  // 对象类型
)

