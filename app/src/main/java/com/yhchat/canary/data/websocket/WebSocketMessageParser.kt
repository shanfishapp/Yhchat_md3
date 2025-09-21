package com.yhchat.canary.data.websocket

import com.yhchat.canary.data.model.ChatMessage
import com.yhchat.canary.data.model.MessageContent
import com.yhchat.canary.data.model.MessageSender
import com.yhchat.canary.data.model.MessageTag
import com.yhchat.canary.data.model.MessageCmd
import com.yhchat.canary.data.model.WebSocketMessage
import com.yhchat.canary.proto.chat_ws_go.heartbeat_ack
import com.yhchat.canary.proto.chat_ws_go.push_message
import com.yhchat.canary.proto.chat_ws_go.edit_message
import com.yhchat.canary.proto.chat_ws_go.draft_input
import com.yhchat.canary.proto.chat_ws_go.bot_board_message
import com.yhchat.canary.proto.chat_ws_go.WsMsg

/**
 * WebSocket消息解析器
 */
object WebSocketMessageParser {
    
    /**
     * 解析WebSocket二进制消息
     */
    fun parseWebSocketMessage(bytes: ByteArray): WebSocketMessage? {
        return try {
            // 先尝试解析为heartbeat_ack获取命令类型
            val tempMsg = heartbeat_ack.parseFrom(bytes)
            val cmd = tempMsg.info.cmd
            val seq = tempMsg.info.seq

            when (cmd) {
                "heartbeat_ack" -> {
                    WebSocketMessage(
                        cmd = cmd,
                        data = null,
                        seq = seq
                    )
                }
                "push_message" -> {
                    val pushMessage = push_message.parseFrom(bytes)
                    if (pushMessage.hasData() && pushMessage.data.hasMsg()) {
                        val message = convertProtoToMessage(pushMessage.data.msg)
                        WebSocketMessage(
                            cmd = "push_message",
                            data = mapOf("message" to message),
                            seq = seq
                        )
                    } else null
                }
                "edit_message" -> {
                    val editMessage = edit_message.parseFrom(bytes)
                    if (editMessage.hasData() && editMessage.data.hasMsg()) {
                        val message = convertProtoToMessage(editMessage.data.msg)
                        WebSocketMessage(
                            cmd = "edit_message",
                            data = mapOf("message" to message),
                            seq = seq
                        )
                    } else null
                }
                "draft_input" -> {
                    val draftInput = draft_input.parseFrom(bytes)
                    WebSocketMessage(
                        cmd = "draft_input",
                        data = mapOf(
                            "chatId" to draftInput.data.draft.chatId,
                            "content" to draftInput.data.draft.input
                        ),
                        seq = seq
                    )
                }
                "bot_board_message" -> {
                    val botBoardMessage = bot_board_message.parseFrom(bytes)
                    WebSocketMessage(
                        cmd = "bot_board_message",
                        data = mapOf(
                            "botId" to botBoardMessage.data.board.botId,
                            "content" to botBoardMessage.data.board.content
                        ),
                        seq = seq
                    )
                }
                else -> {
                    WebSocketMessage(
                        cmd = cmd,
                        data = null,
                        seq = seq
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将ProtoBuf消息转换为ChatMessage对象
     */
    private fun convertProtoToMessage(protoMsg: WsMsg): ChatMessage {
        val sender = MessageSender(
            chatId = protoMsg.sender.chatId,
            chatType = protoMsg.sender.chatType,
            name = protoMsg.sender.name,
            avatarUrl = protoMsg.sender.avatarUrl,
            tagOld = protoMsg.sender.tagOldList,
            tag = protoMsg.sender.tagList.map { tag ->
                MessageTag(
                    id = tag.id,
                    text = tag.text,
                    color = tag.color
                )
            }
        )

        val content = MessageContent(
            text = if (protoMsg.content.text.isNotEmpty()) protoMsg.content.text else null,
            buttons = if (protoMsg.content.buttons.isNotEmpty()) protoMsg.content.buttons else null,
            imageUrl = if (protoMsg.content.imageUrl.isNotEmpty()) protoMsg.content.imageUrl else null,
            fileName = if (protoMsg.content.fileName.isNotEmpty()) protoMsg.content.fileName else null,
            fileUrl = if (protoMsg.content.fileUrl.isNotEmpty()) protoMsg.content.fileUrl else null,
            form = if (protoMsg.content.form.isNotEmpty()) protoMsg.content.form else null,
            quoteMsgText = if (protoMsg.content.quoteMsgText.isNotEmpty()) protoMsg.content.quoteMsgText else null,
            stickerUrl = if (protoMsg.content.stickerUrl.isNotEmpty()) protoMsg.content.stickerUrl else null,
            postId = if (protoMsg.content.postId.isNotEmpty()) protoMsg.content.postId else null,
            postTitle = if (protoMsg.content.postTitle.isNotEmpty()) protoMsg.content.postTitle else null,
            postContent = if (protoMsg.content.postContent.isNotEmpty()) protoMsg.content.postContent else null,
            postContentType = if (protoMsg.content.postContentType.isNotEmpty()) protoMsg.content.postContentType else null,
            expressionId = if (protoMsg.content.expressionId.isNotEmpty()) protoMsg.content.expressionId else null,
            fileSize = if (protoMsg.content.fileSize > 0) protoMsg.content.fileSize else null,
            videoUrl = if (protoMsg.content.videoUrl.isNotEmpty()) protoMsg.content.videoUrl else null,
            audioUrl = if (protoMsg.content.audioUrl.isNotEmpty()) protoMsg.content.audioUrl else null,
            audioTime = if (protoMsg.content.audioTime > 0) protoMsg.content.audioTime else null,
            stickerItemId = if (protoMsg.content.stickerItemId > 0) protoMsg.content.stickerItemId else null,
            stickerPackId = if (protoMsg.content.stickerPackId > 0) protoMsg.content.stickerPackId else null,
            callText = if (protoMsg.content.callText.isNotEmpty()) protoMsg.content.callText else null,
            callStatusText = if (protoMsg.content.callStatusText.isNotEmpty()) protoMsg.content.callStatusText else null,
            width = if (protoMsg.content.width > 0) protoMsg.content.width else null,
            height = if (protoMsg.content.height > 0) protoMsg.content.height else null
        )

        val cmd = if (protoMsg.hasCmd()) {
            MessageCmd(
                name = protoMsg.cmd.name,
                type = protoMsg.cmd.id.toInt() // WebSocket Cmd uses 'id' for type
            )
        } else null

        return ChatMessage(
            msgId = protoMsg.msgId,
            sender = sender,
            direction = "left", // WebSocket消息通常是接收的消息
            contentType = protoMsg.contentType,
            content = content,
            sendTime = protoMsg.timestamp, // WebSocket uses 'timestamp'
            cmd = cmd,
            msgDeleteTime = if (protoMsg.deleteTime > 0) protoMsg.deleteTime else null,
            quoteMsgId = if (protoMsg.quoteMsgId.isNotEmpty()) protoMsg.quoteMsgId else null,
            msgSeq = protoMsg.msgSeq,
            editTime = if (protoMsg.editTime > 0) protoMsg.editTime else null
        )
    }
}
