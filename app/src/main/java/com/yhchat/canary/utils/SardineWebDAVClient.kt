package com.yhchat.canary.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.yhchat.canary.data.model.MountSetting
import com.yhchat.canary.data.model.WebDAVFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * 基于 Sardine 库的 WebDAV 客户端工具类
 * 保留原有的解密逻辑，使用更成熟的 WebDAV 库实现完整功能
 */
object SardineWebDAVClient {
    
    private const val TAG = "SardineWebDAVClient"
    
    /**
     * 创建 Sardine WebDAV 客户端
     * @param mountSetting 已解密的挂载设置（包含解密后的用户名和密码）
     * @return Sardine 客户端实例
     */
    private fun createSardineClient(mountSetting: MountSetting): Sardine {
        return OkHttpSardine().apply {
            setCredentials(mountSetting.webdavUserName, mountSetting.webdavPassword)
        }
    }
    
    private fun resolvePath(mountSetting: MountSetting, relativePath: String): String {
        val normalizedRoot = mountSetting.webdavRootPath.trim('/')
        val normalizedRelative = relativePath.trim('/').takeIf { it.isNotEmpty() }?.split('/') ?: emptyList()
        val baseUrl = mountSetting.webdavUrl.trimEnd('/')
        val basePath = Uri.parse(baseUrl).path?.trim('/') ?: ""
        val baseSegments = if (basePath.isEmpty()) emptyList() else basePath.split('/')
        val baseContainsRoot = normalizedRoot.isNotEmpty() && baseSegments.lastOrNull() == normalizedRoot
        val segments = mutableListOf<String>()
        if (normalizedRoot.isNotEmpty() && !baseContainsRoot) {
            segments.add(normalizedRoot)
        }
        segments.addAll(normalizedRelative)
        return segments.joinToString("/")
    }
    
