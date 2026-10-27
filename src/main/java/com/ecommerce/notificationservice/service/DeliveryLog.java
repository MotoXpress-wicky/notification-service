package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.entity.DeliveryStatus;
import com.ecommerce.notificationservice.entity.SentNotification;
import com.ecommerce.notificationservice.repository.SentNotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * The duplicate guard and the log of what was sent (table sent_notifications).
 * RabbitMQ can deliver the same message twice. A message that is already SENT on a channel
 * is skipped, so the customer never gets the same email twice.
 *
 * Email and push have separate rows (unique message id + channel): if one channel fails and
 * is retried, the other channel is not sent again.
 */
@Slf4j
@Service
public class DeliveryLog {

    private static final int MAX_ERROR_LENGTH = 500;

    private final SentNotificationRepository repository;

    public DeliveryLog(SentNotificationRepository repository) {
        this.repository = repository;
    }

    public boolean alreadySent(UUID messageId, Channel channel) {
        return repository.existsByMessageIdAndChannelAndStatus(messageId, channel, DeliveryStatus.SENT);
    }

    public void recordSent(UUID messageId, Channel channel, String eventName, String recipient, String template) {
        save(messageId, channel, eventName, recipient, template, DeliveryStatus.SENT, null);
    }

    public void recordFailed(UUID messageId, Channel channel, String eventName, String recipient,
                             String template, String error) {
        save(messageId, channel, eventName, recipient, template, DeliveryStatus.FAILED, error);
    }

    private void save(UUID messageId, Channel channel, String eventName, String recipient,
                      String template, DeliveryStatus status, String error) {
        String shortError = error == null ? null
                : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
        try {
            SentNotification row = repository.findByMessageIdAndChannel(messageId, channel)
                    .orElseGet(() -> new SentNotification(messageId, channel, eventName, recipient, template, status, shortError));
            row.setStatus(status);
            row.setError(shortError);
            repository.saveAndFlush(row);
        } catch (DataIntegrityViolationException e) {
            // Another consumer wrote the same row at the same moment. Nothing more to do.
            log.warn("Delivery log row for message {} on {} already exists", messageId, channel);
        }
    }
}
