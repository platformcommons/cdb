package com.platformcommons.cdb.platform.provider.registry.controller;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderRequestDto;
import com.platformcommons.cdb.platform.provider.registry.service.ProviderRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/provider-requests")
@RequiredArgsConstructor
public class ProviderRequestController {

    private final ProviderRequestService requestService;

    @PostMapping
    public ResponseEntity<ProviderRequestDto.Response> createRequest(
            @RequestBody ProviderRequestDto.Create dto) {
        ProviderRequestDto.Response result = requestService.createRequest(dto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/my-requests")
    public ResponseEntity<List<ProviderRequestDto.Response>> getMyRequests(
            @RequestBody ProviderRequestDto.UserIdRequest dto) {
        List<ProviderRequestDto.Response> requests = requestService.getUserRequests(dto.getUserId());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ProviderRequestDto.Response>> getProviderRequests(
            @PathVariable Long providerId) {
        List<ProviderRequestDto.Response> requests = requestService.getRequestsForProvider(providerId);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ProviderRequestDto.Response> approveRequest(
            @PathVariable Long requestId,
            @RequestBody ProviderRequestDto.ApproveRequest payload) {
        ProviderRequestDto.Response result = requestService.approveRequest(requestId, payload.getApproverId(), payload.getApprovalData());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<Void> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody ProviderRequestDto.RejectRequest payload) {
        requestService.rejectRequest(requestId, payload.getApproverId(), payload.getNotes());
        return ResponseEntity.ok().build();
    }
}