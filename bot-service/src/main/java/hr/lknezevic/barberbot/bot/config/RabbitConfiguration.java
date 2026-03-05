package hr.lknezevic.barberbot.bot.config;

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
    Queue replyOutboundQueue() {
        return new Queue(RabbitQueues.REPLY_OUTBOUND_QUEUE, true);
    }

    @Bean
    Binding replyOutboundBinding(Queue replyOutboundQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(replyOutboundQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.REPLY_OUTBOUND_ROUTING_KEY);
    }

    @Bean
    Queue bookingRequestQueue() {
        return new Queue(RabbitQueues.BOOKING_REQUEST_QUEUE, true);
    }

    @Bean
    Binding bookingRequestBinding(Queue bookingRequestQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingRequestQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_REQUEST_ROUTING_KEY);
    }

    @Bean
    Queue bookingConfirmedQueue() {
        return new Queue(RabbitQueues.BOOKING_CONFIRMED_QUEUE, true);
    }

    @Bean
    Binding bookingConfirmedBinding(Queue bookingConfirmedQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingConfirmedQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_CONFIRMED_ROUTING_KEY);
    }

    @Bean
    Queue bookingListRequestQueue() {
        return new Queue(RabbitQueues.BOOKING_LIST_REQUEST_QUEUE, true);
    }

    @Bean
    Binding bookingListRequestBinding(Queue bookingListRequestQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingListRequestQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_LIST_REQUEST_ROUTING_KEY);
    }

    @Bean
    Queue bookingListResponseQueue() {
        return new Queue(RabbitQueues.BOOKING_LIST_RESPONSE_QUEUE, true);
    }

    @Bean
    Binding bookingListResponseBinding(Queue bookingListResponseQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingListResponseQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_LIST_RESPONSE_ROUTING_KEY);
    }

    @Bean
    Queue bookingCancelRequestQueue() {
        return new Queue(RabbitQueues.BOOKING_CANCEL_REQUEST_QUEUE, true);
    }

    @Bean
    Binding bookingCancelRequestBinding(Queue bookingCancelRequestQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingCancelRequestQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_CANCEL_REQUEST_ROUTING_KEY);
    }

    @Bean
    Queue bookingCancelledQueue() {
        return new Queue(RabbitQueues.BOOKING_CANCELLED_QUEUE, true);
    }

    @Bean
    Binding bookingCancelledBinding(Queue bookingCancelledQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingCancelledQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_CANCELLED_ROUTING_KEY);
    }

    @Bean
    Queue bookingHeldQueue() {
        return new Queue(RabbitQueues.BOOKING_HELD_QUEUE, true);
    }

    @Bean
    Binding bookingHeldBinding(Queue bookingHeldQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingHeldQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_HELD_ROUTING_KEY);
    }

    @Bean
    Queue bookingSlotUnavailableQueue() {
        return new Queue(RabbitQueues.BOOKING_SLOT_UNAVAILABLE_QUEUE, true);
    }

    @Bean
    Binding bookingSlotUnavailableBinding(Queue bookingSlotUnavailableQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingSlotUnavailableQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_SLOT_UNAVAILABLE_ROUTING_KEY);
    }

    @Bean
    Queue bookingConfirmHoldQueue() {
        return new Queue(RabbitQueues.BOOKING_CONFIRM_HOLD_QUEUE, true);
    }

    @Bean
    Binding bookingConfirmHoldBinding(Queue bookingConfirmHoldQueue, DirectExchange barberbotExchange) {
        return BindingBuilder
                .bind(bookingConfirmHoldQueue)
                .to(barberbotExchange)
                .with(RabbitQueues.BOOKING_CONFIRM_HOLD_ROUTING_KEY);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
