package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties;
import com.ecommerce.notificationservice.config.NotificationProperties.EmailRule;
import com.ecommerce.notificationservice.config.NotificationProperties.EventRule;
import com.ecommerce.notificationservice.config.NotificationProperties.PushRule;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Looks up "event name -> email rule / push rule". The names come only from configuration,
 * so this class stays free of any shop words.
 */
@Component
public class TemplateMap {

    private final Map<String, EventRule> rules = new LinkedHashMap<>();

    public TemplateMap(NotificationProperties properties) {
        for (EventRule rule : properties.events()) {
            if (rules.put(rule.name(), rule) != null) {
                throw new IllegalStateException("Event '" + rule.name() + "' is listed twice in notification-events.yaml");
            }
        }
    }

    public Optional<EmailRule> emailFor(String eventName) {
        return Optional.ofNullable(rules.get(eventName)).map(EventRule::email);
    }

    public Optional<PushRule> pushFor(String eventName) {
        return Optional.ofNullable(rules.get(eventName)).map(EventRule::push);
    }

    /** Event names that have an email. The email queue is bound to exactly these. */
    public List<String> eventsWithEmail() {
        return rules.values().stream().filter(rule -> rule.email() != null).map(EventRule::name).toList();
    }

    /** Event names that have a push message. The push queue is bound to exactly these. */
    public List<String> eventsWithPush() {
        return rules.values().stream().filter(rule -> rule.push() != null).map(EventRule::name).toList();
    }
}
