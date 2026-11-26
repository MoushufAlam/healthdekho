package com.healthdekho.controller;

import com.healthdekho.dto.customer.CustomerProfileResponse;
import com.healthdekho.dto.policy.DashboardResponse;
import com.healthdekho.dto.product.ProductResponse;
import com.healthdekho.security.CurrentCustomer;
import com.healthdekho.service.CustomerService;
import com.healthdekho.service.PolicyService;
import com.healthdekho.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final PolicyService policyService;
    private final ProductService productService;

    @GetMapping("/customers/me")
    public CustomerProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
        return customerService.getProfile(CurrentCustomer.id(jwt));
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal Jwt jwt) {
        return policyService.getDashboard(CurrentCustomer.id(jwt));
    }

    @GetMapping("/products/available")
    public List<ProductResponse> availableProducts(@AuthenticationPrincipal Jwt jwt) {
        return productService.getAvailableProducts(CurrentCustomer.id(jwt));
    }
}
