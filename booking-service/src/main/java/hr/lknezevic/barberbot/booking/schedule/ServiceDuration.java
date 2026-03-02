package hr.lknezevic.barberbot.booking.schedule;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class ServiceDuration {

    private static final Duration DEFAULT_DURATION = Duration.ofMinutes(30);

    private static final Map<String, Duration> DURATIONS = Map.of(
            "šišanje", Duration.ofMinutes(30),
            "sisanje", Duration.ofMinutes(30),
            "brijanje", Duration.ofMinutes(20),
            "šišanje i brijanje", Duration.ofMinutes(45),
            "sisanje i brijanje", Duration.ofMinutes(45)
    );

    public Duration getDuration(String serviceName) {
        if (serviceName == null) return DEFAULT_DURATION;
        return DURATIONS.getOrDefault(serviceName.trim().toLowerCase(), DEFAULT_DURATION);
    }

    public int getDurationMinutes(String serviceName) {
        return (int) getDuration(serviceName).toMinutes();
    }
}
