package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

data class EditMyGroupNicknameRequest(
    @SerializedName("groupId")
    val groupId: String,
    
    @SerializedName("botId")  // API使用botId字段来存储群昵称
    val botId: String
)