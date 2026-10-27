package com.ecommerce.notificationservice.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Fills {name} blanks in a text from a map. A blank with no value stays as it is, so a config mistake is visible. */
public final class Placeholders {

    private static final Pattern BLANK = Pattern.compile("\\{([A-Za-z0-9_]+)}");

    private Placeholders() {
    }

    public static String fill(String text, Map<String, Object> values) {
        Matcher matcher = BLANK.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            Object value = values.get(matcher.group(1));
            String replacement = value == null ? matcher.group() : String.valueOf(value);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
