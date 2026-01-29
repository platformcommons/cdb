package com.platformcommons.cdb.platform.provider.registry.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@Builder
public class ProviderRequestDto {
    
    @Data
    @Builder
    public static class Response {
        private Long id;
        private Long userId;
        private String userEmail;
        private Long providerId;
        private String providerCode;
        private String providerName;
        private String requestMessage;
        private String status;
        private Long approvedBy;
        private String approvalNotes;
        private String requestedRole;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    public static class Create {
        private Long userId;
        private String userEmail;
        private Long providerId;
        private String requestMessage;
        private String requestedRole;
    }

    @Data
    @Builder
    public static class UserIdRequest {
        private Long userId;
    }

    @Data
    @Builder
    public static class ApprovalData {
        private String approvalNotes;
        private String assignedRole;
    }

    @Data
    @Builder
    public static class ApproveRequest {
        private Long approverId;
        private ApprovalData approvalData;
    }

    @Data
    @Builder
    public static class RejectRequest {
        private Long approverId;
        private String notes;
    }
}