package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties.PushRule;
import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.entity.PushDevice;
import com.ecommerce.notificationservice.listener.IncomingEvent;
import com.ecommerce.notificationservice.listener.InvalidEventException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PushNotificationServiceTest {

    TemplateMap templateMap = mock(TemplateMap.class);
    TemplateRenderer renderer = mock(TemplateRenderer.class);
    PushSender pushSender = mock(PushSender.class);
    PushDeviceService deviceService = mock(PushDeviceService.class);
    DeliveryLog deliveryLog = mock(DeliveryLog.class);
    PushNotificationService service;

    UUID messageId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(templateMap, renderer, pushSender, deviceService, deliveryLog);
        when(templateMap.pushFor("order.shipped"))
                .thenReturn(Optional.of(new PushRule("Order {orderNo} shipped", "Tracking {trackingNumber}")));
        when(renderer.modelFor(any())).thenAnswer(call -> new HashMap<String, Object>(call.getArgument(0)));
    }

    private IncomingEvent shipped() {
        return new IncomingEvent(messageId, "order.shipped",
                Map.of("userId", "42", "orderNo", "MX-1", "trackingNumber", "WB55310"));
    }

    @Test
    void sendsToEveryDeviceOfTheUser() {
        when(deviceService.devicesOf(42L)).thenReturn(List.of(new PushDevice(42L, "t1"), new PushDevice(42L, "t2")));
        when(pushSender.send(anyString(), anyString(), anyString())).thenReturn(PushSender.Result.SENT);

        service.handle(shipped());

        verify(pushSender).send("t1", "Order MX-1 shipped", "Tracking WB55310");
        verify(pushSender).send("t2", "Order MX-1 shipped", "Tracking WB55310");
        verify(deliveryLog).recordSent(messageId, Channel.PUSH, "order.shipped", "42", null);
    }

    @Test
    void deletesADeviceThatFirebaseNoLongerAccepts() {
        when(deviceService.devicesOf(42L)).thenReturn(List.of(new PushDevice(42L, "dead"), new PushDevice(42L, "good")));
        when(pushSender.send(eq("dead"), anyString(), anyString())).thenReturn(PushSender.Result.INVALID_TOKEN);
        when(pushSender.send(eq("good"), anyString(), anyString())).thenReturn(PushSender.Result.SENT);

        service.handle(shipped());

        verify(deviceService).removeInvalidToken("dead");
        verify(deviceService, never()).removeInvalidToken("good");
        verify(deliveryLog).recordSent(messageId, Channel.PUSH, "order.shipped", "42", null);
    }

    @Test
    void aUserWithoutDevicesNeedsNothing() {
        when(deviceService.devicesOf(42L)).thenReturn(List.of());

        service.handle(shipped());

        verifyNoInteractions(pushSender);
        verify(deliveryLog, never()).recordSent(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void skipsAMessageThatWasAlreadySent() {
        when(deliveryLog.alreadySent(messageId, Channel.PUSH)).thenReturn(true);

        service.handle(shipped());

        verifyNoInteractions(pushSender, deviceService);
    }

    @Test
    void aProviderProblemIsLoggedAsFailedAndThrownSoRabbitMqRetries() {
        when(deviceService.devicesOf(42L)).thenReturn(List.of(new PushDevice(42L, "t1"), new PushDevice(42L, "t2")));
        when(pushSender.send(eq("t1"), anyString(), anyString())).thenThrow(new PushDeliveryException("down", null));
        when(pushSender.send(eq("t2"), anyString(), anyString())).thenReturn(PushSender.Result.SENT);

        assertThatThrownBy(() -> service.handle(shipped())).isInstanceOf(PushDeliveryException.class);

        // the second device was still tried
        verify(pushSender, times(2)).send(anyString(), anyString(), anyString());
        verify(deliveryLog).recordFailed(eq(messageId), eq(Channel.PUSH), eq("order.shipped"), eq("42"), any(), anyString());
        verify(deliveryLog, never()).recordSent(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void anEventWithoutAUserIdGoesToTheDeadLetterQueue() {
        var event = new IncomingEvent(messageId, "order.shipped", Map.of("orderNo", "MX-1"));

        assertThatThrownBy(() -> service.handle(event)).isInstanceOf(InvalidEventException.class);
    }

    @Test
    void ignoresAnEventWithoutAPushRule() {
        when(templateMap.pushFor("user.registered")).thenReturn(Optional.empty());

        service.handle(new IncomingEvent(messageId, "user.registered", Map.of("userId", "42")));

        verifyNoInteractions(pushSender, deviceService);
    }
}
