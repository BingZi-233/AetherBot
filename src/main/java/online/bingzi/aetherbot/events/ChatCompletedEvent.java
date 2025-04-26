package online.bingzi.aetherbot.events;

import lombok.Getter;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.User;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChatCompletedEvent extends ApplicationEvent {
    private final User user;
    private final Conversation conversation;
    private final double cost;
    private final String question;
    private final String answer;
    
    // 新增token使用量字段
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Integer totalTokens;

    public ChatCompletedEvent(Object source, User user, Conversation conversation, double cost, String question, String answer,
                             Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        super(source);
        this.user = user;
        this.conversation = conversation;
        this.cost = cost;
        this.question = question;
        this.answer = answer;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }
    
    // 为向后兼容保留旧构造函数
    public ChatCompletedEvent(Object source, User user, Conversation conversation, double cost, String question, String answer) {
        this(source, user, conversation, cost, question, answer, null, null, null);
    }
} 