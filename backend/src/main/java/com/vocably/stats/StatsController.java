package com.vocably.stats;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService service;

    public StatsController(StatsService service) {
        this.service = service;
    }

    /** Estatísticas dos últimos {@code days} dias (incluindo hoje). */
    @GetMapping
    public StatsResponse stats(@AuthenticationPrincipal Jwt jwt,
                               @RequestParam(defaultValue = "30") @Min(7) @Max(365) int days) {
        return service.stats(Long.valueOf(jwt.getSubject()), days);
    }
}
