package online.bingzi.aetherbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.bingzi.aetherbot.enums.TransactionType;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CA交易记录实体
 * 记录用户CA币的充值和消费记录
 */
@Entity
@Table(name = "ca_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaTransaction {

    /**
     * 交易ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("交易ID（主键）")
    private UUID id;

    /**
     * 关联用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Comment("关联用户ID")
    private User user;

    /**
     * 交易金额
     */
    @Column(nullable = false, precision = 19, scale = 9)
    @Comment("交易金额")
    private BigDecimal amount;

    /**
     * 交易类型
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("交易类型：RECHARGE-充值，CONSUME-消费")
    private TransactionType type;

    /**
     * 交易描述
     */
    @Column(length = 255)
    @Comment("交易描述")
    private String description;

    /**
     * 关联会话
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    @Comment("关联会话ID（可选）")
    private Conversation relatedConversation;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    @Comment("创建时间")
    private LocalDateTime createTime = LocalDateTime.now();
} 