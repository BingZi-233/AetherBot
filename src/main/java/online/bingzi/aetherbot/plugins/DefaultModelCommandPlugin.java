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
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.stereotype.Component;

/**
 * 默认AI模型查询插件
 * 处理用户查询当前设置的默认AI模型
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultModelCommandPlugin {

    private final UserService userService;

    /**
     * 处理私聊查询默认模型指令
     * 格式: @defaultmodel
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@defaultmodel$")
    public void handlePrivateDefaultModel(Bot bot, PrivateMessageEvent event) {
        String qq = String.valueOf(event.getUserId());

        // 处理查询默认模型请求
        processDefaultModelRequest(bot, qq, event.getUserId(), null);
    }

    /**
     * 处理群聊查询默认模型指令
     * 格式: @defaultmodel
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@defaultmodel$")
    public void handleGroupDefaultModel(Bot bot, GroupMessageEvent event) {
        String qq = String.valueOf(event.getUserId());

        // 处理查询默认模型请求
        processDefaultModelRequest(bot, qq, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理查询默认模型请求
     *
     * @param bot      机器人实例
     * @param qq       用户QQ
     * @param senderId 发送者ID
     * @param groupId  群ID，如果是私聊则为null
     */
    private void processDefaultModelRequest(Bot bot, String qq, long senderId, Long groupId) {
        try {
            // 查找用户
            User user = userService.findByQQ(qq);

            // 获取默认模型
            AiModel defaultModel = userService.getDefaultAiModel(user);

            MsgUtils msgBuilder = MsgUtils.builder();

            if (defaultModel != null) {
                // 有默认模型
                msgBuilder.text("您当前的默认AI模型是: " + defaultModel.getName())
                        .text("\n提问费用: " + defaultModel.getPromptCostPerThousandTokens() + " CA/千Token")
                        .text("\n回答费用: " + defaultModel.getCompletionCostPerThousandTokens() + " CA/千Token")
                        .text("\n总费用: " + String.format("%.9f", defaultModel.getCostPerRequest()) + " CA/次")
                        .text("\n您可以直接使用 @chat [问题内容] 开始对话");
            } else {
                // 没有默认模型
                msgBuilder.text("您尚未设置默认AI模型")
                        .text("\n请使用 @setmodel [模型名称] 设置默认模型")
                        .text("\n或者使用 @chat [模型名称] [问题内容] 开始对话");
            }

            String msg = msgBuilder.build();
            sendResponse(bot, senderId, groupId, msg);

        } catch (Exception e) {
            log.error("处理查询默认模型请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理查询默认模型请求时发生错误: " + e.getMessage())
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