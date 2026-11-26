package com.healthdekho.service;

import java.time.LocalDate;
import java.time.Period;

public final class AgeCalculator {

    private AgeCalculator() {
    }

    // completed years on the given date (birthday not reached yet = one year less)
    public static int ageOn(LocalDate dateOfBirth, LocalDate date) {
        return Period.between(dateOfBirth, date).getYears();
    }
}
