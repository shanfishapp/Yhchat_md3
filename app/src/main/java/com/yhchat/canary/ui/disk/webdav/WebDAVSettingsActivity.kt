package com.yhchat.canary.ui.disk.webdav

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.api.ApiClient
import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.di.RepositoryFactory
import com.yhchat.canary.data.model.MountSetting
import com.yhchat.canary.data.model.MountSettingCreateRequest
import com.yhchat.canary.data.model.MountSettingDeleteRequest
import com.yhchat.canary.data.model.MountSettingRequest
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.ui.base.BaseActivity
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.utils.RSAEncryptionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api

class WebDAVSettingsActivity : BaseActivity() {

    companion object {
        private const val EXTRA_GROUP_ID = "extra_group_id"
        private const val EXTRA_GROUP_NAME = "extra_group_name"

        fun intent(context: Context, groupId: String, groupName: String): Intent {
            return Intent(context, WebDAVSettingsActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_GROUP_NAME, groupName)
            }
        }

        fun start(context: Context, groupId: String, groupName: String) {
            context.startActivity(intent(context, groupId, groupName))
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
                WebDAVSettingsScreen(
                    groupId = groupId,
                    groupName = groupName,
                    onBackClick = { finish() }
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebDAVSettingsScreen(
    groupId: String,
    groupName: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = remember { WebDAVSettingsViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    var showInputDialog by remember { mutableStateOf(false) }
    var editingMount by remember { mutableStateOf<MountSetting?>(null) }
    var showDeleteDialog by remember { mutableStateOf<MountSetting?>(null) }

    LaunchedEffect(Unit) {
        viewModel.init(context)
        viewModel.loadMountSettings(groupId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "$groupName 的 WebDAV 设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingMount = null
                        showInputDialog = true
                    }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "添加挂载点")
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.mountSettings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "暂无挂载点", style = MaterialTheme.typography.bodyLarge)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.mountSettings, key = { it.id }) { mount ->
                        MountSettingCard(
                            mount = mount,
                            onEdit = {
                                editingMount = mount
                                showInputDialog = true
                            },
                            onDelete = {
                                showDeleteDialog = mount
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInputDialog) {
        val mount = editingMount
        MountSettingInputDialog(
            title = if (mount == null) "添加挂载点" else "编辑挂载点",
            mount = mount,
            isSubmitting = uiState.isSubmitting,
            onDismiss = {
                showInputDialog = false
                editingMount = null
            },
            onConfirm = { formState ->
                viewModel.saveMountSetting(groupId, formState, mount?.id)
                showInputDialog = false
                editingMount = null
            }
        )
    }

    showDeleteDialog?.let { mount ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除挂载点") },
            text = { Text("确定删除挂载点 \"${mount.mountName}\" 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMountSetting(groupId, mount.id)
                    showDeleteDialog = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MountSettingCard(
    mount: MountSetting,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(mount.mountName, style = MaterialTheme.typography.titleMedium)
            Text("地址: ${mount.webdavUrl}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("目录: ${mount.webdavRootPath}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("用户名: ${mount.webdavUserName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun MountSettingInputDialog(
    title: String,
    mount: MountSetting?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (MountFormState) -> Unit
) {
    var mountName by remember { mutableStateOf(mount?.mountName ?: "") }
    var webdavUrl by remember { mutableStateOf(mount?.webdavUrl ?: "") }
    var webdavUserName by remember { mutableStateOf(mount?.webdavUserName ?: "") }
    var webdavPassword by remember { mutableStateOf(mount?.webdavPassword ?: "") }
    var webdavRootPath by remember { mutableStateOf(mount?.webdavRootPath ?: "") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MountTextField(valueState = mountName, label = "挂载名称") { mountName = it }
                MountTextField(valueState = webdavUrl, label = "WebDAV URL") { webdavUrl = it }
                MountTextField(valueState = webdavUserName, label = "用户名") { webdavUserName = it }
                MountTextField(valueState = webdavPassword, label = "密码") { webdavPassword = it }
                MountTextField(valueState = webdavRootPath, label = "根目录") { webdavRootPath = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        MountFormState(
                            mountName = mountName.trim(),
                            webdavUrl = webdavUrl.trim(),
                            webdavUserName = webdavUserName.trim(),
                            webdavPassword = webdavPassword.trim(),
                            webdavRootPath = webdavRootPath.trim()
                        )
                    )
                },
                enabled = !isSubmitting
            ) {
                Text(if (isSubmitting) "提交中..." else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun MountTextField(
    valueState: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = valueState,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

private data class MountFormState(
    val mountName: String,
    val webdavUrl: String,
    val webdavUserName: String,
    val webdavPassword: String,
    val webdavRootPath: String
)

private data class WebDAVSettingsUiState(
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val mountSettings: List<MountSetting> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

private class WebDAVSettingsViewModel : ViewModel() {
    private lateinit var apiService: ApiService
    private lateinit var tokenRepository: TokenRepository
    private lateinit var userRepository: UserRepository

    private val _uiState = MutableStateFlow(WebDAVSettingsUiState())
    val uiState: StateFlow<WebDAVSettingsUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null

    fun init(context: android.content.Context) {
        apiService = ApiClient.apiService
        tokenRepository = RepositoryFactory.getTokenRepository(context)
        userRepository = UserRepository(ApiClient.apiService, tokenRepository)
    }

    fun loadMountSettings(groupId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)
            try {
                val encryptionResult = RSAEncryptionUtil.prepareEncryptionParams()
                    ?: throw IllegalStateException("准备加密参数失败")
                val (encryptKey, encryptIv, rawKeyPair) = encryptionResult
                val token = tokenRepository.getTokenSync() ?: throw IllegalStateException("未登录")
                val response = apiService.getMountSettingList(
                    token,
                    MountSettingRequest(groupId, encryptKey, encryptIv)
                )
                if (response.isSuccessful && response.body()?.code == 1) {
                    val encryptedList = response.body()?.data?.list ?: emptyList()
                    val decryptedList = encryptedList.map { item ->
                        val decryptedPassword = RSAEncryptionUtil.decryptWebDAVPassword(
                            item.webdavPassword,
                            rawKeyPair.first,
                            rawKeyPair.second
                        ) ?: ""
                        item.copy(webdavPassword = decryptedPassword)
                    }
                    val currentUserId = getCurrentUserId()
                    val filtered = if (currentUserId.isNullOrEmpty()) decryptedList else decryptedList.filter { it.userId == currentUserId }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        mountSettings = filtered,
                        error = null
                    )
                } else {
                    throw IllegalStateException(response.body()?.msg ?: "加载挂载点失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun saveMountSetting(groupId: String, form: MountFormState, editingId: Long?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmitting = true, error = null)
            try {
                val token = tokenRepository.getTokenSync() ?: throw IllegalStateException("未登录")

                if (editingId != null) {
                    apiService.deleteMountSetting(token, MountSettingDeleteRequest(editingId))
                }

                val response = apiService.createMountSetting(
                    token,
                    MountSettingCreateRequest(
                        groupId = groupId,
                        mountName = form.mountName,
                        webdavUrl = form.webdavUrl,
                        webdavUserName = form.webdavUserName,
                        webdavPassword = form.webdavPassword,
                        webdavRootPath = form.webdavRootPath
                    )
                )

                if (response.isSuccessful && response.body()?.code == 1) {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        successMessage = if (editingId == null) "创建成功" else "已更新",
                        error = null
                    )
                    loadMountSettings(groupId)
                } else {
                    throw IllegalStateException(response.body()?.message ?: "操作失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSubmitting = false, error = e.message)
            }
        }
    }

    fun deleteMountSetting(groupId: String, mountId: Long) {
        viewModelScope.launch {
            try {
                val token = tokenRepository.getTokenSync() ?: throw IllegalStateException("未登录")
                val response = apiService.deleteMountSetting(token, MountSettingDeleteRequest(mountId))
                if (response.isSuccessful && response.body()?.code == 1) {
                    _uiState.value = _uiState.value.copy(successMessage = "删除成功")
                    loadMountSettings(groupId)
                } else {
                    throw IllegalStateException(response.body()?.message ?: "删除失败")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    private suspend fun getCurrentUserId(): String? {
        currentUserId?.let { return it }
        tokenRepository.getUserIdSync()?.takeIf { it.isNotBlank() }?.let {
            currentUserId = it
            return it
        }
        return userRepository.getUserProfile().getOrNull()?.userId?.also {
            currentUserId = it
        }
    }
}
