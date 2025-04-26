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
import online.bingzi.aetherbot.entity.CaTransaction;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.repository.CaTransactionRepository;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CA代币余额查询指令插件
 * 处理用户查询CA代币余额的指令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class BalanceCommandPlugin {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int RECENT_TRANSACTIONS_COUNT = 3;
    private final UserService userService;
    private final CaTransactionRepository caTransactionRepository;

    /**
     * 处理私聊余额查询指令
     * 格式: @balance
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@balance$")
    public void handlePrivateBalance(Bot bot, PrivateMessageEvent event) {
        String qq = String.valueOf(event.getUserId());

        // 处理余额查询请求
        processBalanceRequest(bot, qq, event.getUserId(), null);
    }

    /**
     * 处理群聊余额查询指令
     * 格式: @balance
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@balance$")
    public void handleGroupBalance(Bot bot, GroupMessageEvent event) {
        String qq = String.valueOf(event.getUserId());

        // 处理余额查询请求
        processBalanceRequest(bot, qq, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理余额查询请求
     *
     * @param bot      机器人实例
     * @param qq       查询者QQ
     * @param senderId 发送者ID
     * @param groupId  群ID，如果是私聊则为null
     */
    private void processBalanceRequest(Bot bot, String qq, long senderId, Long groupId) {
        try {
            // 查找用户信息
            User user = userService.findByQQ(qq);

            // 查询最近的交易记录
            List<CaTransaction> recentTransactions = caTransactionRepository.findByUserOrderByCreateTimeDesc(user);
            if (recentTransactions.size() > RECENT_TRANSACTIONS_COUNT) {
                recentTransactions = recentTransactions.subList(0, RECENT_TRANSACTIONS_COUNT);
            }

            // 构建余额信息
            MsgUtils msgBuilder = MsgUtils.builder()
                    .text("CA代币余额查询\n")
                    .text("用户: " + user.getQq() + "\n")
                    .text("当前余额: " + String.format("%.9f", user.getCaBalance()) + " CA\n");

            // 如果有交易记录，则显示最近的几笔交易
            if (!recentTransactions.isEmpty()) {
                msgBuilder.text("\n最近" + recentTransactions.size() + "笔交易:\n");

                for (int i = 0; i < recentTransactions.size(); i++) {
                    CaTransaction transaction = recentTransactions.get(i);
                    String amountText = transaction.getAmount().compareTo(BigDecimal.ZERO) > 0 ?
                            "+" + String.format("%.9f", transaction.getAmount()) :
                            String.format("%.9f", transaction.getAmount());

                    msgBuilder.text((i + 1) + ". " + transaction.getType() + " " + amountText + " CA")
                            .text(" (" + transaction.getCreateTime().format(FORMATTER) + ")")
                            .text("\n   " + transaction.getDescription());

                    // 如果不是最后一条记录，则添加换行
                    if (i < recentTransactions.size() - 1) {
                        msgBuilder.text("\n");
                    }
                }
            } else {
                msgBuilder.text("\n暂无交易记录");
            }

            String balanceInfo = msgBuilder.build();
            sendResponse(bot, senderId, groupId, balanceInfo);

        } catch (Exception e) {
            log.error("处理余额查询请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理余额查询请求时发生错误: " + e.getMessage())
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