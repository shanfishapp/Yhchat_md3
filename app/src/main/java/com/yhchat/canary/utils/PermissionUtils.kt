package com.yhchat.canary.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 */
object PermissionUtils {
    
    // 请求码
    const val REQUEST_STORAGE_PERMISSION = 1001
    const val REQUEST_NOTIFICATION_PERMISSION = 1002
    
    /**
     * 检查存储权限
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 不需要存储权限来下载文件到公共下载目录
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 使用分区存储，下载到公共目录不需要权限
            true
        } else {
            // Android 9 及以下需要存储权限
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 请求存储权限
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }
    
    /**
     * 检查通知权限
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 及以下默认有通知权限
            true
        }
    }
    
    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }
    
    /**
     * 检查所有下载相关权限
     */
    fun hasAllDownloadPermissions(context: Context): Boolean {
        return hasStoragePermission(context) && hasNotificationPermission(context)
    }
    
    /**
     * 请求所有下载相关权限
     */
    fun requestAllDownloadPermissions(activity: Activity) {
        val permissions = mutableListOf<String>()
        
        // 存储权限（Android 9 及以下）
        if (!hasStoragePermission(activity)) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        // 通知权限（Android 13+）
        if (!hasNotificationPermission(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                permissions.toTypedArray(),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }
    
    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_STORAGE_PERMISSION, REQUEST_NOTIFICATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }
}
