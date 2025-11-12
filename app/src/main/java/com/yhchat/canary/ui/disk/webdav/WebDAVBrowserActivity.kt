package com.yhchat.canary.ui.disk.webdav

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.yhchat.canary.utils.WebDAVDownloader
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import com.yhchat.canary.ui.base.BaseActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.MountSetting
import com.yhchat.canary.data.model.WebDAVFile
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.utils.RSAEncryptionUtil
import com.yhchat.canary.utils.WebDAVClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * WebDAV 浏览器 Activity
 */
class WebDAVBrowserActivity : BaseActivity() {
    
    companion object {
        const val EXTRA_GROUP_ID = "extra_group_id"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        
        fun start(context: Context, groupId: String, groupName: String) {
            val intent = Intent(context, WebDAVBrowserActivity::class.java).apply {
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
                WebDAVBrowserScreen(
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
fun WebDAVBrowserScreen(
    groupId: String,
    groupName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel = remember { WebDAVBrowserViewModel() }
    val coroutineScope = rememberCoroutineScope()
    
    // 下载对话框状态
    var showDownloadDialog by remember { mutableStateOf(false) }
    var fileToDownload by remember { mutableStateOf<WebDAVFile?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.loadMountSettings(groupId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$groupName 的 WebDAV", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 初始加载挂载点列表时显示加载中
                uiState.isLoading && uiState.mountSettings.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                // 加载挂载点列表失败时显示错误（此时还没有挂载点列表）
                uiState.error != null && uiState.mountSettings.isEmpty() && !uiState.isLoading -> {
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
                            Button(onClick = { viewModel.loadMountSettings(groupId) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                // 没有挂载点
                uiState.mountSettings.isEmpty() && !uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "暂无 WebDAV 挂载点",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                // 有挂载点列表时，始终显示分类栏和文件列表（即使文件加载失败）
                else -> {
                    // 分类栏（HorizontalPager）
                    val pagerState = rememberPagerState(
                        initialPage = uiState.selectedMountIndex.coerceAtLeast(0),
                        pageCount = { uiState.mountSettings.size }
                    )
                    
                    // 同步 pager 状态和 viewModel 状态
                    LaunchedEffect(pagerState.currentPage) {
                        if (pagerState.currentPage != uiState.selectedMountIndex) {
                            viewModel.selectMount(pagerState.currentPage)
                        }
                    }
                    
                    LaunchedEffect(uiState.selectedMountIndex) {
                        if (uiState.selectedMountIndex >= 0 && 
                            uiState.selectedMountIndex != pagerState.currentPage &&
                            uiState.selectedMountIndex < uiState.mountSettings.size) {
                            pagerState.animateScrollToPage(uiState.selectedMountIndex)
                        }
                    }
                    
                    // 分类标签栏
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
                            itemsIndexed(uiState.mountSettings) { index, mountSetting ->
                                FilterChip(
                                    selected = uiState.selectedMountIndex == index,
                                    onClick = {
                                        viewModel.selectMount(index)
                                    },
                                    label = { Text(mountSetting.mountName) }
                                )
                            }
                        }
                    }
                    
                    // 文件内容区域 - 使用 HorizontalPager
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        // 每个页面显示对应挂载点的文件列表
                        WebDAVFileListPage(
                            uiState = uiState,
                            viewModel = viewModel,
                            context = context,
                            pageIndex = pageIndex,
                            isCurrentPage = pageIndex == uiState.selectedMountIndex,
                            onFileClick = { file ->
                                // 显示下载确认对话框
                                fileToDownload = file
                                showDownloadDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // 下载确认对话框
    if (showDownloadDialog && fileToDownload != null) {
        AlertDialog(
            onDismissRequest = { 
                if (!isDownloading) {
                    showDownloadDialog = false
                    fileToDownload = null
                }
            },
            title = { Text("下载文件") },
            text = { 
                Column {
                    Text("确定要下载文件 \"${fileToDownload!!.name}\" 吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "文件大小: ${fileToDownload!!.getFormattedSize()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "下载目录: /storage/emulated/0/Download/yhchat",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isDownloading) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = downloadProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "下载中... ${(downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isDownloading) {
                            isDownloading = true
                            downloadProgress = 0f
                            
                            // 开始下载
                            Log.d("WebDAVBrowser", "开始启动下载协程: ${fileToDownload!!.name}")
                            coroutineScope.launch {
                                Log.d("WebDAVBrowser", "下载协程已启动")
                                WebDAVDownloader.downloadFile(
                                    context = context,
                                    file = fileToDownload!!,
                                    onProgress = { downloaded, total ->
                                        if (total > 0) {
                                            downloadProgress = downloaded.toFloat() / total.toFloat()
                                        }
                                    },
                                    onSuccess = { localPath ->
                                        Log.d("WebDAVBrowser", "下载成功回调: $localPath")
                                        isDownloading = false
                                        showDownloadDialog = false
                                        fileToDownload = null
                                        Toast.makeText(context, "下载完成: $localPath", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { error ->
                                        Log.d("WebDAVBrowser", "下载失败回调: $error")
                                        isDownloading = false
                                        showDownloadDialog = false
                                        fileToDownload = null
                                        Toast.makeText(context, "下载失败: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text(if (isDownloading) "下载中..." else "下载")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        if (!isDownloading) {
                            showDownloadDialog = false
                            fileToDownload = null
                        }
                    },
                    enabled = !isDownloading
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun WebDAVFileListPage(
    uiState: WebDAVBrowserUiState,
    viewModel: WebDAVBrowserViewModel,
    context: android.content.Context,
    pageIndex: Int,
    isCurrentPage: Boolean,
    onFileClick: (WebDAVFile) -> Unit
) {
    // 获取当前页面对应的挂载点状态
    val mountState = uiState.mountStates[pageIndex]
    
    when {
        mountState?.isLoading == true -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        mountState?.error != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = mountState.error ?: "加载失败",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = { 
                        val selectedMount = uiState.mountSettings.getOrNull(pageIndex)
                        if (selectedMount != null) {
                            viewModel.loadFiles(selectedMount, mountState.currentPath)
                        }
                    }) {
                        Text("重试")
                    }
                }
            }
        }
        
        mountState?.files?.isEmpty() == true -> {
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
        
        mountState != null -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mountState.files) { file ->
                    WebDAVFileCard(
                        file = file,
                        onClick = {
                            if (file.isDirectory) {
                                viewModel.enterFolder(file.path)
                            } else {
                                // 调用文件点击回调
                                onFileClick(file)
                            }
                        },
                        onLongClick = {
                            // TODO: 显示文件操作菜单
                            Toast.makeText(context, "长按: ${file.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
        
        else -> {
            // 还没有加载过该挂载点，显示空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "滑动到此页面以加载文件",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WebDAVFileCard(
    file: WebDAVFile,
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
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (file.isDirectory) 
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
                Text(
                    text = file.getFormattedSize(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * WebDAV 浏览器 ViewModel
 */
class WebDAVBrowserViewModel : ViewModel() {
    private lateinit var apiService: com.yhchat.canary.data.api.ApiService
    private lateinit var tokenRepository: com.yhchat.canary.data.repository.TokenRepository
    
    private val _uiState = MutableStateFlow(WebDAVBrowserUiState())
    val uiState: StateFlow<WebDAVBrowserUiState> = _uiState.asStateFlow()
    
    fun init(context: Context) {
        apiService = ApiClient.apiService
        tokenRepository = RepositoryFactory.getTokenRepository(context)
    }
    
    fun loadMountSettings(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // 1. 准备加密参数
                val encryptionResult = RSAEncryptionUtil.prepareEncryptionParams()
                if (encryptionResult == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "准备加密参数失败"
                    )
                    return@launch
                }
                
                val (encryptKey, encryptIv, rawKeyPair) = encryptionResult
                
                // 2. 获取用户 token
                val userToken = tokenRepository.getTokenSync()
                if (userToken.isNullOrEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "用户未登录"
                    )
                    return@launch
                }
                
                // 3. 调用 API 获取挂载点列表
                val request = com.yhchat.canary.data.model.MountSettingRequest(
                    groupId = groupId,
                    encryptKey = encryptKey,
                    encryptIv = encryptIv
                )
                
                val response = apiService.getMountSettingList(userToken, request)
                
                if (response.isSuccessful && response.body()?.code == 1) {
                    val encryptedMountSettings = response.body()?.data?.list ?: emptyList()
                    
                    // 4. 解密所有挂载点的密码
                    val decryptedMountSettings = mutableListOf<MountSetting>()
                    for (mountSetting in encryptedMountSettings) {
                        try {
                            val decryptedPassword = RSAEncryptionUtil.decryptWebDAVPassword(
                                mountSetting.webdavPassword,
                                rawKeyPair.first,  // AES key
                                rawKeyPair.second  // AES IV
                            ) ?: ""
                            
                            // 创建解密后的挂载点配置
                            val decryptedMountSetting = mountSetting.copy(
                                webdavPassword = decryptedPassword
                            )
                            decryptedMountSettings.add(decryptedMountSetting)
                            
                            android.util.Log.d("WebDAVBrowser", "挂载点 ${mountSetting.mountName} 密码解密成功")
                        } catch (e: Exception) {
                            android.util.Log.e("WebDAVBrowser", "挂载点 ${mountSetting.mountName} 密码解密失败", e)
                            // 即使密码解密失败，也添加原始配置（密码为空）
                            decryptedMountSettings.add(mountSetting.copy(webdavPassword = ""))
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mountSettings = decryptedMountSettings,
                        selectedMountIndex = if (decryptedMountSettings.isNotEmpty()) 0 else -1
                    )
                    
                    // 5. 自动加载第一个挂载点的文件（webdavUrl + webdavRootPath 就是默认路径）
                    if (decryptedMountSettings.isNotEmpty()) {
                        val firstMount = decryptedMountSettings[0]
                        loadFiles(firstMount, "") // 传入空字符串，使用默认路径
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.body()?.msg ?: "获取挂载点列表失败"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("WebDAVBrowser", "加载挂载点失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载失败: ${e.message}"
                )
            }
        }
    }
    
    fun selectMount(index: Int) {
        if (index in _uiState.value.mountSettings.indices) {
            val selectedMount = _uiState.value.mountSettings[index]
            
            _uiState.value = _uiState.value.copy(
                selectedMountIndex = index
            )
            
            // 如果该挂载点还没有加载过文件，则加载
            val mountState = _uiState.value.mountStates[index]
            if (mountState == null) {
                loadFiles(selectedMount, "") // 传入空字符串，使用默认路径
            }
        }
    }
    
    fun loadFiles(mountSetting: MountSetting, path: String = "") {
        viewModelScope.launch {
            val mountIndex = _uiState.value.selectedMountIndex
            
            // 更新对应挂载点的加载状态
            val currentMountStates = _uiState.value.mountStates.toMutableMap()
            currentMountStates[mountIndex] = currentMountStates[mountIndex]?.copy(
                isLoading = true,
                error = null
            ) ?: MountState(isLoading = true)
            
            _uiState.value = _uiState.value.copy(
                mountStates = currentMountStates,
                isLoadingFiles = true
                // 不清除全局error，因为全局error只用于挂载点列表加载失败
            )
            
            WebDAVClient.listFiles(mountSetting, path).fold(
                onSuccess = { files ->
                    val updatedMountStates = _uiState.value.mountStates.toMutableMap()
                    updatedMountStates[mountIndex] = MountState(
                        files = files,
                        currentPath = path,
                        isLoading = false,
                        error = null
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        mountStates = updatedMountStates,
                        isLoadingFiles = false,
                        files = files,
                        currentPath = path
                        // 不设置error，保持全局error不变（全局error只用于挂载点列表加载失败）
                    )
                },
                onFailure = { error ->
                    val updatedMountStates = _uiState.value.mountStates.toMutableMap()
                    updatedMountStates[mountIndex] = updatedMountStates[mountIndex]?.copy(
                        isLoading = false,
                        error = "加载文件失败: ${error.message}"
                    ) ?: MountState(
                        isLoading = false,
                        error = "加载文件失败: ${error.message}"
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        mountStates = updatedMountStates,
                        isLoadingFiles = false
                        // 不设置全局error，只设置对应挂载点的error，这样分类栏不会消失
                    )
                }
            )
        }
    }
    
    fun enterFolder(fullPath: String) {
        val selectedMount = _uiState.value.mountSettings.getOrNull(_uiState.value.selectedMountIndex)
        if (selectedMount != null) {
            // 将完整路径转换为相对路径
            // fullPath 格式: /remote.php/dav/files/ushio_noa/yh/subfolder
            // webdavRootPath 格式: /yh
            // 需要提取出相对于 webdavRootPath 的部分
            
            val baseUrl = selectedMount.webdavUrl.trimEnd('/')
            val rootPath = selectedMount.webdavRootPath.trimStart('/')
            
            // 构建基础路径模式，用于匹配
            val basePathPattern = "/${rootPath}".replace("//", "/")
            
            // 从完整路径中提取相对路径
            val relativePath = if (fullPath.contains(basePathPattern)) {
                val afterBase = fullPath.substringAfter(basePathPattern)
                afterBase.trimStart('/')
            } else {
                // 如果无法匹配，直接使用文件夹名称
                fullPath.substringAfterLast('/')
            }
            
            Log.d("WebDAVBrowser", "enterFolder: fullPath=$fullPath, basePathPattern=$basePathPattern, relativePath=$relativePath")
            
            loadFiles(selectedMount, relativePath)
        }
    }
}

data class WebDAVBrowserUiState(
    val isLoading: Boolean = false,
    val isLoadingFiles: Boolean = false,
    val mountSettings: List<MountSetting> = emptyList(),
    val selectedMountIndex: Int = -1,
    val files: List<WebDAVFile> = emptyList(),
    val currentPath: String = "",
    val error: String? = null,
    // 为每个挂载点单独管理状态
    val mountStates: Map<Int, MountState> = emptyMap()
)

data class MountState(
    val files: List<WebDAVFile> = emptyList(),
    val currentPath: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

