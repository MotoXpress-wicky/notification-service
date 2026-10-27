package com.ecommerce.notificationservice.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceholdersTest {

    @Test
    void fillsEveryBlank() {
        String text = Placeholders.fill("Order {orderNo} by {courier}", Map.of("orderNo", "MX-1", "courier", "Courier A"));

        assertThat(text).isEqualTo("Order MX-1 by Courier A");
    }

    @Test
    void aBlankWithoutAValueStaysVisible() {
        assertThat(Placeholders.fill("Hello {name}", Map.of())).isEqualTo("Hello {name}");
    }

    @Test
    void valuesAreNotReadAsPatterns() {
        String text = Placeholders.fill("Total {total}", Map.of("total", "$1,000 \\ {other}"));

        assertThat(text).isEqualTo("Total $1,000 \\ {other}");
    }

    @Test
    void numbersAreWrittenAsText() {
        assertThat(Placeholders.fill("{count} parts", Map.of("count", 3))).isEqualTo("3 parts");
    }
}
