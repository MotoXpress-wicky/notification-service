package com.ecommerce.notificationservice.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Everything under "notification:" in application.yaml and notification-events.yaml.
 * If a required value is missing, the service refuses to start (it does not fail later at 3 a.m.).
 */
@Validated
@ConfigurationProperties(prefix = "notification")
public record NotificationProperties(
        @NotBlank String exchange,
        @NotNull @Valid Email email,
        @NotNull @Valid Push push,
        @NotNull List<@Valid EventRule> events
) {

    public record Email(@NotBlank String queue, @NotBlank String fromAddress, @NotBlank String fromName) {
    }

    public record Push(@NotBlank String queue, boolean enabled, String firebaseCredentialsFile) {
    }

    /** What to do for one event name. A missing email or push part means "do not send that channel". */
    public record EventRule(@NotBlank String name, @Valid EmailRule email, @Valid PushRule push) {
    }

    public record EmailRule(@NotBlank String template, @NotBlank String subject) {
    }

    public record PushRule(@NotBlank String title, @NotBlank String body) {
    }
}
