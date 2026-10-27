package com.ecommerce.notificationservice.config;

import com.ecommerce.notificationservice.service.TemplateMap;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * RabbitMQ setup. Rules from docs/mvp/07-events.md:
 * - the exchange is declared by publisher and listener (same name, type topic, durable);
 * - the queue and its bindings are declared only by the listener (this service).
 *
 * The bindings come from the template map: a queue listens to exactly the events that have a rule.
 * A message that fails every retry goes to "<queue>.dlq" (dead-letter queue) for a person to check.
 */
@Configuration
public class RabbitConfig {

    /** Event bodies are JSON. This converter also makes @RabbitListener methods accept a Map. */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /** After the last retry: do not put the message back on the queue (that would loop forever). */
    @Bean
    public MessageRecoverer messageRecoverer() {
        return new RejectAndDontRequeueRecoverer();
    }

    @Bean
    public TopicExchange appEvents(NotificationProperties properties) {
        return new TopicExchange(properties.exchange(), true, false);
    }

    @Bean
    public Declarables emailTopology(NotificationProperties properties, TemplateMap templateMap, TopicExchange appEvents) {
        return topology(properties.email().queue(), templateMap.eventsWithEmail(), appEvents);
    }

    @Bean
    public Declarables pushTopology(NotificationProperties properties, TemplateMap templateMap, TopicExchange appEvents) {
        return topology(properties.push().queue(), templateMap.eventsWithPush(), appEvents);
    }

    private Declarables topology(String queueName, List<String> events, TopicExchange exchange) {
        String deadLetterQueueName = queueName + ".dlq";

        Queue queue = QueueBuilder.durable(queueName)
                // "" = the default exchange, which delivers by queue name.
                .deadLetterExchange("")
                .deadLetterRoutingKey(deadLetterQueueName)
                .build();
        Queue deadLetterQueue = QueueBuilder.durable(deadLetterQueueName).build();

        List<Declarable> all = new ArrayList<>(List.of(queue, deadLetterQueue));
        for (String event : events) {
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(event);
            all.add(binding);
        }
        return new Declarables(all);
    }
}
