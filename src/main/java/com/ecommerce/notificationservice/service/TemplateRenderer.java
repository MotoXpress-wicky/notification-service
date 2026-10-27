package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.config.NotificationProperties;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;

/**
 * Fills an HTML email template with the values of an event body.
 * The template name always comes from configuration, never from the event, so a message
 * on the queue cannot pick which file is read.
 */
@Component
public class TemplateRenderer {

    private final SpringTemplateEngine engine;
    private final String brandName;

    public TemplateRenderer(SpringTemplateEngine engine, NotificationProperties properties) {
        this.engine = engine;
        this.brandName = properties.email().fromName();
    }

    /** Values every text and template may use, on top of the event body. */
    public Map<String, Object> modelFor(Map<String, Object> eventBody) {
        Map<String, Object> model = new HashMap<>(eventBody);
        model.put("brandName", brandName);
        return model;
    }

    public String renderEmail(String template, Map<String, Object> model) {
        Context context = new Context();
        context.setVariables(model);
        return engine.process("emails/" + template, context);
    }
}
