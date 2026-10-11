package com.vinayak.secapp.audit.model;

import java.util.Arrays;
import java.util.Optional;

public enum AuditEventType {
    LOGIN_SUCCESS,
    TOKEN_VALIDATION_SUCCESS,
    TOKEN_REFRESH_SUCCESS,
    LOGOUT_SUCCESS,
    AUTHENTICATION_FAILED,
    AUTHORIZATION_FAILED,
    DATABASE_UNAVAILABLE,
    DATABASE_OPERATION_FAILED,
    BAD_REQUEST,
    DUPLICATE_USER,
    USER_NOT_FOUND,
    USER_ADMIN_ACTION_FAILED,
    VALIDATION_FAILED,
    RATE_LIMITED,
    CIRCUIT_OPEN,
    METHOD_NOT_ALLOWED,
    UNEXPECTED_ERROR,
    AUDIT_LOG_ACCESS,
    USER_LIST_VIEWED,
    USER_VIEWED,
    USER_SELF_VIEWED,
    USER_REGISTERED,
    USER_CREATED,
    USER_DELETED, USER_UPDATE;

    public static Optional<AuditEventType> fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.name().equals(code))
                .findFirst();
    }
}
