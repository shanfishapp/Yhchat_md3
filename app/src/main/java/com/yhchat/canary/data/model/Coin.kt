package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 商品列表请求
 */
data class ProductListRequest(
    @SerializedName("size")
    val size: Int = 100,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 商品列表响应
 */
data class ProductListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: ProductListData,
    
    @SerializedName("msg")
    val msg: String
)

data class ProductListData(
    @SerializedName("list")
    val list: List<Product>,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 商品
 */
data class Product(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("type")
    val type: Int,
    
    @SerializedName("cycle")
    val cycle: Int,  // 会员持续时间（天数）
    
    @SerializedName("info")
    val info: String,
    
    @SerializedName("name")
    val name: String,  // 商品名
    
    @SerializedName("imageUrls")
    val imageUrls: String,  // 商品图片URL（JSON数组字符串）
    
    @SerializedName("price")
    val price: Int,  // 价格（金币）
    
    @SerializedName("priceVip")
    val priceVip: Int,  // VIP专享价格
    
    @SerializedName("stock")
    val stock: Int,  // 库存
    
    @SerializedName("sale")
    val sale: Int,  // 已售
    
    @SerializedName("description")
    val description: String,  // 商品描述
    
    @SerializedName("delTime")
    val delTime: Long = 0,
    
    @SerializedName("createTime")
    val createTime: Long = 0,
    
    @SerializedName("lastUpdate")
    val lastUpdate: Long = 0
) {
    // 解析图片URL列表
    fun getImageUrls(): List<String> {
        return try {
            val jsonArray = org.json.JSONArray(imageUrls)
            List(jsonArray.length()) { i -> jsonArray.getString(i) }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * 我的金币任务响应
 */
data class MyTaskInfoResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: MyTaskInfo,
    
    @SerializedName("msg")
    val msg: String
)

data class MyTaskInfo(
    @SerializedName("adsWatchNumber")
    val adsWatchNumber: Int,  // 广告观看次数
    
    @SerializedName("avatarEditNumber")
    val avatarEditNumber: Int,  // 是否改了头像，0-未更改，1-已更改
    
    @SerializedName("nicknameEditNumber")
    val nicknameEditNumber: Int,  // 是否改了名字，0-未更改，1-已更改
    
    @SerializedName("raffleTimes")
    val raffleTimes: Int  // 抽奖时间
)

/**
 * 商品详情请求
 */
data class ProductDetailRequest(
    @SerializedName("id")
    val id: Long
)

/**
 * 商品详情响应
 */
data class ProductDetailResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: ProductDetailData,
    
    @SerializedName("msg")
    val msg: String
)

data class ProductDetailData(
    @SerializedName("product")
    val product: Product
)

/**
 * 金币增减记录请求
 */
data class GoldCoinRecordRequest(
    @SerializedName("size")
    val size: Int = 20,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 金币增减记录响应
 */
data class GoldCoinRecordResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: GoldCoinRecordData,
    
    @SerializedName("msg")
    val msg: String
)

data class GoldCoinRecordData(
    @SerializedName("goldCoinRecord")
    val goldCoinRecord: List<GoldCoinRecord>,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 金币记录
 */
data class GoldCoinRecord(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: String,
    
    @SerializedName("typ")
    val type: Int,  // 类型
    
    @SerializedName("beforeAmount")
    val beforeAmount: Double,  // 之前金币数量
    
    @SerializedName("afterAmount")
    val afterAmount: Double,  // 之后的金币数量
    
    @SerializedName("changeAmount")
    val changeAmount: Double,  // 增加/减少的金币数量
    
    @SerializedName("reason")
    val reason: String,  // 增加/减少金币的原因
    
    @SerializedName("remark")
    val remark: String = "",  // 备注
    
    @SerializedName("createTime")
    val createTime: Long  // 创建时间戳
)

/**
 * 打赏记录请求
 */
data class RewardRecordRequest(
    @SerializedName("typ")
    val type: String,  // "post"或"comment"
    
    @SerializedName("size")
    val size: Int = 20,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 打赏记录响应
 */
data class RewardRecordResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: RewardRecordData,
    
    @SerializedName("msg")
    val msg: String
)

data class RewardRecordData(
    @SerializedName("rewards")
    val rewards: List<RewardRecord>,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 打赏记录
 */
data class RewardRecord(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("senderId")
    val senderId: String,  // 打赏者id
    
    @SerializedName("recvId")
    val recvId: String,  // 接收者id（文章作者）
    
    @SerializedName("postId")
    val postId: Long,  // 文章id
    
    @SerializedName("commentId")
    val commentId: Long,  // 评论id
    
    @SerializedName("amount")
    val amount: Double,  // 打赏金额
    
    @SerializedName("recvAmount")
    val recvAmount: Double,  // 收到的金币数量
    
    @SerializedName("createTime")
    val createTime: Long,  // 创建时间戳
    
    @SerializedName("reason")
    val reason: String,  // 原因
    
    @SerializedName("remark")
    val remark: String = "",  // 备注
    
    @SerializedName("post")
    val post: RewardPost? = null,  // 文章信息
    
    @SerializedName("sender")
    val sender: RewardUser? = null,  // 打赏者信息
    
    @SerializedName("comment")
    val comment: RewardComment? = null  // 评论信息
)

/**
 * 打赏相关的文章信息
 */
data class RewardPost(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("title")
    val title: String,  // 文章标题
    
    @SerializedName("senderId")
    val senderId: String,
    
    @SerializedName("senderNickname")
    val senderNickname: String,
    
    @SerializedName("senderAvatar")
    val senderAvatar: String,
    
    @SerializedName("createTimeText")
    val createTimeText: String
)

/**
 * 打赏相关的用户信息
 */
data class RewardUser(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("nickname")
    val nickname: String,
    
    @SerializedName("avatar_url")
    val avatarUrl: String
)

/**
 * 打赏相关的评论信息
 */
data class RewardComment(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("postId")
    val postId: Long,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("senderNickname")
    val senderNickname: String,
    
    @SerializedName("createTimeText")
    val createTimeText: String
)

