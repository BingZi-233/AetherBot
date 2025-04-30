package online.bingzi.aetherbot.plugins;

import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.service.AiModelService;
import online.bingzi.aetherbot.service.ConversationService;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

/**
 * 持续对话命令插件
 * 处理开启和关闭持续对话模式的命令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class ContinuousChatCommandPlugin {

    private final UserService userService;
    private final ConversationService conversationService;
    private final AiModelService aiModelService;

    /**
     * 处理开启持续对话模式命令
     * 格式: @continuous-chat on [模型名称]
     * 如果不指定模型名称，则使用用户的默认模型
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@continuous-chat\\s+on(?:\\s+(.+))?$")
    public void handleEnableContinuousChat(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        
        // 尝试提取可能的模型名称
        String modelName = null;
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            modelName = matcher.group(1).trim();
        }
        
        try {
            // 查找用户
            User user = userService.findByQQ(qq);
            
            // 获取默认模型或指定模型
            AiModel model = null;
            
            if (modelName != null && !modelName.isEmpty()) {
                // 尝试找到指定的模型
                model = aiModelService.findByName(modelName);
                
                if (model == null) {
                    // 如果找不到指定的模型，返回错误信息
                    String errorMsg = MsgUtils.builder()
                            .text("未找到指定的模型: " + modelName + "\n")
                            .text("可用模型: \n")
                            .text(aiModelService.getAvailableModelsAsString())
                            .build();
                    
                    bot.sendPrivateMsg(event.getUserId(), errorMsg, false);
                    return;
                }
            } else {
                // 使用默认模型
                model = userService.getDefaultAiModel(user);
                
                if (model == null) {
                    // 如果没有默认模型，返回错误信息
                    String errorMsg = MsgUtils.builder()
                            .text("您尚未设置默认模型，请使用以下格式指定模型：\n")
                            .text("@continuous-chat on [模型名称]\n")
                            .text("或者使用@setmodel [模型名称]设置默认模型。\n")
                            .text("可用模型: \n")
                            .text(aiModelService.getAvailableModelsAsString())
                            .build();
                    
                    bot.sendPrivateMsg(event.getUserId(), errorMsg, false);
                    return;
                }
            }
            
            // 如果指定了模型，设置为默认模型
            if (modelName != null && !modelName.isEmpty()) {
                userService.setDefaultAiModel(user, model);
            }
            
            // 启用持续对话模式
            userService.setContinuousChatEnabled(user, true);
            
            // 返回成功消息
            String successMsg = MsgUtils.builder()
                    .text("已开启持续对话模式\n")
                    .text("使用模型: " + model.getName() + "\n")
                    .text("现在您可以直接发送消息与AI对话，无需添加@chat前缀\n")
                    .text("输入\"@end\"结束当前对话\n")
                    .text("输入\"@continuous-chat off\"关闭持续对话模式")
                    .build();
            
            bot.sendPrivateMsg(event.getUserId(), successMsg, false);
            
        } catch (Exception e) {
            log.error("处理开启持续对话命令时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理命令时发生错误: " + e.getMessage())
                    .build();
            
            bot.sendPrivateMsg(event.getUserId(), errorMsg, false);
        }
    }
    
    /**
     * 处理关闭持续对话模式命令
     * 格式: @continuous-chat off
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@continuous-chat\\s+off$")
    public void handleDisableContinuousChat(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        
        try {
            // 查找用户
            User user = userService.findByQQ(qq);
            
            // 如果持续对话模式未开启，返回提示
            if (!user.isContinuousChatEnabled()) {
                String msg = MsgUtils.builder()
                        .text("持续对话模式当前未开启")
                        .build();
                
                bot.sendPrivateMsg(event.getUserId(), msg, false);
                return;
            }
            
            // 关闭持续对话模式
            userService.setContinuousChatEnabled(user, false);
            
            // 结束当前活跃的对话
            int endedCount = conversationService.endAllActiveConversations(user);
            
            // 返回成功消息
            String successMsg = MsgUtils.builder()
                    .text("已关闭持续对话模式\n")
                    .text("已结束 " + endedCount + " 个活跃对话\n")
                    .text("现在您需要使用\"@chat [问题内容]\"格式与AI对话")
                    .build();
            
            bot.sendPrivateMsg(event.getUserId(), successMsg, false);
            
        } catch (Exception e) {
            log.error("处理关闭持续对话命令时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理命令时发生错误: " + e.getMessage())
                    .build();
            
            bot.sendPrivateMsg(event.getUserId(), errorMsg, false);
        }
    }
    
    /**
     * 处理查询持续对话状态命令
     * 格式: @continuous-chat status
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@continuous-chat(?:\\s+status)?$")
    public void handleContinuousChatStatus(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        
        try {
            // 查找用户
            User user = userService.findByQQ(qq);
            
            // 获取持续对话状态
            boolean isEnabled = user.isContinuousChatEnabled();
            
            // 构建状态消息
            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("持续对话模式当前");
            
            if (isEnabled) {
                AiModel model = userService.getDefaultAiModel(user);
                msgBuilder.text("已开启\n")
                        .text("使用模型: " + (model != null ? model.getName() : "未设置") + "\n")
                        .text("您可以直接发送消息与AI对话，无需添加@chat前缀\n")
                        .text("输入\"@end\"结束当前对话\n")
                        .text("输入\"@continuous-chat off\"关闭持续对话模式");
            } else {
                msgBuilder.text("未开启\n")
                        .text("您可以使用\"@continuous-chat on [模型名称]\"开启持续对话模式");
            }
            
            String statusMsg = msgBuilder.build();
            bot.sendPrivateMsg(event.getUserId(), statusMsg, false);
            
        } catch (Exception e) {
            log.error("处理查询持续对话状态命令时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理命令时发生错误: " + e.getMessage())
                    .build();
            
            bot.sendPrivateMsg(event.getUserId(), errorMsg, false);
        }
    }
} 