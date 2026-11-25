package com.healthdekho.repository;

import com.healthdekho.entity.AiRecommendationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRecommendationRequestRepository extends JpaRepository<AiRecommendationRequest, Long> {
}
