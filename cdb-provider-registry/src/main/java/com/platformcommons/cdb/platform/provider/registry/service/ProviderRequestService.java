package com.platformcommons.cdb.platform.provider.registry.service;

import com.platformcommons.cdb.platform.provider.registry.dto.ProviderRequestDto;
import com.platformcommons.cdb.platform.provider.registry.model.Provider;
import com.platformcommons.cdb.platform.provider.registry.model.ProviderRequest;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderRepository;
import com.platformcommons.cdb.platform.provider.registry.repository.ProviderRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProviderRequestService {

    private final ProviderRequestRepository requestRepository;
    private final ProviderRepository providerRepository;

    @Transactional
    public ProviderRequestDto.Response createRequest(ProviderRequestDto.Create dto) {
        Provider provider = providerRepository.findById(dto.getProviderId())
                .orElseThrow(() -> new RuntimeException("Provider not found"));

        if (requestRepository.existsByUserIdAndProviderIdAndStatus(dto.getUserId(), dto.getProviderId(), "PENDING")) {
            throw new RuntimeException("Request already exists");
        }

        ProviderRequest request = ProviderRequest.builder()
                .userId(dto.getUserId())
                .userEmail(dto.getUserEmail())
                .providerId(dto.getProviderId())
                .providerCode(provider.getCode())
                .requestMessage(dto.getRequestMessage())
                .requestedRole(dto.getRequestedRole() != null ? dto.getRequestedRole() : "USER")
                .status("PENDING")
                .build();

        request = requestRepository.save(request);
        return mapToDto(request, provider.getName());
    }

    public List<ProviderRequestDto.Response> getRequestsForProvider(Long providerId) {
        List<ProviderRequest> requests = requestRepository.findByProviderIdAndStatus(providerId, "PENDING");
        return requests.stream()
                .map(req -> {
                    Provider provider = providerRepository.findById(req.getProviderId()).orElse(null);
                    return mapToDto(req, provider != null ? provider.getName() : "Unknown");
                })
                .toList();
    }

    public List<ProviderRequestDto.Response> getUserRequests(Long userId) {
        List<ProviderRequest> requests = requestRepository.findByUserIdAndStatus(userId, "PENDING");
        return requests.stream()
                .map(req -> {
                    Provider provider = providerRepository.findById(req.getProviderId()).orElse(null);
                    return mapToDto(req, provider != null ? provider.getName() : "Unknown");
                })
                .toList();
    }

    @Transactional
    public ProviderRequestDto.Response approveRequest(Long requestId, Long approverId, ProviderRequestDto.ApprovalData dto) {
        ProviderRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request already processed");
        }

        request.setStatus("APPROVED");
        request.setApprovedBy(approverId);
        request.setApprovalNotes(dto.getApprovalNotes());
        if (dto.getAssignedRole() != null) {
            request.setRequestedRole(dto.getAssignedRole());
        }

        request = requestRepository.save(request);
        
        Provider provider = providerRepository.findById(request.getProviderId()).orElse(null);
        return mapToDto(request, provider != null ? provider.getName() : "Unknown");
    }

    @Transactional
    public void rejectRequest(Long requestId, Long approverId, String notes) {
        ProviderRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        request.setStatus("REJECTED");
        request.setApprovedBy(approverId);
        request.setApprovalNotes(notes);
        requestRepository.save(request);
    }

    private ProviderRequestDto.Response mapToDto(ProviderRequest request, String providerName) {
        return ProviderRequestDto.Response.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .userEmail(request.getUserEmail())
                .providerId(request.getProviderId())
                .providerCode(request.getProviderCode())
                .providerName(providerName)
                .requestMessage(request.getRequestMessage())
                .status(request.getStatus())
                .approvedBy(request.getApprovedBy())
                .approvalNotes(request.getApprovalNotes())
                .requestedRole(request.getRequestedRole())
                .createdAt(LocalDateTime.from(request.getCreatedAt()))
                .updatedAt(LocalDateTime.from(request.getUpdatedAt()))
                .build();
    }
}