package com.healthdekho.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DashboardResponse(
        String customerName,
        int totalPolicies,
        List<PolicySummaryResponse> policies,
        String message) {
}
