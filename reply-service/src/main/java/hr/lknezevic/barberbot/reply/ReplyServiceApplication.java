package hr.lknezevic.barberbot.reply;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ReplyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReplyServiceApplication.class, args);
    }
}
