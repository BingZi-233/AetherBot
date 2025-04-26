package online.bingzi.aetherbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.AiModel;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.ConversationStatus;
import online.bingzi.aetherbot.repository.ConversationRepository;
import online.bingzi.aetherbot.repository.MessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

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
        conversation.setStatus(ConversationStatus.CLOSED);
        conversation.setUpdateTime(LocalDateTime.now());
        
        Conversation savedConversation = conversationRepository.save(conversation);
        log.info("已结束对话: {}", savedConversation);
        return savedConversation;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Conversation getActiveConversation(User user) {
        List<Conversation> activeConversations = conversationRepository.findByUserAndStatus(
                user, ConversationStatus.ACTIVE);
        
        if (activeConversations.isEmpty()) {
            return null;
        }
        
        // 返回最近创建的对话
        return activeConversations.stream()
                .max(Comparator.comparing(Conversation::getCreateTime))
                .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreateTimeAsc(conversation);
    }
    
    @Override
    @Transactional
    public int endAllActiveConversations(User user) {
        List<Conversation> activeConversations = conversationRepository.findByUserAndStatus(
                user, ConversationStatus.ACTIVE);
        
        if (activeConversations.isEmpty()) {
            return 0;
        }
        
        for (Conversation conversation : activeConversations) {
            endConversation(conversation);
        }
        
        log.info("已结束用户 {} 的所有活跃对话: {} 个", user.getQq(), activeConversations.size());
        return activeConversations.size();
    }
} 