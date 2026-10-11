package com.vocably.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService service;

    public ReviewController(ReviewService service) {
        this.service = service;
    }

    /** Cards que vencem hoje (ou estão atrasados), na ordem em que devem ser revisados. */
    @GetMapping("/due")
    public List<CardResponse> due(@AuthenticationPrincipal Jwt jwt,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return service.dueCards(userId(jwt), limit);
    }

    @PostMapping("/{cardId}")
    public ReviewResult review(@AuthenticationPrincipal Jwt jwt, @PathVariable Long cardId,
                               @Valid @RequestBody ReviewRequest request) {
        return service.review(userId(jwt), cardId, request.grade());
    }

    @GetMapping("/summary")
    public ReviewSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return service.summary(userId(jwt));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
