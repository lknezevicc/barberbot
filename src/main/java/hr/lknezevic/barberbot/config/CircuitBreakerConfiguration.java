package hr.lknezevic.barberbot.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
class CircuitBreakerConfiguration {

    @Bean
    CircuitBreaker redisQueueCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("redisQueue");

        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> {
                    log.warn(
                            "CircuitBreaker[{}] state transition {} -> {}",
                            event.getCircuitBreakerName(),
                            event.getStateTransition().getFromState(),
                            event.getStateTransition().getToState()
                    );
                });

        return circuitBreaker;
    }
}
