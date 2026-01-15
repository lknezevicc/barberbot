package hr.lknezevic.barberbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class BarberBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BarberBotApplication.class, args);
    }

}
