package com.ecommerce.notificationservice.listener;

import java.util.Map;
import java.util.UUID;

/**
 * One event from RabbitMQ. The event name is the routing key. The body is read as a plain map,
 * because this service does not know what is inside (it only fills templates).
 */
public record IncomingEvent(UUID messageId, String name, Map<String, Object> body) {

    public static IncomingEvent from(String routingKey, String messageIdHeader, Map<String, Object> body) {
        if (routingKey == null || routingKey.isBlank()) {
            throw new InvalidEventException("Event has no routing key");
        }
        if (body == null) {
            throw new InvalidEventException("Event '" + routingKey + "' has no body");
        }
        try {
            return new IncomingEvent(UUID.fromString(String.valueOf(messageIdHeader)), routingKey, body);
        } catch (IllegalArgumentException e) {
            throw new InvalidEventException("Event '" + routingKey + "' has no valid messageId header");
        }
    }

    /** A text field of the body, or null when it is missing. */
    public String text(String field) {
        Object value = body.get(field);
        return value == null ? null : String.valueOf(value);
    }
}
