package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties;
import com.ecommerce.notificationservice.config.NotificationProperties.EmailRule;
import com.ecommerce.notificationservice.config.NotificationProperties.EventRule;
import com.ecommerce.notificationservice.config.NotificationProperties.PushRule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TemplateMapTest {

    @Autowired
    TemplateMap map;

    @Test
    void readsTheRealMapFromNotificationEventsYaml() {
        assertThat(map.emailFor("order.shipped")).get().extracting(EmailRule::template).isEqualTo("order-shipped");
        assertThat(map.pushFor("order.shipped")).get().extracting(PushRule::title).isEqualTo("Your order is on its way");
    }

    @Test
    void accountEventsHaveEmailButNoPush() {
        assertThat(map.emailFor("user.registered")).isPresent();
        assertThat(map.pushFor("user.registered")).isEmpty();
    }

    @Test
    void anEventNobodyConfiguredHasNothing() {
        assertThat(map.emailFor("something.else")).isEmpty();
        assertThat(map.pushFor("something.else")).isEmpty();
    }

    @Test
    void queueBindingsFollowTheMap() {
        assertThat(map.eventsWithEmail()).contains("user.registered", "user.password.reset", "order.placed");
        assertThat(map.eventsWithPush()).containsExactly(
                "order.placed", "order.shipped", "order.delivered", "order.returned", "order.cancelled");
    }

    @Test
    void refusesAnEventListedTwice() {
        var rule = new EventRule("a.b", new EmailRule("t", "s"), null);
        var properties = new NotificationProperties("x",
                new NotificationProperties.Email("q", "a@b.c", "Shop"),
                new NotificationProperties.Push("q", false, null),
                List.of(rule, rule));

        assertThatThrownBy(() -> new TemplateMap(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("a.b");
    }
}
