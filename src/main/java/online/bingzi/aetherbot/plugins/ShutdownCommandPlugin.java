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
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

/**
 * 关闭命令插件
 * 处理管理员关闭SpringBoot应用的命令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class ShutdownCommandPlugin {

    // 确认码，用于防止误操作
    private static final String CONFIRM_CODE = "confirm";
    private final UserService userService;
    private final ConfigurableApplicationContext applicationContext;

    /**
     * 处理私聊关闭指令
     * 格式: @shutdown [确认码]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@shutdown(?:\\s+(.+))?$")
    public void handlePrivateShutdown(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());

        // 提取确认码
        String confirmCode = matcher.groupCount() >= 1 && matcher.group(1) != null
                ? matcher.group(1).trim()
                : "";

        // 处理关闭请求
        processShutdownRequest(bot, qq, confirmCode, event.getUserId(), null);
    }

    /**
     * 处理群聊关闭指令
     * 格式: @shutdown [确认码]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@shutdown(?:\\s+(.+))?$")
    public void handleGroupShutdown(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());

        // 提取确认码
        String confirmCode = matcher.groupCount() >= 1 && matcher.group(1) != null
                ? matcher.group(1).trim()
                : "";

        // 处理关闭请求
        processShutdownRequest(bot, qq, confirmCode, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理关闭请求
     *
     * @param bot         机器人实例
     * @param qq          用户QQ
     * @param confirmCode 确认码
     * @param senderId    发送者ID
     * @param groupId     群ID，如果是私聊则为null
     */
    private void processShutdownRequest(Bot bot, String qq, String confirmCode, long senderId, Long groupId) {
        try {
            // 查找用户信息
            User user = userService.findByQQ(qq);

            // 检查用户权限
            if (!userService.isAdmin(user)) {
                String errorMsg = MsgUtils.builder()
                        .text("权限不足，只有管理员可以执行关闭操作。")
                        .build();
                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 验证确认码
            if (!CONFIRM_CODE.equalsIgnoreCase(confirmCode)) {
                String errorMsg = MsgUtils.builder()
                        .text("确认码不正确，请使用正确的确认码以防止误操作。\n")
                        .text("正确格式: @shutdown confirm")
                        .build();
                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 发送即将关闭的通知
            String shutdownMsg = MsgUtils.builder()
                    .text("系统即将关闭...\n")
                    .text("管理员: " + user.getQq() + " 执行了关闭操作。")
                    .build();
            sendResponse(bot, senderId, groupId, shutdownMsg);

            // 记录关闭日志
            log.info("系统即将关闭，操作者: {}", user.getQq());

            // 设置一个短暂的延迟，确保消息发送出去
            Thread.sleep(2000);

            // 关闭应用程序
            // 使用新线程执行关闭操作，确保响应消息能够发送出去
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    applicationContext.close();
                } catch (Exception e) {
                    log.error("关闭应用程序时出错", e);
                }
            }).start();

        } catch (Exception e) {
            log.error("处理关闭请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理关闭请求时发生错误: " + e.getMessage())
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