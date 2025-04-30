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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * AI模型列表查询指令插件
 * 处理用户查询可用AI模型的指令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class ModelsCommandPlugin {

    // 每页显示的模型数量
    private static final int PAGE_SIZE = 5;
    private final AiModelService aiModelService;

    /**
     * 处理私聊模型列表查询指令
     * 格式: @models [页码]，默认第1页
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@models(?:\\s+(\\d+))?$")
    public void handlePrivateModels(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        // 提取页码参数，默认为1
        int page = 1;
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            try {
                page = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // 忽略无效页码，使用默认值1
            }
        }

        if (page < 1) page = 1;

        // 处理模型列表查询请求
        processModelsRequest(bot, event.getUserId(), null, page);
    }

    /**
     * 处理群聊模型列表查询指令
     * 格式: @models [页码]，默认第1页
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@models(?:\\s+(\\d+))?$")
    public void handleGroupModels(Bot bot, GroupMessageEvent event, Matcher matcher) {
        // 提取页码参数，默认为1
        int page = 1;
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            try {
                page = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                // 忽略无效页码，使用默认值1
            }
        }

        if (page < 1) page = 1;

        // 处理模型列表查询请求
        processModelsRequest(bot, event.getUserId(), event.getGroupId(), page);
    }
    
    /**
     * 处理私聊模型搜索指令
     * 格式: @search-model 关键词
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@search-model(?:\\s+(.+))?$")
    public void handlePrivateSearchModel(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        // 提取搜索关键词
        String keyword = "";
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            keyword = matcher.group(1).trim();
        }
        
        // 如果关键词为空，返回错误提示
        if (keyword.isEmpty()) {
            String errorMsg = MsgUtils.builder()
                    .text("请提供搜索关键词，格式: @search-model 关键词")
                    .build();
            sendResponse(bot, event.getUserId(), null, errorMsg);
            return;
        }
        
        // 处理模型搜索请求
        processModelSearchRequest(bot, event.getUserId(), null, keyword);
    }
    
    /**
     * 处理群聊模型搜索指令
     * 格式: @search-model 关键词
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@search-model(?:\\s+(.+))?$")
    public void handleGroupSearchModel(Bot bot, GroupMessageEvent event, Matcher matcher) {
        // 提取搜索关键词
        String keyword = "";
        if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
            keyword = matcher.group(1).trim();
        }
        
        // 如果关键词为空，返回错误提示
        if (keyword.isEmpty()) {
            String errorMsg = MsgUtils.builder()
                    .text("请提供搜索关键词，格式: @search-model 关键词")
                    .build();
            sendResponse(bot, event.getUserId(), event.getGroupId(), errorMsg);
            return;
        }
        
        // 处理模型搜索请求
        processModelSearchRequest(bot, event.getUserId(), event.getGroupId(), keyword);
    }

    /**
     * 处理模型列表查询请求
     *
     * @param bot      机器人实例
     * @param senderId 发送者ID
     * @param groupId  群ID，如果是私聊则为null
     * @param page     当前页码，从1开始计数
     */
    private void processModelsRequest(Bot bot, long senderId, Long groupId, int page) {
        try {
            // 获取所有可用的AI模型
            List<AiModel> allModels = aiModelService.getAvailableModels();

            // 手动分页
            int totalItems = allModels.size();
            int totalPages = (int) Math.ceil((double) totalItems / PAGE_SIZE);

            // 如果请求的页码超出了总页数，则使用最后一页
            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }

            // 计算当前页的开始和结束索引
            int fromIndex = (page - 1) * PAGE_SIZE;
            int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

            List<AiModel> pageModels = fromIndex < toIndex
                    ? allModels.subList(fromIndex, toIndex)
                    : new ArrayList<>();

            // 构建模型列表信息
            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("可用AI模型列表 (第" + page + "页/共" + totalPages + "页)\n")
                    .text("====================\n");

            if (pageModels.isEmpty()) {
                msgBuilder.text("暂无可用模型");
            } else {
                for (int i = 0; i < pageModels.size(); i++) {
                    AiModel model = pageModels.get(i);

                    msgBuilder.text("■ " + model.getName() + "\n");

                    // 显示Token费用信息
                    msgBuilder.text("  提问费用: " + model.getPromptCostPerThousandTokens() + " CA/千Token\n")
                            .text("  回答费用: " + model.getCompletionCostPerThousandTokens() + " CA/千Token\n")
                            .text("  费用倍率: " + model.getMultiplier() + "\n");

                    if (model.getDescription() != null && !model.getDescription().isEmpty()) {
                        msgBuilder.text("  描述: " + model.getDescription() + "\n");
                    }

                    // 如果不是最后一个模型，则添加分隔线
                    if (i < pageModels.size() - 1) {
                        msgBuilder.text("--------------------\n");
                    }
                }

                // 添加分页提示和使用说明
                msgBuilder.text("\n");

                // 如果有多个页面，显示分页导航提示
                if (totalPages > 1) {
                    if (page > 1) {
                        msgBuilder.text("◀ 上一页: @models " + (page - 1) + "\n");
                    }

                    if (page < totalPages) {
                        msgBuilder.text("▶ 下一页: @models " + (page + 1) + "\n");
                    }

                    msgBuilder.text("\n");
                }

                // 添加使用说明
                msgBuilder.text("使用方法: @chat [模型名称] [问题内容]");
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
     * 处理模型搜索请求
     *
     * @param bot      机器人实例
     * @param senderId 发送者ID
     * @param groupId  群ID，如果是私聊则为null
     * @param keyword  搜索关键词
     */
    private void processModelSearchRequest(Bot bot, long senderId, Long groupId, String keyword) {
        try {
            // 搜索匹配关键词的模型
            List<AiModel> matchedModels = aiModelService.searchModelsByKeyword(keyword);
            
            // 构建搜索结果信息
            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("搜索结果: \"" + keyword + "\"\n")
                    .text("====================\n");

            if (matchedModels.isEmpty()) {
                msgBuilder.text("未找到匹配的模型\n")
                        .text("提示: 尝试使用 @models 命令查看所有可用模型");
            } else {
                msgBuilder.text("找到 " + matchedModels.size() + " 个匹配模型:\n");
                
                for (int i = 0; i < matchedModels.size(); i++) {
                    AiModel model = matchedModels.get(i);

                    msgBuilder.text("■ " + model.getName() + "\n");

                    // 显示Token费用信息
                    msgBuilder.text("  提问费用: " + model.getPromptCostPerThousandTokens() + " CA/千Token\n")
                            .text("  回答费用: " + model.getCompletionCostPerThousandTokens() + " CA/千Token\n")
                            .text("  费用倍率: " + model.getMultiplier() + "\n");

                    if (model.getDescription() != null && !model.getDescription().isEmpty()) {
                        msgBuilder.text("  描述: " + model.getDescription() + "\n");
                    }

                    // 如果不是最后一个模型，则添加分隔线
                    if (i < matchedModels.size() - 1) {
                        msgBuilder.text("--------------------\n");
                    }
                }
                
                // 添加使用说明
                msgBuilder.text("\n使用方法: @chat [模型名称] [问题内容]");
            }

            String searchResult = msgBuilder.build();
            sendResponse(bot, senderId, groupId, searchResult);

        } catch (Exception e) {
            log.error("处理模型搜索请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理模型搜索请求时发生错误: " + e.getMessage())
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