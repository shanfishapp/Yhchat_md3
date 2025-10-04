撤回信息

POST /v1/msg/recall-msg

请求头:
名称	必须	备注
token	是	空

请求体:

msg_id: "123123123123123123" // 信息ID
chat_id: "123" // 信息所属对象ID
chat_type: 2 // 信息所属对象类型, 1-用户 2-群聊 3-机器人

ProtoBuf数据结构

// 通过msgId撤回消息
message recall_msg_send {
    string msg_id = 2; // 信息ID
    string chat_id = 3; // 信息所属对象ID
    uint64 chat_type = 4; // 信息所属对象类型, 1-用户 2-群聊 3-机器人
}

// 通过msgId撤回消息
message recall_msg_send {
    string msg_id = 2; // 信息ID
    string chat_id = 3; // 信息所属对象ID
    uint64 chat_type = 4; // 信息所属对象类型, 1-用户 2-群聊 3-机器人
}

响应体:

status {
  number: 114514
  code: 1
  msg: "success"
}

ProtoBuf数据结构

// 撤回消息返回数据
message recall_msg {
    Status status = 1;
}

// 撤回消息返回数据
message recall_msg {
    Status status = 1;
}