package com.yhchat.canary.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

/**
 * HTML 设置页面Activity
 */
class HtmlSettingsActivity : ComponentActivity() {
    
    companion object {
        const val PREFS_NAME = "html_settings"
        const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
        const val KEY_DOM_STORAGE_ENABLED = "dom_storage_enabled"
        const val KEY_LOAD_WITH_OVERVIEW_MODE = "load_with_overview_mode"
        const val KEY_USE_WIDE_VIEW_PORT = "use_wide_view_port"
        const val KEY_BUILTIN_ZOOM_CONTROLS = "builtin_zoom_controls"
        const val KEY_DISPLAY_ZOOM_CONTROLS = "display_zoom_controls"
        const val KEY_SUPPORT_ZOOM = "support_zoom"
        
        /**
         * 启动 HTML 设置Activity
         */
        fun start(context: Context) {
            val intent = Intent(context, HtmlSettingsActivity::class.java)
            context.startActivity(intent)
        }
        
        /**
         * 获取设置项
         */
        fun getSettings(context: Context): HtmlSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return HtmlSettings(
                javaScriptEnabled = prefs.getBoolean(KEY_JAVASCRIPT_ENABLED, true),
                domStorageEnabled = prefs.getBoolean(KEY_DOM_STORAGE_ENABLED, true),
                loadWithOverviewMode = prefs.getBoolean(KEY_LOAD_WITH_OVERVIEW_MODE, true),
                useWideViewPort = prefs.getBoolean(KEY_USE_WIDE_VIEW_PORT, true),
                builtInZoomControls = prefs.getBoolean(KEY_BUILTIN_ZOOM_CONTROLS, true),
                displayZoomControls = prefs.getBoolean(KEY_DISPLAY_ZOOM_CONTROLS, false),
                supportZoom = prefs.getBoolean(KEY_SUPPORT_ZOOM, true)
            )
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            YhchatCanaryTheme {
                HtmlSettingsScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

/**
 * HTML 设置数据类
 */
data class HtmlSettings(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val loadWithOverviewMode: Boolean = true,
    val useWideViewPort: Boolean = true,
    val builtInZoomControls: Boolean = true,
    val displayZoomControls: Boolean = false,
    val supportZoom: Boolean = true
)

/**
 * HTML 设置界面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlSettingsScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { 
        context.getSharedPreferences(HtmlSettingsActivity.PREFS_NAME, Context.MODE_PRIVATE) 
    }
    
    // 设置状态
    var javaScriptEnabled by remember { 
        mutableStateOf(prefs.getBoolean(HtmlSettingsActivity.KEY_JAVASCRIPT_ENABLED, true)) 
    }
    var domStorageEnabled by remember { 
        mutableStateOf(prefs.getBoolean(HtmlSettingsActivity.KEY_DOM_STORAGE_ENABLED, true)) 
    }
    var loadWithOverviewMode by remember { 
        mutableStateOf(prefs.getBoolean(HtmlSettingsActivity.KEY_LOAD_WITH_OVERVIEW_MODE, true)) 
    }
    var useWideViewPort by remember { 
        mutableStateOf(prefs.getBoolean(HtmlSettingsActivity.KEY_USE_WIDE_VIEW_PORT, true)) 
    }
    var builtInZoomControls by remember { 
        mutableStateOf(prefs.getBoolean(HtmlSettingsActivity.KEY_BUILTIN_ZOOM_CONTROLS, true)) 
    }
    var displayZoomControls by remember { 
        mutableStateOf(prefs.getBoolean(HtmlSettingsActivity.KEY_DISPLAY_ZOOM_CONTROLS, false)) 
    }
    var supportZoom by remember { 
        mutableStateOf(prefs.getBoolean(HtmlSettingsActivity.KEY_SUPPORT_ZOOM, true)) 
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // 顶部应用栏
        TopAppBar(
            title = {
                Text(
                    text = "HTML 设置",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "WebView 设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingSwitchItem(
                    title = "启用 JavaScript",
                    subtitle = "允许网页运行 JavaScript 代码",
                    checked = javaScriptEnabled,
                    onCheckedChange = { 
                        javaScriptEnabled = it
                        prefs.edit().putBoolean(HtmlSettingsActivity.KEY_JAVASCRIPT_ENABLED, it).apply()
                    }
                )
            }
            
            item {
                SettingSwitchItem(
                    title = "启用 DOM 存储",
                    subtitle = "允许网页使用本地存储",
                    checked = domStorageEnabled,
                    onCheckedChange = { 
                        domStorageEnabled = it
                        prefs.edit().putBoolean(HtmlSettingsActivity.KEY_DOM_STORAGE_ENABLED, it).apply()
                    }
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "显示设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingSwitchItem(
                    title = "概览模式加载",
                    subtitle = "以概览模式加载网页",
                    checked = loadWithOverviewMode,
                    onCheckedChange = { 
                        loadWithOverviewMode = it
                        prefs.edit().putBoolean(HtmlSettingsActivity.KEY_LOAD_WITH_OVERVIEW_MODE, it).apply()
                    }
                )
            }
            
            item {
                SettingSwitchItem(
                    title = "使用宽视口",
                    subtitle = "使用宽视口显示网页",
                    checked = useWideViewPort,
                    onCheckedChange = { 
                        useWideViewPort = it
                        prefs.edit().putBoolean(HtmlSettingsActivity.KEY_USE_WIDE_VIEW_PORT, it).apply()
                    }
                )
            }
            
            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "缩放设置",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                SettingSwitchItem(
                    title = "支持缩放",
                    subtitle = "允许用户缩放网页",
                    checked = supportZoom,
                    onCheckedChange = { 
                        supportZoom = it
                        prefs.edit().putBoolean(HtmlSettingsActivity.KEY_SUPPORT_ZOOM, it).apply()
                    }
                )
            }
            
            item {
                SettingSwitchItem(
                    title = "内置缩放控件",
                    subtitle = "启用内置的缩放控件",
                    checked = builtInZoomControls,
                    onCheckedChange = { 
                        builtInZoomControls = it
                        prefs.edit().putBoolean(HtmlSettingsActivity.KEY_BUILTIN_ZOOM_CONTROLS, it).apply()
                    }
                )
            }
            
            item {
                SettingSwitchItem(
                    title = "显示缩放控件",
                    subtitle = "显示屏幕上的缩放按钮",
                    checked = displayZoomControls,
                    onCheckedChange = { 
                        displayZoomControls = it
                        prefs.edit().putBoolean(HtmlSettingsActivity.KEY_DISPLAY_ZOOM_CONTROLS, it).apply()
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                // 应用设置按钮
                Button(
                    onClick = {
                        // 强制触发WebView重新组合以应用新设置
                        // 可以通过发送广播或其他方式通知WebView刷新
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "设置已自动保存",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "注意：设置修改后会自动保存，下次打开网页时生效。如需立即生效，请刷新当前网页。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * 设置开关项组件
 */
@Composable
fun SettingSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
