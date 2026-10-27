package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties.PushRule;
import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.entity.PushDevice;
import com.ecommerce.notificationservice.listener.IncomingEvent;
import com.ecommerce.notificationservice.listener.InvalidEventException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One event -> a push message to every browser of the user in the event.
 * Email and push are independent: a push problem never blocks the email, and the other way round.
 */
@Slf4j
@Service
public class PushNotificationService {

    private final TemplateMap templateMap;
    private final TemplateRenderer renderer;
    private final PushSender pushSender;
    private final PushDeviceService deviceService;
    private final DeliveryLog deliveryLog;

    public PushNotificationService(TemplateMap templateMap, TemplateRenderer renderer, PushSender pushSender,
                                   PushDeviceService deviceService, DeliveryLog deliveryLog) {
        this.templateMap = templateMap;
        this.renderer = renderer;
        this.pushSender = pushSender;
        this.deviceService = deviceService;
        this.deliveryLog = deliveryLog;
    }

    public void handle(IncomingEvent event) {
        if (deliveryLog.alreadySent(event.messageId(), Channel.PUSH)) {
            log.info("Skipped '{}' (message {}): the push was already sent", event.name(), event.messageId());
            return;
        }

        Optional<PushRule> rule = templateMap.pushFor(event.name());
        if (rule.isEmpty()) {
            log.warn("No push rule for event '{}'. Ignored.", event.name());
            return;
        }

        Long userId = readUserId(event);
        List<PushDevice> devices = deviceService.devicesOf(userId);
        if (devices.isEmpty()) {
            log.info("User {} has no push devices. Nothing to send for '{}'", userId, event.name());
            return;
        }

        Map<String, Object> model = renderer.modelFor(event.body());
        String title = Placeholders.fill(rule.get().title(), model);
        String text = Placeholders.fill(rule.get().body(), model);

        int sent = 0;
        RuntimeException firstProblem = null;

        for (PushDevice device : devices) {
            try {
                PushSender.Result result = pushSender.send(device.getToken(), title, text);
                if (result == PushSender.Result.INVALID_TOKEN) {
                    deviceService.removeInvalidToken(device.getToken());
                } else {
                    sent++;
                }
            } catch (RuntimeException e) {
                log.warn("Push to one device of user {} failed: {}", userId, e.getMessage());
                if (firstProblem == null) {
                    firstProblem = e;
                }
            }
        }

        String recipient = String.valueOf(userId);
        if (firstProblem != null) {
            deliveryLog.recordFailed(event.messageId(), Channel.PUSH, event.name(), recipient, null, firstProblem.getMessage());
            // RabbitMQ will try the whole message again. A device that already got it may get it twice.
            throw firstProblem;
        }

        log.info("Sent push for '{}' to {} device(s) of user {}", event.name(), sent, userId);
        deliveryLog.recordSent(event.messageId(), Channel.PUSH, event.name(), recipient, null);
    }

    private Long readUserId(IncomingEvent event) {
        try {
            return Long.valueOf(String.valueOf(event.text("userId")));
        } catch (NumberFormatException e) {
            throw new InvalidEventException("Event '" + event.name() + "' has no valid userId");
        }
    }
}
