package online.bingzi.aetherbot.service;

import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.UserRole;

import java.math.BigDecimal;

public interface UserService {

    /**
     * 根据QQ号查找用户，如果不存在则创建新用户
     *
     * @param qq QQ号
     * @return 用户实体
     */
    User findByQQ(String qq);

    /**
     * 更新用户CA代币余额
     *
     * @param user   用户实体
     * @param amount 变动金额（正数为增加，负数为减少）
     * @return 更新后的用户实体
     */
    User updateCaBalance(User user, BigDecimal amount);

    /**
     * 检查用户是否为管理员
     *
     * @param user 用户实体
     * @return 是否为管理员
     */
    boolean isAdmin(User user);

    /**
     * 设置用户角色
     *
     * @param user 用户实体
     * @param role 角色
     * @return 更新后的用户实体
     */
    User setUserRole(User user, UserRole role);

    /**
     * 设置用户默认AI模型
     *
     * @param user  用户实体
     * @param model AI模型
     * @return 更新后的用户实体
     */
    User setDefaultAiModel(User user, AiModel model);

    /**
     * 获取用户默认AI模型
     *
     * @param user 用户实体
     * @return 默认AI模型，如果未设置则返回null
     */
    AiModel getDefaultAiModel(User user);

    /**
     * 检查用户是否开启了持续对话模式
     *
     * @param user 用户实体
     * @return 是否开启了持续对话模式
     */
    boolean isContinuousChatEnabled(User user);

    /**
     * 设置用户的持续对话模式状态
     *
     * @param user    用户实体
     * @param enabled 是否启用持续对话
     * @return 更新后的用户实体
     */
    User setContinuousChatEnabled(User user, boolean enabled);
} 