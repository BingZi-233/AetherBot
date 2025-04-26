package online.bingzi.aetherbot.tasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.CaTransaction;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.TransactionType;
import online.bingzi.aetherbot.enums.UserStatus;
import online.bingzi.aetherbot.repository.CaTransactionRepository;
import online.bingzi.aetherbot.repository.UserRepository;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CA代币奖励定时任务
 * 定期给所有正常状态的用户发放CA代币
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CaRewardScheduledTask {

    // 每日奖励金额
    private static final double DAILY_REWARD_AMOUNT = 10.0;
    private final UserRepository userRepository;
    private final CaTransactionRepository caTransactionRepository;
    private final UserService userService;

    /**
     * 每天凌晨2点执行
     * cron表达式：秒 分 时 日 月 星期
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void dailyReward() {
        log.info("开始执行每日CA代币奖励任务");

        try {
            // 查询所有正常状态的用户
            List<User> activeUsers = userRepository.findByStatus(UserStatus.NORMAL);

            if (activeUsers.isEmpty()) {
                log.info("没有符合条件的用户，跳过发放");
                return;
            }

            int count = 0;
            for (User user : activeUsers) {
                try {
                    // 更新用户余额
                    double oldBalance = user.getCaBalance();
                    user = userService.updateCaBalance(user, DAILY_REWARD_AMOUNT);

                    // 记录交易
                    CaTransaction transaction = new CaTransaction();
                    transaction.setUser(user);
                    transaction.setAmount(DAILY_REWARD_AMOUNT);
                    transaction.setType(TransactionType.RECHARGE);
                    transaction.setDescription("每日登录奖励");
                    transaction.setCreateTime(LocalDateTime.now());
                    caTransactionRepository.save(transaction);

                    log.debug("用户 {} 发放每日奖励 {} CA，当前余额: {}", user.getQq(), DAILY_REWARD_AMOUNT, user.getCaBalance());
                    count++;
                } catch (Exception e) {
                    log.error("处理用户 {} 的每日奖励时出错: {}", user.getQq(), e.getMessage());
                }
            }

            log.info("每日CA代币奖励任务完成，成功发放: {}/{} 人", count, activeUsers.size());
        } catch (Exception e) {
            log.error("执行每日CA代币奖励任务时出错", e);
        }
    }

    /**
     * 每周一凌晨3点执行额外奖励
     * cron表达式：秒 分 时 日 月 星期(1-7代表周一到周日)
     */
    @Scheduled(cron = "0 0 3 ? * 1")
    @Transactional
    public void weeklyExtraReward() {
        log.info("开始执行每周额外CA代币奖励任务");

        try {
            // 查询所有正常状态的用户
            List<User> activeUsers = userRepository.findByStatus(UserStatus.NORMAL);

            if (activeUsers.isEmpty()) {
                log.info("没有符合条件的用户，跳过发放");
                return;
            }

            // 每周额外奖励
            double weeklyExtraAmount = DAILY_REWARD_AMOUNT * 2;

            int count = 0;
            for (User user : activeUsers) {
                try {
                    // 更新用户余额
                    double oldBalance = user.getCaBalance();
                    user = userService.updateCaBalance(user, weeklyExtraAmount);

                    // 记录交易
                    CaTransaction transaction = new CaTransaction();
                    transaction.setUser(user);
                    transaction.setAmount(weeklyExtraAmount);
                    transaction.setType(TransactionType.RECHARGE);
                    transaction.setDescription("每周额外奖励");
                    transaction.setCreateTime(LocalDateTime.now());
                    caTransactionRepository.save(transaction);

                    log.debug("用户 {} 发放每周额外奖励 {} CA，当前余额: {}", user.getQq(), weeklyExtraAmount, user.getCaBalance());
                    count++;
                } catch (Exception e) {
                    log.error("处理用户 {} 的每周额外奖励时出错: {}", user.getQq(), e.getMessage());
                }
            }

            log.info("每周额外CA代币奖励任务完成，成功发放: {}/{} 人", count, activeUsers.size());
        } catch (Exception e) {
            log.error("执行每周额外CA代币奖励任务时出错", e);
        }
    }
} 