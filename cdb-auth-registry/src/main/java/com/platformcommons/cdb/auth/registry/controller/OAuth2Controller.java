package com.platformcommons.cdb.auth.registry.controller;

import com.platformcommons.cdb.auth.registry.dto.TokenResponse;
import com.platformcommons.cdb.auth.registry.model.OAuth2Client;
import com.platformcommons.cdb.auth.registry.service.OAuth2Service;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/oauth2")
public class OAuth2Controller {

    private final OAuth2Service oauth2Service;

    public OAuth2Controller(OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    @GetMapping("/authorize")
    public String authorize(@RequestParam String client_id,
                          @RequestParam String redirect_uri,
                          @RequestParam String response_type,
                          @RequestParam(required = false) String scope,
                          @RequestParam(required = false) String state,
                          @RequestParam(required = false) String code_challenge,
                          @RequestParam(required = false) String code_challenge_method,
                          HttpSession session,
                          Model model) {

        OAuth2Client client = oauth2Service.validateClient(client_id, redirect_uri);
        
        // Store authorization request in session
        session.setAttribute("oauth2_request", Map.of(
            "client_id", client_id,
            "redirect_uri", redirect_uri,
            "response_type", response_type,
            "scope", scope != null ? scope : "",
            "state", state != null ? state : "",
            "code_challenge", code_challenge != null ? code_challenge : "",
            "code_challenge_method", code_challenge_method != null ? code_challenge_method : ""
        ));

        // Check if user is authenticated
        String email = (String) session.getAttribute("authenticated_user");
        if (email == null) {
            model.addAttribute("client", client);
            model.addAttribute("redirect_uri", redirect_uri);
            return "oauth2/login";
        }

        // Show consent screen if required
        if (client.isRequireConsent()) {
            model.addAttribute("client", client);
            model.addAttribute("scopes", scope != null ? scope.split(" ") : new String[0]);
            return "oauth2/consent";
        }

        // Generate authorization code and redirect
        return processAuthorization(session);
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                       @RequestParam String password,
                       HttpSession session,
                       Model model) {
        
        try {
            boolean authenticated = oauth2Service.authenticate(email, password);
            if (authenticated) {
                session.setAttribute("authenticated_user", email);
                
                // Check if consent is required
                @SuppressWarnings("unchecked")
                Map<String, String> request = (Map<String, String>) session.getAttribute("oauth2_request");
                OAuth2Client client = oauth2Service.validateClient(request.get("client_id"), request.get("redirect_uri"));
                
                if (client.isRequireConsent()) {
                    model.addAttribute("client", client);
                    model.addAttribute("scopes", request.get("scope").split(" "));
                    return "oauth2/consent";
                }
                
                return processAuthorization(session);
            } else {
                model.addAttribute("error", "Invalid credentials");
                return "oauth2/login";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Authentication failed");
            return "oauth2/login";
        }
    }

    @PostMapping("/consent")
    public String consent(@RequestParam(required = false) String approve,
                         HttpSession session) {
        
        if (!"true".equals(approve)) {
            return "redirect:/oauth2/error?error=access_denied";
        }
        
        return processAuthorization(session);
    }

    @GetMapping("/signup")
    public String signup(@RequestParam(required = false) String client_id,
                        @RequestParam(required = false) String redirect_uri,
                        @RequestParam(required = false) String state,
                        Model model) {
        
        // Pass parameters to the signup page for later use
        if (client_id != null) {
            model.addAttribute("client_id", client_id);
        }
        if (redirect_uri != null) {
            model.addAttribute("redirect_uri", redirect_uri);
        }
        if (state != null) {
            model.addAttribute("state", state);
        }
        
        return "oauth2/signup";
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "oauth2/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, Model model) {
        oauth2Service.sendPasswordResetEmail(email);
        model.addAttribute("message", "Password reset email sent");
        return "oauth2/forgot-password";
    }

    /**
     * OAuth2 token endpoint with PKCE support
     */
    @PostMapping("/token")
    public ResponseEntity<?> token(
            @RequestParam String grant_type,
            @RequestParam(required = false) String code,
            @RequestParam String client_id,
            @RequestParam(required = false) String client_secret,
            @RequestParam(required = false) String redirect_uri,
            @RequestParam(required = false) String code_verifier) {

        if ("authorization_code".equals(grant_type)) {
            try {
                TokenResponse accessToken = oauth2Service.exchangeCodeForToken(code, client_id, code_verifier);
                return ResponseEntity.ok(accessToken);
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "invalid_grant");
                error.put("error_description", e.getMessage());
                return ResponseEntity.badRequest().body(error);
            }
        }
        Map<String, Object> error = new HashMap<>();
        error.put("error", "unsupported_grant_type");
        return ResponseEntity.badRequest().body(error);
    }
    private String processAuthorization(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<String, String> request = (Map<String, String>) session.getAttribute("oauth2_request");
        String username = (String) session.getAttribute("authenticated_user");
        
        String code = oauth2Service.generateAuthorizationCode(
            request.get("client_id"),
            username,
            request.get("redirect_uri"),
            request.get("scope"),
            request.get("code_challenge"),
            request.get("code_challenge_method")
        );
        
        String redirectUrl = request.get("redirect_uri") + "?code=" + code;
        if (!request.get("state").isEmpty()) {
            redirectUrl += "&state=" + request.get("state");
        }
        
        session.removeAttribute("oauth2_request");
        session.removeAttribute("authenticated_user");
        
        return "redirect:" + redirectUrl;
    }
}