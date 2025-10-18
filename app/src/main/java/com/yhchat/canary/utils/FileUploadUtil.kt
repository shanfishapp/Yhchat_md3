package com.yhchat.canary.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.yhchat.canary.data.api.QiniuUploadResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 文件上传工具类
 * 实现文件上传到七牛云的完整流程
 */
object FileUploadUtil {
    
    private const val TAG = "FileUploadUtil"
    
    // 文件bucket名称
    private const val FILE_BUCKET = "chat68-file"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)  // 文件可能较大，增加超时时间
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    
    /**
     * 上传文件到七牛云
     * @param context 上下文
     * @param fileUri 文件URI
     * @param uploadToken 七牛上传token（从/v1/misc/qiniu-token2获取）
     * @return 上传结果，包含key、hash等信息
     */
    suspend fun uploadFile(
        context: Context,
        fileUri: Uri,
        uploadToken: String
    ): Result<QiniuUploadResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📤 ========== 开始上传文件 ==========")
            Log.d(TAG, "📤 文件URI: $fileUri")
            Log.d(TAG, "📤 上传Token: ${uploadToken.take(30)}...")
            
            // 1. 读取文件数据
            val inputStream = context.contentResolver.openInputStream(fileUri)
                ?: return@withContext Result.failure(Exception("无法读取文件"))
            
            val fileBytes = inputStream.readBytes()
            inputStream.close()
            
            val fileSizeBytes = fileBytes.size.toLong()
            Log.d(TAG, "✅ 文件读取成功，大小: $fileSizeBytes bytes (${fileSizeBytes / 1024}KB)")
            
            // 2. 计算MD5
            val md5 = calculateMD5(fileBytes)
            Log.d(TAG, "✅ MD5计算完成: $md5")
            
            // 3. 获取文件名和后缀
            val originalFileName = getFileName(context, fileUri) ?: "unnamed_file"
            val extension = getFileExtension(originalFileName)
            
            // 文件key = disk/MD5.扩展名（注意这里要加disk/前缀）
            val fileKey = "disk/$md5.$extension"
            Log.d(TAG, "✅ 原始文件名: $originalFileName")
            Log.d(TAG, "✅ 文件扩展名: $extension")
            Log.d(TAG, "✅ 文件key: $fileKey")
            
            // 4. 获取MIME类型
            val mimeType = context.contentResolver.getType(fileUri) 
                ?: getMimeTypeFromExtension(extension)
                ?: "application/octet-stream"
            Log.d(TAG, "✅ MIME类型: $mimeType")
            
            // 5. 保存文件到临时目录
            val cacheDir = context.cacheDir
            val tempFile = File(cacheDir, "$md5.$extension")
            FileOutputStream(tempFile).use { outputStream ->
                outputStream.write(fileBytes)
            }
            Log.d(TAG, "✅ 临时文件: ${tempFile.absolutePath}")
            
            // 6. 查询正确的上传host（参考Python SDK实现）
            Log.d(TAG, "📤 查询上传区域...")
            val ak = uploadToken.split(":")[0]
            val queryUrl = "https://api.qiniu.com/v4/query?ak=$ak&bucket=$FILE_BUCKET"
            Log.d(TAG, "📤 查询URL: $queryUrl")
            
            val queryRequest = Request.Builder()
                .url(queryUrl)
                .get()
                .build()
            
            val queryResponse = client.newCall(queryRequest).execute()
            val uploadHost = if (queryResponse.isSuccessful) {
                val queryJson = JSONObject(queryResponse.body?.string() ?: "{}")
                Log.d(TAG, "📥 区域查询响应: $queryJson")
                val hosts = queryJson.getJSONArray("hosts")
                val host = hosts.getJSONObject(0)
                val up = host.getJSONObject("up")
                val domains = up.getJSONArray("domains")
                val resultHost = domains.getString(0)
                Log.d(TAG, "✅ 上传host: $resultHost")
                resultHost
            } else {
                Log.w(TAG, "⚠️ 查询host失败，使用默认: upload-z2.qiniup.com")
                "upload-z2.qiniup.com"
            }
            
            // 7. 使用OkHttp的MultipartBody构建请求（自动处理正确的格式）
            Log.d(TAG, "📤 构建multipart/form-data请求体...")
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("token", uploadToken)
                .addFormDataPart("key", fileKey)
                .addFormDataPart(
                    "file",
                    originalFileName,
                    tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                )
                .build()
            
            Log.d(TAG, "✅ 请求体构建完成")
            
            val uploadUrl = "https://$uploadHost/"
            Log.d(TAG, "📤 上传URL: $uploadUrl")
            
            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("user-agent", "QiniuDart")
                .addHeader("accept-encoding", "gzip")
                .post(requestBody)
                .build()
            
            Log.d(TAG, "📤 开始上传到七牛云...")
            Log.d(TAG, "📤 请求头:")
            request.headers.forEach {
                Log.d(TAG, "   ${it.first}: ${it.second}")
            }
            
            // 9. 执行上传
            val response = client.newCall(request).execute()
            
            Log.d(TAG, "📥 七牛云响应码: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d(TAG, "✅ 上传成功！响应: $responseBody")
                
                if (responseBody != null) {
                    val json = JSONObject(responseBody)
                    
                    // 解析响应 - 包含key, hash, fsize
                    val uploadResponse = QiniuUploadResponse(
                        key = json.getString("key"),
                        hash = json.getString("hash"),  // 这个hash就是fileEtag
                        fsize = json.getLong("fsize"),
                        avinfo = null  // 文件没有avinfo
                    )
                    
                    Log.d(TAG, "✅ ========== 上传完成 ==========")
                    Log.d(TAG, "✅ key: ${uploadResponse.key}")
                    Log.d(TAG, "✅ hash (etag): ${uploadResponse.hash}")
                    Log.d(TAG, "✅ size: ${uploadResponse.fsize}")
                    
                    // 清理临时文件
                    tempFile.delete()
                    
                    Result.success(uploadResponse)
                } else {
                    Log.e(TAG, "❌ 响应体为空")
                    tempFile.delete()
                    Result.failure(Exception("上传失败：响应为空"))
                }
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "❌ 上传失败: ${response.code}")
                Log.e(TAG, "❌ 错误详情: $errorBody")
                tempFile.delete()
                Result.failure(Exception("上传失败: ${response.code} - $errorBody"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 上传异常", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * 计算字节数组的MD5值
     */
    private fun calculateMD5(bytes: ByteArray): String {
        val md5Digest = MessageDigest.getInstance("MD5")
        val md5Bytes = md5Digest.digest(bytes)
        return md5Bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 从URI获取文件名
     * 优先使用ContentResolver的DISPLAY_NAME，确保获取正确的文件名
     */
    private fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        
        // 优先尝试从ContentProvider获取DISPLAY_NAME
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ 无法从ContentResolver获取文件名", e)
        }
        
        // 如果ContentProvider失败，从URI的path获取并清理
        if (fileName == null) {
            uri.lastPathSegment?.let { segment ->
                // 移除可能的前缀（如 "primary:Download/"）
                fileName = if (segment.contains('/')) {
                    segment.substringAfterLast('/')
                } else if (segment.contains(':')) {
                    segment.substringAfterLast(':')
                } else {
                    segment
                }
            }
        }
        
        // 如果还是空，使用默认名称
        if (fileName.isNullOrBlank()) {
            fileName = "file_${System.currentTimeMillis()}"
        }
        
        return fileName
    }
    
    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0 && lastDot < fileName.length - 1) {
            fileName.substring(lastDot + 1)
        } else {
            "dat"  // 默认扩展名
        }
    }
    
    /**
     * 根据扩展名获取MIME类型
     */
    private fun getMimeTypeFromExtension(extension: String): String? {
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
    }
}

