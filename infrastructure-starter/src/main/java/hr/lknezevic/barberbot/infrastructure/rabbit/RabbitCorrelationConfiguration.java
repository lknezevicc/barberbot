package hr.lknezevic.barberbot.infrastructure.rabbit;

import org.slf4j.MDC;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnClass(RabbitTemplate.class)
public class RabbitCorrelationConfiguration {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";

    @Bean
    public MessagePostProcessor correlationIdPostProcessor() {
        return message -> {
            String correlationId = MDC.get("correlationId");
            if (correlationId != null) {
                message.getMessageProperties().setHeader(CORRELATION_ID_HEADER, correlationId);
            }
            String tenantId = MDC.get("tenantId");
            if (tenantId != null) {
                message.getMessageProperties().setHeader(TENANT_ID_HEADER, tenantId);
            }
            return message;
        };
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter,
                                         MessagePostProcessor correlationIdPostProcessor) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.addBeforePublishPostProcessors(correlationIdPostProcessor);
        return template;
    }

    @Bean
    @Primary
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAfterReceivePostProcessors(message -> {
            Object correlationId = message.getMessageProperties().getHeader(CORRELATION_ID_HEADER);
            if (correlationId != null) {
                MDC.put("correlationId", correlationId.toString());
            }
            Object tenantId = message.getMessageProperties().getHeader(TENANT_ID_HEADER);
            if (tenantId != null) {
                MDC.put("tenantId", tenantId.toString());
            }
            return message;
        });
        return factory;
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
