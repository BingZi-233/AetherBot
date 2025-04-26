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
import online.bingzi.aetherbot.service.AiModelService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI模型列表查询指令插件
 * 处理用户查询可用AI模型的指令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelsCommandPlugin {

    private final AiModelService aiModelService;

    /**
     * 处理私聊模型列表查询指令
     * 格式: @models
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@models$")
    public void handlePrivateModels(Bot bot, PrivateMessageEvent event) {
        // 处理模型列表查询请求
        processModelsRequest(bot, event.getUserId(), null);
    }

    /**
     * 处理群聊模型列表查询指令
     * 格式: @models
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@models$")
    public void handleGroupModels(Bot bot, GroupMessageEvent event) {
        // 处理模型列表查询请求
        processModelsRequest(bot, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理模型列表查询请求
     *
     * @param bot      机器人实例
     * @param senderId 发送者ID
     * @param groupId  群ID，如果是私聊则为null
     */
    private void processModelsRequest(Bot bot, long senderId, Long groupId) {
        try {
            // 获取所有可用的AI模型
            List<AiModel> models = aiModelService.getAvailableModels();

            // 构建模型列表信息
            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("可用AI模型列表\n")
                    .text("====================\n");

            if (models.isEmpty()) {
                msgBuilder.text("暂无可用模型");
            } else {
                for (int i = 0; i < models.size(); i++) {
                    AiModel model = models.get(i);

                    msgBuilder.text("■ " + model.getName() + "\n");

                    // 显示Token费用信息
                    msgBuilder.text("  提问费用: " + model.getPromptCostPerThousandTokens() + " CA/千Token\n")
                            .text("  回答费用: " + model.getCompletionCostPerThousandTokens() + " CA/千Token\n");

                    if (model.getDescription() != null && !model.getDescription().isEmpty()) {
                        msgBuilder.text("  描述: " + model.getDescription() + "\n");
                    }

                    // 如果不是最后一个模型，则添加分隔线
                    if (i < models.size() - 1) {
                        msgBuilder.text("--------------------\n");
                    }
                }

                // 添加使用说明
                msgBuilder.text("\n使用方法: @chat [模型名称] [问题内容]");
            }

            String modelsInfo = msgBuilder.build();
            sendResponse(bot, senderId, groupId, modelsInfo);

        } catch (Exception e) {
            log.error("处理模型列表查询请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理模型列表查询请求时发生错误: " + e.getMessage())
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