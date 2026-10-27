package com.ecommerce.notificationservice.service;

/** The email could not be sent. The listener lets it escape, so RabbitMQ tries the message again. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
