package com.ecommerce.notificationservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One browser (or phone) of one user that allowed push messages. */
@Entity
@Table(name = "push_devices")
@Getter
@Setter
@NoArgsConstructor
public class PushDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Copied from user-service. There is no foreign key between databases. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** The Firebase device token. */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    public PushDevice(Long userId, String token) {
        this.userId = userId;
        this.token = token;
        this.createdAt = Instant.now();
    }
}
