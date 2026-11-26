package com.healthdekho.exception;

// HTTP 409: the request clashes with existing data (duplicate policy, installment already paid).
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
