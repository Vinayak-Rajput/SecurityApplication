package com.hdfclife.capstoneone.auth.exception;

public class DisallowedRequestOriginException extends RuntimeException {
    public DisallowedRequestOriginException(String message) {
        super(message);
    }
}
