package com.vinayak.secapp.auth.exception;

public class DisallowedRequestOriginException extends RuntimeException {
    public DisallowedRequestOriginException(String message) {
        super(message);
    }
}
