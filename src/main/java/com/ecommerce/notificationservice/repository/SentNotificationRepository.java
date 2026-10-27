package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.entity.DeliveryStatus;
import com.ecommerce.notificationservice.entity.SentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SentNotificationRepository extends JpaRepository<SentNotification, Long> {

    boolean existsByMessageIdAndChannel(UUID messageId, Channel channel);

    /** The duplicate check: only a SENT row means "done". A FAILED row may still be retried. */
    boolean existsByMessageIdAndChannelAndStatus(UUID messageId, Channel channel, DeliveryStatus status);

    Optional<SentNotification> findByMessageIdAndChannel(UUID messageId, Channel channel);
}
