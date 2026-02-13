package hr.lknezevic.barberbot.gateway.publisher;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(TelegramUpdateEvent event) {
        log.info("Publishing update event to RabbitMQ, tenant={}", event.tenantId());
        rabbitTemplate.convertAndSend(
                RabbitQueues.EXCHANGE,
                RabbitQueues.UPDATE_INBOUND_ROUTING_KEY,
                event
        );
    }
}
