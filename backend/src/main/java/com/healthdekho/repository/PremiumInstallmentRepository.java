package com.healthdekho.repository;

import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface PremiumInstallmentRepository extends JpaRepository<PremiumInstallment, Long> {

    // ============================================================================
    // OPTIONAL: FR-4 / FR-5 (not evaluated). FR-3 registration only uses save()/saveAll()
    // from JpaRepository; every query below exists for payment (FR-4) or search (FR-5).
    // ============================================================================

    // OPTIONAL: FR-4 - installment schedule
    List<PremiumInstallment> findByPolicy_IdOrderByInstallmentNumberAsc(Long policyId);

    // OPTIONAL: FR-4 - payment
    // SELECT ... FOR UPDATE: a second payment request for the same installment waits here until the
    // first one commits, then sees it as PAID - this is what stops double payments.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i from PremiumInstallment i
            where i.policy.id = :policyId and i.id in :installmentIds
            order by i.installmentNumber
            """)
    List<PremiumInstallment> findForUpdate(Long policyId, Collection<Long> installmentIds);

    // OPTIONAL: FR-4 - "is every installment paid?" check
    long countByPolicy_IdAndPaymentStatusNot(Long policyId, PaymentStatus status);

    // OPTIONAL: FR-5 - payment totals shown in search results
    // Premium paid excludes penalties: a PAID installment counts as its scheduled amount,
    // anything above that is penalty. Unpaid installments can only hold premium (penalties are paid in full).
    @Query("""
            select new com.healthdekho.repository.PolicyPaymentTotals(
                i.policy.id,
                sum(case when i.paymentStatus = :paid then i.scheduledAmount else i.paidAmount end),
                sum(case when i.paymentStatus = :paid then i.paidAmount - i.scheduledAmount
                         else cast(0 as BigDecimal) end),
                sum(case when i.paymentStatus <> :paid and i.gracePeriodEndDate < :today then 1L else 0L end))
            from PremiumInstallment i
            where i.policy.id in :policyIds
            group by i.policy.id
            """)
    List<PolicyPaymentTotals> sumByPolicyIds(Collection<Long> policyIds, PaymentStatus paid, LocalDate today);
}
