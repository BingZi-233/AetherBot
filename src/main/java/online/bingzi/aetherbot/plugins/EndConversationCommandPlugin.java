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
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.service.ConversationService;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.stereotype.Component;

/**
 * 结束对话指令插件
 * 处理用户结束当前对话的指令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class EndConversationCommandPlugin {

    private final UserService userService;
    private final ConversationService conversationService;

    /**
     * 处理私聊结束对话指令
     * 格式: @end
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@end$")
    public void handlePrivateEnd(Bot bot, PrivateMessageEvent event) {
        String qq = String.valueOf(event.getUserId());

        // 处理结束对话请求
        processEndRequest(bot, qq, event.getUserId(), null);
    }

    /**
     * 处理群聊结束对话指令
     * 格式: @end
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@end$")
    public void handleGroupEnd(Bot bot, GroupMessageEvent event) {
        String qq = String.valueOf(event.getUserId());

        // 处理结束对话请求
        processEndRequest(bot, qq, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理结束对话请求
     *
     * @param bot      机器人实例
     * @param qq       用户QQ
     * @param senderId 发送者ID
     * @param groupId  群ID，如果是私聊则为null
     */
    private void processEndRequest(Bot bot, String qq, long senderId, Long groupId) {
        try {
            // 查找用户信息
            User user = userService.findByQQ(qq);

            // 获取用户的活跃对话
            Conversation activeConversation = conversationService.getActiveConversation(user);

            if (activeConversation == null) {
                String msg = MsgUtils.builder()
                        .text("您当前没有活跃的对话。")
                        .build();

                sendResponse(bot, senderId, groupId, msg);
                return;
            }
            
            // 检查是否是私聊，并且用户开启了持续对话模式
            boolean isContinuousChatEnabled = false;
            if (groupId == null && userService.isContinuousChatEnabled(user)) {
                isContinuousChatEnabled = true;
                // 在私聊环境下，如果用户开启了持续对话模式，则同时关闭持续对话
                userService.setContinuousChatEnabled(user, false);
            }

            // 结束对话
            conversationService.endConversation(activeConversation);

            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("已结束当前对话。\n")
                    .text("对话模型: " + activeConversation.getAiModel().getName() + "\n")
                    .text("开始时间: " + activeConversation.getCreateTime());
            
            // 如果关闭了持续对话模式，添加提示信息
            if (isContinuousChatEnabled) {
                msgBuilder.text("\n\n同时已关闭持续对话模式。")
                          .text("\n现在您需要使用\"@chat [问题内容]\"格式与AI对话");
            }
            
            String msg = msgBuilder.build();
            sendResponse(bot, senderId, groupId, msg);

        } catch (Exception e) {
            log.error("处理结束对话请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理结束对话请求时发生错误: " + e.getMessage())
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