    /**
     * 测试 WebDAV 连接
     * @param mountSetting 已解密的挂载设置
     * @return 连接是否成功
     */
    suspend fun testConnection(mountSetting: MountSetting): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "测试WebDAV连接: ${mountSetting.webdavUrl}")
            
            val sardine = createSardineClient(mountSetting)
            val testUrl = buildUrl(mountSetting.webdavUrl, mountSetting.webdavRootPath)
            
            // 尝试列出根目录来测试连接
            sardine.exists(testUrl)
            
            Log.d(TAG, "WebDAV连接测试成功")
            true
        } catch (e: Exception) {
            Log.e(TAG, "WebDAV连接测试失败", e)
            false
        }
    }
    
    /**
     * 列出指定路径下的文件和文件夹
     * @param mountSetting 已解密的挂载设置
     * @param relativePath 相对于根路径的路径
     * @return WebDAV文件列表
     */
    suspend fun listFiles(
        mountSetting: MountSetting, 
        relativePath: String = ""
    ): Result<List<WebDAVFile>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "列出文件: ${mountSetting.mountName} - $relativePath")
            
            val sardine = createSardineClient(mountSetting)
            val fullPath = resolvePath(mountSetting, relativePath)
            val url = buildUrl(mountSetting.webdavUrl, fullPath)
            
            Log.d(TAG, "请求URL: $url")
            
            // 使用 Sardine 列出目录内容
            val resources: List<DavResource> = sardine.list(url)
            
            val files = resources.drop(1).mapNotNull { resource ->
                // 跳过当前目录项
                if (resource.path == fullPath || resource.path == "$fullPath/") {
                    return@mapNotNull null
                }
                
                // 解析文件名
                val fileName = resource.name ?: resource.path.substringAfterLast('/')
                if (fileName.isBlank()) return@mapNotNull null
                
                // 构建相对路径
                val fileRelativePath = if (relativePath.isEmpty()) {
                    fileName
                } else {
                    "$relativePath/$fileName"
                }
                
                WebDAVFile(
                    name = fileName,
                    path = fileRelativePath,
                    isDirectory = resource.isDirectory,
                    size = resource.contentLength ?: 0L,
                    lastModified = resource.modified?.time ?: 0L,
                    mountSetting = mountSetting
                )
            }
            
            Log.d(TAG, "成功列出 ${files.size} 个文件")
            Result.success(files)
            
        } catch (e: Exception) {
            Log.e(TAG, "列出文件失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 下载文件
     * @param context 上下文
     * @param file WebDAV文件信息
     * @param onProgress 下载进度回调 (已下载字节数, 总字节数)
     * @param onSuccess 下载成功回调 (本地文件路径)
     * @param onError 下载失败回调 (错误信息)
     */
    suspend fun downloadFile(
        context: Context,
        file: WebDAVFile,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            ensureActive() // 检查协程是否被取消
            
            Log.d(TAG, "开始下载文件: ${file.name}")
            
            val sardine = createSardineClient(file.mountSetting)
            val fullPath = resolvePath(file.mountSetting, file.path)
            val url = buildUrl(file.mountSetting.webdavUrl, fullPath)
            
            Log.d(TAG, "下载URL: $url")
            
            // 创建公共下载目录: /storage/emulated/0/Download/yhchat/
            val downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val downloadsDir = File(downloadsRoot, "yhchat")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            
            val localFile = File(downloadsDir, file.name)
            var tempFile: File? = null
            
            try {
                // 创建临时文件
                tempFile = File(downloadsDir, "${file.name}.tmp")
                
                // 使用 Sardine 下载文件
                sardine.get(url).use { inputStream ->
                    FileOutputStream(tempFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var totalBytesRead = 0L
                        var bytesRead: Int
                        
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            ensureActive() // 检查协程是否被取消
                            
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 报告进度
                            withContext(Dispatchers.Main) {
                                onProgress(totalBytesRead, file.size)
                            }
                        }
                    }
                }
                
                // 下载完成，重命名临时文件
                if (tempFile.renameTo(localFile)) {
                    Log.d(TAG, "文件下载成功: ${localFile.absolutePath}")
                    withContext(Dispatchers.Main) {
                        onSuccess(localFile.absolutePath)
                    }
                } else {
                    throw IOException("无法重命名临时文件")
                }
                
            } catch (e: CancellationException) {
                Log.d(TAG, "下载被取消: ${file.name}")
                tempFile?.delete()
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "下载过程中发生错误", e)
                tempFile?.delete()
                throw e
            }
            
        } catch (e: CancellationException) {
            // 协程被取消，不报告错误
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "下载文件失败: ${file.name}", e)
            withContext(Dispatchers.Main) {
                onError("下载失败: ${e.message}")
            }
        }
    }
    
    /**
     * 上传文件
     * @param localFile 本地文件
     * @param mountSetting 已解密的挂载设置
     * @param remotePath 远程路径（相对于根路径）
     * @param onProgress 上传进度回调 (已上传字节数, 总字节数)
     * @param onSuccess 上传成功回调
     * @param onError 上传失败回调 (错误信息)
     */
    suspend fun uploadFile(
        localFile: File,
        mountSetting: MountSetting,
        remotePath: String,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            ensureActive()
            
            Log.d(TAG, "开始上传文件: ${localFile.name} -> $remotePath")
            
            val sardine = createSardineClient(mountSetting)
            val fullPath = resolvePath(mountSetting, remotePath)
            val url = buildUrl(mountSetting.webdavUrl, fullPath)
            
            Log.d(TAG, "上传URL: $url")
            
            // 使用 Sardine 上传文件
            sardine.put(url, localFile, null as String?)
            
            Log.d(TAG, "文件上传成功: ${localFile.name}")
            onSuccess()
            
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "上传文件失败: ${localFile.name}", e)
            onError("上传失败: ${e.message}")
        }
    }
    
    /**
     * 创建目录
     * @param mountSetting 已解密的挂载设置
     * @param remotePath 远程路径（相对于根路径）
     * @return 是否创建成功
     */
    suspend fun createDirectory(
        mountSetting: MountSetting,
        remotePath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "创建目录: $remotePath")
            
            val sardine = createSardineClient(mountSetting)
            val fullPath = resolvePath(mountSetting, remotePath)
            val url = buildUrl(mountSetting.webdavUrl, fullPath)
            
            sardine.createDirectory(url)
            
            Log.d(TAG, "目录创建成功: $remotePath")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "创建目录失败: $remotePath", e)
            false
        }
    }
    
    /**
     * 删除文件或目录
     * @param mountSetting 已解密的挂载设置
     * @param remotePath 远程路径（相对于根路径）
     * @return 是否删除成功
     */
    suspend fun delete(
        mountSetting: MountSetting,
        remotePath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "删除: $remotePath")
            
            val sardine = createSardineClient(mountSetting)
            val fullPath = resolvePath(mountSetting, remotePath)
            val url = buildUrl(mountSetting.webdavUrl, fullPath)
            
            sardine.delete(url)
            
            Log.d(TAG, "删除成功: $remotePath")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "删除失败: $remotePath", e)
            false
        }
    }
    
    /**
     * 移动/重命名文件或目录
     * @param mountSetting 已解密的挂载设置
     * @param sourcePath 源路径（相对于根路径）
     * @param targetPath 目标路径（相对于根路径）
     * @return 是否移动成功
     */
    suspend fun move(
        mountSetting: MountSetting,
        sourcePath: String,
        targetPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "移动: $sourcePath -> $targetPath")
            
            val sardine = createSardineClient(mountSetting)
            val sourceFullPath = resolvePath(mountSetting, sourcePath)
            val targetFullPath = resolvePath(mountSetting, targetPath)
            val sourceUrl = buildUrl(mountSetting.webdavUrl, sourceFullPath)
            val targetUrl = buildUrl(mountSetting.webdavUrl, targetFullPath)
            
            sardine.move(sourceUrl, targetUrl)
            
            Log.d(TAG, "移动成功: $sourcePath -> $targetPath")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "移动失败: $sourcePath -> $targetPath", e)
            false
        }
    }
    
    /**
     * 构建完整的WebDAV URL
     */
    private fun buildUrl(baseUrl: String, path: String): String {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val cleanPath = path.trim('/').split('/').joinToString("/") { part ->
            URLEncoder.encode(part, StandardCharsets.UTF_8.toString())
        }
        
        return if (cleanPath.isEmpty()) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/$cleanPath"
        }
    }
    
}
