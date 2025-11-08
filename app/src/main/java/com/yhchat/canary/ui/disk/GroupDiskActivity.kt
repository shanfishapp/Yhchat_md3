package com.yhchat.canary.ui.disk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.DiskFile
import com.yhchat.canary.data.repository.DiskRepository
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.utils.ImageUploadUtil
import com.yhchat.canary.ui.disk.webdav.WebDAVBrowserActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 群网盘Activity
 */
class GroupDiskActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        
        fun start(context: Context, groupId: String, groupName: String) {
            val intent = Intent(context, GroupDiskActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_GROUP_NAME, groupName)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: ""
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "群聊"
        
        if (groupId.isEmpty()) {
            finish()
            return
        }
        
        setContent {
            YhchatCanaryTheme {
                GroupDiskScreen(
                    groupId = groupId,
                    groupName = groupName,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupDiskScreen(
    groupId: String,
    groupName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { GroupDiskViewModel() }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.loadFiles(groupId, 0)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<DiskFile?>(null) }
    var showFileMenu by remember { mutableStateOf(false) }
    var showDownloadConfirmDialog by remember { mutableStateOf(false) }
    var fileToDownload by remember { mutableStateOf<DiskFile?>(null) }
    
    // 监听当前文件夹变化，重新加载
    LaunchedEffect(uiState.currentFolderId) {
        viewModel.loadFiles(groupId, uiState.currentFolderId)
    }
    
    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.uploadFile(context, it, groupId)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$groupName 的网盘", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            WebDAVBrowserActivity.start(context, groupId, groupName)
                        },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "WebDAV"
                        )
                    }
                    IconButton(
                        onClick = { showCreateFolderDialog = true },
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = "创建文件夹"
                        )
                    }
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        enabled = !uiState.isUploading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "上传文件"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isLoading && !uiState.isUploading) {
                FloatingActionButton(
                    onClick = { viewModel.loadFiles(groupId) }
                ) {
                    Icon(Icons.Default.Refresh, "刷新")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 面包屑导航
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 根目录
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                viewModel.navigateToFolder(0)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "根目录",
                                modifier = Modifier.size(20.dp),
                                tint = if (uiState.currentFolderId == 0L) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "根目录",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (uiState.currentFolderId == 0L) 
                                    FontWeight.Bold 
                                else 
                                    FontWeight.Normal,
                                color = if (uiState.currentFolderId == 0L) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // 面包屑路径
                    itemsIndexed(uiState.breadcrumbs) { index, breadcrumb ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = breadcrumb.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (index == uiState.breadcrumbs.lastIndex) 
                                    FontWeight.Bold 
                                else 
                                    FontWeight.Normal,
                                color = if (index == uiState.breadcrumbs.lastIndex) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable {
                                    viewModel.navigateToFolder(breadcrumb.id)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "加载失败",
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(onClick = { viewModel.loadFiles(groupId) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                uiState.files.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "文件夹为空",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.files) { file ->
                            DiskFileCard(
                                file = file,
                                onClick = {
                                    if (file.objectType == 1) {
                                        // 文件夹 - 进入
                                        viewModel.enterFolder(file.id, file.name)
                                    } else {
                                        // 文件 - 显示下载确认对话框
                                        fileToDownload = file
                                        showDownloadConfirmDialog = true
                                    }
                                },
                                onLongClick = {
                                    selectedFile = file
                                    showFileMenu = true
                                }
                            )
                        }
                    }
                }
            }
            
            // 上传进度
            if (uiState.isUploading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "上传中...",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
    
    // 创建文件夹对话框
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onConfirm = { folderName ->
                viewModel.createFolder(groupId, folderName)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }
    
    // 文件操作菜单
    if (showFileMenu && selectedFile != null) {
        FileActionDialog(
            file = selectedFile!!,
            onRename = {
                showFileMenu = false
                showRenameDialog = true
            },
            onDelete = {
                viewModel.deleteFile(selectedFile!!.id, selectedFile!!.objectType)
                showFileMenu = false
                selectedFile = null
            },
            onDownload = {
                downloadFileWithService(context, selectedFile!!)
            },
            onDismiss = {
                showFileMenu = false
                selectedFile = null
            }
        )
    }
    
    // 重命名对话框
    if (showRenameDialog && selectedFile != null) {
        RenameFileDialog(
            currentName = selectedFile!!.name,
            onConfirm = { newName ->
                viewModel.renameFile(selectedFile!!.id, selectedFile!!.objectType, newName)
                showRenameDialog = false
                selectedFile = null
            },
            onDismiss = {
                showRenameDialog = false
                selectedFile = null
            }
        )
    }
    
    // 下载确认对话框
    if (showDownloadConfirmDialog && fileToDownload != null) {
        DownloadConfirmDialog(
            file = fileToDownload!!,
            onConfirm = {
                downloadFileWithService(context, fileToDownload!!)
                showDownloadConfirmDialog = false
                fileToDownload = null
            },
            onDismiss = {
                showDownloadConfirmDialog = false
                fileToDownload = null
            }
        )
    }
    
    // 操作成功提示
    LaunchedEffect(uiState.operationSuccess) {
        if (uiState.operationSuccess) {
            Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
            viewModel.resetOperationSuccess()
            viewModel.loadFiles(groupId)
        }
    }
}

/**
 * 使用FileDownloadService下载文件
 */
private fun downloadFileWithService(context: Context, file: DiskFile) {
    if (file.qiniuKey.isBlank()) {
        Toast.makeText(context, "文件链接无效", Toast.LENGTH_SHORT).show()
        return
    }
    
    try {
        com.yhchat.canary.service.FileDownloadService.startDownload(
            context = context,
            fileUrl = file.qiniuKey,
            fileName = file.name,
            fileSize = file.fileSize,
            autoOpen = false
        )
        Toast.makeText(context, "开始下载: ${file.name}", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DiskFileCard(
    file: DiskFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isFolder()) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (file.isFolder()) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.secondary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = file.getFormattedSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (file.uploadByName.isNotEmpty()) {
                        Text(
                            text = "by ${file.uploadByName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateFolderDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建文件夹") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("文件夹名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (folderName.isNotBlank()) onConfirm(folderName) },
                enabled = folderName.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun FileActionDialog(
    file: DiskFile,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件操作") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("选中: ${file.name}")
                Spacer(modifier = Modifier.height(8.dp))
                
                // 如果是文件（objectType=2），显示下载选项
                if (file.objectType == 2) {
                    TextButton(
                        onClick = {
                            onDownload()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("下载")
                    }
                }
                
                TextButton(
                    onClick = onRename,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重命名")
                }
                
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Delete, 
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun DownloadConfirmDialog(
    file: DiskFile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("确认下载") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("确定要下载以下文件吗？")
                Spacer(modifier = Modifier.height(8.dp))
                
                // 文件信息
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "文件名：${file.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "大小：${formatFileSize(file.fileSize)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "上传者：${file.uploadByName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "文件将保存到 : /storage/emulated/0/Download/yhchat/ 目录下",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun RenameFileDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("重命名") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("新名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (newName.isNotBlank()) onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

/**
 * 格式化文件大小
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

/**
 * 群网盘ViewModel
 */
class GroupDiskViewModel : ViewModel() {
    private lateinit var diskRepository: DiskRepository
    private lateinit var apiService: com.yhchat.canary.data.api.ApiService
    
    private val _uiState = MutableStateFlow(GroupDiskUiState())
    val uiState: StateFlow<GroupDiskUiState> = _uiState.asStateFlow()
    
    fun init(context: Context) {
        diskRepository = RepositoryFactory.getDiskRepository(context)
        apiService = RepositoryFactory.apiService
    }
    
    fun loadFiles(groupId: String, folderId: Long = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            diskRepository.getFileList(
                chatId = groupId,
                chatType = 2,  // 群聊
                folderId = folderId
            ).fold(
                onSuccess = { files ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        files = files,
                        currentFolderId = folderId
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
    
    fun createFolder(groupId: String, folderName: String) {
        viewModelScope.launch {
            diskRepository.createFolder(
                chatId = groupId,
                chatType = 2,
                folderName = folderName,
                parentFolderId = uiState.value.currentFolderId
            ).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(operationSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun uploadFile(context: Context, fileUri: Uri, groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploading = true, error = null)
            
            try {
                val tag = "GroupDiskUpload"
                android.util.Log.d(tag, "📤 ========== 开始群网盘文件上传 ==========")
                android.util.Log.d(tag, "📤 群组ID: $groupId")
                android.util.Log.d(tag, "📤 当前文件夹ID: ${_uiState.value.currentFolderId}")
                
                // 1. 获取七牛上传token
                val tokenRepo = RepositoryFactory.getTokenRepository(context)
                val userToken = tokenRepo.getTokenSync()
                if (userToken.isNullOrEmpty()) {
                    throw Exception("用户未登录")
                }
                
                android.util.Log.d(tag, "📤 获取七牛上传token...")
                val tokenResponse = apiService.getQiniuFileToken(userToken)
                if (!tokenResponse.isSuccessful || tokenResponse.body()?.code != 1) {
                    throw Exception("获取上传token失败: ${tokenResponse.body()?.msg}")
                }
                
                val qiniuToken = tokenResponse.body()?.data?.token
                    ?: throw Exception("上传token为空")
                android.util.Log.d(tag, "✅ 七牛Token获取成功")
                
                // 2. 获取文件名（用于显示和记录）
                val fileName = getFileNameFromUri(context, fileUri) ?: "未命名文件"
                android.util.Log.d(tag, "📤 文件名: $fileName")
                
                // 3. 使用FileUploadUtil上传文件到七牛云
                android.util.Log.d(tag, "📤 开始上传文件到七牛云...")
                val uploadResult = com.yhchat.canary.utils.FileUploadUtil.uploadFile(
                    context = context,
                    fileUri = fileUri,
                    uploadToken = qiniuToken
                )
                
                uploadResult.fold(
                    onSuccess = { uploadResponse ->
                        android.util.Log.d(tag, "✅ 七牛云上传成功！")
                        android.util.Log.d(tag, "   key: ${uploadResponse.key}")
                        android.util.Log.d(tag, "   hash: ${uploadResponse.hash}")
                        android.util.Log.d(tag, "   size: ${uploadResponse.fsize}")
                        
                        // 4. 提取MD5（从key中提取）
                        val fileMd5 = uploadResponse.key.substringAfter("disk/").substringBefore(".")
                        android.util.Log.d(tag, "✅ 文件MD5: $fileMd5")
                        
                        // 5. 调用上传文件记录API
                        android.util.Log.d(tag, "📤 记录文件上传信息...")
                        val uploadFileRequest = com.yhchat.canary.data.model.UploadFileRequest(
                            chatId = groupId,
                            chatType = 2,  // 群聊
                            fileSize = uploadResponse.fsize,
                            fileName = fileName,
                            fileMd5 = fileMd5,
                            fileEtag = uploadResponse.hash,  // hash就是etag
                            qiniuKey = uploadResponse.key,
                            folderId = _uiState.value.currentFolderId  // 当前文件夹
                        )
                        
                        val recordResponse = apiService.uploadFileToDisk(userToken, uploadFileRequest)
                        if (recordResponse.isSuccessful && recordResponse.body()?.code == 1) {
                            android.util.Log.d(tag, "✅ 文件上传记录成功！")
                            android.util.Log.d(tag, "✅ ========== 群网盘文件上传完成 ==========")
                            
                            _uiState.value = _uiState.value.copy(
                                isUploading = false,
                                operationSuccess = true
                            )
                        } else {
                            android.util.Log.e(tag, "❌ 文件上传记录失败: ${recordResponse.body()?.message}")
                            throw Exception("文件上传记录失败: ${recordResponse.body()?.message}")
                        }
                    },
                    onFailure = { error ->
                        android.util.Log.e(tag, "❌ 七牛云上传失败", error)
                        throw error
                    }
                )
                
            } catch (e: Exception) {
                android.util.Log.e("GroupDiskUpload", "❌ 上传失败", e)
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    error = "上传失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 从URI获取文件名
     */
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
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
            android.util.Log.w("GroupDiskUpload", "⚠️ 无法从ContentResolver获取文件名", e)
        }
        
        // 如果ContentProvider失败，从URI的path获取并清理
        if (fileName == null) {
            uri.lastPathSegment?.let { segment ->
                fileName = if (segment.contains('/')) {
                    segment.substringAfterLast('/')
                } else if (segment.contains(':')) {
                    segment.substringAfterLast(':')
                } else {
                    segment
                }
            }
        }
        
        return fileName
    }
    
    fun renameFile(fileId: Long, objectType: Int, newName: String) {
        viewModelScope.launch {
            diskRepository.renameFile(fileId, objectType, newName).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(operationSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun deleteFile(fileId: Long, objectType: Int) {
        viewModelScope.launch {
            diskRepository.removeFile(fileId, objectType).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(operationSuccess = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(error = error.message)
                }
            )
        }
    }
    
    fun resetOperationSuccess() {
        _uiState.value = _uiState.value.copy(operationSuccess = false)
    }
    
    /**
     * 进入文件夹
     */
    fun enterFolder(folderId: Long, folderName: String) {
        val newBreadcrumbs = _uiState.value.breadcrumbs.toMutableList()
        newBreadcrumbs.add(Breadcrumb(folderId, folderName))
        
        _uiState.value = _uiState.value.copy(
            currentFolderId = folderId,
            breadcrumbs = newBreadcrumbs
        )
    }
    
    /**
     * 导航到指定文件夹（通过面包屑）
     */
    fun navigateToFolder(folderId: Long) {
        if (folderId == 0L) {
            // 返回根目录
            _uiState.value = _uiState.value.copy(
                currentFolderId = 0,
                breadcrumbs = emptyList()
            )
        } else {
            // 返回到面包屑中的某个文件夹
            val breadcrumbIndex = _uiState.value.breadcrumbs.indexOfFirst { it.id == folderId }
            if (breadcrumbIndex != -1) {
                val newBreadcrumbs = _uiState.value.breadcrumbs.take(breadcrumbIndex + 1)
                _uiState.value = _uiState.value.copy(
                    currentFolderId = folderId,
                    breadcrumbs = newBreadcrumbs
                )
            }
        }
    }
}

/**
 * 面包屑数据类
 */
data class Breadcrumb(
    val id: Long,
    val name: String
)

data class GroupDiskUiState(
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val files: List<DiskFile> = emptyList(),
    val currentFolderId: Long = 0,
    val breadcrumbs: List<Breadcrumb> = emptyList(),
    val operationSuccess: Boolean = false,
    val error: String? = null
)

