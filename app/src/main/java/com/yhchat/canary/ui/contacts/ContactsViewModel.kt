package com.yhchat.canary.ui.contacts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yhchat.canary.data.repository.FriendRepository
import com.yhchat.canary.data.repository.TokenRepository
import yh_user.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 联系人数据
 */
data class Contact(
    val chatId: String,
    val name: String,
    val avatarUrl: String,
    val permissionLevel: Int = 0,
    val noDisturb: Boolean = false,
    val chatType: Int = 1 // 1-用户，2-群聊，3-机器人
)

/**
 * 联系人分组
 */
data class ContactGroup(
    val title: String,
    val contacts: List<Contact>
)

/**
 * 通讯录UI状态
 */
data class ContactsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val friends: List<Contact> = emptyList(),
    val groups: List<Contact> = emptyList(),
    val bots: List<Contact> = emptyList(),
    val myBots: List<Contact> = emptyList(),  // 我创建的机器人
    val friendsExpanded: Boolean = false,  // 默认不展开
    val groupsExpanded: Boolean = false,  // 默认不展开
    val botsExpanded: Boolean = false,  // 默认不展开
    val myBotsExpanded: Boolean = false  // 默认不展开
)

@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val tokenRepository: TokenRepository,
    private val botRepository: com.yhchat.canary.data.repository.BotRepository
) : ViewModel() {
    
    private val tag = "ContactsViewModel"
    
    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()
    
    init {
        loadContacts()
    }
    
    /**
     * 加载所有联系人
     */
    fun loadContacts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                Log.d(tag, "开始加载通讯录...")
                
                val result = friendRepository.getAddressBookList()
                
                result.fold(
                    onSuccess = { data ->
                        Log.d(tag, "✅ 通讯录加载成功，总分组数: ${data.dataCount}")
                        
                        // 解析protobuf数据
                        val friends = mutableListOf<Contact>()
                        val groups = mutableListOf<Contact>()
                        val bots = mutableListOf<Contact>()
                        
                        Log.d(tag, "开始解析分组数据...")
                        data.dataList.forEachIndexed { index, group ->
                            Log.d(tag, "处理分组[$index]: ${group.listName}, 成员数: ${group.dataCount}")
                        }
                        
                        data.dataList.forEach { group ->
                            when (group.listName) {
                                "好友", "用户" -> {
                                    group.dataList.forEach { item ->
                                        friends.add(
                                            Contact(
                                                chatId = item.chatId,
                                                name = item.name,
                                                avatarUrl = item.avatarUrl,
                                                permissionLevel = item.permissonLevel,
                                                noDisturb = item.noDisturb,
                                                chatType = 1 // 用户
                                            )
                                        )
                                    }
                                }
                                "我加入的群聊" -> {
                                    group.dataList.forEach { item ->
                                        groups.add(
                                            Contact(
                                                chatId = item.chatId,
                                                name = item.name,
                                                avatarUrl = item.avatarUrl,
                                                permissionLevel = item.permissonLevel,
                                                noDisturb = item.noDisturb,
                                                chatType = 2 // 群聊
                                            )
                                        )
                                    }
                                }
                                "机器人" -> {
                                    group.dataList.forEach { item ->
                                        bots.add(
                                            Contact(
                                                chatId = item.chatId,
                                                name = item.name,
                                                avatarUrl = item.avatarUrl,
                                                permissionLevel = item.permissonLevel,
                                                noDisturb = item.noDisturb,
                                                chatType = 3 // 机器人
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        
                        Log.d(tag, "好友数量: ${friends.size}, 群聊数量: ${groups.size}, 机器人数量: ${bots.size}")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            friends = friends,
                            groups = groups,
                            bots = bots
                        )
                    },
                    onFailure = { error ->
                        Log.e(tag, "❌ 通讯录加载失败: ${error.message}", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = error.message ?: "加载失败"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "❌ 通讯录加载异常", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载异常"
                )
            }
        }
    }
    
    /**
     * 切换好友列表展开/收起
     */
    fun toggleFriendsExpanded() {
        _uiState.value = _uiState.value.copy(
            friendsExpanded = !_uiState.value.friendsExpanded
        )
    }
    
    /**
     * 切换群聊列表展开/收起
     */
    fun toggleGroupsExpanded() {
        _uiState.value = _uiState.value.copy(
            groupsExpanded = !_uiState.value.groupsExpanded
        )
    }
    
    /**
     * 切换机器人列表展开/收起
     */
    fun toggleBotsExpanded() {
        _uiState.value = _uiState.value.copy(
            botsExpanded = !_uiState.value.botsExpanded
        )
    }
    
    /**
     * 切换"我创建的机器人"展开状态
     */
    fun toggleMyBotsExpanded() {
        val newExpanded = !_uiState.value.myBotsExpanded
        _uiState.value = _uiState.value.copy(
            myBotsExpanded = newExpanded
        )
        
        // 如果展开且还没有加载数据，则加载
        if (newExpanded && _uiState.value.myBots.isEmpty()) {
            loadMyBots()
        }
    }
    
    /**
     * 加载我创建的机器人列表
     */
    private fun loadMyBots() {
        viewModelScope.launch {
            try {
                Log.d(tag, "开始加载我创建的机器人列表...")
                
                val result = botRepository.getMyBotList()
                
                result.fold(
                    onSuccess = { botList ->
                        Log.d(tag, "✅ 我创建的机器人列表加载成功，共 ${botList.size} 个")
                        
                        val myBots = botList.map { bot ->
                            Contact(
                                chatId = bot.botId,
                                name = bot.nickname,
                                avatarUrl = bot.avatarUrl,
                                chatType = 3
                            )
                        }
                        
                        _uiState.value = _uiState.value.copy(myBots = myBots)
                    },
                    onFailure = { error ->
                        Log.e(tag, "❌ 加载我创建的机器人列表失败", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(tag, "加载我创建的机器人列表异常", e)
            }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

