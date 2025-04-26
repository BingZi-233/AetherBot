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

    public ChatCompletedEvent(Object source, User user, Conversation conversation, double cost, String question, String answer) {
        super(source);
        this.user = user;
        this.conversation = conversation;
        this.cost = cost;
        this.question = question;
        this.answer = answer;
    }
} 