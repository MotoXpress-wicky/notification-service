package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties.EmailRule;
import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.listener.IncomingEvent;
import com.ecommerce.notificationservice.listener.InvalidEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailNotificationServiceTest {

    TemplateMap templateMap = mock(TemplateMap.class);
    TemplateRenderer renderer = mock(TemplateRenderer.class);
    EmailSender emailSender = mock(EmailSender.class);
    DeliveryLog deliveryLog = mock(DeliveryLog.class);
    EmailNotificationService service;

    UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new EmailNotificationService(templateMap, renderer, emailSender, deliveryLog);
        when(templateMap.emailFor("order.shipped"))
                .thenReturn(Optional.of(new EmailRule("order-shipped", "Order {orderNo} shipped")));
        when(renderer.modelFor(any())).thenAnswer(call -> new HashMap<String, Object>(call.getArgument(0)));
        when(renderer.renderEmail(eq("order-shipped"), any())).thenReturn("<p>html</p>");
    }

    private IncomingEvent event(String name, Map<String, Object> body) {
        return new IncomingEvent(messageId, name, body);
    }

    @Test
    void sendsTheEmailAndWritesTheLog() {
        service.handle(event("order.shipped", Map.of("email", "nimal@example.com", "orderNo", "MX-1")));

        verify(emailSender).send("nimal@example.com", "Order MX-1 shipped", "<p>html</p>");
        verify(deliveryLog).recordSent(messageId, Channel.EMAIL, "order.shipped", "nimal@example.com", "order-shipped");
    }

    @Test
    void skipsAMessageThatWasAlreadySent() {
        when(deliveryLog.alreadySent(messageId, Channel.EMAIL)).thenReturn(true);

        service.handle(event("order.shipped", Map.of("email", "nimal@example.com")));

        verifyNoInteractions(emailSender);
        verify(deliveryLog, never()).recordSent(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void ignoresAnEventWithoutAnEmailRule() {
        when(templateMap.emailFor("something.else")).thenReturn(Optional.empty());

        service.handle(event("something.else", Map.of("email", "nimal@example.com")));

        verifyNoInteractions(emailSender);
    }

    @Test
    void aMissingOrBadAddressGoesToTheDeadLetterQueue() {
        assertThatThrownBy(() -> service.handle(event("order.shipped", Map.of("orderNo", "MX-1"))))
                .isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> service.handle(event("order.shipped", Map.of("email", "not-an-address"))))
                .isInstanceOf(InvalidEventException.class);

        verifyNoInteractions(emailSender);
    }

    @Test
    void aSendProblemIsLoggedAsFailedAndThrownSoRabbitMqRetries() {
        doThrow(new EmailDeliveryException("down", null)).when(emailSender).send(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.handle(event("order.shipped", Map.of("email", "nimal@example.com", "orderNo", "MX-1"))))
                .isInstanceOf(EmailDeliveryException.class);

        verify(deliveryLog).recordFailed(eq(messageId), eq(Channel.EMAIL), eq("order.shipped"),
                eq("nimal@example.com"), eq("order-shipped"), any());
        verify(deliveryLog, never()).recordSent(any(), any(), anyString(), anyString(), anyString());
    }

    @Test
    void incomingEventNeedsAValidMessageId() {
        assertThat(IncomingEvent.from("order.placed", messageId.toString(), Map.of()).messageId()).isEqualTo(messageId);

        assertThatThrownBy(() -> IncomingEvent.from("order.placed", null, Map.of()))
                .isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> IncomingEvent.from("order.placed", "not-a-uuid", Map.of()))
                .isInstanceOf(InvalidEventException.class);
        assertThatThrownBy(() -> IncomingEvent.from("order.placed", messageId.toString(), null))
                .isInstanceOf(InvalidEventException.class);
    }
}
