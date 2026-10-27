package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties.EmailRule;
import com.ecommerce.notificationservice.entity.Channel;
import com.ecommerce.notificationservice.listener.IncomingEvent;
import com.ecommerce.notificationservice.listener.InvalidEventException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * One event -> one email. Steps: already sent? -> find the rule -> fill the template -> send -> write the log.
 * The event body is never written to the log: it can hold a secret link (password reset).
 */
@Slf4j
@Service
public class EmailNotificationService {

    /** A loose check only. The mail server decides if an address really works. */
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+$");

    private final TemplateMap templateMap;
    private final TemplateRenderer renderer;
    private final EmailSender emailSender;
    private final DeliveryLog deliveryLog;

    public EmailNotificationService(TemplateMap templateMap, TemplateRenderer renderer,
                                    EmailSender emailSender, DeliveryLog deliveryLog) {
        this.templateMap = templateMap;
        this.renderer = renderer;
        this.emailSender = emailSender;
        this.deliveryLog = deliveryLog;
    }

    public void handle(IncomingEvent event) {
        if (deliveryLog.alreadySent(event.messageId(), Channel.EMAIL)) {
            log.info("Skipped '{}' (message {}): the email was already sent", event.name(), event.messageId());
            return;
        }

        Optional<EmailRule> rule = templateMap.emailFor(event.name());
        if (rule.isEmpty()) {
            log.warn("No email rule for event '{}'. Ignored.", event.name());
            return;
        }

        String to = event.text("email");
        if (to == null || !EMAIL.matcher(to).matches()) {
            throw new InvalidEventException("Event '" + event.name() + "' has no valid email address");
        }

        String template = rule.get().template();
        try {
            Map<String, Object> model = renderer.modelFor(event.body());
            String subject = Placeholders.fill(rule.get().subject(), model);
            String html = renderer.renderEmail(template, model);

            emailSender.send(to, subject, html);
        } catch (RuntimeException e) {
            deliveryLog.recordFailed(event.messageId(), Channel.EMAIL, event.name(), to, template, e.getMessage());
            throw e;
        }

        deliveryLog.recordSent(event.messageId(), Channel.EMAIL, event.name(), to, template);
    }
}
