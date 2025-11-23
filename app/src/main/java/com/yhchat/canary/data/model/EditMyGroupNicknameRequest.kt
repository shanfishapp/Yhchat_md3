package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

data class EditMyGroupNicknameRequest(
    @SerializedName("groupId")
    val groupId: String,
    
    @SerializedName("botId")
    val botId: String  // 用botId字段来存储群昵称
)