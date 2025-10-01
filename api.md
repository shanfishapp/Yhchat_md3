POST /v1/group/edit-group

请求头:
名称	必须	备注
token	是	群聊管理员token

请求体:

group_id: "123" // 目标群聊ID
name: "测试群聊名称" // 群聊名称
introduction: "测试群聊简介" // 群聊简介
avatarUrl: "https://..." // 群聊头像url
direct_join: 0 // 进群免审核,1为开启
history_msg: 1 // 历史消息,1为开启
category_name: "无" // 分类名
category_id: 40 // 分类ID
private: 0 // 是否私有,1为私有

ProtoBuf数据结构

message edit_group_send {
  string group_id = 2; // 目标群聊ID
  string name = 3; // 群聊名称
  string introduction = 4; // 群聊简介
  string avatarUrl = 5; // 群聊头像url
  uint64 direct_join = 6; // 进群免审核,1为开启
  uint64 history_msg = 7; // 历史消息,1为开启
  string category_name = 8; // 分类名
  uint64 category_id = 9; // 分类ID
  uint64 private = 10; // 是否私有,1为私有
}

message edit_group_send {
  string group_id = 2; // 目标群聊ID
  string name = 3; // 群聊名称
  string introduction = 4; // 群聊简介
  string avatarUrl = 5; // 群聊头像url
  uint64 direct_join = 6; // 进群免审核,1为开启
  uint64 history_msg = 7; // 历史消息,1为开启
  string category_name = 8; // 分类名
  uint64 category_id = 9; // 分类ID
  uint64 private = 10; // 是否私有,1为私有
}

响应体:

status {
  number: 114514
  code: 1
  msg: "success"
}

ProtoBuf数据结构

message edit_group {
    Status status = 1;
}

message edit_group {
    Status status = 1;
    message Status {
        uint64 number = 1; // 不知道干啥的,可能是请求ID
        uint64 code = 2; // 状态码,1为正常
        string msg = 3; // 返回消息
    }
}