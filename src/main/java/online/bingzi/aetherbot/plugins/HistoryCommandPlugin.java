package online.bingzi.aetherbot.plugins;

import com.mikuac.shiro.annotation.GroupMessageHandler;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.MessageType;
import online.bingzi.aetherbot.repository.ConversationRepository;
import online.bingzi.aetherbot.repository.MessageRepository;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * 对话历史查询指令插件
 * 处理用户查询对话历史的指令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryCommandPlugin {

    private final UserService userService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int PAGE_SIZE = 5;
    private static final int MAX_MESSAGE_LENGTH = 50;

    /**
     * 处理私聊历史记录查询指令
     * 格式: @history [页码]，默认第1页
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@history(?:\\s+(\\d+))?$")
    public void handlePrivateHistory(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        int page = 1;
        
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            try {
                page = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // 忽略无效页码，使用默认值1
            }
        }
        
        if (page < 1) page = 1;
        
        // 处理历史记录查询请求
        processHistoryRequest(bot, qq, page, event.getUserId(), null);
    }

    /**
     * 处理群聊历史记录查询指令
     * 格式: @history [页码]，默认第1页
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@history(?:\\s+(\\d+))?$")
    public void handleGroupHistory(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        int page = 1;
        
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            try {
                page = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // 忽略无效页码，使用默认值1
            }
        }
        
        if (page < 1) page = 1;
        
        // 处理历史记录查询请求
        processHistoryRequest(bot, qq, page, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理历史记录查询请求
     * 
     * @param bot 机器人实例
     * @param qq 查询者QQ
     * @param page 页码，从1开始
     * @param senderId 发送者ID
     * @param groupId 群ID，如果是私聊则为null
     */
    private void processHistoryRequest(Bot bot, String qq, int page, long senderId, Long groupId) {
        try {
            // 查找用户信息
            User user = userService.findByQQ(qq);
            
            // 获取用户的对话列表，按创建时间降序排序
            List<Conversation> allConversations = conversationRepository.findByUser(user);
            
            // 手动分页
            int totalItems = allConversations.size();
            int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);
            
            // 如果请求的页码超出了总页数，则使用最后一页
            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }
            
            // 计算当前页的开始和结束索引
            int fromIndex = (page - 1) * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
            
            List<Conversation> pageConversations = fromIndex < toIndex 
                ? allConversations.stream()
                    .sorted((c1, c2) -> c2.getCreateTime().compareTo(c1.getCreateTime()))
                    .collect(Collectors.toList())
                    .subList(fromIndex, toIndex)
                : new ArrayList<>();
            
            // 构建历史记录信息
            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("对话历史记录\n")
                    .text("====================\n");
            
            if (pageConversations.isEmpty()) {
                msgBuilder.text("暂无对话记录");
            } else {
                // 收集每个对话的第一条用户消息和AI回复
                Map<Conversation, List<Message>> conversationMessages = new HashMap<>();
                
                for (Conversation conversation : pageConversations) {
                    List<Message> messages = messageRepository.findByConversationOrderByCreateTimeAsc(conversation);
                    conversationMessages.put(conversation, messages);
                }
                
                // 构建每个对话的摘要信息
                int index = fromIndex + 1;
                for (Conversation conversation : pageConversations) {
                    List<Message> messages = conversationMessages.get(conversation);
                    
                    msgBuilder.text(index + ". 模型: " + conversation.getAiModel().getName() + "\n")
                             .text("   时间: " + conversation.getCreateTime().format(FORMATTER) + "\n");
                    
                    // 如果有消息，则显示第一对问答
                    if (messages != null && !messages.isEmpty()) {
                        Message userMessage = messages.stream()
                                .filter(m -> m.getType() == MessageType.USER)
                                .findFirst()
                                .orElse(null);
                        
                        Message aiMessage = messages.stream()
                                .filter(m -> m.getType() == MessageType.AI)
                                .findFirst()
                                .orElse(null);
                        
                        if (userMessage != null) {
                            String userContent = truncateMessage(userMessage.getContent());
                            msgBuilder.text("   问: " + userContent + "\n");
                        }
                        
                        if (aiMessage != null) {
                            String aiContent = truncateMessage(aiMessage.getContent());
                            msgBuilder.text("   答: " + aiContent + "\n");
                        }
                    } else {
                        msgBuilder.text("   暂无消息内容\n");
                    }
                    
                    // 如果不是最后一个对话，则添加分隔线
                    if (index < fromIndex + pageConversations.size()) {
                        msgBuilder.text("--------------------\n");
                    }
                    
                    index++;
                }
                
                // 添加分页信息
                msgBuilder.text("\n第 " + page + "/" + totalPages + " 页，共 " + totalItems + " 条对话记录");
                if (page < totalPages) {
                    msgBuilder.text("\n使用 @history " + (page + 1) + " 查看下一页");
                }
            }
            
            String historyInfo = msgBuilder.build();
            sendResponse(bot, senderId, groupId, historyInfo);
            
        } catch (Exception e) {
            log.error("处理历史记录查询请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理历史记录查询请求时发生错误: " + e.getMessage())
                    .build();
            
            sendResponse(bot, senderId, groupId, errorMsg);
        }
    }
    
    /**
     * 截断过长的消息内容
     * 
     * @param message 原始消息内容
     * @return 截断后的消息内容
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        
        if (message.length() <= MAX_MESSAGE_LENGTH) {
            return message;
        }
        
        return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }
    
    /**
     * 发送回复消息
     * 
     * @param bot 机器人实例
     * @param senderId 发送者ID
     * @param groupId 群ID，如果是私聊则为null
     * @param message 消息内容
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