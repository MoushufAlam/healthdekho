// ============================================================================
// OPTIONAL: FR-4 (not evaluated: only FR-1 to FR-3 are assessed)
// What this file does: money and status rules for one installment (outstanding, penalty, amount due, OVERDUE from today's date).
// Only FR-4 uses this file. To remove the feature, delete every piece tagged
// "OPTIONAL: FR-4" (search the project for it). Theory and flow: docs/steps/step7.readme.md
// ============================================================================
package com.healthdekho.service;

import com.healthdekho.entity.PremiumInstallment;
import com.healthdekho.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Money and status rules for a single installment (FR-4).
 * <p>
 * Model: inside the grace period partial payments are allowed and carry no penalty.
 * After the grace period a 10% penalty is added to the outstanding premium and the installment
 * must be settled in full (premium + penalty) in one payment. Because of that, an unpaid installment
 * only ever holds premium in paidAmount, and for a PAID one everything above scheduledAmount is penalty.
 */
public final class PremiumRules {

    public static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.10");
    private static final BigDecimal ZERO = new BigDecimal("0.00"); // keeps money at 2 decimals in JSON

    private PremiumRules() {
    }

    public static boolean isPaid(PremiumInstallment installment) {
        return installment.getPaymentStatus() == PaymentStatus.PAID;
    }

    public static boolean isPastGrace(PremiumInstallment installment, LocalDate today) {
        return today.isAfter(installment.getGracePeriodEndDate());
    }

    public static BigDecimal premiumPaid(PremiumInstallment installment) {
        return isPaid(installment) ? installment.getScheduledAmount() : installment.getPaidAmount();
    }

    public static BigDecimal penaltyPaid(PremiumInstallment installment) {
        return isPaid(installment)
                ? installment.getPaidAmount().subtract(installment.getScheduledAmount()).max(ZERO)
                : ZERO;
    }

    public static BigDecimal outstandingPremium(PremiumInstallment installment) {
        return isPaid(installment)
                ? ZERO
                : installment.getScheduledAmount().subtract(installment.getPaidAmount());
    }

    public static BigDecimal penaltyIfPaidOn(PremiumInstallment installment, LocalDate paymentDate) {
        if (isPaid(installment) || !isPastGrace(installment, paymentDate)) {
            return ZERO;
        }
        return outstandingPremium(installment).multiply(LATE_PENALTY_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal amountDueOn(PremiumInstallment installment, LocalDate paymentDate) {
        return outstandingPremium(installment).add(penaltyIfPaidOn(installment, paymentDate));
    }

    // What the customer should see today. The stored status is only updated by payments,
    // so OVERDUE is derived from the date at read time.
    public static PaymentStatus effectiveStatus(PremiumInstallment installment, LocalDate today) {
        return switch (installment.getPaymentStatus()) {
            case PAID -> PaymentStatus.PAID;
            case PENDING, PARTIALLY_PAID, OVERDUE -> {
                if (isPastGrace(installment, today)) {
                    yield PaymentStatus.OVERDUE;
                }
                yield installment.getPaidAmount().signum() > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PENDING;
            }
        };
    }

    public static PaymentStatus statusAfterPayment(BigDecimal premiumPaid, BigDecimal scheduledAmount) {
        return switch (Integer.signum(premiumPaid.compareTo(scheduledAmount))) {
            case 0, 1 -> PaymentStatus.PAID;
            default -> premiumPaid.signum() > 0 ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PENDING;
        };
    }
}
