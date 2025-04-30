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
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
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

    // 确认码有效期（分钟）
    private static final int CODE_VALID_MINUTES = 5;
    // 确认码长度
    private static final int CODE_LENGTH = 6;
    // 用于存储用户确认码的Map: key是用户QQ，value是确认码信息
    private final Map<String, ConfirmationCode> confirmationCodeMap = new ConcurrentHashMap<>();
    private final UserService userService;
    private final ConfigurableApplicationContext applicationContext;

    // 随机数生成器
    private final Random random = new Random();

    /**
     * 处理私聊关闭指令
     * 格式: @shutdown [确认码]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@shutdown(?:\\s+(.+))?$")
    public void handlePrivateShutdown(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());

        // 提取确认码，如果有的话
        String providedCode = matcher.groupCount() >= 1 && matcher.group(1) != null
                ? matcher.group(1).trim()
                : "";

        // 处理关闭请求
        processShutdownRequest(bot, qq, providedCode, event.getUserId(), null);
    }

    /**
     * 处理群聊关闭指令
     * 格式: @shutdown [确认码]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@shutdown(?:\\s+(.+))?$")
    public void handleGroupShutdown(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());

        // 提取确认码，如果有的话
        String providedCode = matcher.groupCount() >= 1 && matcher.group(1) != null
                ? matcher.group(1).trim()
                : "";

        // 处理关闭请求
        processShutdownRequest(bot, qq, providedCode, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理关闭请求
     *
     * @param bot          机器人实例
     * @param qq           用户QQ
     * @param providedCode 用户提供的确认码，如果为空则生成新的确认码
     * @param senderId     发送者ID
     * @param groupId      群ID，如果是私聊则为null
     */
    private void processShutdownRequest(Bot bot, String qq, String providedCode, long senderId, Long groupId) {
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

            // 如果没有提供确认码，则生成新的确认码并返回
            if (providedCode.isEmpty()) {
                String newCode = generateConfirmationCode(qq);
                String codeMsg = MsgUtils.builder()
                        .text("请确认系统关闭操作\n")
                        .text("您的确认码是: " + newCode + "\n")
                        .text("请在" + CODE_VALID_MINUTES + "分钟内使用以下命令确认关闭:\n")
                        .text("@shutdown " + newCode)
                        .build();
                sendResponse(bot, senderId, groupId, codeMsg);
                return;
            }

            // 验证确认码
            if (!validateConfirmationCode(qq, providedCode)) {
                String errorMsg = MsgUtils.builder()
                        .text("确认码不正确或已过期，请重新获取。\n")
                        .text("使用@shutdown获取新的确认码。")
                        .build();
                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 确认码验证通过，清除该用户的确认码
            confirmationCodeMap.remove(qq);

            // 发送即将关闭的通知
            String shutdownMsg = MsgUtils.builder()
                    .text("确认码验证成功，系统即将关闭...\n")
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
                    log.info("开始执行关闭程序...");
                    Thread.sleep(1000);

                    // 获取退出码
                    int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                    log.info("Spring应用程序已关闭，退出码: {}", exitCode);

                    // 最后强制关闭JVM
                    log.info("正在退出JVM...");
                    System.exit(exitCode);
                } catch (Exception e) {
                    log.error("关闭应用程序时出错", e);
                    System.exit(1); // 发生错误时使用非零退出码
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
     * 生成随机确认码
     *
     * @param qq 用户QQ
     * @return 生成的确认码
     */
    private String generateConfirmationCode(String qq) {
        // 生成由数字和大写字母组成的随机码
        StringBuilder codeBuilder = new StringBuilder();
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 排除容易混淆的字符

        for (int i = 0; i < CODE_LENGTH; i++) {
            codeBuilder.append(chars.charAt(random.nextInt(chars.length())));
        }

        String code = codeBuilder.toString();

        // 存储确认码和生成时间
        ConfirmationCode confirmationCode = new ConfirmationCode(code, LocalDateTime.now());
        confirmationCodeMap.put(qq, confirmationCode);

        return code;
    }

    /**
     * 验证确认码
     *
     * @param qq   用户QQ
     * @param code 用户提供的确认码
     * @return 验证是否通过
     */
    private boolean validateConfirmationCode(String qq, String code) {
        // 获取用户的确认码信息
        ConfirmationCode confirmationCode = confirmationCodeMap.get(qq);

        // 如果没有找到确认码或确认码已过期，返回false
        if (confirmationCode == null ||
            confirmationCode.getGeneratedTime().plusMinutes(CODE_VALID_MINUTES).isBefore(LocalDateTime.now())) {
            return false;
        }

        // 不区分大小写比较确认码
        return confirmationCode.getCode().equalsIgnoreCase(code);
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

    /**
     * 确认码信息内部类
     */
    private static class ConfirmationCode {
        private final String code;
        private final LocalDateTime generatedTime;

        public ConfirmationCode(String code, LocalDateTime generatedTime) {
            this.code = code;
            this.generatedTime = generatedTime;
        }

        public String getCode() {
            return code;
        }

        public LocalDateTime getGeneratedTime() {
            return generatedTime;
        }
    }
} 