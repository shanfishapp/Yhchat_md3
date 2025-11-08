package com.yhchat.canary.ui.disk.webdav

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
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
class WebDAVBrowserActivity : ComponentActivity() {
    
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
                            Button(onClick = { viewModel.loadMountSettings(groupId) }) {
                                Text("重试")
                            }
                        }
                    }
                }
                
                uiState.mountSettings.isEmpty() -> {
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
                
                else -> {
                    // 分类栏（Tab）
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
                    
                    // 文件列表
                    when {
                        uiState.isLoadingFiles -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
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
                                    WebDAVFileCard(
                                        file = file,
                                        onClick = {
                                            if (file.isDirectory) {
                                                viewModel.enterFolder(file.path)
                                            } else {
                                                // TODO: 下载文件
                                                Toast.makeText(context, "下载文件: ${file.name}", Toast.LENGTH_SHORT).show()
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
                    }
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
                val encryptionParams = RSAEncryptionUtil.prepareEncryptionParams()
                if (encryptionParams == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "准备加密参数失败"
                    )
                    return@launch
                }
                
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
                    encryptKey = encryptionParams.first,
                    encryptIv = encryptionParams.second
                )
                
                val response = apiService.getMountSettingList(userToken, request)
                
                if (response.isSuccessful && response.body()?.code == 1) {
                    val mountSettings = response.body()?.data?.list ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mountSettings = mountSettings,
                        selectedMountIndex = if (mountSettings.isNotEmpty()) 0 else -1
                    )
                    
                    // 自动加载第一个挂载点的文件
                    if (mountSettings.isNotEmpty()) {
                        loadFiles(mountSettings[0])
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
            _uiState.value = _uiState.value.copy(
                selectedMountIndex = index,
                currentPath = "",
                files = emptyList()
            )
            loadFiles(_uiState.value.mountSettings[index])
        }
    }
    
    fun loadFiles(mountSetting: MountSetting, path: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingFiles = true, error = null)
            
            WebDAVClient.listFiles(mountSetting, path).fold(
                onSuccess = { files ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingFiles = false,
                        files = files
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingFiles = false,
                        error = "加载文件失败: ${error.message}"
                    )
                }
            )
        }
    }
    
    fun enterFolder(path: String) {
        val selectedMount = _uiState.value.mountSettings.getOrNull(_uiState.value.selectedMountIndex)
        if (selectedMount != null) {
            _uiState.value = _uiState.value.copy(currentPath = path)
            loadFiles(selectedMount, path)
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
    val error: String? = null
)

