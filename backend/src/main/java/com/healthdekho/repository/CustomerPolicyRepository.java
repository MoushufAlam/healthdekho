package com.healthdekho.repository;

import com.healthdekho.entity.CustomerPolicy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerPolicyRepository extends JpaRepository<CustomerPolicy, Long> {

    // join fetch avoids one extra product query per row (N+1)
    @Query("""
            select p from CustomerPolicy p
            join fetch p.product
            where p.customer.id = :customerId
            order by p.startDate desc, p.id desc
            """)
    List<CustomerPolicy> findAllByCustomerIdWithProduct(Long customerId);

    // OPTIONAL: FR-4 + FR-5 (not evaluated). Used only by the payment lookup and the search.
    // Every policy lookup is scoped to the logged-in customer: someone else's policy number simply isn't found
    @Query("""
            select p from CustomerPolicy p
            join fetch p.product
            join fetch p.customer c
            where p.policyNumber = :policyNumber and c.id = :customerId
            """)
    Optional<CustomerPolicy> findOwnedPolicy(String policyNumber, Long customerId);

    boolean existsByCustomer_IdAndProduct_Id(Long customerId, Long productId);

    // numbers are zero-padded (HDP-2026-0007), so the text max is also the numeric max
    @Query("select max(p.policyNumber) from CustomerPolicy p where p.policyNumber like concat(:prefix, '%')")
    Optional<String> findMaxPolicyNumberStartingWith(String prefix);

    // OPTIONAL: FR-5 (not evaluated). Search by customer name, paged.
    @Query(value = """
            select p from CustomerPolicy p
            join fetch p.product
            join fetch p.customer c
            where c.id = :customerId and lower(c.fullName) like lower(concat('%', :name, '%'))
            """,
            countQuery = """
            select count(p) from CustomerPolicy p
            where p.customer.id = :customerId and lower(p.customer.fullName) like lower(concat('%', :name, '%'))
            """)
    Page<CustomerPolicy> searchOwnedByCustomerName(Long customerId, String name, Pageable pageable);
}
