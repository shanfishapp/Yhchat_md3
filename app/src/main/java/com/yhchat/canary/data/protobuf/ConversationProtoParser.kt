package com.yhchat.canary.data.protobuf

import com.yhchat.canary.data.model.Conversation as ModelConversation
import com.yhchat.canary.data.model.AtData
import yh_conversation.Conversation

/**
 * Conversation ProtoBuf解析器
 */
object ConversationProtoParser {
    
    /**
     * 解析会话列表ProtoBuf响应
     */
    fun parseConversationList(protoBytes: ByteArray): Result<List<ModelConversation>> {
        return try {
            val conversationList = Conversation.ConversationList.parseFrom(protoBytes)
            
            if (conversationList.status.code == 1) {
                val conversations = conversationList.dataList.map { protoData ->
                    convertProtoToConversation(protoData)
                }
                Result.success(conversations)
            } else {
                Result.failure(Exception("获取会话列表失败: ${conversationList.status.msg}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("解析会话列表失败: ${e.message}"))
        }
    }
    
    /**
     * 将ProtoBuf数据转换为Conversation对象
     */
    private fun convertProtoToConversation(protoData: Conversation.ConversationList.ConversationData): ModelConversation {
        return ModelConversation(
            chatId = protoData.chatId,
            chatType = protoData.chatType,
            name = protoData.name,
            chatContent = protoData.chatContent,
            timestampMs = protoData.timestampMs,
            unreadMessage = protoData.unreadMessage,
            at = protoData.at,
            avatarId = protoData.avatarId,
            avatarUrl = if (protoData.avatarUrl.isNotEmpty()) protoData.avatarUrl else null,
            doNotDisturb = protoData.doNotDisturb,
            timestamp = protoData.timestamp,
            atData = if (protoData.hasAtData()) convertProtoAtData(protoData.atData) else null,
            certificationLevel = protoData.certificationLevel
        )
    }
    
    /**
     * 转换@数据
     */
    private fun convertProtoAtData(protoAtData: Conversation.ConversationList.ConversationData.AtData): AtData {
        return AtData(
            unknown = protoAtData.unknown,
            mentionedId = if (protoAtData.mentionedId.isNotEmpty()) protoAtData.mentionedId else null,
            mentionedName = if (protoAtData.mentionedName.isNotEmpty()) protoAtData.mentionedName else null,
            mentionedIn = if (protoAtData.mentionedIn.isNotEmpty()) protoAtData.mentionedIn else null,
            mentionerId = if (protoAtData.mentionerId.isNotEmpty()) protoAtData.mentionerId else null,
            mentionerName = if (protoAtData.mentionerName.isNotEmpty()) protoAtData.mentionerName else null,
            msgSeq = protoAtData.msgSeq
        )
    }
}