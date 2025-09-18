package com.yhchat.canary.data.websocket

import com.yhchat.canary.data.model.Message
import com.yhchat.canary.data.model.MessageContent
import yh_ws_go.*

/**
 * WebSocket消息解析器
 */
object WebSocketMessageParser {
    
    /**
     * 解析WebSocket二进制消息
     */
    fun parseWebSocketMessage(bytes: ByteArray): com.yhchat.canary.data.model.WebSocketMessage? {
        return try {
            // 先尝试解析为heartbeat_ack
            val heartbeatAck = ChatWsGo.heartbeat_ack.parseFrom(bytes)
            if (heartbeatAck.hasInfo()) {
                return com.yhchat.canary.data.model.WebSocketMessage(
                    cmd = heartbeatAck.info.cmd,
                    data = null,
                    seq = heartbeatAck.info.seq
                )
            }
            
            // 根据cmd类型解析不同的消息
            val info = ChatWsGo.heartbeat_ack.parseFrom(bytes).info
            when (info.cmd) {
                "push_message" -> {
                    val pushMessage = ChatWsGo.push_message.parseFrom(bytes)
                    if (pushMessage.hasData()) {
                        val message = convertProtoToMessage(pushMessage.data)
                        com.yhchat.canary.data.model.WebSocketMessage(
                            cmd = "push_message",
                            data = mapOf("message" to message),
                            seq = info.seq
                        )
                    } else null
                }
                "edit_message" -> {
                    val editMessage = ChatWsGo.edit_message.parseFrom(bytes)
                    if (editMessage.hasData()) {
                        val message = convertProtoToMessage(editMessage.data)
                        com.yhchat.canary.data.model.WebSocketMessage(
                            cmd = "edit_message",
                            data = mapOf("message" to message),
                            seq = info.seq
                        )
                    } else null
                }
                "draft_input" -> {
                    val draftInput = ChatWsGo.draft_input.parseFrom(bytes)
                    com.yhchat.canary.data.model.WebSocketMessage(
                        cmd = "draft_input",
                        data = mapOf(
                            "chatId" to draftInput.chatId,
                            "chatType" to draftInput.chatType,
                            "content" to draftInput.text
                        ),
                        seq = info.seq
                    )
                }
                "bot_board_message" -> {
                    val botBoardMessage = ChatWsGo.bot_board_message.parseFrom(bytes)
                    if (botBoardMessage.hasData()) {
                        val message = convertProtoToMessage(botBoardMessage.data)
                        com.yhchat.canary.data.model.WebSocketMessage(
                            cmd = "bot_board_message",
                            data = mapOf("message" to message),
                            seq = info.seq
                        )
                    } else null
                }
                else -> {
                    com.yhchat.canary.data.model.WebSocketMessage(
                        cmd = info.cmd,
                        data = null,
                        seq = info.seq
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将ProtoBuf消息转换为Message对象
     */
    private fun convertProtoToMessage(protoMsg: ChatWsGo.Msg): Message {
        return Message(
            msgId = protoMsg.msgId,
            sender = com.yhchat.canary.data.model.MessageSender(
                chatId = protoMsg.sender.chatId,
                chatType = protoMsg.sender.chatType,
                name = protoMsg.sender.name,
                avatarUrl = protoMsg.sender.avatarUrl
            ),
            direction = "left", // 默认接收消息
            contentType = protoMsg.contentType,
            content = MessageContent(text = protoMsg.content.text),
            sendTime = protoMsg.timestamp,
            msgSeq = protoMsg.msgSeq,
            quoteMsgId = if (protoMsg.quoteMsgId.isNotEmpty()) protoMsg.quoteMsgId else null,
            editTime = protoMsg.editTime,
            msgDeleteTime = protoMsg.deleteTime,
            cmd = if (protoMsg.hasCmd()) com.yhchat.canary.data.model.MessageCmd(
                name = protoMsg.cmd.name,
                type = protoMsg.cmd.id.toInt()
            ) else null
        )
    }
}