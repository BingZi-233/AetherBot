package online.bingzi.aetherbot.events;

import lombok.Getter;
import online.bingzi.aetherbot.entity.Conversation;
import online.bingzi.aetherbot.entity.User;
import org.springframework.context.ApplicationEvent;

/**
 * 聊天错误事件
 * 在聊天过程中发生错误时触发，不计算CA消费
 */
@Getter
public class ChatErrorEvent extends ApplicationEvent {
    private final User user;
    private final Conversation conversation;
    private final String question;
    private final String errorMessage;

    public ChatErrorEvent(Object source, User user, Conversation conversation, String question, String errorMessage) {
        super(source);
        this.user = user;
        this.conversation = conversation;
        this.question = question;
        this.errorMessage = errorMessage;
    }
} 