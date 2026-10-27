package com.ecommerce.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /push-devices: the Firebase token the browser got from Firebase. */
public record PushDeviceRequest(
        @NotBlank
        @Size(max = 512)
        String token
) {
}
