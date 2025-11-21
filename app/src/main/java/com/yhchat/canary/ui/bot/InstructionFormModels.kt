package com.yhchat.canary.ui.bot

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// 共享的数据类和枚举
enum class CustomFieldType(
    val displayName: String,
    val jsonType: String,
    val key: Int,
    val requiresOptions: Boolean = false,
    val supportsPlaceholder: Boolean = false,
    val supportsDefault: Boolean = false
) {
    RADIO("单选框", "radio", 0, requiresOptions = true),
    INPUT("输入框", "input", 1, supportsPlaceholder = true, supportsDefault = true),
    SWITCH("开关", "switch", 2, supportsDefault = true),
    CHECKBOX("多选框", "checkbox", 3, requiresOptions = true),
    TEXTAREA("多行输入框", "textarea", 4, supportsPlaceholder = true),
    SELECT("选择器", "select", 5, requiresOptions = true);
    
    companion object {
        fun fromJson(type: String): CustomFieldType? =
            values().firstOrNull { it.jsonType.equals(type, ignoreCase = true) }
    }
}

data class InstructionFormField(
    val id: String = randomFieldId(),
    val type: CustomFieldType,
    val label: String = "",
    val placeholder: String = "",
    val options: String = "",
    val defaultValue: String = ""
)

fun randomFieldId(): String =
    UUID.randomUUID().toString().replace("-", "").take(6)

fun parseCustomFields(json: String?): List<InstructionFormField> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val type = CustomFieldType.fromJson(obj.optString("type")) ?: continue
                val propsValue = obj.optJSONObject("propsValue") ?: JSONObject()
                add(
                    InstructionFormField(
                        id = obj.optString("id", randomFieldId()),
                        type = type,
                        label = propsValue.optString("label", ""),
                        placeholder = propsValue.optString("placeholder", ""),
                        options = propsValue.optString("options", ""),
                        defaultValue = propsValue.optString("defaultValue", "")
                    )
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}

fun buildCustomJson(fields: List<InstructionFormField>): String {
    val array = JSONArray()
    fields.forEach { field ->
        val obj = JSONObject()
        obj.put("key", field.type.key)
        obj.put("type", field.type.jsonType)
        obj.put("title", field.label.ifBlank { field.type.displayName })
        obj.put("id", field.id.ifBlank { randomFieldId() })
        
        val propsValue = JSONObject().apply {
            put("label", field.label)
            if (field.type.supportsPlaceholder) {
                put("placeholder", field.placeholder)
            }
            if (field.type.requiresOptions) {
                put("options", field.options)
            }
            if (field.type.supportsDefault || field.type == CustomFieldType.SWITCH) {
                put("defaultValue", field.defaultValue)
            }
        }
        obj.put("propsValue", propsValue)
        obj.put("props", buildProps(field))
        array.put(obj)
    }
    return array.toString()
}

private fun buildProps(field: InstructionFormField): JSONArray {
    val props = JSONArray()
    fun addProp(type: String, name: String, placeholder: String? = null, value: String = "") {
        val obj = JSONObject()
        obj.put("type", type)
        obj.put("name", name)
        obj.put("value", value)
        placeholder?.let { obj.put("placeholder", it) }
        props.put(obj)
    }
    
    addProp("label", "标签", value = field.label)
    when (field.type) {
        CustomFieldType.RADIO, CustomFieldType.CHECKBOX, CustomFieldType.SELECT -> {
            addProp(
                type = "options",
                name = "选项",
                placeholder = "用#分割，如：北京#上海#天津",
                value = field.options
            )
        }
        CustomFieldType.INPUT -> {
            addProp("defaultValue", "默认内容", value = field.defaultValue)
            addProp("placeholder", "占位文本", value = field.placeholder)
        }
        CustomFieldType.TEXTAREA -> {
            addProp("placeholder", "占位文本", value = field.placeholder)
        }
        CustomFieldType.SWITCH -> {
            addProp("defaultValue", "默认状态", placeholder = "1=打开,0=关闭", value = field.defaultValue)
        }
    }
    
    return props
}
