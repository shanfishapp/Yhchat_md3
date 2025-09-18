package com.yhchat.canary.data.repository

import com.yhchat.canary.data.api.ApiService
import com.yhchat.canary.data.api.*
import com.yhchat.canary.data.model.*
import retrofit2.Response

/**
 * 社区数据仓库
 */
class CommunityRepository(
    private val apiService: ApiService
) {
    
    /**
     * 获取分区列表
     */
    suspend fun getBoardList(
        token: String,
        typ: Int = 2,
        size: Int = 20,
        page: Int = 1
    ): Result<BoardListResponse> {
        return try {
            val request = BoardListRequest(typ = typ, size = size, page = page)
            val response = apiService.getBoardList(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.msg ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取分区信息
     */
    suspend fun getBoardInfo(
        token: String,
        boardId: Int
    ): Result<BoardInfoResponse> {
        return try {
            val request = BoardInfoRequest(id = boardId)
            val response = apiService.getBoardInfo(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.msg ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取文章列表
     */
    suspend fun getPostList(
        token: String,
        baId: Int,
        typ: Int = 1,
        size: Int = 20,
        page: Int = 1
    ): Result<PostListResponse> {
        return try {
            val request = PostListRequest(typ = typ, baId = baId, size = size, page = page)
            val response = apiService.getPostList(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.msg ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取文章详情
     */
    suspend fun getPostDetail(
        token: String,
        postId: Int
    ): Result<PostDetailResponse> {
        return try {
            val request = PostDetailRequest(id = postId)
            val response = apiService.getPostDetail(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.msg ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取评论列表
     */
    suspend fun getCommentList(
        token: String,
        postId: Int,
        size: Int = 10,
        page: Int = 1
    ): Result<CommentListResponse> {
        return try {
            val request = CommentListRequest(postId = postId, size = size, page = page)
            val response = apiService.getCommentList(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.msg ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 点赞文章
     */
    suspend fun likePost(
        token: String,
        postId: Int
    ): Result<ApiStatus> {
        return try {
            val request = LikePostRequest(id = postId)
            val response = apiService.likePost(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.message ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 收藏文章
     */
    suspend fun collectPost(
        token: String,
        postId: Int
    ): Result<ApiStatus> {
        return try {
            val request = CollectPostRequest(id = postId)
            val response = apiService.collectPost(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.message ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 打赏文章
     */
    suspend fun rewardPost(
        token: String,
        postId: Int,
        amount: Double
    ): Result<ApiStatus> {
        return try {
            val request = RewardPostRequest(postId = postId, amount = amount)
            val response = apiService.rewardPost(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.message ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 评论文章
     */
    suspend fun commentPost(
        token: String,
        postId: Int,
        content: String,
        commentId: Int = 0
    ): Result<ApiStatus> {
        return try {
            val request = CommentPostRequest(postId = postId, commentId = commentId, content = content)
            val response = apiService.commentPost(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.message ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建文章
     */
    suspend fun createPost(
        token: String,
        boardId: Int,
        title: String,
        content: String,
        contentType: Int,
        groupId: String = ""
    ): Result<CreatePostResponse> {
        return try {
            val request = CreatePostRequest(
                baId = boardId,
                groupId = groupId,
                title = title,
                content = content,
                contentType = contentType
            )
            val response = apiService.createPost(token, request)
            
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.code == 1) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("API返回错误: ${body?.msg ?: "未知错误"}"))
                }
            } else {
                Result.failure(Exception("网络请求失败: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
