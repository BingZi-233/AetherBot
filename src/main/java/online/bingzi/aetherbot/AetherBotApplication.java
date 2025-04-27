package online.bingzi.aetherbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class AetherBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(AetherBotApplication.class, args);
    }

}
