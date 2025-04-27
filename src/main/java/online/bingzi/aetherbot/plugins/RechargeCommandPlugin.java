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
import online.bingzi.aetherbot.enums.TransactionType;
import online.bingzi.aetherbot.repository.CaTransactionRepository;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.regex.Matcher;

/**
 * CA代币充值指令插件
 * 处理管理员的充值命令
 */
@Shiro
@Component
@Slf4j
@RequiredArgsConstructor
public class RechargeCommandPlugin {

    private final UserService userService;
    private final CaTransactionRepository caTransactionRepository;

    /**
     * 处理私聊充值指令
     * 格式: @recharge [QQ号] [金额] 或 @recharge [金额]
     */
    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "^@recharge\\s+(.+)$")
    public void handlePrivateRecharge(Bot bot, PrivateMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        String params = matcher.group(1);

        // 处理充值请求
        processRechargeRequest(bot, qq, params, event.getUserId(), null);
    }

    /**
     * 处理群聊充值指令
     * 格式: @recharge [QQ号] [金额] 或 @recharge [金额]
     */
    @GroupMessageHandler
    @MessageHandlerFilter(cmd = "^@recharge\\s+(.+)$")
    public void handleGroupRecharge(Bot bot, GroupMessageEvent event, Matcher matcher) {
        String qq = String.valueOf(event.getUserId());
        String params = matcher.group(1);

        // 处理充值请求
        processRechargeRequest(bot, qq, params, event.getUserId(), event.getGroupId());
    }

    /**
     * 处理充值请求
     *
     * @param bot        机器人实例
     * @param operatorQQ 操作者QQ
     * @param params     参数字符串
     * @param senderId   发送者ID
     * @param groupId    群ID，如果是私聊则为null
     */
    @Transactional
    private void processRechargeRequest(Bot bot, String operatorQQ, String params, long senderId, Long groupId) {
        try {
            // 查找操作者信息
            User operator = userService.findByQQ(operatorQQ);

            // 检查是否为管理员
            if (!userService.isAdmin(operator)) {
                String errorMsg = MsgUtils.builder()
                        .text("您没有充值权限，此操作仅限管理员使用！")
                        .build();

                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 解析参数，判断是给别人充值还是给自己充值
            String targetQQ;
            BigDecimal amount;
            String[] parts = params.trim().split("\\s+");

            if (parts.length == 1) {
                // 格式：@recharge [金额] - 给自己充值
                targetQQ = operatorQQ;
                amount = parseAmount(parts[0]);
            } else if (parts.length == 2) {
                // 格式：@recharge [QQ号] [金额] - 给别人充值
                targetQQ = parts[0];
                amount = parseAmount(parts[1]);
            } else {
                String errorMsg = MsgUtils.builder()
                        .text("参数格式错误！\n")
                        .text("正确格式：@recharge [QQ号] [金额] 或 @recharge [金额]")
                        .build();

                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 验证金额有效性
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                String errorMsg = MsgUtils.builder()
                        .text("充值金额必须大于0！")
                        .build();

                sendResponse(bot, senderId, groupId, errorMsg);
                return;
            }

            // 查找目标用户
            User targetUser = userService.findByQQ(targetQQ);

            // 记录旧余额用于显示
            BigDecimal oldBalance = targetUser.getCaBalance();

            // 进行充值
            targetUser = userService.updateCaBalance(targetUser, amount);

            // 记录交易
            CaTransaction transaction = new CaTransaction();
            transaction.setUser(targetUser);
            transaction.setAmount(amount);
            transaction.setType(TransactionType.RECHARGE);

            if (!operatorQQ.equals(targetQQ)) {
                transaction.setDescription("管理员充值 - 操作者: " + operatorQQ);
            } else {
                transaction.setDescription("管理员自充值");
            }

            transaction.setCreateTime(LocalDateTime.now());
            caTransactionRepository.save(transaction);

            // 构建充值成功消息
            String successMsg = MsgUtils.builder()
                    .text("充值成功！\n")
                    .text("用户: " + targetQQ + "\n")
                    .text("充值金额: " + amount.toPlainString() + " CA\n")
                    .text("充值前余额: " + oldBalance.toPlainString() + " CA\n")
                    .text("当前余额: " + targetUser.getCaBalance().toPlainString() + " CA")
                    .build();

            sendResponse(bot, senderId, groupId, successMsg);

            // 如果不是自充值，则通知目标用户
            if (!operatorQQ.equals(targetQQ)) {
                String notifyMsg = MsgUtils.builder()
                        .text("您收到一笔CA代币充值！\n")
                        .text("充值金额: " + amount.toPlainString() + " CA\n")
                        .text("当前余额: " + targetUser.getCaBalance().toPlainString() + " CA\n")
                        .text("充值时间: " + transaction.getCreateTime())
                        .build();

                bot.sendPrivateMsg(Long.parseLong(targetQQ), notifyMsg, false);
            }

            log.info("充值完成 - 操作者: {}, 目标用户: {}, 金额: {} CA", operatorQQ, targetQQ, amount.toPlainString());

        } catch (NumberFormatException e) {
            String errorMsg = MsgUtils.builder()
                    .text("金额格式错误，请输入有效的数字！")
                    .build();

            sendResponse(bot, senderId, groupId, errorMsg);
        } catch (Exception e) {
            log.error("处理充值请求时出错", e);
            String errorMsg = MsgUtils.builder()
                    .text("处理充值请求时发生错误: " + e.getMessage())
                    .build();

            sendResponse(bot, senderId, groupId, errorMsg);
        }
    }

    /**
     * 解析金额
     *
     * @param amountStr 金额字符串
     * @return 解析后的金额
     * @throws NumberFormatException 如果解析失败
     */
    private BigDecimal parseAmount(String amountStr) throws NumberFormatException {
        return new BigDecimal(amountStr);
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