package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.entity.DeliveryStatus;
import com.ecommerce.notificationservice.repository.SentNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DeliveryLog.class)
class DeliveryLogTest {

    @Autowired
    DeliveryLog log;

    @Autowired
    SentNotificationRepository repository;

    UUID id = UUID.randomUUID();

    @Test
    void aFailedTryDoesNotBlockTheRetry() {
        log.recordFailed(id, Channel.EMAIL, "order.placed", "a@b.c", "order-placed", "SMTP down");

        assertThat(log.alreadySent(id, Channel.EMAIL)).isFalse();
    }

    @Test
    void theRetryTurnsTheSameRowIntoSent() {
        log.recordFailed(id, Channel.EMAIL, "order.placed", "a@b.c", "order-placed", "SMTP down");
        log.recordSent(id, Channel.EMAIL, "order.placed", "a@b.c", "order-placed");

        assertThat(log.alreadySent(id, Channel.EMAIL)).isTrue();
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByMessageIdAndChannel(id, Channel.EMAIL)).get()
                .satisfies(row -> {
                    assertThat(row.getStatus()).isEqualTo(DeliveryStatus.SENT);
                    assertThat(row.getError()).isNull();
                });
    }

    @Test
    void emailAndPushAreTrackedSeparately() {
        log.recordSent(id, Channel.EMAIL, "order.placed", "a@b.c", "order-placed");

        assertThat(log.alreadySent(id, Channel.EMAIL)).isTrue();
        assertThat(log.alreadySent(id, Channel.PUSH)).isFalse();
    }

    @Test
    void aLongErrorIsCutToFitTheColumn() {
        log.recordFailed(id, Channel.PUSH, "order.placed", "42", "-", "x".repeat(2000));

        assertThat(repository.findByMessageIdAndChannel(id, Channel.PUSH)).get()
                .satisfies(row -> assertThat(row.getError()).hasSize(500));
    }
}
