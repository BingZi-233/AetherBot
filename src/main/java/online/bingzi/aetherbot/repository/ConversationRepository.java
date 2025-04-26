package online.bingzi.aetherbot.repository;

import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    
    /**
     * 查找用户的所有对话
     * 
     * @param user 用户
     * @return 对话列表
     */
    List<Conversation> findByUser(User user);
} 