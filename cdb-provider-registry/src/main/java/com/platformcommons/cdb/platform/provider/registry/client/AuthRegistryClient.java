package com.platformcommons.cdb.platform.provider.registry.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthRegistryClient {
    //todo.. update and fetch from feign client
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${cdb.auth-registry.base-url:http://localhost:8083}")
    private String authBaseUrl;

    public record UserRegistrationRequest(String username, String password, String firstName, String lastName,
                                          String otpKey, String otp,
                                          Long providerId, String providerCode) {}
    public record UserResponse(Long id, String username, String email, boolean enabled) {}

    public record OtpInitiateRequest(String email) {}
    public record OtpInitiateResponse(String key) {}

    public boolean userExists(String email) {
        String url = authBaseUrl + "/api/v1/users/exists?username=" + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
        Boolean exists = restTemplate.getForObject(url, Boolean.class);
        return exists != null && exists;
    }

    public String initiateOtp(String email) {
        String url = authBaseUrl + "/api/v1/otp/initiate";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OtpInitiateRequest> entity = new HttpEntity<>(new OtpInitiateRequest(email), headers);
        OtpInitiateResponse resp = restTemplate.postForObject(url, entity, OtpInitiateResponse.class);
        return resp == null ? null : resp.key();
    }
    
    public UserResponse registerUser(UserRegistrationRequest req) {
        String url = authBaseUrl + "/api/v1/users/register";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserRegistrationRequest> entity = new HttpEntity<>(req, headers);
        return restTemplate.postForObject(url, entity, UserResponse.class);
    }

    public record UserProviderMappingRequest(Long userId, Long providerId, String providerCode, String role) {}
    
    public void mapUserToProvider(Long userId, Long providerId, String providerCode, String role,String token) {
        String url = authBaseUrl + "/api/v1/user-provider-mappings";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        UserProviderMappingRequest req = new UserProviderMappingRequest(userId, providerId, providerCode, role);
        HttpEntity<UserProviderMappingRequest> entity = new HttpEntity<>(req, headers);
        restTemplate.postForObject(url, entity, Void.class);
    }
}
