package hr.lknezevic.barberbot.booking.config;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RabbitConfiguration {

    @Bean
    MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(RabbitQueues.EXCHANGE);
    }

    @Bean
    Queue bookingRequestQueue() {
        return new Queue(RabbitQueues.BOOKING_REQUEST_QUEUE, true);
    }

    @Bean
    Binding bookingRequestBinding(Queue bookingRequestQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookingRequestQueue)
                .to(exchange).with(RabbitQueues.BOOKING_REQUEST_ROUTING_KEY);
    }

    @Bean
    Queue bookingConfirmedQueue() {
        return new Queue(RabbitQueues.BOOKING_CONFIRMED_QUEUE, true);
    }

    @Bean
    Binding bookingConfirmedBinding(Queue bookingConfirmedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookingConfirmedQueue)
                .to(exchange).with(RabbitQueues.BOOKING_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    Queue bookingListRequestQueue() {
        return new Queue(RabbitQueues.BOOKING_LIST_REQUEST_QUEUE, true);
    }

    @Bean
    Binding bookingListRequestBinding(Queue bookingListRequestQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookingListRequestQueue)
                .to(exchange).with(RabbitQueues.BOOKING_LIST_REQUEST_ROUTING_KEY);
    }

    @Bean
    Queue bookingListResponseQueue() {
        return new Queue(RabbitQueues.BOOKING_LIST_RESPONSE_QUEUE, true);
    }

    @Bean
    Binding bookingListResponseBinding(Queue bookingListResponseQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookingListResponseQueue)
                .to(exchange).with(RabbitQueues.BOOKING_LIST_RESPONSE_ROUTING_KEY);
    }

    @Bean
    Queue bookingCancelRequestQueue() {
        return new Queue(RabbitQueues.BOOKING_CANCEL_REQUEST_QUEUE, true);
    }

    @Bean
    Binding bookingCancelRequestBinding(Queue bookingCancelRequestQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookingCancelRequestQueue)
                .to(exchange).with(RabbitQueues.BOOKING_CANCEL_REQUEST_ROUTING_KEY);
    }

    @Bean
    Queue bookingCancelledQueue() {
        return new Queue(RabbitQueues.BOOKING_CANCELLED_QUEUE, true);
    }

    @Bean
    Binding bookingCancelledBinding(Queue bookingCancelledQueue, TopicExchange exchange) {
        return BindingBuilder.bind(bookingCancelledQueue)
                .to(exchange).with(RabbitQueues.BOOKING_CANCELLED_ROUTING_KEY);
    }
}
