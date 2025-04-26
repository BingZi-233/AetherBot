package online.bingzi.aetherbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.enums.MessageType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AI聊天服务实现类
 * 提供AI聊天服务的基本实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    @Override
    public String chat(AiModel model, String question, List<Message> history) {
        log.info("处理聊天请求 - 模型: {}, 问题: {}, 历史记录数: {}", model.getName(), question, history.size());
        
        // 构建聊天上下文
        StringBuilder context = new StringBuilder();
        if (!history.isEmpty()) {
            for (Message msg : history) {
                String role = msg.getType() == MessageType.USER ? "用户" : "AI";
                context.append(role).append(": ").append(msg.getContent()).append("\n");
            }
        }
        
        try {
            // 实际项目中，这里应该调用真实的AI服务API
            // 这里模拟一个简单的响应
            return generateResponse(model, question, context.toString());
        } catch (Exception e) {
            log.error("生成AI回复时出错", e);
            return "抱歉，处理您的请求时出现了问题，请稍后再试。";
        }
    }
    
    /**
     * 生成模拟的AI回复
     * 实际项目中应替换为调用真实的AI服务API
     */
    private String generateResponse(AiModel model, String question, String context) {
        // 基于问题内容生成一些有意义的回复
        String[] responses = {
            "根据您的问题，我认为需要考虑多方面因素。首先，" + extractKeyPoint(question) + "是个重要考量点。",
            "您提到的" + extractKeyPoint(question) + "很有意思。在我的理解中，这涉及到多个方面。",
            "关于" + extractKeyPoint(question) + "的问题，专业观点认为这是一个复杂的话题。",
            "针对您询问的" + extractKeyPoint(question) + "，我可以提供一些见解和建议。",
            "您好！关于" + extractKeyPoint(question) + "，我认为这是个很好的问题。从专业角度来看..."
        };
        
        int randomIndex = ThreadLocalRandom.current().nextInt(responses.length);
        String baseResponse = responses[randomIndex];
        
        // 添加模型特定信息
        return baseResponse + "\n\n(回答由" + model.getName() + "模型生成)";
    }
    
    /**
     * 从问题中提取关键点
     */
    private String extractKeyPoint(String question) {
        // 简单实现，实际可以使用NLP技术提取关键词
        String[] words = question.split("\\s+");
        if (words.length <= 3) {
            return question;
        }
        
        // 选取问题中间部分作为"关键点"
        int midPoint = words.length / 2;
        return words[midPoint - 1] + words[midPoint] + (words.length > midPoint + 1 ? words[midPoint + 1] : "");
    }
} 