package online.bingzi.aetherbot.service;

import online.bingzi.aetherbot.entity.AiModel;

import java.util.List;
import java.util.UUID;

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
} 