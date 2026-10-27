package com.ecommerce.notificationservice.service;

/** A push message could not be sent now, but a retry may work. RabbitMQ then tries the message again. */
public class PushDeliveryException extends RuntimeException {

    public PushDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
