package com.cbs.logistics.package_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ pour le Package Service.
 *
 * <p>Déclare l'exchange "package-status" (topic) et le convertisseur
 * JSON (Jackson) pour la sérialisation des événements.</p>
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "package-status";

    @Bean
    public TopicExchange packageStatusExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
