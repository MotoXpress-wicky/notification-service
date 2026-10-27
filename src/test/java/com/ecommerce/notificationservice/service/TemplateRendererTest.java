package com.ecommerce.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Renders every email template with a body like the one in docs/mvp/07-events.md. */
@SpringBootTest
class TemplateRendererTest {

    @Autowired
    TemplateRenderer renderer;

    @Autowired
    TemplateMap map;

    private Map<String, Object> orderBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", "42");
        body.put("email", "nimal@example.com");
        body.put("name", "Nimal Perera");
        body.put("orderNo", "MX-10238");
        body.put("reference", "MX-10238");
        body.put("paymentMethod", "COD");
        body.put("itemsTotal", "9000.00");
        body.put("deliveryFee", "500.00");
        body.put("total", "9500.00");
        body.put("currency", "LKR");
        body.put("courier", "Courier A");
        body.put("trackingNumber", "WB55310");
        body.put("cancelledBy", "CUSTOMER");
        body.put("lines", List.of(Map.of("partNumber", "123HG", "name", "Sprocket",
                "quantity", 2, "unitPrice", "4500.00", "lineTotal", "9000.00")));
        return body;
    }

    @Test
    void everyConfiguredEmailTemplateRenders() {
        Map<String, Object> accountBody = Map.of(
                "name", "Nimal Perera", "email", "nimal@example.com",
                "resetLink", "http://localhost/reset-password?token=abc", "expiresInMinutes", 30);

        for (String event : map.eventsWithEmail()) {
            String template = map.emailFor(event).orElseThrow().template();
            Map<String, Object> body = event.startsWith("order.") ? orderBody() : accountBody;

            String html = renderer.renderEmail(template, renderer.modelFor(body));

            assertThat(html).as("template for " + event).contains("Nimal Perera").contains("TestShop");
            assertThat(html).as("template for " + event).doesNotContain("th:text");
        }
    }

    @Test
    void orderPlacedShowsLinesAndTotals() {
        String html = renderer.renderEmail("order-placed", renderer.modelFor(orderBody()));

        assertThat(html).contains("MX-10238", "Sprocket", "123HG", "9500.00 LKR", "pay in cash");
    }

    @Test
    void shippedEmailShowsTheTrackingNumber() {
        String html = renderer.renderEmail("order-shipped", renderer.modelFor(orderBody()));

        assertThat(html).contains("Courier A", "WB55310");
    }

    @Test
    void passwordResetShowsTheLinkAndExpiry() {
        Map<String, Object> body = Map.of("name", "Nimal",
                "resetLink", "http://localhost/reset-password?token=abc", "expiresInMinutes", 30);

        String html = renderer.renderEmail("password-reset", renderer.modelFor(body));

        assertThat(html).contains("http://localhost/reset-password?token=abc").contains("30");
    }

    @Test
    void valuesFromTheEventAreHtmlEscaped() {
        Map<String, Object> body = Map.of("name", "<script>alert(1)</script>");

        String html = renderer.renderEmail("welcome", renderer.modelFor(body));

        assertThat(html).doesNotContain("<script>alert(1)</script>").contains("&lt;script&gt;");
    }

    @Test
    void aMissingNameFallsBackToThere() {
        String html = renderer.renderEmail("welcome", renderer.modelFor(Map.of()));

        assertThat(html).contains("<p>Hi <span>there</span>,</p>");
    }
}
