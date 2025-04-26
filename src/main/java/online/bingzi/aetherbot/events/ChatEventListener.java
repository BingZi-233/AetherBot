package online.bingzi.aetherbot.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.bingzi.aetherbot.entity.CaTransaction;
import online.bingzi.aetherbot.entity.Message;
import online.bingzi.aetherbot.entity.User;
import online.bingzi.aetherbot.enums.MessageType;
import online.bingzi.aetherbot.enums.TransactionType;
import online.bingzi.aetherbot.repository.CaTransactionRepository;
import online.bingzi.aetherbot.repository.MessageRepository;
import online.bingzi.aetherbot.service.UserService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 聊天事件监听器
 * 处理聊天完成事件，记录消息并扣除CA代币
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    private final UserService userService;
    private final CaTransactionRepository caTransactionRepository;
    private final MessageRepository messageRepository;

    /**
     * 处理聊天完成事件
     *
     * @param event 聊天完成事件
     */
    @EventListener
    @Transactional
    public void handleChatCompletedEvent(ChatCompletedEvent event) {
        log.info("处理聊天完成事件: {}", event);

        // 获取事件相关信息
        User user = event.getUser();
        double cost = event.getCost();
        String question = event.getQuestion();
        String answer = event.getAnswer();

        try {
            // 记录用户问题和AI回答
            saveMessages(user, event);

            // 扣除CA代币
            deductCa(user, cost, event);

            log.info("聊天事件处理完成，用户：{}，消费：{} CA", user.getQq(), cost);
        } catch (Exception e) {
            log.error("处理聊天完成事件时出错", e);
        }
    }

    /**
     * 保存用户问题和AI回答消息
     */
    private void saveMessages(User user, ChatCompletedEvent event) {
        // 保存用户问题
        Message userMessage = new Message();
        userMessage.setUser(user);
        userMessage.setConversation(event.getConversation());
        userMessage.setContent(event.getQuestion());
        userMessage.setType(MessageType.USER);
        userMessage.setCreateTime(LocalDateTime.now());
        messageRepository.save(userMessage);

        // 保存AI回答
        Message aiMessage = new Message();
        aiMessage.setUser(user);
        aiMessage.setConversation(event.getConversation());
        aiMessage.setContent(event.getAnswer());
        aiMessage.setType(MessageType.AI);
        aiMessage.setCreateTime(LocalDateTime.now());
        messageRepository.save(aiMessage);
    }

    /**
     * 扣除CA代币并记录交易
     */
    private void deductCa(User user, double cost, ChatCompletedEvent event) {
        // 扣除CA代币
        userService.updateCaBalance(user, -cost);

        // 记录CA交易
        CaTransaction transaction = new CaTransaction();
        transaction.setUser(user);
        transaction.setAmount(-cost);
        transaction.setType(TransactionType.CONSUME);
        transaction.setDescription("聊天消费 - 使用模型: " + event.getConversation().getAiModel().getName());
        transaction.setRelatedConversation(event.getConversation());
        transaction.setCreateTime(LocalDateTime.now());
        caTransactionRepository.save(transaction);
    }
} 