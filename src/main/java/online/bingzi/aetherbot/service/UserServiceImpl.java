package online.bingzi.aetherbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.User;
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
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        
        User savedUser = userRepository.save(newUser);
        log.info("已创建新用户: {}", savedUser);
        return savedUser;
    }
} 