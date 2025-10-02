创建分享链接

POST /v1/share/create

请求头:
名称	必须	备注
token	是	无

请求体：

{
  "chatId": "会话id",
  "chatType": 2, // 会话类型，1-用户，2-群聊，3-机器人
  "chatName": "会话名称"
}

响应体：

{
  "code": 1, // 请求状态码，1为正常
  "data": {
    "imageKey": "share/...", // 图片key
    "key": "123123123", // 分享链接的key
    "shareUrl": "https://yhfx.jwznb.com/", // 分享开头的url
    "ts": 123123123 // 分享链接创建时间戳
  },
  "msg": "success" // 返回消息
}