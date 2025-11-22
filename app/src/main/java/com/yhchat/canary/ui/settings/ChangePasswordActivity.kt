package com.yhchat.canary.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yhchat.canary.data.repository.UserRepository
import com.yhchat.canary.data.repository.TokenRepository
import com.yhchat.canary.data.model.ChangePasswordRequest
import com.yhchat.canary.data.model.CaptchaData
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import com.yhchat.canary.data.di.RepositoryFactory
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

class ChangePasswordActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val userEmail = intent.getStringExtra("user_email") ?: ""
        
        setContent {
            YhchatCanaryTheme {
                ChangePasswordScreen(
                    userEmail = userEmail,
                    onBackClick = { finish() }
                )
            }
        }
    }
    
    companion object {
        fun createIntent(context: Context, userEmail: String): Intent {
            return Intent(context, ChangePasswordActivity::class.java).apply {
                putExtra("user_email", userEmail)
            }
        }
    }
}

@Composable
fun CaptchaImage(
    captchaImage: String?,
    onRefresh: () -> Unit
) {
    if (!captchaImage.isNullOrEmpty()) {
        val imageResult = remember(captchaImage) {
            try {
                // 处理特殊格式: "image//png;base64,..." 或 "data:image/png;base64,..." -> 提取base64部分
                val base64Data = when {
                    captchaImage.startsWith("data:image/") -> {
                        val parts = captchaImage.split(";base64,")
                        if (parts.size > 1) parts[1] else captchaImage
                    }
                    captchaImage.startsWith("image//") -> {
                        val parts = captchaImage.split(";base64,")
                        if (parts.size > 1) parts[1] else captchaImage
                    }
                    else -> captchaImage
                }
                
                val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (bitmap != null) {
                    Result.success(bitmap)
                } else {
                    Result.failure<Bitmap>(Exception("Failed to decode bitmap"))
                }
            } catch (e: Exception) {
                Result.failure<Bitmap>(Exception("Decode error: ${e.message}"))
            }
        }
        
        when {
            imageResult.isSuccess -> {
                Image(
                    bitmap = imageResult.getOrNull()!!.asImageBitmap(),
                    contentDescription = "验证码",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onRefresh() },
                    contentScale = ContentScale.FillBounds
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("加载失败，点击刷新")
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onRefresh() },
            contentAlignment = Alignment.Center
        ) {
            Text("点击刷新")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    userEmail: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { 
        val userRepo: UserRepository = RepositoryFactory.getUserRepository(context)
        val tokenRepo: TokenRepository = RepositoryFactory.getTokenRepository(context)
        userRepo.setTokenRepository(tokenRepo)
        userRepo
    }
    val scope = rememberCoroutineScope()
    
    var email by remember { mutableStateOf(userEmail) }
    var captchaCode by remember { mutableStateOf("") }
    var captchaId by remember { mutableStateOf("") }
    var captchaImage by remember { mutableStateOf<String?>(null) }
    var emailVerificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    
    // 获取验证码
    fun getCaptcha() {
        scope.launch {
            isLoading = true
            error = null
            repository.getCaptcha()
                .onSuccess { captchaData: CaptchaData ->
                    captchaImage = captchaData.b64s
                    captchaId = captchaData.id
                }
                .onFailure { e: Throwable ->
                    error = e.message ?: "获取验证码失败"
                }
            isLoading = false
        }
    }
    
    // 发送邮箱验证码
    fun sendEmailVerificationCode() {
        if (captchaCode.isEmpty()) {
            error = "请输入图片验证码"
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            repository.getEmailVerificationCode(email, captchaCode, captchaId)
                .onSuccess { response: Map<String, Any> ->
                    val code = response["code"] as? Int
                    if (code == 1) {
                        error = "验证码已发送到邮箱，请查收"
                    } else {
                        error = response["msg"] as? String ?: "发送验证码失败"
                    }
                }
                .onFailure { e: Throwable ->
                    error = e.message ?: "发送验证码失败"
                }
            isLoading = false
        }
    }
    
    // 更改密码
    fun changePassword() {
        if (emailVerificationCode.isEmpty()) {
            error = "请输入邮箱验证码"
            return
        }
        if (newPassword.isEmpty()) {
            error = "请输入新密码"
            return
        }
        if (newPassword != confirmPassword) {
            error = "两次输入的密码不一致"
            return
        }
        
        scope.launch {
            isLoading = true
            error = null
            repository.changePassword(ChangePasswordRequest(email, emailVerificationCode, newPassword))
                .onSuccess { response: Map<String, Any> ->
                    val code = response["code"] as? Int
                    if (code == 1) {
                        success = true
                    } else {
                        error = response["msg"] as? String ?: "更改密码失败"
                    }
                }
                .onFailure { e: Throwable ->
                    error = e.message ?: "更改密码失败"
                }
            isLoading = false
        }
    }
    
    // 自动获取验证码
    LaunchedEffect(Unit) {
        getCaptcha()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { Text("更改密码") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )
        
        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 邮箱输入框
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("邮箱") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 图片验证码
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "图片验证码",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 验证码图片容器
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else if (captchaImage != null) {
                            CaptchaImage(
                                captchaImage = captchaImage,
                                onRefresh = { getCaptcha() }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("点击刷新")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = captchaCode,
                            onValueChange = { captchaCode = it },
                            label = { Text("请输入验证码") },
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = { getCaptcha() },
                            enabled = !isLoading
                        ) {
                            Text("刷新")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = { sendEmailVerificationCode() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && captchaCode.isNotEmpty()
                    ) {
                        Text("发送邮箱验证码")
                    }
                }
            }
            
            // 邮箱验证码输入
            OutlinedTextField(
                value = emailVerificationCode,
                onValueChange = { emailVerificationCode = it },
                label = { Text("邮箱验证码") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // 新密码输入
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("新密码") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "隐藏密码" else "显示密码"
                        )
                    }
                }
            )
            
            // 确认密码输入
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("确认密码") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                        Icon(
                            imageVector = if (showConfirmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showConfirmPassword) "隐藏密码" else "显示密码"
                        )
                    }
                }
            )
            
            // 错误信息
            if (error != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // 更改密码按钮
            Button(
                onClick = { changePassword() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && emailVerificationCode.isNotEmpty() && newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("更改密码")
            }
        }
    }
    
    // 成功对话框
    if (success) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("更改成功") },
            text = { Text("密码已成功更改，请使用新密码登录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        success = false
                        onBackClick()
                    }
                ) {
                    Text("确定")
                }
            }
        )
    }
}
