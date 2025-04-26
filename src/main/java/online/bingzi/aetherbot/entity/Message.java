package online.bingzi.aetherbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.bingzi.aetherbot.enums.MessageType;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 消息实体
 * 存储对话会话中的消息记录
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    /**
     * 消息ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("消息ID（主键）")
    private UUID id;

    /**
     * 所属用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("所属用户ID")
    private User user;

    /**
     * 所属会话
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @Comment("所属会话ID")
    private Conversation conversation;

    /**
     * 消息内容
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    @Comment("消息内容")
    private String content;

    /**
     * 消息类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("消息类型：USER-用户消息，AI-AI回复")
    private MessageType type;

    /**
     * Token计数
     */
    @Column
    @Comment("Token计数")
    private Integer tokenCount;

    /**
     * CA消费
     */
    @Column
    @Comment("消耗的CA币")
    private Double caCost;

    /**
     * 是否为错误消息
     */
    @Column(nullable = false)
    @Comment("是否为异常消息")
    private Boolean isError = false;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    @Comment("创建时间")
    private LocalDateTime createTime = LocalDateTime.now();
} 