package com.healthdekho.controller;

import com.healthdekho.dto.recommendation.RecommendationRequest;
import com.healthdekho.dto.recommendation.RecommendationResponse;
import com.healthdekho.security.CurrentCustomer;
import com.healthdekho.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse recommend(@AuthenticationPrincipal Jwt jwt,
                                            @Valid @RequestBody RecommendationRequest request) {
        return recommendationService.recommend(CurrentCustomer.id(jwt), request);
    }
}
