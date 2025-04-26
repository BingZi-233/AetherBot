package online.bingzi.aetherbot.enums;

/**
 * 对话会话状态枚举
 * 定义会话的活跃状态
 */
public enum ConversationStatus {
    /**
     * 活跃状态
     * 会话仍在进行中，可以继续对话
     */
    ACTIVE,    // 活跃

    /**
     * 关闭状态
     * 会话已结束，不能继续对话
     */
    CLOSED     // 关闭
} 