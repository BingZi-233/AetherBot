package online.bingzi.aetherbot.repository;

import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    
    /**
     * 查找用户的指定状态的对话
     * 
     * @param user 用户
     * @param status 对话状态
     * @return 对话列表
     */
    List<Conversation> findByUserAndStatus(User user, ConversationStatus status);
    
    /**
     * 查找用户的指定状态的对话，并预先加载AI模型
     * 使用JOIN FETCH解决懒加载问题
     * 
     * @param user 用户
     * @param status 对话状态
     * @return 对话列表，已预加载AI模型
     */
    @Query("SELECT c FROM Conversation c JOIN FETCH c.aiModel WHERE c.user = :user AND c.status = :status")
    List<Conversation> findByUserAndStatusWithAiModel(@Param("user") User user, @Param("status") ConversationStatus status);
} 