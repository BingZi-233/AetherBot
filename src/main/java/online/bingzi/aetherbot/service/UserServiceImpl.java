package online.bingzi.aetherbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.config.AdminProperties;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.UserRole;
import online.bingzi.aetherbot.enums.UserStatus;
import online.bingzi.aetherbot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    @Override
    @Transactional
    public User findByQQ(String qq) {
        return userRepository.findByQq(qq)
                .orElseGet(() -> createNewUser(qq));
    }

    @Override
    @Transactional
    public User updateCaBalance(User user, double amount) {
        user.setCaBalance(user.getCaBalance() + amount);
        user.setUpdateTime(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        // 如果用户角色已经是ADMIN，直接返回true
        if (UserRole.ADMIN.equals(user.getRole())) {
            return true;
        }
        // 否则检查QQ是否在管理员列表中
        return adminProperties.isAdmin(user.getQq());
    }

    @Override
    @Transactional
    public User setUserRole(User user, UserRole role) {
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        log.info("已更新用户角色: {}, 角色: {}", user.getQq(), role);
        return savedUser;
    }

    /**
     * 创建新用户
     *
     * @param qq QQ号
     * @return 新创建的用户实体
     */
    private User createNewUser(String qq) {
        User newUser = new User();
        newUser.setQq(qq);
        newUser.setCaBalance(0.0);
        newUser.setStatus(UserStatus.NORMAL);

        // 检查QQ是否在管理员列表中，设置对应角色
        UserRole role = adminProperties.isAdmin(qq) ? UserRole.ADMIN : UserRole.USER;
        newUser.setRole(role);

        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());

        User savedUser = userRepository.save(newUser);
        log.info("已创建新用户: {}, 角色: {}", savedUser, role);
        return savedUser;
    }
} 