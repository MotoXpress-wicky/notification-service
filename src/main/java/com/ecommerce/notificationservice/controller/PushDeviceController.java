package com.ecommerce.notificationservice.controller;

import com.ecommerce.notificationservice.dto.CurrentUser;
import com.ecommerce.notificationservice.dto.PushDeviceRequest;
import com.ecommerce.notificationservice.service.PushDeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The browser tells us which Firebase token to use for push, and removes it on logout. */
@RestController
@RequestMapping("/api/v1/notification/push-devices")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    public PushDeviceController(PushDeviceService pushDeviceService) {
        this.pushDeviceService = pushDeviceService;
    }

    @PostMapping
    public ResponseEntity<Void> register(@AuthenticationPrincipal CurrentUser user,
                                         @Valid @RequestBody PushDeviceRequest request) {
        pushDeviceService.register(user.id(), request.token());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal CurrentUser user,
                                       @PathVariable String token) {
        pushDeviceService.remove(user.id(), token);
        return ResponseEntity.noContent().build();
    }
}
