package hr.lknezevic.barberbot.webhook;

import hr.lknezevic.barberbot.config.application.ApplicationConfig;
import hr.lknezevic.barberbot.config.application.TelegramConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({TelegramConfig.class, ApplicationConfig.class})
public class WebhookServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(WebhookServiceApplication.class, args);
	}

}
