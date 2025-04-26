package online.bingzi.aetherbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.enums.ModelStatus;
import online.bingzi.aetherbot.repository.AiModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .map(model -> model.getName() + " (消耗: " + String.format("%.9f", model.getCostPerRequest()) + " CA)")
                .collect(Collectors.joining("\n"));
    }
    
    @Override
    @Transactional
    public AiModel createModel(String name, Double costPerThousandTokens, String description) {
        // 检查模型名称是否已存在
        if (aiModelRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("模型名称 '" + name + "' 已存在");
        }
        
        // 创建新的模型实体
        AiModel model = new AiModel();
        model.setName(name);
        model.setCostPerThousandTokens(costPerThousandTokens);
        model.setDescription(description);
        model.setStatus(ModelStatus.ACTIVE);
        model.setMultiplier(1.0); // 默认倍率为1.0
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
} 