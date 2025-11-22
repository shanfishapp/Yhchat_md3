package com.yhchat.canary.ui.qrcode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.yhchat.canary.ui.base.BaseActivity
import com.yhchat.canary.ui.theme.YhchatCanaryTheme
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QRCodeScannerActivity : BaseActivity() {
    
    private lateinit var cameraExecutor: ExecutorService
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，会在 Compose 中自动启动相机
        } else {
            Toast.makeText(this, "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        setContent {
            YhchatCanaryTheme {
                ScannerScreen(
                    onBackClick = { finish() },
                    onScanResult = { result ->
                        handleScanResult(result)
                    }
                )
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ScannerScreen(
        onBackClick: () -> Unit,
        onScanResult: (String) -> Unit
    ) {
        val context = LocalContext.current
        val previewView = remember { 
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }
        var hasCameraPermission by remember {
            mutableStateOf(ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED)
        }
        
        LaunchedEffect(hasCameraPermission) {
            if (!hasCameraPermission) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 相机预览
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            
            // 当有权限时启动相机
            if (hasCameraPermission) {
                LaunchedEffect(Unit) {
                    startCamera(previewView, onScanResult)
                }
            }
            
            // 顶部标题栏
            TopAppBar(
                title = { Text("扫描二维码") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // 扫描框
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .border(
                        width = 2.dp,
                        color = Color(0xFF4CAF50),
                        shape = MaterialTheme.shapes.medium
                    )
            )
            
            // 扫描动画线
            var animationPosition by remember { mutableStateOf(0f) }
            
            LaunchedEffect(Unit) {
                while (true) {
                    animationPosition = 0f
                    repeat(100) {
                        animationPosition += 0.01f
                        delay(16)
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(250.dp)
                    .clip(MaterialTheme.shapes.medium)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .offset(y = ((animationPosition - 0.5f) * 250).dp)
                        .background(Color(0xFF4CAF50))
                )
            }
            
            // 四个角
            // 左上角
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 125.dp, y = 125.dp)
                    .size(20.dp, 2.dp)
                    .background(Color(0xFF4CAF50))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 125.dp, y = 125.dp)
                    .size(2.dp, 20.dp)
                    .background(Color(0xFF4CAF50))
            )
            
            // 右上角
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-125).dp, y = 125.dp)
                    .size(20.dp, 2.dp)
                    .background(Color(0xFF4CAF50))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-125).dp, y = 125.dp)
                    .size(2.dp, 20.dp)
                    .background(Color(0xFF4CAF50))
            )
            
            // 左下角
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 125.dp, y = (-125).dp)
                    .size(20.dp, 2.dp)
                    .background(Color(0xFF4CAF50))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 125.dp, y = (-125).dp)
                    .size(2.dp, 20.dp)
                    .background(Color(0xFF4CAF50))
            )
            
            // 右下角
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-125).dp, y = (-125).dp)
                    .size(20.dp, 2.dp)
                    .background(Color(0xFF4CAF50))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-125).dp, y = (-125).dp)
                    .size(2.dp, 20.dp)
                    .background(Color(0xFF4CAF50))
            )
        }
    }

    // 其他方法保持不变...
    private fun startCamera(previewView: PreviewView, onScanResult: (String) -> Unit) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        
        val scanner = BarcodeScanning.getClient(options)
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(scanner, imageProxy, onScanResult)
                    }
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "相机启动失败: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun processImageProxy(
        scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
        imageProxy: androidx.camera.core.ImageProxy,
        onScanResult: (String) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue
                        if (!rawValue.isNullOrEmpty()) {
                            runOnUiThread {
                                onScanResult(rawValue)
                            }
                            break
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // 可以在这里处理扫描错误
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
    
    private fun handleScanResult(result: String) {
        // 处理扫描结果，根据二维码内容决定如何处理
        if (result.startsWith("yunhu://") || result.startsWith("https://") || result.startsWith("http://")) {
            // 如果是云湖链接或网页链接，可以进行相应的处理
            val intent = Intent()
            intent.putExtra("scan_result", result)
            setResult(RESULT_OK, intent)
            finish()
        } else {
            // 对于其他类型的二维码内容，可以显示处理或尝试处理
            Toast.makeText(this, "扫描结果: $result", Toast.LENGTH_LONG).show()
            // 在实际应用中，你可能希望在这里做更多的处理
            // 比如解析特定格式的二维码内容并执行相应操作
            val intent = Intent()
            intent.putExtra("scan_result", result)
            setResult(RESULT_OK, intent)
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
    
    companion object {
        fun start(context: ComponentActivity) {
            val intent = Intent(context, QRCodeScannerActivity::class.java)
            context.startActivity(intent)
        }
    }
}