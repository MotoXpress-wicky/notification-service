package com.ecommerce.notificationservice.listener;

import com.ecommerce.notificationservice.config.filter.RequestIdFilter;
import com.ecommerce.notificationservice.service.PushNotificationService;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Reads the queue notification.push. Every message becomes a push message to the user's browsers. */
@Component
public class PushEventListener {

    private final PushNotificationService service;

    public PushEventListener(PushNotificationService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${notification.push.queue}")
    public void onEvent(Map<String, Object> body,
                        @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String eventName,
                        @Header(name = "messageId", required = false) String messageId,
                        @Header(name = "traceId", required = false) String traceId) {

        if (traceId != null) {
            MDC.put(RequestIdFilter.MDC_KEY, traceId);
        }
        try {
            service.handle(IncomingEvent.from(eventName, messageId, body));
        } finally {
            MDC.remove(RequestIdFilter.MDC_KEY);
        }
    }
}
