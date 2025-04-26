package online.bingzi.aetherbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.bingzi.aetherbot.enums.ModelStatus;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AI模型实体
 * 存储AI模型信息和计费设置
 */
@Entity
@Table(name = "ai_models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiModel {

    /**
     * 模型ID（主键）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Comment("模型ID（主键）")
    private UUID id;

    /**
     * 模型名称
     */
    @Column(unique = true, nullable = false, length = 100)
    @Comment("模型名称")
    private String name;

    /**
     * 每千Token的CA费用
     */
    @Column(nullable = false)
    @Comment("每千Token的CA费用")
    private Double costPerThousandTokens;

    /**
     * 费用倍率
     */
    @Column(nullable = false)
    @Comment("费用倍率，默认1.0")
    private Double multiplier = 1.0;

    /**
     * 模型描述
     */
    @Column(length = 500)
    @Comment("模型描述")
    private String description;

    /**
     * 模型状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Comment("模型状态：ACTIVE-活跃，DISABLED-禁用")
    private ModelStatus status = ModelStatus.ACTIVE;

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

    /**
     * 获取每次请求的成本
     * 这是一个简化的计算，实际应用中可以根据请求的复杂度等因素进行更复杂的计算
     *
     * @return 每次请求的成本
     */
    public Double getCostPerRequest() {
        // 这里假设一个简单的计算方式：默认一次请求大约使用2000个token
        int estimatedTokensPerRequest = 2000;

        // 使用BigDecimal进行精确计算，确保精度不会丢失
        BigDecimal tokenCost = new BigDecimal(costPerThousandTokens.toString());
        BigDecimal estimatedTokens = new BigDecimal(estimatedTokensPerRequest);
        BigDecimal divisor = new BigDecimal("1000");
        BigDecimal multiplierValue = new BigDecimal(multiplier.toString());

        // 计算: (costPerThousandTokens * estimatedTokensPerRequest / 1000) * multiplier
        BigDecimal result = tokenCost.multiply(estimatedTokens)
                .divide(divisor, 9, RoundingMode.HALF_UP)
                .multiply(multiplierValue);

        return result.doubleValue();
    }
} 