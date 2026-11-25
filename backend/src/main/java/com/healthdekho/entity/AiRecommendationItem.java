package com.healthdekho.entity;

import com.healthdekho.entity.enums.Suitability;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_recommendation_item",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_ai_item_request_product",
                             columnNames = {"recommendation_request_id", "product_id"}),
           @UniqueConstraint(name = "uq_ai_item_request_rank",
                             columnNames = {"recommendation_request_id", "rank_order"})
       })
@Getter
@Setter
@NoArgsConstructor
public class AiRecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_request_id", nullable = false)
    private AiRecommendationRequest request;

    // product must be active when the recommendation is generated - checked in the service
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @Column(name = "rank_order", nullable = false)
    private Integer rankOrder;

    @Column(name = "recommendation_reason", nullable = false, length = 1000)
    private String recommendationReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Suitability suitability;

    @Column(name = "selected_for_registration", nullable = false)
    private Boolean selectedForRegistration = Boolean.FALSE;
}
