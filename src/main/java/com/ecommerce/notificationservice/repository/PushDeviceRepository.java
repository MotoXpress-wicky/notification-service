package com.ecommerce.notificationservice.repository;

import com.ecommerce.notificationservice.entity.PushDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {

    Optional<PushDevice> findByToken(String token);

    List<PushDevice> findByUserId(Long userId);

    /** Returns how many rows were deleted (0 or 1). Only the owner can remove a device. */
    long deleteByTokenAndUserId(String token, Long userId);

    long deleteByToken(String token);
}
