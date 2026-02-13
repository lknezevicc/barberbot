package hr.lknezevic.barberbot.gateway.config;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RabbitConfiguration {

    @Bean
    DirectExchange barberbotExchange() {
        return new DirectExchange(RabbitQueues.EXCHANGE, true, false);
    }

    @Bean
    Queue updateInboundQueue() {
        return new Queue(RabbitQueues.UPDATE_INBOUND_QUEUE, true);
    }

    @Bean
    Binding updateInboundBinding(Queue updateInboundQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(updateInboundQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.UPDATE_INBOUND_ROUTING_KEY);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
