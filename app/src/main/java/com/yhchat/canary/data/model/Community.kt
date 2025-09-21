package com.yhchat.canary.data.model

import com.google.gson.annotations.SerializedName

/**
 * 社区分区
 */
data class CommunityBoard(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("avatar")
    val avatar: String,
    
    @SerializedName("delTime")
    val delTime: Long,
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("lastActive")
    val lastActive: Long,
    
    @SerializedName("memberNum")
    val memberNum: Int,
    
    @SerializedName("postNum")
    val postNum: Int,
    
    @SerializedName("groupNum")
    val groupNum: Int,
    
    @SerializedName("createTimeText")
    val createTimeText: String,
    
    @SerializedName("isFollowed")
    val isFollowed: String
)

/**
 * 社区文章
 */
data class CommunityPost(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("baId")
    val baId: Int,
    
    @SerializedName("senderId")
    val senderId: String,
    
    @SerializedName("senderNicknameId")
    val senderNicknameId: Int,
    
    @SerializedName("senderAvatarId")
    val senderAvatarId: Int,
    
    @SerializedName("groupId")
    val groupId: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("contentType")
    val contentType: Int, // 1-文本，2-markdown
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("delTime")
    val delTime: Long,
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("updateTime")
    val updateTime: Long,
    
    @SerializedName("editTime")
    val editTime: Long,
    
    @SerializedName("lastActive")
    val lastActive: Long,
    
    @SerializedName("likeNum")
    val likeNum: Int,
    
    @SerializedName("commentNum")
    val commentNum: Int,
    
    @SerializedName("collectNum")
    val collectNum: Int,
    
    @SerializedName("amountNum")
    val amountNum: Double,
    
    @SerializedName("senderNickname")
    val senderNickname: String,
    
    @SerializedName("senderAvatar")
    val senderAvatar: String,
    
    @SerializedName("createTimeText")
    val createTimeText: String,
    
    @SerializedName("group")
    val group: CommunityGroup?,
    
    @SerializedName("isLiked")
    val isLiked: String,
    
    @SerializedName("isCollected")
    val isCollected: Int,
    
    @SerializedName("isReward")
    val isReward: Int,
    
    @SerializedName("isVip")
    val isVip: Int
)

/**
 * 社区群组
 */
data class CommunityGroup(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("groupId")
    val groupId: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("introduction")
    val introduction: String,
    
    @SerializedName("createBy")
    val createBy: String,
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("avatarId")
    val avatarId: Int,
    
    @SerializedName("del_flag")
    val delFlag: Int,
    
    @SerializedName("avatarUrl")
    val avatarUrl: String,
    
    @SerializedName("headcount")
    val headcount: Int,
    
    @SerializedName("readHistory")
    val readHistory: Int,
    
    @SerializedName("alwaysAgree")
    val alwaysAgree: Int,
    
    @SerializedName("categoryId")
    val categoryId: Int,
    
    @SerializedName("category")
    val category: String,
    
    @SerializedName("private")
    val private: Int,
    
    @SerializedName("banId")
    val banId: Int,
    
    @SerializedName("gag")
    val gag: Int,
    
    @SerializedName("gagBy")
    val gagBy: String,
    
    @SerializedName("msgTypeLimit")
    val msgTypeLimit: String
)

/**
 * 社区评论
 */
data class CommunityComment(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("postId")
    val postId: Int,
    
    @SerializedName("parentId")
    val parentId: Int,
    
    @SerializedName("senderId")
    val senderId: String,
    
    @SerializedName("sender_nicknameId")
    val senderNicknameId: Int,
    
    @SerializedName("sender_avatarUd")
    val senderAvatarId: Int,
    
    @SerializedName("content")
    val content: String,
    
    @SerializedName("delTime")
    val delTime: Long,
    
    @SerializedName("createTime")
    val createTime: Long,
    
    @SerializedName("likeNum")
    val likeNum: Int,
    
    @SerializedName("repliesNum")
    val repliesNum: Int,
    
    @SerializedName("amountNum")
    val amountNum: Double,
    
    @SerializedName("auditStatus")
    val auditStatus: Int,
    
    @SerializedName("replies")
    val replies: List<CommunityComment>?,
    
    @SerializedName("senderNickname")
    val senderNickname: String,
    
    @SerializedName("senderAvatar")
    val senderAvatar: String,
    
    @SerializedName("createTimeText")
    val createTimeText: String,
    
    @SerializedName("isLiked")
    val isLiked: String,
    
    @SerializedName("isReward")
    val isReward: Int,
    
    @SerializedName("isVip")
    val isVip: Int
)

/**
 * 分区列表响应
 */
data class BoardListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: BoardListData,
    
    @SerializedName("msg")
    val msg: String
)

data class BoardListData(
    @SerializedName("ba")
    val boards: List<CommunityBoard>,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 文章列表响应
 */
data class PostListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: PostListData,
    
    @SerializedName("msg")
    val msg: String
)

data class PostListData(
    @SerializedName("posts")
    val posts: List<CommunityPost>,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 文章详情响应
 */
data class PostDetailResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: PostDetailData,
    
    @SerializedName("msg")
    val msg: String
)

data class PostDetailData(
    @SerializedName("ba")
    val board: CommunityBoard,
    
    @SerializedName("isAdmin")
    val isAdmin: Int,
    
    @SerializedName("post")
    val post: CommunityPost
)

/**
 * 评论列表响应
 */
data class CommentListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: CommentListData,
    
    @SerializedName("msg")
    val msg: String
)

data class CommentListData(
    @SerializedName("comments")
    val comments: List<CommunityComment>,
    
    @SerializedName("isAdmin")
    val isAdmin: Int,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 分区信息响应
 */
data class BoardInfoResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: BoardInfoData,
    
    @SerializedName("msg")
    val msg: String
)

data class BoardInfoData(
    @SerializedName("ba")
    val board: CommunityBoard
)

/**
 * 创建文章响应
 */
data class CreatePostResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: CreatePostData,
    
    @SerializedName("msg")
    val msg: String
)

data class CreatePostData(
    @SerializedName("audioUrl")
    val postId: Int  // 注意：API返回的字段名是audioUrl，但实际是文章ID
)

/**
 * 我的文章列表响应
 */
data class MyPostListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: MyPostListData,
    
    @SerializedName("msg")
    val msg: String
)

data class MyPostListData(
    @SerializedName("posts")
    val posts: List<CommunityPost>,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 关注分区列表响应
 */
data class FollowingBoardListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: FollowingBoardListData,
    
    @SerializedName("msg")
    val msg: String
)

data class FollowingBoardListData(
    @SerializedName("ba")
    val boards: List<CommunityBoard>,
    
    @SerializedName("total")
    val total: Int
)