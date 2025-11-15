---
title: mount-setting
---

未特别说明情况下请求域名均为 `https://chat-go.jwzhd.com`  
没写请求/响应项目表示不需要相关参数.  

## 添加群WebDAV挂载

POST /v1/mount-setting/create

请求头:  

|名称|必须|备注|
|-----|-----|-----|
|token|是|目标群管理员token|

请求体:  

```JSONC
{
  "groupId": "big", // 目标群聊ID
  "mountName": "测试挂载名称", // 挂载名称
  "webdavUrl": "https://...", // 挂载网址
  "webdavUserName": "测试挂载用户名", // 挂载用户名
  "webdavPassword": "测试挂载密码", // 挂载密码
  "webdavRootPath": "测试挂载目录" // 挂载目录
}
```

响应体:  

```JSONC
{
  "code": 1, // 请求状态码，1为正常
  "msg": "success" // 返回消息
}
```

## 删除群WebDAV挂载

POST /v1/mount-setting/delete

请求头:  

|名称|必须|备注|
|-----|-----|-----|
|token|是|目标群管理员token|

请求体:  

```JSONC
{
  "id": 123 // 挂载ID
}
```

响应体:  

```JSONC
{
  "code": 1, // 请求状态码，1为正常
  "msg": "success" // 返回消息
}
```


## 请求webdav设置

POST
https://chat-go.jwzhd.com/v1/mount-setting/list

请求体：

```JSONC
{
  "groupId": "979377289", // 群聊id
  "encryptKey": "", // 加密密钥（和RSAEncryptionUtil.kt中生成后的key一致）
  "encryptIv": "" // 加密iv（和RSAEncryptionUtil.kt中生成后的iv一致）
}
```

响应体：

```JSONC
{
  "code": 1,
  "data": {
    "list": [
      {
        "id": 213, // 挂载ID
        "groupId": "979377289", // 群聊id
        "mountName": "disroot", // 挂载名称
        "webdavUrl": "https://cloud.disroot.org/remote.php/dav/files/ushio_noa", // 挂载网址
        "webdavUserName": "ushio_noa", // 挂载用户名
        "webdavPassword": "Kq3FAaPj14b5b+7BxE059g==", // 挂载密码（在用户ui层面显示要解密）
        "webdavRootPath": "/yh", // 挂载目录
        "createTime": 1759833024, // 创建时间戳
        "delFlag": 0, // 删除标志
        "userId": "5940358" // 挂载点创建者用户id（如果用户id与个人资料中的id不一致，那么不显示这个挂载点）
      }
    ]
  },
  "msg": "success"
}
```
