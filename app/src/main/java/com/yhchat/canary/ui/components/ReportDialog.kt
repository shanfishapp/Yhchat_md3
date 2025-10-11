package com.yhchat.canary.ui.components

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.repository.ReportRepository
import com.yhchat.canary.utils.ImageUploadUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 举报对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(
    chatId: String,
    chatType: Int,  // 1-用户，2-群聊，3-机器人
    chatName: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = remember { ReportViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    var reportContent by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    // 图片选择器
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            viewModel.uploadImage(context, it)
        }
    }
    
    // 举报成功后自动关闭
    LaunchedEffect(uiState.submitSuccess) {
        if (uiState.submitSuccess) {
            onSuccess()
            onDismiss()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("举报${when(chatType) {
                1 -> "用户"
                2 -> "群聊"
                3 -> "机器人"
                else -> "对象"
            }}")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "举报对象: $chatName",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = reportContent,
                    onValueChange = { reportContent = it },
                    label = { Text("举报原因") },
                    placeholder = { Text("请描述举报原因...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5,
                    enabled = !uiState.isLoading
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 图片上传按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        enabled = !uiState.isLoading && !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "上传图片",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (uiState.imageUrl != null) "已上传" else "上传图片")
                    }
                    
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                // 显示已上传的图片
                if (uiState.imageUrl != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "举报图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
                
                if (uiState.error != null) {
                    Text(
                        text = uiState.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (reportContent.isNotBlank()) {
                        viewModel.submitReport(
                            chatId = chatId,
                            chatType = chatType,
                            chatName = chatName,
                            content = reportContent,
                            imageUrl = uiState.imageUrl ?: ""
                        )
                    }
                },
                enabled = !uiState.isLoading && reportContent.isNotBlank()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("提交")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isLoading
            ) {
                Text("取消")
            }
        }
    )
}

/**
 * 举报ViewModel
 */
class ReportViewModel : ViewModel() {
    private lateinit var reportRepository: ReportRepository
    private lateinit var tokenRepository: com.yhchat.canary.data.repository.TokenRepository
    private lateinit var apiService: com.yhchat.canary.data.api.ApiService
    
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()
    
    fun init(context: Context) {
        reportRepository = RepositoryFactory.getReportRepository(context)
        tokenRepository = RepositoryFactory.getTokenRepository(context)
        apiService = RepositoryFactory.apiService
    }
    
    /**
     * 上传图片
     */
    fun uploadImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // 1. 获取用户token
                val userToken = tokenRepository.getTokenSync()
                if (userToken.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "用户未登录"
                    )
                    return@launch
                }
                
                // 2. 获取七牛上传token
                val tokenResponse = apiService.getQiniuImageToken(userToken)
                
                if (!tokenResponse.isSuccessful || tokenResponse.body() == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "获取上传token失败: ${tokenResponse.code()}"
                    )
                    return@launch
                }
                
                val qiniuData = tokenResponse.body()!!.data
                if (qiniuData == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "获取上传token失败: 数据为空"
                    )
                    return@launch
                }
                
                val uploadToken = qiniuData.token
                
                // 2. 上传图片到七牛
                ImageUploadUtil.uploadImage(context, imageUri, uploadToken).fold(
                    onSuccess = { uploadResponse ->
                        val imageUrl = "https://chat-img.jwznb.com/${uploadResponse.key}"
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            imageUrl = imageUrl
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "图片上传失败: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "上传异常: ${e.message}"
                )
            }
        }
    }
    
    fun submitReport(
        chatId: String,
        chatType: Int,
        chatName: String,
        content: String,
        imageUrl: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            reportRepository.submitReport(
                chatId = chatId,
                chatType = chatType,
                chatName = chatName,
                content = content,
                imageUrl = imageUrl
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        submitSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
}

data class ReportUiState(
    val isLoading: Boolean = false,
    val submitSuccess: Boolean = false,
    val imageUrl: String? = null,
    val error: String? = null
)

