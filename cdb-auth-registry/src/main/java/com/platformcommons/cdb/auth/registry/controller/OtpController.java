package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.service.OtpRegistryService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

/**
 * OTP endpoints for registration verification in Auth Registry.
 */
@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {

    private final OtpRegistryService otpService;

    public OtpController(OtpRegistryService otpService) {
        this.otpService = otpService;
    }

    public record OtpInitiateReq(@NotBlank @Email String email) {}
    public record OtpInitiateResp(String key) {}
    public record OtpVerifyReq(@NotBlank String key, @NotBlank @Email String email, @NotBlank String otp) {}
    public record ExistingOtpResp(String key, String otp, long expiresAtEpochMillis) {}

    @PostMapping("/initiate")
    public ResponseEntity<OtpInitiateResp> initiate(@RequestBody OtpInitiateReq req) {
        String key = otpService.initiate(req.email());
        return ResponseEntity.ok(new OtpInitiateResp(key));
    }

    @PostMapping("/verify")
    public ResponseEntity<Boolean> verify(@RequestBody OtpVerifyReq req) {
        boolean ok = otpService.verify(req.key(), req.email(), req.otp());
        return ResponseEntity.ok(ok);
    }

    @GetMapping("/existing")
    public ResponseEntity<ExistingOtpResp> getExisting(@RequestParam @NotBlank @Email String email) {
        Optional<OtpRegistryService.PendingOtp> maybe = otpService.getExistingPendingByEmail(email);
        if (maybe.isEmpty()) return ResponseEntity.notFound().build();
        OtpRegistryService.PendingOtp p = maybe.get();
        long expMs = p.expiresAt() == null ? 0L : p.expiresAt().toEpochMilli();
        return ResponseEntity.ok(new ExistingOtpResp(p.key(), p.otp(), expMs));
    }
}
