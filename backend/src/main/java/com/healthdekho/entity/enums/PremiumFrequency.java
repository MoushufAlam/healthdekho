package com.healthdekho.entity.enums;

public enum PremiumFrequency {
    MONTHLY(1),
    QUARTERLY(3),
    HALF_YEARLY(6),
    YEARLY(12);

    // months covered by one installment
    private final int months;

    PremiumFrequency(int months) {
        this.months = months;
    }

    public int getMonths() {
        return months;
    }
}
