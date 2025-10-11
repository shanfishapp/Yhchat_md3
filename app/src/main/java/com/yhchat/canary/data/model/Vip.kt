package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * VIP价格列表请求
 */
data class VipProductListRequest(
    @SerializedName("platform")
    val platform: String = "android"  // 平台标识
)

/**
 * VIP价格列表响应
 */
data class VipProductListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: VipProductListData,
    
    @SerializedName("msg")
    val msg: String
)

data class VipProductListData(
    @SerializedName("list")
    val list: List<VipProduct>,
    
    @SerializedName("qrCodeUrl")
    val qrCodeUrl: String  // 二维码获取地址
)

/**
 * VIP商品
 */
data class VipProduct(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,  // 商品名
    
    @SerializedName("description")
    val description: String,  // 商品描述
    
    @SerializedName("price")
    val price: Double,  // 价格（人民币元）
    
    @SerializedName("priceOriginal")
    val priceOriginal: Double,  // 原价
    
    @SerializedName("day")
    val day: Int,  // VIP持续天数
    
    @SerializedName("productId")
    val productId: String = ""
)

/**
 * VIP特权列表响应
 */
data class VipBenefitsListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: VipBenefitsListData,
    
    @SerializedName("msg")
    val msg: String
)

data class VipBenefitsListData(
    @SerializedName("list")
    val list: List<VipBenefit>
)

/**
 * VIP特权
 */
data class VipBenefit(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("name")
    val name: String,  // 特权名称
    
    @SerializedName("description")
    val description: String,  // 特权描述
    
    @SerializedName("target")
    val target: String = "",  // 针对对象
    
    @SerializedName("sort")
    val sort: Int = 0  // 排序
)

