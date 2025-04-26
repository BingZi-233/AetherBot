package online.bingzi.aetherbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.bingzi.aetherbot.enums.ConversationStatus;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 对话会话实体
 * 存储用户与AI模型的对话会话信息
 */
@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    /**
     * 会话ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("会话ID（主键）")
    private UUID id;

    /**
     * 会话所属用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("会话所属用户ID")
    private User user;

    /**
     * 使用的AI模型
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_model_id", nullable = false)
    @Comment("使用的AI模型ID")
    private AiModel aiModel;

    /**
     * 会话状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("会话状态：ACTIVE-活跃，CLOSED-关闭")
    private ConversationStatus status = ConversationStatus.ACTIVE;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    @Comment("创建时间")
    private LocalDateTime createTime = LocalDateTime.now();

    /**
     * 更新时间
     */
    @Column(nullable = false)
    @Comment("更新时间")
    private LocalDateTime updateTime = LocalDateTime.now();
} 