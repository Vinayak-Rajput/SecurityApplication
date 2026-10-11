package com.vinayak.secapp.auth.exception;

public class InactiveTokenException extends RuntimeException {
    public InactiveTokenException(String message) {
        super(message);
    }
}
