package com.healthdekho.exception;

// HTTP 401: login failed. The message never says whether the email or the password was wrong.
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
