package com.healthdekho.exception;

// HTTP 400: the input is well-formed but breaks a business rule (age limits, overpayment, ...).
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
