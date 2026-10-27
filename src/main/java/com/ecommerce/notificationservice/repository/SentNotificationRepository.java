package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.entity.SentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SentNotificationRepository extends JpaRepository<SentNotification, Long> {

    boolean existsByMessageIdAndChannel(UUID messageId, Channel channel);
}
