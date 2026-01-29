package com.platformcommons.cdb.platform.api.gateway.route;

import com.platformcommons.cdb.platform.api.gateway.service.RoutingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.CacheManager;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RoutingService routingService;


    public RouteController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/resolve/{providerCode}/{envType}")
    public ResponseEntity<String> resolveProviderBaseUrl(
            @PathVariable String providerCode,
            @PathVariable String envType,
            @RequestParam(required = false, defaultValue = "false") boolean bypassCache) {

        return ResponseEntity.ok(routingService.resolveRoute(providerCode,envType,bypassCache));
    }

}
