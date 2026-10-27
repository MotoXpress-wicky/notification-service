package com.ecommerce.notificationservice;

import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.entity.DeliveryStatus;
import com.ecommerce.notificationservice.entity.PushDevice;
import com.ecommerce.notificationservice.entity.SentNotification;
import com.ecommerce.notificationservice.repository.PushDeviceRepository;
import com.ecommerce.notificationservice.repository.SentNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real Flyway file V1__create_tables.sql (on H2 in PostgreSQL mode) and lets Hibernate
 * check that the entities match the tables. A typo in the SQL or in an entity fails here.
 */
@DataJpaTest
class RepositoryTest {

    @Autowired
    PushDeviceRepository devices;

    @Autowired
    SentNotificationRepository sent;

    @Test
    void findsDevicesOfAUser() {
        devices.saveAndFlush(new PushDevice(7L, "token-a"));
        devices.saveAndFlush(new PushDevice(7L, "token-b"));
        devices.saveAndFlush(new PushDevice(8L, "token-c"));

        assertThat(devices.findByUserId(7L)).extracting(PushDevice::getToken)
                .containsExactlyInAnyOrder("token-a", "token-b");
    }

    @Test
    void sameTokenCannotBeSavedTwice() {
        devices.saveAndFlush(new PushDevice(7L, "token-a"));

        assertThatThrownBy(() -> devices.saveAndFlush(new PushDevice(8L, "token-a")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyTheOwnerCanDeleteADevice() {
        devices.saveAndFlush(new PushDevice(7L, "token-a"));

        assertThat(devices.deleteByTokenAndUserId("token-a", 99L)).isZero();
        assertThat(devices.deleteByTokenAndUserId("token-a", 7L)).isEqualTo(1);
    }

    @Test
    void remembersWhichEventWasSentOnWhichChannel() {
        UUID id = UUID.randomUUID();
        sent.saveAndFlush(new SentNotification(id, Channel.EMAIL, "order.shipped", "a@b.c",
                "order-shipped", DeliveryStatus.SENT, null));

        assertThat(sent.existsByMessageIdAndChannel(id, Channel.EMAIL)).isTrue();
        assertThat(sent.existsByMessageIdAndChannel(id, Channel.PUSH)).isFalse();
    }
}
