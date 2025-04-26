package online.bingzi.aetherbot.service;

import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.entity.User;

import java.util.List;
import java.util.UUID;

public interface ConversationService {
    
    /**
     * 创建新的对话
     * 
     * @param user 用户
     * @param model AI模型
     * @return 新创建的对话实体
     */
    Conversation createConversation(User user, AiModel model);
    
    /**
     * 根据ID查找对话
     * 
     * @param id 对话ID
     * @return 对话实体，如果不存在则返回null
     */
    Conversation findById(UUID id);
    
    /**
     * 结束对话
     * 
     * @param conversation 要结束的对话
     * @return 更新后的对话实体
     */
    Conversation endConversation(Conversation conversation);
    
    /**
     * 获取用户的活跃对话
     * 如果用户有多个活跃对话，返回最近创建的一个
     * 
     * @param user 用户
     * @return 活跃对话，如果不存在则返回null
     */
    Conversation getActiveConversation(User user);
    
    /**
     * 获取对话的消息列表
     * 
     * @param conversation 对话
     * @return 消息列表
     */
    List<Message> getConversationMessages(Conversation conversation);
    
    /**
     * 结束用户的所有活跃对话
     * 
     * @param user 用户
     * @return 结束的对话数量
     */
    int endAllActiveConversations(User user);
} 