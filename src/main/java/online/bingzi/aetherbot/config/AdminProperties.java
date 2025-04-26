package online.bingzi.aetherbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理员配置属性
 * 用于从配置文件中读取管理员QQ列表
 */
@Component
@ConfigurationProperties(prefix = "aether.admin")
@Data
public class AdminProperties {

    /**
     * 管理员QQ列表
     */
    private List<String> qqList = new ArrayList<>();

    /**
     * 检查指定QQ是否为管理员
     *
     * @param qq QQ号
     * @return 是否为管理员
     */
    public boolean isAdmin(String qq) {
        return qqList.contains(qq);
    }
} 