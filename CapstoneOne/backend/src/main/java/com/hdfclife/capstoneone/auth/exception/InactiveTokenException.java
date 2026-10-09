package com.hdfclife.capstoneone.auth.exception;

public class InactiveTokenException extends RuntimeException {
    public InactiveTokenException(String message) {
        super(message);
    }
}
