package com.yhchat.canary.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.yhchat.canary.data.repository.TokenRepository

/**
 * 登录界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String, String) -> Unit, // token, userId
    tokenRepository: TokenRepository? = null,
    viewModel: LoginViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val captchaData by viewModel.captchaData.collectAsState()
    
    // 设置tokenRepository
    LaunchedEffect(tokenRepository) {
        viewModel.setTokenRepository(tokenRepository)
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }
    var mobile by remember { mutableStateOf("") }
    var imageCaptcha by remember { mutableStateOf("") }
    var smsCaptcha by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }
    
    // 处理登录成功
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            val loginData = uiState.loginData
            if (loginData != null) {
                // 使用token作为用户ID，或者使用一个默认值
                onLoginSuccess(loginData.token, loginData.token)
            }
        }
    }
    
    // 获取初始验证码
    LaunchedEffect(Unit) {
        viewModel.getCaptcha()
    }
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 添加顶部间距，确保内容不会贴顶
        Spacer(modifier = Modifier.height(24.dp))
        // Logo
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "云湖",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 应用名称
        Text(
            text = "云湖聊天",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 登录方式选择
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("手机登录") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("邮箱登录") }
            )
        }
        
        // 登录表单
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                when (selectedTab) {
                    0 -> {
                        // 手机登录
                        
                        // 验证码图片显示 - 移到最上面
                        captchaData?.let { captcha ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 验证码图片
                                androidx.compose.foundation.Image(
                                    painter = rememberAsyncImagePainter(
                                        model = android.util.Base64.decode(
                                            captcha.b64s.substringAfter(","),
                                            android.util.Base64.DEFAULT
                                        ).let { bytes ->
                                            coil.request.ImageRequest.Builder(LocalContext.current)
                                                .data(bytes)
                                                .build()
                                        }
                                    ),
                                    contentDescription = "验证码图片",
                                    modifier = Modifier
                                        .height(48.dp)
                                        .width(96.dp),
                                    contentScale = ContentScale.Fit
                                )
                                
                                // 刷新按钮
                                OutlinedButton(
                                    onClick = { viewModel.getCaptcha() },
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("刷新", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        
                        // 手机号输入
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("手机号") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // 图片验证码输入
                        OutlinedTextField(
                            value = imageCaptcha,
                            onValueChange = { imageCaptcha = it },
                            label = { Text("图片验证码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // 短信验证码输入和获取按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = smsCaptcha,
                                onValueChange = { smsCaptcha = it },
                                label = { Text("短信验证码") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Button(
                                onClick = { 
                                    viewModel.getSmsCaptcha(mobile, imageCaptcha)
                                },
                                enabled = !uiState.isLoading && mobile.isNotBlank() && imageCaptcha.isNotBlank(),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text("获取短信", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        
                        // 成功提示
                        if (uiState.smsSuccess) {
                            Text(
                                text = "短信验证码发送成功，请查收",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    1 -> {
                        // 邮箱登录
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("邮箱") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("密码") },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Text(
                                        text = if (passwordVisible) "隐藏" else "显示",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                
                // 错误信息
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
            // 登录按钮
            Button(
                onClick = {
                    when (selectedTab) {
                        0 -> viewModel.loginWithCaptcha(mobile, smsCaptcha)
                        1 -> viewModel.loginWithEmail(email, password)
                    }
                },
                enabled = !uiState.isLoading && (
                    (selectedTab == 0 && mobile.isNotBlank() && smsCaptcha.isNotBlank()) ||
                    (selectedTab == 1 && email.isNotBlank() && password.isNotBlank())
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = "登录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Token登录按钮
            OutlinedButton(
                onClick = { showTokenDialog = true },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "使用Token登录",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        
        // 底部说明
        Text(
            text = "登录即表示同意用户协议和隐私政策",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 添加底部间距，确保内容不会贴底
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Token登录对话框
    if (showTokenDialog) {
        TokenLoginDialog(
            tokenInput = tokenInput,
            onTokenChange = { tokenInput = it },
            onConfirm = {
                if (tokenInput.isNotBlank()) {
                    viewModel.loginWithToken(tokenInput)
                    showTokenDialog = false
                    tokenInput = ""
                }
            },
            onDismiss = {
                showTokenDialog = false
                tokenInput = ""
            }
        )
    }
}

/**
 * Token登录对话框
 */
@Composable
private fun TokenLoginDialog(
    tokenInput: String,
    onTokenChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Token登录") },
        text = {
            Column {
                Text(
                    text = "请输入用户Token进行登录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = onTokenChange,
                    label = { Text("用户Token") },
                    placeholder = { Text("请输入Token...") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = tokenInput.isNotBlank()
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
