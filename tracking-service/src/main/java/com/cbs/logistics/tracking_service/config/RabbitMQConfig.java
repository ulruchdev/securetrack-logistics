package com.cbs.logistics.tracking_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration RabbitMQ pour le Tracking Service.
 *
 * <p>Déclare :</p>
 * <ul>
 *   <li>L'exchange "package-status" (topic) — partagé avec Package Service</li>
 *   <li>La queue "tracking-service.package-status" — queue dédiée au consumer</li>
 *   <li>La binding entre les deux avec la routing key "status.changed"</li>
 *   <li>Un convertisseur JSON (Jackson) pour sérialiser/désérialiser les événements</li>
 * </ul>
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "package-status";
    public static final String QUEUE_NAME = "tracking-service.package-status";
    public static final String ROUTING_KEY = "status.changed";

    @Bean
    public TopicExchange packageStatusExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue trackingStatusQueue() {
        return new Queue(QUEUE_NAME, true); // durable
    }

    @Bean
    public Binding binding(Queue trackingStatusQueue, TopicExchange packageStatusExchange) {
        return BindingBuilder
                .bind(trackingStatusQueue)
                .to(packageStatusExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
