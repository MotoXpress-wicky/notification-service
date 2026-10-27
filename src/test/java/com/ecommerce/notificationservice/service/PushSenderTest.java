package com.ecommerce.notificationservice.service;

import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PushSenderTest {

    @Autowired
    PushSender sender;

    @Test
    void pushIsOffByDefaultSoTheLoggingSenderIsUsed() {
        assertThat(sender).isInstanceOf(LoggingPushSender.class);
        assertThat(sender.send("token", "Title", "Body")).isEqualTo(PushSender.Result.SENT);
    }

    @Test
    void onlyDeadTokenCodesDeleteTheDevice() {
        assertThat(FcmPushSender.isDeadToken(MessagingErrorCode.UNREGISTERED)).isTrue();
        assertThat(FcmPushSender.isDeadToken(MessagingErrorCode.INVALID_ARGUMENT)).isTrue();
        assertThat(FcmPushSender.isDeadToken(MessagingErrorCode.SENDER_ID_MISMATCH)).isTrue();

        assertThat(FcmPushSender.isDeadToken(MessagingErrorCode.UNAVAILABLE)).isFalse();
        assertThat(FcmPushSender.isDeadToken(MessagingErrorCode.INTERNAL)).isFalse();
        assertThat(FcmPushSender.isDeadToken(MessagingErrorCode.QUOTA_EXCEEDED)).isFalse();
        assertThat(FcmPushSender.isDeadToken(null)).isFalse();
    }
}
