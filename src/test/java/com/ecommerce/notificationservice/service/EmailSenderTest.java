package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailSenderTest {

    JavaMailSender mailSender = mock(JavaMailSender.class);
    EmailSender sender;

    @BeforeEach
    void setUp() {
        var properties = new NotificationProperties("app.events",
                new NotificationProperties.Email("notification.email", "no-reply@test.local", "TestShop"),
                new NotificationProperties.Push("notification.push", false, null),
                List.of());
        when(mailSender.createMimeMessage()).thenReturn(new JavaMailSenderImpl().createMimeMessage());
        sender = new EmailSender(mailSender, properties);
    }

    @Test
    void buildsTheMessageAndSendsIt() throws Exception {
        sender.send("nimal@example.com", "Hello", "<p>Hi</p>");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage message = captor.getValue();

        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("nimal@example.com");
        assertThat(message.getSubject()).isEqualTo("Hello");
        assertThat(message.getFrom()[0].toString()).contains("TestShop").contains("no-reply@test.local");
    }

    @Test
    void aMailServerProblemBecomesAnEmailDeliveryException() {
        doThrow(new MailSendException("down")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send("nimal@example.com", "Hello", "<p>Hi</p>"))
                .isInstanceOf(EmailDeliveryException.class);
    }
}
