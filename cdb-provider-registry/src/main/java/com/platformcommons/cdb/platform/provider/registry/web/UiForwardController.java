package com.platformcommons.cdb.platform.provider.registry.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to forward React Router client-side routes to the SPA entry (index.html).
 *
 * Author: Aashish Aadarsh (aashish@platformcommons.com)
 * Version: 1.0.0
 * Since: 2025-09-15
 */
@Controller
public class UiForwardController {

    // Frontend routes handled by React Router should be forwarded to index.html
    @GetMapping({"/", "/login", "/signup", "/no-provider", "/app", "/app/**", "/apis", "/apis/**"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
