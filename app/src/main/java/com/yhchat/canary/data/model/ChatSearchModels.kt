package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

data class ChatSearchMessage(
    @SerializedName("id")
    val id: String,
    @SerializedName("sequence")
    val sequence: Long,
    @SerializedName("chatId")
    val chatId: String,
    @SerializedName("chatType")
    val chatType: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("avatarUrl")
    val avatarUrl: String?,
    @SerializedName("content")
    val content: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("time")
    val time: Long,
    @SerializedName("timeText")
    val timeText: String
)

data class ChatSearchRequest(
    @SerializedName("word")
    val word: String,
    @SerializedName("chatId")
    val chatId: String,
    @SerializedName("chatType")
    val chatType: Int,
    @SerializedName("type")
    val type: String = "all",
    @SerializedName("size")
    val size: Int = 30,
    @SerializedName("time")
    val time: Long = 9999999999999L,
    @SerializedName("direction")
    val direction: Int = 1
)

data class ChatSearchResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("data")
    val data: ChatSearchData?,
    @SerializedName("msg")
    val msg: String
)

data class ChatSearchData(
    @SerializedName("list")
    val list: List<ChatSearchMessage>
)
