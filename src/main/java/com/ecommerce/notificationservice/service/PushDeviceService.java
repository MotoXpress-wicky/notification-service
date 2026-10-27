package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.entity.PushDevice;
import com.ecommerce.notificationservice.repository.PushDeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PushDeviceService {

    private final PushDeviceRepository devices;

    public PushDeviceService(PushDeviceRepository devices) {
        this.devices = devices;
    }

    /**
     * Saves the browser's token for this user. Safe to call again with the same token.
     * If another account used this browser before, the token moves to the new account.
     */
    @Transactional
    public void register(Long userId, String token) {
        Optional<PushDevice> existing = devices.findByToken(token);

        if (existing.isPresent()) {
            PushDevice device = existing.get();
            device.setUserId(userId);
            device.setLastUsedAt(Instant.now());
            return;
        }

        devices.save(new PushDevice(userId, token));
        log.info("Saved a push device for user {}", userId);
    }

    /** Removes the token of this user. Does nothing if it is not theirs, so nobody can guess tokens. */
    @Transactional
    public void remove(Long userId, String token) {
        long removed = devices.deleteByTokenAndUserId(token, userId);
        if (removed > 0) {
            log.info("Removed a push device of user {}", userId);
        }
    }

    @Transactional(readOnly = true)
    public List<PushDevice> devicesOf(Long userId) {
        return devices.findByUserId(userId);
    }

    /** Called when Firebase says a token is no longer valid. */
    @Transactional
    public void removeInvalidToken(String token) {
        devices.deleteByToken(token);
        log.info("Removed a push device that Firebase no longer accepts");
    }
}
