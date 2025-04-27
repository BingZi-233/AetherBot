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

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        BigDecimal cost = event.getCost();
        String question = event.getQuestion();
        String answer = event.getAnswer();

        try {
            // 记录用户问题和AI回答
            saveMessages(user, event);

            // 扣除CA代币
            deductCa(user, cost, event);

            // 此处不再记录详细的消费信息，因为在deductCa方法中已经记录了真实消费
            log.info("聊天事件处理完成，用户：{}", user.getQq());
        } catch (Exception e) {
            log.error("处理聊天完成事件时出错", e);
        }
    }

    /**
     * 处理聊天错误事件
     *
     * @param event 聊天错误事件
     */
    @EventListener
    @Transactional
    public void handleChatErrorEvent(ChatErrorEvent event) {
        log.info("处理聊天错误事件: {}", event);

        // 获取事件相关信息
        User user = event.getUser();
        String question = event.getQuestion();
        String errorMessage = event.getErrorMessage();

        try {
            // 保存用户问题和错误消息
            Message userMessage = new Message();
            userMessage.setUser(user);
            userMessage.setConversation(event.getConversation());
            userMessage.setContent(question);
            userMessage.setType(MessageType.USER);
            userMessage.setCreateTime(LocalDateTime.now());
            messageRepository.save(userMessage);

            // 保存错误消息
            Message errorMsg = new Message();
            errorMsg.setUser(user);
            errorMsg.setConversation(event.getConversation());
            errorMsg.setContent(errorMessage);
            errorMsg.setType(MessageType.AI);
            errorMsg.setIsError(true);
            errorMsg.setCreateTime(LocalDateTime.now());
            messageRepository.save(errorMsg);

            log.info("聊天错误事件处理完成，用户：{}", user.getQq());
        } catch (Exception e) {
            log.error("处理聊天错误事件时出错", e);
        }
    }

    private void saveMessages(User user, ChatCompletedEvent event) {
        // 保存用户问题
        Message userMessage = new Message();
        userMessage.setUser(user);
        userMessage.setConversation(event.getConversation());
        userMessage.setContent(event.getQuestion());
        userMessage.setType(MessageType.USER);
        userMessage.setCreateTime(LocalDateTime.now());

        // 设置提问token数量（如果有）
        if (event.getPromptTokens() != null) {
            userMessage.setTokenCount(event.getPromptTokens());
        }

        messageRepository.save(userMessage);

        // 保存AI回答
        Message aiMessage = new Message();
        aiMessage.setUser(user);
        aiMessage.setConversation(event.getConversation());
        aiMessage.setContent(event.getAnswer());
        aiMessage.setType(MessageType.AI);
        aiMessage.setCreateTime(LocalDateTime.now());

        // 设置回答token数量（如果有）
        if (event.getCompletionTokens() != null) {
            aiMessage.setTokenCount(event.getCompletionTokens());
        }

        // 计算并设置CA消费（如果可能）
        if (event.getCompletionTokens() != null && event.getConversation().getAiModel() != null) {
            BigDecimal completionCostPerThousandTokens = event.getConversation().getAiModel().getCompletionCostPerThousandTokens();
            BigDecimal multiplier = event.getConversation().getAiModel().getMultiplier();
            BigDecimal tokenCount = new BigDecimal(event.getCompletionTokens());
            BigDecimal divisor = new BigDecimal("1000");

            BigDecimal caCost = completionCostPerThousandTokens.multiply(tokenCount)
                    .divide(divisor, 9, RoundingMode.HALF_UP)
                    .multiply(multiplier);

            aiMessage.setCaCost(caCost);
        }

        messageRepository.save(aiMessage);
    }

    /**
     * 扣除CA代币并记录交易
     */
    private void deductCa(User user, BigDecimal cost, ChatCompletedEvent event) {
        // 计算基于实际Token消耗的CA费用
        BigDecimal actualCost = cost; // 默认使用预估费用

        // 如果存在真实的Token消耗信息，则重新计算费用
        if (event.getPromptTokens() != null && event.getCompletionTokens() != null &&
            event.getConversation().getAiModel() != null) {

            // 使用AiModel的calculateActualCost方法计算实际费用
            actualCost = event.getConversation().getAiModel().calculateActualCost(
                    event.getPromptTokens(), event.getCompletionTokens());

            log.debug("基于实际Token的费用计算 - 提问Token: {}, 回答Token: {}, 预估费用: {}, 实际费用: {}",
                    event.getPromptTokens(), event.getCompletionTokens(),
                    cost.toPlainString(), actualCost.toPlainString());
        } else {
            log.debug("使用预估费用 - 未获取到真实Token消耗信息");
        }

        // 扣除CA代币
        userService.updateCaBalance(user, actualCost.negate());

        // 记录CA交易
        CaTransaction transaction = new CaTransaction();
        transaction.setUser(user);
        transaction.setAmount(actualCost.negate());
        transaction.setType(TransactionType.CONSUME);

        // 添加token使用量到交易描述中（如果有）
        if (event.getTotalTokens() != null) {
            transaction.setDescription("聊天消费 - 使用模型: " + event.getConversation().getAiModel().getName() +
                                       ", 使用Token: " + event.getTotalTokens());
        } else {
            transaction.setDescription("聊天消费 - 使用模型: " + event.getConversation().getAiModel().getName());
        }

        transaction.setRelatedConversation(event.getConversation());
        transaction.setCreateTime(LocalDateTime.now());
        caTransactionRepository.save(transaction);

        // 记录实际消费信息
        log.info("聊天消费完成 - 用户: {}, 预估费用: {} CA, 实际费用: {} CA, Token总量: {}",
                user.getQq(), cost.toPlainString(), actualCost.toPlainString(), event.getTotalTokens());
    }
} 