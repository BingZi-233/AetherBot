package online.bingzi.aetherbot.service;

import online.bingzi.aetherbot.entity.User;

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
     * @param user 用户实体
     * @param amount 变动金额（正数为增加，负数为减少）
     * @return 更新后的用户实体
     */
    User updateCaBalance(User user, double amount);
} 