package online.bingzi.aetherbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.enums.ModelStatus;
import online.bingzi.aetherbot.repository.AiModelRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .map(model -> model.getName() + " (消耗: " + model.getCostPerRequest() + " CA)")
                .collect(Collectors.joining("\n"));
    }
} 