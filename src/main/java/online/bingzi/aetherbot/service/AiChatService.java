package online.bingzi.aetherbot.service;

import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Message;

import java.util.List;

/**
 * AI聊天服务接口
 * 负责处理与AI模型的聊天通信
 */
public interface AiChatService {

    /**
     * 向AI模型发送聊天请求
     *
     * @param model    使用的AI模型
     * @param question 当前问题
     * @param history  对话历史记录
     * @return AI的回复内容
     */
    String chat(AiModel model, String question, List<Message> history);
} 