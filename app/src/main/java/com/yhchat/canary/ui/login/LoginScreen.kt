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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 添加顶部间距，确保内容不会贴顶
        Spacer(modifier = Modifier.height(32.dp))
        // Logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    MaterialTheme.colorScheme.primary,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "云湖",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold
            )
        }
        
        // 应用名称
        Text(
            text = "云湖聊天",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // 手机登录
                        
                        // 验证码图片显示 - 移到最上面
                        captchaData?.let { captcha ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                                            .height(60.dp)
                                            .width(120.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    
                                    // 刷新按钮
                                    OutlinedButton(
                                        onClick = { viewModel.getCaptcha() },
                                        enabled = !uiState.isLoading,
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Text("刷新验证码")
                                    }
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
                            singleLine = true,
                            supportingText = { Text("请输入11位手机号码") }
                        )

                        // 图片验证码输入
                        OutlinedTextField(
                            value = imageCaptcha,
                            onValueChange = { imageCaptcha = it },
                            label = { Text("图片验证码") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text("请输入上方图片中的验证码") }
                        )

                        // 短信验证码输入和获取按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            OutlinedTextField(
                                value = smsCaptcha,
                                onValueChange = { smsCaptcha = it },
                                label = { Text("短信验证码") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                supportingText = { Text("请输入6位短信验证码") }
                            )

                            Button(
                                onClick = { 
                                    viewModel.getSmsCaptcha(mobile, imageCaptcha)
                                },
                                enabled = !uiState.isLoading && mobile.isNotBlank() && imageCaptcha.isNotBlank(),
                                modifier = Modifier
                                    .height(56.dp)
                                    .padding(top = 8.dp)
                            ) {
                                Text("获取短信")
                            }
                        }
                        
                        // 成功提示
                        if (uiState.smsSuccess) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Text(
                                    text = "短信验证码发送成功，请查收",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
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
        Spacer(modifier = Modifier.height(32.dp))
    }
}
