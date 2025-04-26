package online.bingzi.aetherbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.ConversationStatus;
import online.bingzi.aetherbot.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;

    @Override
    @Transactional
    public Conversation createConversation(User user, AiModel model) {
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setAiModel(model);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setCreateTime(LocalDateTime.now());
        conversation.setUpdateTime(LocalDateTime.now());
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("已创建新对话: {}", savedConversation);
        return savedConversation;
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation findById(UUID id) {
        return conversationRepository.findById(id).orElse(null);
    }

    @Override
    @Transactional
    public Conversation endConversation(Conversation conversation) {
        conversation.setStatus(ConversationStatus.ENDED);
        conversation.setUpdateTime(LocalDateTime.now());
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("已结束对话: {}", savedConversation);
        return savedConversation;
    }
} 