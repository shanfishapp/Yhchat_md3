package com.yhchat.canary.data.api

import com.google.gson.annotations.SerializedName
import com.yhchat.canary.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * 云湖API服务接口
 */
interface ApiService {
    
    // 用户相关API
    @GET("v1/user/info")
    suspend fun getUserInfo(
        @Header("token") token: String
    ): Response<User>
    
    @GET("v1/user/info")
    suspend fun getUserProfile(
        @Header("token") token: String
    ): Response<ResponseBody>

    
    @POST("v1/user/captcha")
    suspend fun getCaptcha(): Response<CaptchaResponse>

    @POST("v1/verification/get-verification-code")
    suspend fun getSmsCaptcha(
        @Body request: SmsCaptchaRequest
    ): Response<Map<String, Any>>

    @POST("v1/verification/get-email-verification-code")
    suspend fun getEmailVerificationCode(
        @Body request: EmailVerificationRequest
    ): Response<Map<String, Any>>

    @POST("v1/user/forget-password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): Response<Map<String, Any>>

    @POST("v1/search/home-search")
    suspend fun homeSearch(
        @Header("token") token: String,
        @Body request: com.yhchat.canary.data.model.SearchRequest
    ): Response<com.yhchat.canary.data.model.SearchResponse>

    @POST("v1/sticky/list")
    suspend fun getStickyList(
        @Header("token") token: String
    ): Response<StickyResponse>

    @POST("v1/sticky/add")
    suspend fun addSticky(
        @Header("token") token: String,
        @Body request: StickyOperationRequest
    ): Response<Map<String, Any>>

    @POST("v1/sticky/delete")
    suspend fun deleteSticky(
        @Header("token") token: String,
        @Body request: StickyOperationRequest
    ): Response<Map<String, Any>>

    @POST("v1/sticky/topping")
    suspend fun topSticky(
        @Header("token") token: String,
        @Body request: StickyTopRequest
    ): Response<Map<String, Any>>

    @POST("v1/user/verification-login")
    suspend fun verificationLogin(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    
    @POST("v1/user/email-login")
    suspend fun emailLogin(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    
    @POST("v1/user/logout")
    suspend fun logout(
        @Header("token") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>
    
    // ========== 消息相关API（使用protobuf） ==========
    
    /**
     * 获取消息列表（按序列）- 使用protobuf
     */
    @POST("v1/msg/list-message-by-seq")
    suspend fun listMessageBySeq(
        @Header("token") token: String,
        @Body request: okhttp3.RequestBody
    ): Response<okhttp3.ResponseBody>
    
    /**
     * 获取消息列表 - 使用protobuf
     */
    @POST("v1/msg/list-message")
    suspend fun listMessage(
        @Header("token") token: String,
        @Body request: okhttp3.RequestBody
    ): Response<okhttp3.ResponseBody>
    
    /**
     * 发送消息 - 使用protobuf
     */
    @POST("v1/msg/send-message")
    suspend fun sendMessage(
        @Header("token") token: String,
        @Body request: okhttp3.RequestBody
    ): Response<okhttp3.ResponseBody>
    
    @POST("v1/msg/recall-msg")
    suspend fun recallMessage(
        @Header("token") token: String,
        @Body request: RecallMessageRequest
    ): Response<ApiStatus>
    
    @POST("v1/msg/recall-msg-batch")
    suspend fun recallMessagesBatch(
        @Header("token") token: String,
        @Body request: RecallMessagesBatchRequest
    ): Response<ApiStatus>
    
    @POST("v1/msg/button-report")
    suspend fun buttonReport(
        @Header("token") token: String,
        @Body request: ButtonReportRequest
    ): Response<ApiStatus>
    
    @POST("v1/msg/list-message-edit-record")
    suspend fun listMessageEditRecord(
        @Header("token") token: String,
        @Body request: ListMessageEditRecordRequest
    ): Response<Map<String, Any>>
    
    @POST("v1/msg/file-download-record")
    suspend fun fileDownloadRecord(
        @Header("token") token: String,
        @Body request: FileDownloadRecordRequest
    ): Response<Map<String, Any>>
    
    // 会话相关API
    @POST("v1/conversation/list")
    suspend fun listConversations(
        @Header("token") token: String
    ): Response<ResponseBody>
    
    @POST("v1/conversation/dismiss-notification")
    suspend fun dismissNotification(
        @Header("token") token: String,
        @Body request: DismissNotificationRequest
    ): Response<DismissNotificationResponse>
    
    // 社区相关API
    @POST("v1/community/ba/following-ba-list")
    suspend fun getBoardList(
        @Header("token") token: String,
        @Body request: BoardListRequest
    ): Response<com.yhchat.canary.data.model.BoardListResponse>
    
    @POST("v1/community/ba/following-ba-list")
    suspend fun getFollowingBoardList(
        @Header("token") token: String,
        @Body request: BoardListRequest
    ): Response<com.yhchat.canary.data.model.FollowingBoardListResponse>
    
    @POST("v1/community/posts/my-post-list")
    suspend fun getMyPostList(
        @Header("token") token: String,
        @Body request: MyPostListRequest
    ): Response<com.yhchat.canary.data.model.MyPostListResponse>
    
    @POST("v1/community/ba/info")
    suspend fun getBoardInfo(
        @Header("token") token: String,
        @Body request: BoardInfoRequest
    ): Response<com.yhchat.canary.data.model.BoardInfoResponse>
    
    @POST("v1/community/ba/user-follow-ba")
    suspend fun followBoard(
        @Header("token") token: String,
        @Body request: FollowBoardRequest
    ): Response<ApiStatus>

    @POST("v1/community/ba/user-unfollow-ba")
    suspend fun unfollowBoard(
        @Header("token") token: String,
        @Body request: UnfollowBoardRequest
    ): Response<ApiStatus>
    
    @POST("v1/community/posts/post-list")
    suspend fun getPostList(
        @Header("token") token: String,
        @Body request: PostListRequest
    ): Response<com.yhchat.canary.data.model.PostListResponse>
    
    @POST("v1/community/posts/post-detail")
    suspend fun getPostDetail(
        @Header("token") token: String,
        @Body request: PostDetailRequest
    ): Response<com.yhchat.canary.data.model.PostDetailResponse>
    
    @POST("v1/community/comment/comment-list")
    suspend fun getCommentList(
        @Header("token") token: String,
        @Body request: CommentListRequest
    ): Response<com.yhchat.canary.data.model.CommentListResponse>
    
    @POST("v1/community/posts/post-like")
    suspend fun likePost(
        @Header("token") token: String,
        @Body request: LikePostRequest
    ): Response<ApiStatus>
    
    @POST("v1/community/comment/comment-like")
    suspend fun likeComment(
        @Header("token") token: String,
        @Body request: LikeCommentRequest
    ): Response<ApiStatus>
    
    @POST("v1/community/posts/post-collect")
    suspend fun collectPost(
        @Header("token") token: String,
        @Body request: CollectPostRequest
    ): Response<ApiStatus>
    
    @POST("v1/community/posts/post-reward")
    suspend fun rewardPost(
        @Header("token") token: String,
        @Body request: RewardPostRequest
    ): Response<ApiStatus>
    
    @POST("v1/community/comment/comment")
    suspend fun commentPost(
        @Header("token") token: String,
        @Body request: CommentPostRequest
    ): Response<ApiStatus>
    
    @POST("v1/community/posts/create")
    suspend fun createPost(
        @Header("token") token: String,
        @Body request: CreatePostRequest
    ): Response<com.yhchat.canary.data.model.CreatePostResponse>
    
    @POST("v1/community/search")
    suspend fun searchCommunity(
        @Header("token") token: String,
        @Body request: SearchRequest
    ): SearchResponse
    
    @POST("v1/community/ba/group-list")
    suspend fun getBoardGroupList(
        @Header("token") token: String,
        @Body request: GroupListRequest
    ): GroupListResponse

    // ========== 好友相关API ==========

    /**
     * 添加好友/加入群聊
     */
    @POST("v1/friend/apply")
    suspend fun addFriend(
        @Header("token") token: String,
        @Body request: AddFriendRequest
    ): Response<ApiStatus>
    
}

/**
 * 列表消息请求
 */
data class ListMessageRequest(
    @SerializedName("msg_count")
    val msgCount: Int,
    
    @SerializedName("msg_id")
    val msgId: String = "",
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("chat_id")
    val chatId: String
)

/**
 * 按序列列表消息请求
 */
data class ListMessageBySeqRequest(
    @SerializedName("msg_start")
    val msgStart: Long = 0,
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("chat_id")
    val chatId: String
)

/**
 * 按消息ID列表消息请求
 */
data class ListMessageByMidSeqRequest(
    @SerializedName("request_id")
    val requestId: Long? = null,
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("unknown")
    val unknown: Int = 0,
    
    @SerializedName("msg_count")
    val msgCount: Int,
    
    @SerializedName("msg_id")
    val msgId: String
)

/**
 * 编辑消息请求
 */
data class EditMessageRequest(
    @SerializedName("msg_id")
    val msgId: String,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("content_type")
    val contentType: Int,
    
    @SerializedName("content")
    val content: MessageContent,
    
    @SerializedName("quote_msg_id")
    val quoteMsgId: String? = null
)

/**
 * 撤回消息请求
 */
data class RecallMessageRequest(
    @SerializedName("msg_id")
    val msgId: String,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int
)

/**
 * 批量撤回消息请求
 */
data class RecallMessagesBatchRequest(
    @SerializedName("msg_id")
    val msgIds: List<String>,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("chat_type")
    val chatType: Int
)

/**
 * 按钮报告请求
 */
data class ButtonReportRequest(
    @SerializedName("msg_id")
    val msgId: String,
    
    @SerializedName("chat_type")
    val chatType: Int,
    
    @SerializedName("chat_id")
    val chatId: String,
    
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("button_value")
    val buttonValue: String
)

/**
 * 消息编辑记录请求
 */
data class ListMessageEditRecordRequest(
    @SerializedName("msgId")
    val msgId: String,
    
    @SerializedName("size")
    val size: Int = 10,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 文件下载记录请求
 */
data class FileDownloadRecordRequest(
    @SerializedName("msgId")
    val msgId: String,
    
    @SerializedName("downloadPath")
    val downloadPath: String
)

/**
 * 分区列表请求
 */
data class BoardListRequest(
    @SerializedName("typ")
    val typ: Int = 2,
    
    @SerializedName("size")
    val size: Int = 20,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 分区信息请求
 */
data class BoardInfoRequest(
    @SerializedName("id")
    val id: Int
)

/**
 * 文章列表请求
 */
data class PostListRequest(
    @SerializedName("typ")
    val typ: Int = 1,
    
    @SerializedName("baId")
    val baId: Int,
    
    @SerializedName("size")
    val size: Int = 20,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 文章详情请求
 */
data class PostDetailRequest(
    @SerializedName("id")
    val id: Int
)

/**
 * 评论列表请求
 */
data class CommentListRequest(
    @SerializedName("postId")
    val postId: Int,
    
    @SerializedName("size")
    val size: Int = 10,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 点赞文章请求
 */
data class LikePostRequest(
    @SerializedName("id")
    val id: Int
)

/**
 * 点赞评论请求
 */
data class LikeCommentRequest(
    @SerializedName("id")
    val id: Int // 评论ID
)

/**
 * 收藏文章请求
 */
data class CollectPostRequest(
    @SerializedName("id")
    val id: Int
)

/**
 * 打赏文章请求
 */
data class RewardPostRequest(
    @SerializedName("postId")
    val postId: Int,
    
    @SerializedName("amount")
    val amount: Double
)

/**
 * 评论文章请求
 */
data class CommentPostRequest(
    @SerializedName("postId")
    val postId: Int,
    
    @SerializedName("commentId")
    val commentId: Int = 0,
    
    @SerializedName("content")
    val content: String
)

/**
 * 创建文章请求
 */
data class CreatePostRequest(
    @SerializedName("baId")
    val baId: Int,

    @SerializedName("groupId")
    val groupId: String = "",

    @SerializedName("title")
    val title: String,

    @SerializedName("content")
    val content: String,

    @SerializedName("contentType")
    val contentType: Int  // 1-文本，2-markdown
)

/**
 * 添加好友/加入群聊请求
 */
data class AddFriendRequest(
    @SerializedName("chatId")
    val chatId: String,
    
    @SerializedName("chatType")
    val chatType: Int, // 1-用户，2-群聊，3-机器人
    
    @SerializedName("remark")
    val remark: String
)

/**
 * 搜索社区请求
 */
data class SearchRequest(
    @SerializedName("typ")
    val typ: Int = 3,
    
    @SerializedName("keyword")
    val keyword: String,
    
    @SerializedName("size")
    val size: Int = 50,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 搜索社区响应
 */
data class SearchResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: SearchData,
    
    @SerializedName("msg")
    val msg: String
)

/**
 * 搜索数据
 */
data class SearchData(
    @SerializedName("ba")
    val boards: List<com.yhchat.canary.data.model.CommunityBoard>? = emptyList(),
    
    @SerializedName("posts") 
    val posts: List<com.yhchat.canary.data.model.CommunityPost>? = emptyList()
)

/**
 * 群聊列表请求
 */
data class GroupListRequest(
    @SerializedName("baId")
    val baId: Int,
    
    @SerializedName("size")
    val size: Int = 20,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 群聊列表响应
 */
data class GroupListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("data")
    val data: GroupListData,
    
    @SerializedName("msg")
    val msg: String
)

/**
 * 我的文章列表请求
 */
data class MyPostListRequest(
    @SerializedName("size")
    val size: Int = 20,
    
    @SerializedName("page")
    val page: Int = 1
)

/**
 * 群聊列表数据
 */
data class GroupListData(
    @SerializedName("groups")
    val groups: List<com.yhchat.canary.data.model.CommunityGroup>,
    
    @SerializedName("total")
    val total: Int
)

/**
 * 关注分区请求
 */
data class FollowBoardRequest(
    @SerializedName("baId")
    val baId: Int,
    
    @SerializedName("followSource")
    val followSource: Int = 2
)

/**
 * 取消关注分区请求
 */
data class UnfollowBoardRequest(
    @SerializedName("baId")
    val baId: Int
)
