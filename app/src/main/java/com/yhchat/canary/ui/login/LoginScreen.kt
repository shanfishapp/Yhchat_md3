package com.yhchat.canary.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
    
    // 获取验证码
    LaunchedEffect(Unit) {
        viewModel.getCaptcha()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 应用名称
        Text(
            text = "云湖聊天",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("手机号") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // 图片验证码
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = imageCaptcha,
                                onValueChange = { imageCaptcha = it },
                                label = { Text("图片验证码") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Button(
                                onClick = { viewModel.getCaptcha() },
                                enabled = !uiState.isLoading
                            ) {
                                Text("获取图片")
                            }
                        }

                        // 短信验证码
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                onClick = { viewModel.getSmsCaptcha(mobile, imageCaptcha) },
                                enabled = !uiState.isLoading && mobile.isNotBlank() && imageCaptcha.isNotBlank()
                            ) {
                                Text("获取短信")
                            }
                        }
                        
                        // 验证码图片显示
                        captchaData?.let { captcha ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "验证码图片",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // 使用Coil显示base64图片
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
                                            .height(80.dp)
                                            .fillMaxWidth(),
                                        contentScale = ContentScale.Fit
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "请输入上图中的验证码",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 底部说明
        Text(
            text = "登录即表示同意用户协议和隐私政策",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
