package online.bingzi.aetherbot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.enums.MessageType;
import online.bingzi.aetherbot.service.AiChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI聊天服务实现类
 * 使用Spring AI与OpenAI集成提供AI聊天服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    // 注入Spring AI的ChatClient.Builder
    private final ChatClient.Builder chatClientBuilder;
    
    // 保存最后一次AI响应
    private ChatResponse lastResponse;

    @Override
    public String chat(AiModel model, String question, List<Message> history) {
        log.info("处理聊天请求 - 模型: {}, 问题: {}, 历史记录数: {}", model.getName(), question, history.size());

        try {
            // 创建ChatClient实例
            ChatClient chatClient = chatClientBuilder
                    .defaultSystem("你是一个友好、专业的AI助手。请提供简洁、准确和有帮助的回答。")
                    .build();

            // 准备聊天提示构建器
            var promptBuilder = chatClient.prompt();

            // 添加历史消息到消息列表
            List<Message> messages = new ArrayList<>();

            // 添加历史消息
            if (!history.isEmpty()) {
                for (Message msg : history) {
                    if (msg.getType() == MessageType.USER) {
                        promptBuilder = promptBuilder.user(msg.getContent());
                    } else {
                        // 对于AI回复消息，使用正确的API添加为Assistant消息
                        promptBuilder = promptBuilder.messages(new AssistantMessage(msg.getContent()));
                    }
                }
            }

            // 添加当前用户问题
            promptBuilder = promptBuilder.user(question);

            // 设置模型选项（如果有特定模型名称）
            if (model != null && model.getName() != null && !model.getName().isEmpty()) {
                promptBuilder = promptBuilder.options(
                        OpenAiChatOptions.builder()
                                .model(model.getName())
                                .build()
                );
            }

            // 调用API获取响应
            log.debug("发送请求到OpenAI API, 模型: {}", model.getName());
            // 保存完整响应
            this.lastResponse = promptBuilder.call().chatResponse();
            String aiResponse = this.lastResponse.getResult().getOutput().getText();

            log.debug("收到AI回复: {}", aiResponse);
            
            // 记录token使用量(如果可用)
            if (this.lastResponse.getMetadata() != null && this.lastResponse.getMetadata().getUsage() != null) {
                log.debug("Token使用量 - 提示: {}, 完成: {}, 总计: {}", 
                        this.lastResponse.getMetadata().getUsage().getPromptTokens(),
                        this.lastResponse.getMetadata().getUsage().getCompletionTokens(),
                        this.lastResponse.getMetadata().getUsage().getTotalTokens());
            }

            return aiResponse;
        } catch (Exception e) {
            log.error("生成AI回复时出错", e);
            return "抱歉，处理您的请求时出现了问题，请稍后再试。";
        }
    }
    
    @Override
    public ChatResponse getLastResponse() {
        return this.lastResponse;
    }
} 