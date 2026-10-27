package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/** Sends one HTML email through SMTP (Mailpit on a laptop, a real mail server later). */
@Slf4j
@Component
public class EmailSender {

    private final JavaMailSender mailSender;
    private final NotificationProperties.Email settings;

    public EmailSender(JavaMailSender mailSender, NotificationProperties properties) {
        this.mailSender = mailSender;
        this.settings = properties.email();
    }

    /** Throws EmailDeliveryException when the mail server does not accept the message. */
    public void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_NO, StandardCharsets.UTF_8.name());

            helper.setFrom(settings.fromAddress(), settings.fromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Sent email '{}'", subject);

        } catch (MessagingException | UnsupportedEncodingException | MailException e) {
            throw new EmailDeliveryException("Could not send email '" + subject + "'", e);
        }
    }
}
