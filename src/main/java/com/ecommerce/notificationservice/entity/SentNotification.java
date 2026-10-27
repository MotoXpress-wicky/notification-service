package com.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** One line of the log: this event was sent (or failed) on this channel. Also the duplicate check. */
@Entity
@Table(name = "sent_notifications")
@Getter
@Setter
@NoArgsConstructor
public class SentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Channel channel;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    /** The email address, or the user id for push. */
    @Column(nullable = false)
    private String recipient;

    @Column(length = 100)
    private String template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeliveryStatus status;

    @Column(length = 500)
    private String error;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public SentNotification(UUID messageId, Channel channel, String eventName, String recipient,
                            String template, DeliveryStatus status, String error) {
        this.messageId = messageId;
        this.channel = channel;
        this.eventName = eventName;
        this.recipient = recipient;
        this.template = template;
        this.status = status;
        this.error = error;
        this.createdAt = Instant.now();
    }
}
