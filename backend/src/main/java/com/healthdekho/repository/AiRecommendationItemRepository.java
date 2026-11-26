package com.healthdekho.repository;

import com.healthdekho.entity.AiRecommendationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AiRecommendationItemRepository extends JpaRepository<AiRecommendationItem, Long> {

    @Query("""
            select i from AiRecommendationItem i
            join fetch i.request r
            where i.id = :itemId and r.customer.id = :customerId
            """)
    Optional<AiRecommendationItem> findOwnedItem(Long itemId, Long customerId);
}
