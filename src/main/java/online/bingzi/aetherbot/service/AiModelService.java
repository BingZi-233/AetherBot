package online.bingzi.aetherbot.service;

import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.enums.ModelStatus;

import java.util.List;

public interface AiModelService {

    /**
     * 根据模型名称查找模型
     *
     * @param name 模型名称
     * @return 模型实体，如果不存在则返回null
     */
    AiModel findByName(String name);

    /**
     * 获取所有可用的模型
     *
     * @return 可用模型列表
     */
    List<AiModel> getAvailableModels();

    /**
     * 获取所有可用模型的名称，格式化为字符串
     *
     * @return 格式化的模型名称字符串
     */
    String getAvailableModelsAsString();

    /**
     * 创建新AI模型
     *
     * @param name                            模型名称
     * @param promptCostPerThousandTokens     每千提问Token的CA费用
     * @param completionCostPerThousandTokens 每千回答Token的CA费用
     * @param description                     模型描述
     * @return 创建的AI模型实体
     */
    AiModel createModel(String name, Double promptCostPerThousandTokens,
                        Double completionCostPerThousandTokens, String description);

    /**
     * 更新AI模型状态
     *
     * @param name   模型名称
     * @param status 新状态
     * @return 更新后的AI模型实体，如果模型不存在则返回null
     */
    AiModel updateModelStatus(String name, ModelStatus status);
} 