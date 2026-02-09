package hr.lknezevic.barberbot.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
class CircuitBreakerConfiguration {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @PostConstruct
    public void registerCircuitBreakerListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(this::registerListener);
    }

    private void registerListener(CircuitBreaker circuitBreaker) {
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> log.warn(
                        "CircuitBreaker[{}] state transition {} -> {}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()
                ));
    }
}
