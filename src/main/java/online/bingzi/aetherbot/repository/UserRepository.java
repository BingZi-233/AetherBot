package online.bingzi.aetherbot.repository;

import online.bingzi.aetherbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * 根据QQ号查找用户
     * 
     * @param qq QQ号
     * @return 可能存在的用户
     */
    Optional<User> findByQq(String qq);
} 