package online.bingzi.aetherbot.service;

import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.User;

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
} 