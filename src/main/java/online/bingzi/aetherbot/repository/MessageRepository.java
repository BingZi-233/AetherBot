package online.bingzi.aetherbot.repository;

import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * 根据对话查询消息列表
     *
     * @param conversation 对话
     * @return 消息列表
     */
    List<Message> findByConversationOrderByCreateTimeAsc(Conversation conversation);

    /**
     * 根据用户查询消息列表
     *
     * @param user 用户
     * @return 消息列表
     */
    List<Message> findByUserOrderByCreateTimeDesc(User user);
} 