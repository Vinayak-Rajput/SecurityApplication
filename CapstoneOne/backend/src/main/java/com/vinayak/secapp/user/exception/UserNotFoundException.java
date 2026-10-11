package com.vinayak.secapp.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long id) {
        super("User " + id + " was not found");
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
