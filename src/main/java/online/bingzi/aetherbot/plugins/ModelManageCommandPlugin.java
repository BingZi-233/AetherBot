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
import online.bingzi.aetherbot.enums.ModelStatus;
import online.bingzi.aetherbot.service.AiModelService;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

/**
 * AI模型管理指令插件
 * 处理管理员对AI模型的管理操作
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelManageCommandPlugin {

    private final AiModelService aiModelService;
    private final UserService userService;

    /**
     * 处理私聊添加模型指令
     * 格式: @addmodel [模型名称] [每千Token费用] [描述]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@addmodel\\s+([\\w-]+)\\s+([0-9]+(\\.[0-9]+)?)(?:\\s+(.+))?$")
    public void handlePrivateAddModel(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        processAddModelRequest(bot, qq, matcher, event.getUserId(), null);
    }

    /**
     * 处理群聊添加模型指令
     * 格式: @addmodel [模型名称] [每千Token费用] [描述]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@addmodel\\s+([\\w-]+)\\s+([0-9]+(\\.[0-9]+)?)(?:\\s+(.+))?$")
    public void handleGroupAddModel(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        processAddModelRequest(bot, qq, matcher, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理私聊模型状态更新指令
     * 格式: @modelstatus [模型名称] [ACTIVE|DISABLED]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@modelstatus\\s+([\\w-]+)\\s+(ACTIVE|DISABLED)$")
    public void handlePrivateModelStatus(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        processModelStatusRequest(bot, qq, matcher, event.getUserId(), null);
    }

    /**
     * 处理群聊模型状态更新指令
     * 格式: @modelstatus [模型名称] [ACTIVE|DISABLED]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@modelstatus\\s+([\\w-]+)\\s+(ACTIVE|DISABLED)$")
    public void handleGroupModelStatus(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        processModelStatusRequest(bot, qq, matcher, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理添加模型请求
     */
    private void processAddModelRequest(Bot bot, String qq, Matcher matcher, long senderId, Long groupId) {
        try {
            // 检查用户权限
            User user = userService.findByQQ(qq);
            if (!userService.isAdmin(user)) {
                String errorMsg = MsgUtils.builder()
                        .text("权限不足，只有管理员可以添加模型。")
                        .build();
                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 解析参数
            String modelName = matcher.group(1);
            double costPerThousandTokens = Double.parseDouble(matcher.group(2));
            String description = matcher.groupCount() >= 4 && matcher.group(4) != null
                    ? matcher.group(4).trim() : "";

            // 创建新模型
            AiModel newModel = aiModelService.createModel(modelName, costPerThousandTokens, description);

            // 构建成功消息
            String successMsg = MsgUtils.builder()
                    .text("模型添加成功！\n")
                    .text("==================\n")
                    .text("名称: " + newModel.getName() + "\n")
                    .text("费用: " + newModel.getCostPerRequest() + " CA/次\n")
                    .text("描述: " + (newModel.getDescription() != null ? newModel.getDescription() : "无"))
                    .build();

            sendResponse(bot, senderId, groupId, successMsg);

        } catch (IllegalArgumentException e) {
            // 处理参数错误
            String errorMsg = MsgUtils.builder()
                    .text("添加模型失败: " + e.getMessage())
                    .build();
            sendResponse(bot, senderId, groupId, errorMsg);

        } catch (Exception e) {
            // 处理其他错误
            log.error("处理添加模型请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("添加模型时发生错误: " + e.getMessage())
                    .build();
            sendResponse(bot, senderId, groupId, errorMsg);
        }
    }

    /**
     * 处理模型状态更新请求
     */
    private void processModelStatusRequest(Bot bot, String qq, Matcher matcher, long senderId, Long groupId) {
        try {
            // 检查用户权限
            User user = userService.findByQQ(qq);
            if (!userService.isAdmin(user)) {
                String errorMsg = MsgUtils.builder()
                        .text("权限不足，只有管理员可以更新模型状态。")
                        .build();
                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 解析参数
            String modelName = matcher.group(1);
            String statusStr = matcher.group(2);
            ModelStatus status = ModelStatus.valueOf(statusStr);

            // 更新模型状态
            AiModel updatedModel = aiModelService.updateModelStatus(modelName, status);

            if (updatedModel == null) {
                String errorMsg = MsgUtils.builder()
                        .text("模型 '" + modelName + "' 不存在。")
                        .build();
                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 构建成功消息
            String statusText = status == ModelStatus.ACTIVE ? "启用" : "禁用";
            String successMsg = MsgUtils.builder()
                    .text("模型状态更新成功！\n")
                    .text("==================\n")
                    .text("名称: " + updatedModel.getName() + "\n")
                    .text("状态: " + statusText)
                    .build();

            sendResponse(bot, senderId, groupId, successMsg);

        } catch (IllegalArgumentException e) {
            // 处理参数错误
            String errorMsg = MsgUtils.builder()
                    .text("更新模型状态失败: " + e.getMessage())
                    .build();
            sendResponse(bot, senderId, groupId, errorMsg);

        } catch (Exception e) {
            // 处理其他错误
            log.error("处理更新模型状态请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("更新模型状态时发生错误: " + e.getMessage())
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