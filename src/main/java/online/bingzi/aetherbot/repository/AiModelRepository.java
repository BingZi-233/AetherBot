package online.bingzi.aetherbot.repository;

import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.enums.ModelStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiModelRepository extends JpaRepository<AiModel, UUID> {

    /**
     * 根据模型名称查找模型
     *
     * @param name 模型名称
     * @return 可能存在的模型
     */
    Optional<AiModel> findByName(String name);

    /**
     * 根据状态查找模型列表
     *
     * @param status 模型状态
     * @return 符合状态的模型列表
     */
    List<AiModel> findByStatus(ModelStatus status);
} 