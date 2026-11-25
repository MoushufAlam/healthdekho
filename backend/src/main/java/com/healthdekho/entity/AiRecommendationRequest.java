package com.healthdekho.entity;

import com.healthdekho.entity.enums.PolicyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ai_recommendation_request")
@Getter
@Setter
@NoArgsConstructor
public class AiRecommendationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_request_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "insurance_goals", length = 1000)
    private String insuranceGoals;

    // free text from the form (e.g. "20 years"), kept as entered for audit
    @Column(name = "preferred_term", length = 50)
    private String preferredTerm;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_policy_type", length = 20)
    private PolicyType preferredPolicyType;

    @Column(name = "calculated_age", nullable = false)
    private Integer calculatedAge;

    // model + prompt version let us trace which setup produced a recommendation
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;
}
