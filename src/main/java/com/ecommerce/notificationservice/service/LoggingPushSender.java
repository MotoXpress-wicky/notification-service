package com.ecommerce.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Used when push is turned off (notification.push.enabled=false, the default). It sends nothing
 * and only writes a log line, so the rest of the system works without a Firebase account.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notification.push.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingPushSender implements PushSender {

    @Override
    public Result send(String deviceToken, String title, String body) {
        log.info("Push is turned off. Would send '{}' to one device", title);
        return Result.SENT;
    }
}
