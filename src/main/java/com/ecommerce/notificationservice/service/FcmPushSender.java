package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * The only class that knows Firebase Cloud Messaging.
 * Active only when notification.push.enabled is true. It needs the service-account key file
 * (a secret, never in git): notification.push.firebase-credentials-file.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "notification.push.enabled", havingValue = "true")
public class FcmPushSender implements PushSender {

    private static final String APP_NAME = "notification-service";

    /** Firebase answers with these codes when the token itself is dead or wrong. */
    private static final Set<MessagingErrorCode> DEAD_TOKEN_CODES = Set.of(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
            MessagingErrorCode.SENDER_ID_MISMATCH);

    private final FirebaseMessaging messaging;

    public FcmPushSender(NotificationProperties properties) {
        Path keyFile = Path.of(properties.push().firebaseCredentialsFile());
        if (!Files.isRegularFile(keyFile)) {
            throw new IllegalStateException("Push is enabled but the Firebase key file was not found: " + keyFile);
        }

        try (InputStream key = Files.newInputStream(keyFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(key))
                    .build();

            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(existing -> existing.getName().equals(APP_NAME))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));

            this.messaging = FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            throw new IllegalStateException("The Firebase key file could not be read: " + keyFile, e);
        }
    }

    @Override
    public Result send(String deviceToken, String title, String body) {
        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build();

        try {
            messaging.send(message);
            return Result.SENT;
        } catch (FirebaseMessagingException e) {
            if (isDeadToken(e.getMessagingErrorCode())) {
                return Result.INVALID_TOKEN;
            }
            throw new PushDeliveryException("Firebase could not send the message: " + e.getMessagingErrorCode(), e);
        }
    }

    static boolean isDeadToken(MessagingErrorCode code) {
        return code != null && DEAD_TOKEN_CODES.contains(code);
    }
}
