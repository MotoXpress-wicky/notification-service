package com.ecommerce.notificationservice.service;

/** "Send this title and text to this device token." The only thing the rest of the service knows about push. */
public interface PushSender {

    enum Result {
        SENT,
        /** The provider says this token will never work again. The device row must be deleted. */
        INVALID_TOKEN
    }

    /** Throws PushDeliveryException for a problem that may go away (provider down, quota). */
    Result send(String deviceToken, String title, String body);
}
