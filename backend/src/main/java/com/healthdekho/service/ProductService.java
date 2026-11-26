package com.healthdekho.service;

import com.healthdekho.dto.product.ProductResponse;
import com.healthdekho.entity.InsuranceProduct;
import com.healthdekho.repository.CustomerPolicyRepository;
import com.healthdekho.repository.InsuranceProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final InsuranceProductRepository productRepository;
    private final CustomerPolicyRepository policyRepository;

    public List<ProductResponse> getAvailableProducts(Long customerId) {
        return availableProductsFor(customerId).stream()
                .map(ProductResponse::from)
                .toList();
    }

    // Active products minus the ones this customer already holds (duplicate rule = one policy per product)
    public List<InsuranceProduct> availableProductsFor(Long customerId) {
        Set<Long> ownedProductIds = policyRepository.findAllByCustomerIdWithProduct(customerId).stream()
                .map(policy -> policy.getProduct().getId())
                .collect(Collectors.toSet());

        return productRepository.findByActiveTrueOrderByProductCodeAsc().stream()
                .filter(product -> !ownedProductIds.contains(product.getId()))
                .toList();
    }
}
