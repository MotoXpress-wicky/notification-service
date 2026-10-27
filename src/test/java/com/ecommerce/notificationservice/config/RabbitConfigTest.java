package com.ecommerce.notificationservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RabbitConfigTest {

    @Autowired
    @Qualifier("emailTopology")
    Declarables email;

    @Autowired
    @Qualifier("pushTopology")
    Declarables push;

    @Test
    void emailQueueIsBoundToEveryEventThatHasAnEmail() {
        assertThat(email.getDeclarablesByType(Binding.class))
                .extracting(Binding::getRoutingKey)
                .contains("user.registered", "user.password.reset", "user.password.changed", "order.placed", "order.cancelled")
                .allMatch(key -> email.getDeclarablesByType(Binding.class).stream()
                        .allMatch(b -> b.getExchange().equals("app.events") && b.getDestination().equals("notification.email")));
    }

    @Test
    void pushQueueIsBoundOnlyToOrderEvents() {
        assertThat(push.getDeclarablesByType(Binding.class))
                .extracting(Binding::getRoutingKey)
                .containsExactly("order.placed", "order.shipped", "order.delivered", "order.returned", "order.cancelled");
    }

    @Test
    void everyQueueIsDurableAndHasADeadLetterQueue() {
        var queues = email.getDeclarablesByType(Queue.class);

        assertThat(queues).extracting(Queue::getName).containsExactlyInAnyOrder("notification.email", "notification.email.dlq");
        assertThat(queues).allMatch(Queue::isDurable);

        Queue main = queues.stream().filter(q -> q.getName().equals("notification.email")).findFirst().orElseThrow();
        assertThat(main.getArguments())
                .containsEntry("x-dead-letter-exchange", "")
                .containsEntry("x-dead-letter-routing-key", "notification.email.dlq");
    }
}
