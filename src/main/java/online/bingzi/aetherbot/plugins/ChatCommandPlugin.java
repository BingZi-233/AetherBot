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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import online.bingzi.aetherbot.service.UserService;
import online.bingzi.aetherbot.events.ChatCompletedEvent;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.service.ConversationService;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.service.AiModelService;

import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Shiro
@Component
@Slf4j
public class ChatCommandPlugin {

    private final UserService userService;
    private final ConversationService conversationService;
    private final AiModelService aiModelService;
    private final ApplicationEventPublisher eventPublisher;

    public ChatCommandPlugin(UserService userService, 
                          ConversationService conversationService,
                          AiModelService aiModelService,
                          ApplicationEventPublisher eventPublisher) {
        this.userService = userService;
        this.conversationService = conversationService;
        this.aiModelService = aiModelService;
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
        try {
            // 查找用户，如果不存在则创建
            User user = userService.findByQQ(qq);
            
            // 检查是否有活跃对话
            Conversation conversation = conversationService.getActiveConversation(user);
            
            // 解析输入内容
            AiModel model;
            String question;
            
            if (conversation == null) {
                // 没有活跃对话，需要指定模型名称
                String[] parts = input.trim().split("\\s+", 2);
                if (parts.length < 2) {
                    String errorMsg = MsgUtils.builder()
                            .text("格式错误，请使用 @chat [模型名称] [问题内容] 开始新对话。")
                            .build();
                    
                    sendResponse(bot, senderId, groupId, errorMsg);
                    return;
                }
                
                String modelName = parts[0];
                question = parts[1];
                
                // 查找AI模型
                model = aiModelService.findByName(modelName);
                if (model == null) {
                    String errorMsg = MsgUtils.builder()
                            .text("未找到指定的模型: " + modelName)
                            .text("\n可用模型: ")
                            .text(aiModelService.getAvailableModelsAsString())
                            .build();
                    
                    sendResponse(bot, senderId, groupId, errorMsg);
                    return;
                }
                
                // 创建新的对话
                conversation = conversationService.createConversation(user, model);
            } else {
                // 已有活跃对话，直接使用输入内容作为问题
                model = conversation.getAiModel();
                question = input;
            }
            
            // 检查用户余额
            double cost = model.getCostPerRequest();
            if (user.getCaBalance() < cost) {
                String errorMsg = MsgUtils.builder()
                        .text("CA代币余额不足！")
                        .text("\n当前余额: " + user.getCaBalance())
                        .text("\n本次需要: " + cost)
                        .build();
                
                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }
            
            // 获取对话历史作为上下文
            List<Message> history = conversationService.getConversationMessages(conversation);
            
            // TODO: 调用AI服务处理问题，传入历史消息作为上下文
            // 这里简化处理，实际应该调用AI服务API
            StringBuilder context = new StringBuilder();
            if (!history.isEmpty()) {
                // 构建对话历史上下文
                context.append("历史对话：\n");
                for (Message msg : history) {
                    String role = msg.getType().name();
                    context.append(role).append(": ").append(msg.getContent()).append("\n");
                }
                context.append("\n");
            }
            
            String aiResponse = "这是一个AI回复示例。上下文: " + context.toString() + " 问题是：" + question;
            
            // 发送回复
            String responseMsg = MsgUtils.builder()
                    .text(aiResponse)
                    .build();
            
            sendResponse(bot, senderId, groupId, responseMsg);
            
            // 触发聊天完成事件，扣除CA代币
            eventPublisher.publishEvent(new ChatCompletedEvent(this, user, conversation, cost, question, aiResponse));
            
        } catch (Exception e) {
            log.error("处理聊天请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理请求时发生错误: " + e.getMessage())
                    .build();
            
            sendResponse(bot, senderId, groupId, errorMsg);
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