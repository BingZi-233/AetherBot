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
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.service.AiModelService;
import online.bingzi.aetherbot.service.ConversationService;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

/**
 * 设置对话模型指令插件
 * 处理用户设置当前对话使用的AI模型
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class SetModelCommandPlugin {

    private final UserService userService;
    private final ConversationService conversationService;
    private final AiModelService aiModelService;

    /**
     * 处理私聊设置模型指令
     * 格式: @setmodel [模型名称]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@setmodel\\s+([\\w-]+)$")
    public void handlePrivateSetModel(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        String modelName = matcher.group(1);

        // 处理设置模型请求
        processSetModelRequest(bot, qq, modelName, event.getUserId(), null);
    }

    /**
     * 处理群聊设置模型指令
     * 格式: @setmodel [模型名称]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@setmodel\\s+([\\w-]+)$")
    public void handleGroupSetModel(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        String modelName = matcher.group(1);

        // 处理设置模型请求
        processSetModelRequest(bot, qq, modelName, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理设置模型请求
     *
     * @param bot       机器人实例
     * @param qq        用户QQ
     * @param modelName 模型名称
     * @param senderId  发送者ID
     * @param groupId   群ID，如果是私聊则为null
     */
    private void processSetModelRequest(Bot bot, String qq, String modelName,
                                        long senderId, Long groupId) {
        try {
            // 查找用户，如果不存在则创建
            User user = userService.findByQQ(qq);

            // 查找AI模型
            AiModel model = aiModelService.findByName(modelName);
            if (model == null) {
                String errorMsg = MsgUtils.builder()
                        .text("未找到指定的模型: " + modelName)
                        .text("\n可用模型: ")
                        .text(aiModelService.getAvailableModelsAsString())
                        .build();

                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 结束用户当前的活跃对话(如果有)
            Conversation activeConversation = conversationService.getActiveConversation(user);
            if (activeConversation != null) {
                conversationService.endConversation(activeConversation);
            }

            // 创建新的对话
            Conversation newConversation = conversationService.createConversation(user, model);

            // 发送成功消息
            String successMsg = MsgUtils.builder()
                    .text("已成功设置对话模型为: " + model.getName())
                    .text("\n费用: " + String.format("%.9f", model.getCostPerRequest()) + " CA/次")
                    .text("\n现在可以直接使用 @chat [问题内容] 进行对话")
                    .build();

            sendResponse(bot, senderId, groupId, successMsg);

        } catch (Exception e) {
            log.error("处理设置模型请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理设置模型请求时发生错误: " + e.getMessage())
                    .build();

            sendResponse(bot, senderId, groupId, errorMsg);
        }
    }

    /**
     * 发送回复消息
     *
     * @param bot      机器人实例
     * @param senderId 发送者ID
     * @param groupId  群ID，如果是私聊则为null
     * @param message  消息内容
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