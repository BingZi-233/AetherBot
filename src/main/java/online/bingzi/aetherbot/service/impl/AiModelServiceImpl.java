package online.bingzi.aetherbot.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.enums.ModelStatus;
import online.bingzi.aetherbot.repository.AiModelRepository;
import online.bingzi.aetherbot.service.AiModelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiModelServiceImpl implements AiModelService {

    private final AiModelRepository aiModelRepository;

    @Override
    @Transactional(readOnly = true)
    public AiModel findByName(String name) {
        return aiModelRepository.findByName(name).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModel> getAvailableModels() {
        return aiModelRepository.findByStatus(ModelStatus.ACTIVE);
    }

    @Override
    public String getAvailableModelsAsString() {
        List<AiModel> models = getAvailableModels();

        if (models.isEmpty()) {
            return "暂无可用模型";
        }

        return models.stream()
                .map(model -> model.getName() + " (消耗: " + model.getCostPerRequest().toPlainString() + " CA)")
                .collect(Collectors.joining("\n"));
    }

    @Override
    @Transactional
    public AiModel createModel(String name, BigDecimal promptCostPerThousandTokens,
                               BigDecimal completionCostPerThousandTokens, String description) {
        // 检查模型名称是否已存在
        if (aiModelRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("模型名称 '" + name + "' 已存在");
        }

        // 创建新的模型实体
        AiModel model = new AiModel();
        model.setName(name);
        // 设置提问和回答的费用
        model.setPromptCostPerThousandTokens(promptCostPerThousandTokens);
        model.setCompletionCostPerThousandTokens(completionCostPerThousandTokens);
        model.setDescription(description);
        model.setStatus(ModelStatus.ACTIVE);
        model.setMultiplier(new BigDecimal("1.1")); // 默认倍率为1.1
        model.setCreateTime(LocalDateTime.now());
        model.setUpdateTime(LocalDateTime.now());

        // 保存到数据库
        return aiModelRepository.save(model);
    }

    @Override
    @Transactional
    public AiModel updateModelStatus(String name, ModelStatus status) {
        // 查找模型
        AiModel model = aiModelRepository.findByName(name).orElse(null);

        if (model == null) {
            return null;
        }

        // 更新状态和更新时间
        model.setStatus(status);
        model.setUpdateTime(LocalDateTime.now());

        // 保存更新
        return aiModelRepository.save(model);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<AiModel> searchModelsByKeyword(String keyword) {
        // 使用仓库方法进行模糊搜索，仅搜索激活状态的模型
        return aiModelRepository.findByNameContainingIgnoreCaseAndStatus(keyword, ModelStatus.ACTIVE);
    }
} 