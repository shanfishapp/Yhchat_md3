package com.yhchat.canary.ui.bot

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yhchat.canary.data.model.BotInstruction
import com.yhchat.canary.ui.theme.YhchatCanaryTheme

class InstructionPreviewActivity : ComponentActivity() {
    
    companion object {
        private const val EXTRA_INSTRUCTION = "instruction"
        
        fun start(context: Context, instruction: BotInstruction) {
            val intent = Intent(context, InstructionPreviewActivity::class.java).apply {
                putExtra(EXTRA_INSTRUCTION, instruction)
            }
            context.startActivity(intent)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val instruction = intent.getSerializableExtra(EXTRA_INSTRUCTION) as? BotInstruction ?: run {
            finish()
            return
        }
        
        setContent {
            YhchatCanaryTheme {
                InstructionPreviewScreen(
                    instruction = instruction,
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructionPreviewScreen(
    instruction: BotInstruction,
    onBackClick: () -> Unit
) {
    val customFields = remember { parseCustomFields(instruction.customJson) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "指令预览 - ${instruction.name}",
                        fontWeight = FontWeight.Bold
                    ) 
                },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 指令基本信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "指令信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = instruction.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = when (instruction.instructionType) {
                                    1 -> "普通指令"
                                    2 -> "直发指令"
                                    5 -> "自定义输入指令"
                                    else -> "未知类型"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    
                    if (!instruction.desc.isNullOrEmpty()) {
                        Text(
                            text = instruction.desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (!instruction.hintText.isNullOrEmpty()) {
                        Text(
                            text = "提示文字: ${instruction.hintText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (!instruction.defaultText.isNullOrEmpty()) {
                        Text(
                            text = "默认文字: ${instruction.defaultText}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 自定义字段预览（仅自定义输入指令）
            if (instruction.instructionType == 5 && customFields.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "表单预览",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        customFields.forEach { field ->
                            CustomFieldPreview(field = field)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomFieldPreview(field: InstructionFormField) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = field.label.ifBlank { field.type.displayName },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
        
        when (field.type) {
            CustomFieldType.RADIO -> {
                val options = field.options.split("#").filter { it.isNotBlank() }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = false, onClick = { })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            
            CustomFieldType.INPUT -> {
                OutlinedTextField(
                    value = field.defaultValue,
                    onValueChange = { },
                    placeholder = if (field.placeholder.isNotBlank()) {
                        { Text(field.placeholder) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    singleLine = true
                )
            }
            
            CustomFieldType.SWITCH -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Switch(
                        checked = field.defaultValue == "1",
                        onCheckedChange = { },
                        enabled = false
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (field.defaultValue == "1") "开启" else "关闭",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            CustomFieldType.CHECKBOX -> {
                val options = field.options.split("#").filter { it.isNotBlank() }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    options.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = false, onCheckedChange = { }, enabled = false)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = option, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            
            CustomFieldType.TEXTAREA -> {
                OutlinedTextField(
                    value = field.defaultValue,
                    onValueChange = { },
                    placeholder = if (field.placeholder.isNotBlank()) {
                        { Text(field.placeholder) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    minLines = 3,
                    maxLines = 5
                )
            }
            
            CustomFieldType.SELECT -> {
                val options = field.options.split("#").filter { it.isNotBlank() }
                
                OutlinedTextField(
                    value = options.firstOrNull() ?: "请选择...",
                    onValueChange = { },
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "下拉"
                        )
                    }
                )
            }
        }
    }
}

