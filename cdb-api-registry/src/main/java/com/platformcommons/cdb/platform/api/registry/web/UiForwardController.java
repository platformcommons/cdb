package com.platformcommons.cdb.platform.api.registry.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to forward React Router client-side routes to the SPA entry (index.html).
 *
 * This mirrors the approach used in the Provider Registry module.
 */
@Controller
public class UiForwardController {

    // Frontend routes handled by React Router should be forwarded to index.html
    @GetMapping({"/", "/search", "/api", "/api/**"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
