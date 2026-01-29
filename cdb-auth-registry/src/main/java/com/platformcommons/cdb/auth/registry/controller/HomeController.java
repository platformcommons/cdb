package com.platformcommons.cdb.auth.registry.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String root() {
        return "oauth2/login";
    }

    @GetMapping({"/login", "/login/"})
    public String login() {
        return "oauth2/login";
    }

    @GetMapping({"/signup", "/signup/"})
    public String signup(@RequestParam(required = false) String client_id,
                        @RequestParam(required = false) String redirect_uri,
                        @RequestParam(required = false) String state) {
        
        // If OAuth2 parameters are present, redirect to OAuth2 signup
        if (client_id != null || redirect_uri != null) {
            StringBuilder redirectUrl = new StringBuilder("redirect:/oauth2/signup");
            boolean hasParams = false;
            
            if (client_id != null) {
                redirectUrl.append(hasParams ? "&" : "?").append("client_id=").append(client_id);
                hasParams = true;
            }
            if (redirect_uri != null) {
                redirectUrl.append(hasParams ? "&" : "?").append("redirect_uri=").append(redirect_uri);
                hasParams = true;
            }
            if (state != null) {
                redirectUrl.append(hasParams ? "&" : "?").append("state=").append(state);
            }
            
            return redirectUrl.toString();
        }
        
        return "oauth2/signup";
    }
}