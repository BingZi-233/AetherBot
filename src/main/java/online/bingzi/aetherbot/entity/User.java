package online.bingzi.aetherbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.bingzi.aetherbot.enums.UserRole;
import online.bingzi.aetherbot.enums.UserStatus;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户实体
 * 存储用户的基本信息、CA余额和状态
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 用户ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("用户ID（主键）")
    private UUID id;

    /**
     * QQ号码，用于识别用户
     */
    @Column(unique = true, nullable = false)
    @Comment("QQ号码，用于识别用户")
    private String qq;

    /**
     * 用户名
     */
    @Column(length = 50)
    @Comment("用户名")
    private String username;

    /**
     * CA余额
     */
    @Column(nullable = false, precision = 19, scale = 9)
    @Comment("CA余额")
    private BigDecimal caBalance = BigDecimal.ZERO;

    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("用户状态：NORMAL-正常，BANNED-禁用")
    private UserStatus status = UserStatus.NORMAL;

    /**
     * 用户角色
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("用户角色：ADMIN-管理员，USER-普通用户")
    private UserRole role = UserRole.USER;

    /**
     * 默认AI模型
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "default_ai_model_id")
    @Comment("用户默认的AI模型")
    private AiModel defaultAiModel;

    /**
     * 是否开启持续对话模式（仅在私聊中有效）
     */
    @Column(nullable = false)
    @Comment("是否开启持续对话模式（仅在私聊中有效）")
    private boolean continuousChatEnabled = false;

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