package com.healthdekho.exception;

// HTTP 404. Also used for records owned by another customer, so their existence is not revealed.
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
