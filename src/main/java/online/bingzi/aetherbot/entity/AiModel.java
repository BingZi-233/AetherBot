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
     * 每千提问Token的CA费用
     */
    @Column(nullable = false, precision = 19, scale = 9)
    @Comment("每千提问Token的CA费用")
    private BigDecimal promptCostPerThousandTokens;

    /**
     * 每千回答Token的CA费用
     */
    @Column(nullable = false, precision = 19, scale = 9)
    @Comment("每千回答Token的CA费用")
    private BigDecimal completionCostPerThousandTokens;

    /**
     * 费用倍率
     */
    @Column(nullable = false, precision = 19, scale = 9)
    @Comment("费用倍率，默认1.1")
    private BigDecimal multiplier = new BigDecimal("1.1");

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
    public BigDecimal getCostPerRequest() {
        // 假设一次请求使用的提问Token和回答Token的分布比例
        int estimatedPromptTokens = 500;     // 提问Token估计值
        int estimatedCompletionTokens = 1500; // 回答Token估计值

        // 使用BigDecimal进行精确计算
        BigDecimal promptTokens = new BigDecimal(estimatedPromptTokens);
        BigDecimal completionTokens = new BigDecimal(estimatedCompletionTokens);
        BigDecimal divisor = new BigDecimal("1000");

        // 计算提问部分费用: (promptCostPerThousandTokens * estimatedPromptTokens / 1000)
        BigDecimal promptResult = promptCostPerThousandTokens.multiply(promptTokens)
                .divide(divisor, 9, RoundingMode.HALF_UP);

        // 计算回答部分费用: (completionCostPerThousandTokens * estimatedCompletionTokens / 1000)
        BigDecimal completionResult = completionCostPerThousandTokens.multiply(completionTokens)
                .divide(divisor, 9, RoundingMode.HALF_UP);

        // 合并两部分费用并应用倍率: (promptResult + completionResult) * multiplier
        BigDecimal totalResult = promptResult.add(completionResult).multiply(multiplier);

        return totalResult;
    }
    
    /**
     * 基于实际Token消耗计算请求成本
     *
     * @param promptTokens 提问Token数量
     * @param completionTokens 回答Token数量
     * @return 实际请求成本
     */
    public BigDecimal calculateActualCost(Integer promptTokens, Integer completionTokens) {
        if (promptTokens == null || completionTokens == null) {
            // 如果没有实际Token信息，则返回预估费用
            return getCostPerRequest();
        }
        
        // 使用BigDecimal进行精确计算
        BigDecimal promptTokensValue = new BigDecimal(promptTokens);
        BigDecimal completionTokensValue = new BigDecimal(completionTokens);
        BigDecimal divisor = new BigDecimal("1000");

        // 计算提问部分费用: (promptCostPerThousandTokens * promptTokens / 1000)
        BigDecimal promptResult = promptCostPerThousandTokens.multiply(promptTokensValue)
                .divide(divisor, 9, RoundingMode.HALF_UP);

        // 计算回答部分费用: (completionCostPerThousandTokens * completionTokens / 1000)
        BigDecimal completionResult = completionCostPerThousandTokens.multiply(completionTokensValue)
                .divide(divisor, 9, RoundingMode.HALF_UP);

        // 合并两部分费用并应用倍率: (promptResult + completionResult) * multiplier
        BigDecimal totalResult = promptResult.add(completionResult).multiply(multiplier);

        return totalResult;
    }
} 