package com.ecommerce.notificationservice.listener;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * The message can never be handled (no messageId, no email address, ...). Retrying will not help,
 * so the message is rejected without requeue and RabbitMQ moves it to the dead-letter queue.
 */
public class InvalidEventException extends AmqpRejectAndDontRequeueException {

    public InvalidEventException(String message) {
        super(message);
    }
}
