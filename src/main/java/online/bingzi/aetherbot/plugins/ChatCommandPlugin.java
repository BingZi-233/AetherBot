package online.bingzi.aetherbot.plugins;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.events.ChatCompletedEvent;
import online.bingzi.aetherbot.events.ChatErrorEvent;
import online.bingzi.aetherbot.service.AiChatService;
import online.bingzi.aetherbot.service.AiModelService;
import online.bingzi.aetherbot.service.ConversationService;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;

@Shiro
@Component
@Slf4j
public class ChatCommandPlugin {

    private final UserService userService;
    private final ConversationService conversationService;
    private final AiModelService aiModelService;
    private final AiChatService aiChatService;
    private final ApplicationEventPublisher eventPublisher;

    public ChatCommandPlugin(UserService userService,
                             ConversationService conversationService,
                             AiModelService aiModelService,
                             AiChatService aiChatService,
                             ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.conversationService = conversationService;
        this.aiModelService = aiModelService;
        this.aiChatService = aiChatService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 处理私聊聊天指令
     * 格式: @chat [模型名称] [问题内容] 或继续对话: @chat [问题内容]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@chat\\s+(.+)$")
    public void handlePrivateChat(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        // 获取QQ号
        String qq = String.valueOf(event.getUserId());
        // 获取问题内容
        String input = matcher.group(1);

        // 处理聊天请求
        processChatRequest(bot, qq, input, event.getUserId(), null);
    }

    /**
     * 处理群聊聊天指令
     * 格式: @chat [模型名称] [问题内容] 或继续对话: @chat [问题内容]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@chat\\s+(.+)$")
    public void handleGroupChat(Bot bot, GroupMessageEvent event, Matcher matcher) {
        // 获取QQ号
        String qq = String.valueOf(event.getUserId());
        // 获取问题内容
        String input = matcher.group(1);

        // 处理聊天请求
        processChatRequest(bot, qq, input, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理聊天请求
     */
    private void processChatRequest(Bot bot, String qq, String input,
                                    long senderId, Long groupId) {
        User user = null;
        Conversation conversation = null;
        String question = null;

        try {
            // 查找用户，如果不存在则创建
            user = userService.findByQQ(qq);

            // 检查是否有活跃对话
            conversation = conversationService.getActiveConversation(user);

            // 解析输入内容
            AiModel model;

            if (conversation == null) {
                // 没有活跃对话，检查是否指定了模型名称
                String[] parts = input.trim().split("\\s+", 2);

                // 尝试获取用户的默认模型
                AiModel defaultModel = userService.getDefaultAiModel(user);

                if (parts.length < 2) {
                    // 输入中只有问题内容，没有指定模型名称
                    if (defaultModel != null) {
                        // 有默认模型，使用默认模型
                        model = defaultModel;
                        question = input;
                    } else {
                        // 没有默认模型，返回错误信息
                        String errorMsg = MsgUtils.builder()
                                .text("您尚未设置默认模型，请使用以下格式开始新对话：")
                                .text("\n@chat [模型名称] [问题内容]")
                                .text("\n或者先使用@setmodel [模型名称]设置默认模型。")
                                .text("\n可用模型: ")
                                .text(aiModelService.getAvailableModelsAsString())
                                .build();

                        sendResponse(bot, senderId, groupId, errorMsg);
                        return;
                    }
                } else {
                    // 输入中包含了可能的模型名称和问题内容
                    String modelName = parts[0];
                    question = parts[1];

                    // 查找AI模型
                    model = aiModelService.findByName(modelName);
                    if (model == null) {
                        // 如果指定的模型不存在，可能整个输入都是问题内容
                        if (defaultModel != null) {
                            // 有默认模型，使用默认模型，并将全部输入视为问题
                            model = defaultModel;
                            question = input;
                        } else {
                            // 没有默认模型，返回错误信息
                            String errorMsg = MsgUtils.builder()
                                    .text("未找到指定的模型: " + modelName)
                                    .text("\n可用模型: ")
                                    .text(aiModelService.getAvailableModelsAsString())
                                    .build();

                            sendResponse(bot, senderId, groupId, errorMsg);
                            return;
                        }
                    }
                }

                // 创建新的对话
                conversation = conversationService.createConversation(user, model);
            } else {
                // 已有活跃对话，直接使用输入内容作为问题
                model = conversation.getAiModel();
                question = input;
            }

            // 检查用户余额
            BigDecimal cost = model.getCostPerRequest();
            BigDecimal userBalance = new BigDecimal(String.valueOf(user.getCaBalance()));

            if (userBalance.compareTo(cost) < 0) {
                String errorMsg = MsgUtils.builder()
                        .text("CA代币余额不足！")
                        .text("\n当前余额: " + userBalance.toPlainString())
                        .text("\n本次需要: " + cost.toPlainString())
                        .build();

                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 获取对话历史作为上下文
            List<Message> history = conversationService.getConversationMessages(conversation);

            // 调用AI服务处理问题，传入模型、问题和历史消息作为上下文
            String aiResponse = aiChatService.chat(model, question, history);

            // 发送回复
            String responseMsg = MsgUtils.builder()
                    .text(aiResponse)
                    .build();

            sendResponse(bot, senderId, groupId, responseMsg);

            // 从API响应中获取token使用量
            ChatResponse chatResponse = null;
            Integer promptTokens = null;
            Integer completionTokens = null;
            Integer totalTokens = null;

            // 尝试从aiChatService获取ChatResponse对象，以获取token使用信息
            if (aiChatService.getLastResponse() != null) {
                chatResponse = aiChatService.getLastResponse();
                if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                    Usage usage = chatResponse.getMetadata().getUsage();
                    promptTokens = usage.getPromptTokens();
                    completionTokens = usage.getCompletionTokens();
                    totalTokens = usage.getTotalTokens();
                }
            }

            // 触发聊天完成事件，扣除CA代币，并传递token使用量
            eventPublisher.publishEvent(new ChatCompletedEvent(this, user, conversation, cost, question, aiResponse,
                    promptTokens, completionTokens, totalTokens));

        } catch (Exception e) {
            log.error("处理聊天请求时出错", e);
            String errorMsg = "处理请求时发生错误: " + e.getMessage();

            // 发送错误消息回复
            String formattedErrorMsg = MsgUtils.builder()
                    .text(errorMsg)
                    .build();
            sendResponse(bot, senderId, groupId, formattedErrorMsg);

            // 如果已经创建了对话和用户，发布错误事件
            if (user != null && conversation != null && question != null) {
                eventPublisher.publishEvent(new ChatErrorEvent(this, user, conversation, question, errorMsg));
            }
        }
    }

    /**
     * 发送回复消息
     */
    private void sendResponse(Bot bot, long senderId, Long groupId, String message) {
        if (groupId != null) {
            // 群聊回复，添加@用户
            String atMsg = MsgUtils.builder()
                    .at(senderId)
                    .text("\n")
                    .text(message)
                    .build();
            bot.sendGroupMsg(groupId, atMsg, false);
        } else {
            // 私聊回复
            bot.sendPrivateMsg(senderId, message, false);
        }
    }
} 