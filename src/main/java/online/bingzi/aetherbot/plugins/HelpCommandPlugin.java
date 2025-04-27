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
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;

/**
 * 帮助指令插件
 * 处理用户查询所有命令和特定命令的帮助功能
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class HelpCommandPlugin {

    private final UserService userService;
    private final Map<String, CommandInfo> commandInfoMap = new HashMap<>();
    private final Map<String, List<String>> categoryCommandsMap = new LinkedHashMap<>();

    /**
     * 构造函数中初始化命令信息
     */ {
        // 初始化命令分类
        categoryCommandsMap.put("CA币管理", new ArrayList<>());
        categoryCommandsMap.put("对话功能", new ArrayList<>());
        categoryCommandsMap.put("系统功能", new ArrayList<>());

        // 添加CA币管理相关命令
        addCommand("recharge", "充值CA代币",
                "为指定用户充值CA代币，只有管理员可以使用此命令。\n如果不指定QQ号，则为自己充值。",
                "@recharge [QQ号] [金额] 或 @recharge [金额]",
                true, "CA币管理");

        addCommand("balance", "查询CA代币余额",
                "查询自己当前的CA代币余额和最近的交易记录。",
                "@balance",
                false, "CA币管理");

        // 添加对话功能相关命令
        addCommand("chat", "与AI对话",
                "使用指定的AI模型进行对话。\n首次对话需要指定模型名称；\n同一会话内的后续对话无需再指定模型名称。",
                "首次对话: @chat [模型名称] [问题内容]\n继续对话: @chat [问题内容]",
                false, "对话功能");

        addCommand("setmodel", "设置对话模型",
                "设置当前会话使用的AI模型。\n使用此命令后，可以直接用@chat进行对话，无需每次指定模型。",
                "@setmodel [模型名称]",
                false, "对话功能");

        addCommand("end", "结束当前对话",
                "结束当前正在进行的对话会话。",
                "@end",
                false, "对话功能");

        addCommand("models", "查询可用AI模型",
                "查询系统中所有可用的AI模型及其CA币消耗。",
                "@models",
                false, "对话功能");

        addCommand("history", "查询对话历史",
                "查询自己的对话历史记录，支持分页查看。",
                "@history [页码]，默认显示第1页",
                false, "对话功能");

        // 添加系统功能相关命令
        addCommand("help", "查看帮助信息",
                "查看所有可用的命令或特定命令的详细帮助信息。",
                "@help 或 @help [命令名]",
                false, "系统功能");

        // 添加模型管理相关命令
        addCommand("addmodel", "添加AI模型",
                "添加新的AI模型到系统中，区分提问Token和回答Token的费用，只有管理员可以使用此命令。",
                "@addmodel [模型名称] [提问每千Token费用] [回答每千Token费用] [可选描述]",
                true, "系统功能");

        addCommand("modelstatus", "更新模型状态",
                "更新指定AI模型的状态（启用/禁用），只有管理员可以使用此命令。",
                "@modelstatus [模型名称] [ACTIVE|DISABLED]",
                true, "系统功能");

        addCommand("shutdown", "关闭系统",
                "安全地关闭SpringBoot应用程序，需要确认码以防误操作，只有管理员可以使用此命令。",
                "@shutdown confirm",
                true, "系统功能");
    }

    /**
     * 添加命令信息到映射表
     */
    private void addCommand(String name, String shortDescription, String detailedDescription,
                            String usage, boolean adminOnly, String category) {
        CommandInfo info = new CommandInfo(name, shortDescription, detailedDescription, usage, adminOnly, category);
        commandInfoMap.put(name, info);
        categoryCommandsMap.get(category).add(name);
    }

    /**
     * 处理私聊帮助指令
     * 格式: @help 或 @help [命令名]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@help(?:\\s+([\\w-]+))?$")
    public void handlePrivateHelp(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());

        // 如果有命令名参数，则显示特定命令的详细帮助
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            String commandName = matcher.group(1);
            processSpecificHelpRequest(bot, qq, commandName, event.getUserId(), null);
        } else {
            // 否则显示所有命令的帮助列表
            processGeneralHelpRequest(bot, qq, event.getUserId(), null);
        }
    }

    /**
     * 处理群聊帮助指令
     * 格式: @help 或 @help [命令名]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@help(?:\\s+([\\w-]+))?$")
    public void handleGroupHelp(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());

        // 如果有命令名参数，则显示特定命令的详细帮助
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            String commandName = matcher.group(1);
            processSpecificHelpRequest(bot, qq, commandName, event.getUserId(), event.getGroupId());
        } else {
            // 否则显示所有命令的帮助列表
            processGeneralHelpRequest(bot, qq, event.getUserId(), event.getGroupId());
        }
    }

    /**
     * 处理通用帮助请求，显示所有命令列表
     */
    private void processGeneralHelpRequest(Bot bot, String qq, long senderId, Long groupId) {
        try {
            // 查找用户信息
            User user = userService.findByQQ(qq);
            boolean isAdmin = userService.isAdmin(user);

            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("AetherBot 命令帮助\n")
                    .text("===================\n\n");

            // 按照分类构建命令列表
            for (Map.Entry<String, List<String>> category : categoryCommandsMap.entrySet()) {
                String categoryName = category.getKey();
                List<String> commands = category.getValue();

                if (!commands.isEmpty()) {
                    msgBuilder.text("【" + categoryName + "】\n");

                    for (String cmdName : commands) {
                        CommandInfo info = commandInfoMap.get(cmdName);

                        // 如果命令需要管理员权限，且用户不是管理员，则跳过
                        if (info.adminOnly && !isAdmin) {
                            continue;
                        }

                        msgBuilder.text("• " + info.name + ": " + info.shortDescription + "\n");
                    }

                    msgBuilder.text("\n");
                }
            }

            // 添加查看详细帮助的说明
            msgBuilder.text("输入\"@help [命令名]\"可查看特定命令的详细使用说明。");

            String helpMsg = msgBuilder.build();
            sendResponse(bot, senderId, groupId, helpMsg);

        } catch (Exception e) {
            log.error("处理帮助请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理帮助请求时发生错误: " + e.getMessage())
                    .build();

            sendResponse(bot, senderId, groupId, errorMsg);
        }
    }

    /**
     * 处理特定命令的帮助请求，显示详细说明
     */
    private void processSpecificHelpRequest(Bot bot, String qq, String commandName, long senderId, Long groupId) {
        try {
            // 查找用户信息
            User user = userService.findByQQ(qq);
            boolean isAdmin = userService.isAdmin(user);

            // 查找命令信息
            CommandInfo info = commandInfoMap.get(commandName);

            if (info == null) {
                String errorMsg = MsgUtils.builder()
                        .text("未找到命令 \"" + commandName + "\" 的帮助信息。\n")
                        .text("输入\"@help\"查看所有可用命令。")
                        .build();

                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 如果命令需要管理员权限，且用户不是管理员，则显示权限错误
            if (info.adminOnly && !isAdmin) {
                String errorMsg = MsgUtils.builder()
                        .text("您没有查看该命令帮助的权限，此命令仅限管理员使用。")
                        .build();

                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 构建命令详细帮助信息
            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("命令: @" + info.name + "\n")
                    .text("===================\n\n")
                    .text("【描述】\n" + info.shortDescription + "\n\n")
                    .text("【详细说明】\n" + info.detailedDescription + "\n\n")
                    .text("【用法】\n" + info.usage + "\n\n");

            if (info.adminOnly) {
                msgBuilder.text("【权限】\n仅限管理员使用\n\n");
            }

            msgBuilder.text("【分类】\n" + info.category);

            String helpMsg = msgBuilder.build();
            sendResponse(bot, senderId, groupId, helpMsg);

        } catch (Exception e) {
            log.error("处理特定命令帮助请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理帮助请求时发生错误: " + e.getMessage())
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

    /**
     * 命令信息内部类
     */
    private static class CommandInfo {
        private final String name;
        private final String shortDescription;
        private final String detailedDescription;
        private final String usage;
        private final boolean adminOnly;
        private final String category;

        public CommandInfo(String name, String shortDescription, String detailedDescription,
                           String usage, boolean adminOnly, String category) {
            this.name = name;
            this.shortDescription = shortDescription;
            this.detailedDescription = detailedDescription;
            this.usage = usage;
            this.adminOnly = adminOnly;
            this.category = category;
        }
    }
} 