package com.yhchat.canary.utils

import android.content.Context
import com.yhchat.canary.data.model.DownloadStatus
import com.yhchat.canary.data.model.WebDAVDownloadTask
import com.yhchat.canary.data.model.WebDAVFile
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 管理 WebDAV 下载任务的单例
 */
object WebDAVDownloadManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _tasks = MutableStateFlow<List<WebDAVDownloadTask>>(emptyList())
    val tasks: StateFlow<List<WebDAVDownloadTask>> = _tasks

    private val jobMap = mutableMapOf<String, kotlinx.coroutines.Job>()

    fun startDownload(context: Context, file: WebDAVFile): String {
        val appContext = context.applicationContext
        val taskId = UUID.randomUUID().toString()
        val initialTask = WebDAVDownloadTask(
            id = taskId,
            fileName = file.name,
            filePath = file.path,
            mountName = file.mountSetting.mountName,
            totalBytes = file.size.takeIf { it > 0 } ?: 0L
        )
        _tasks.update { listOf(initialTask) + it }

        val job = scope.launch {
            try {
                updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }
                SardineWebDAVClient.downloadFile(
                    context = appContext,
                    file = file,
                    onProgress = { downloaded, total ->
                        updateTask(taskId) {
                            it.copy(
                                downloadedBytes = downloaded,
                                totalBytes = if (total > 0) total else it.totalBytes
                            )
                        }
                    },
                    onSuccess = { localPath ->
                        updateTask(taskId) {
                            it.copy(
                                status = DownloadStatus.COMPLETED,
                                downloadedBytes = it.totalBytes.takeIf { total -> total > 0 } ?: it.downloadedBytes,
                                localPath = localPath
                            )
                        }
                    },
                    onError = { error ->
                        updateTask(taskId) {
                            it.copy(status = DownloadStatus.FAILED, errorMessage = error)
                        }
                    }
                )
            } catch (e: CancellationException) {
                updateTask(taskId) { it.copy(status = DownloadStatus.CANCELED) }
                throw e
            } catch (e: Exception) {
                updateTask(taskId) { it.copy(status = DownloadStatus.FAILED, errorMessage = e.message) }
            } finally {
                jobMap.remove(taskId)
            }
        }
        jobMap[taskId] = job
        return taskId
    }

    fun cancelDownload(taskId: String) {
        val job = jobMap.remove(taskId)
        if (job != null) {
            job.cancel()
        } else {
            updateTask(taskId) { it.copy(status = DownloadStatus.CANCELED) }
        }
    }

    private fun updateTask(taskId: String, transform: (WebDAVDownloadTask) -> WebDAVDownloadTask) {
        _tasks.update { list ->
            list.map { task ->
                if (task.id == taskId) transform(task) else task
            }
        }
    }
}
