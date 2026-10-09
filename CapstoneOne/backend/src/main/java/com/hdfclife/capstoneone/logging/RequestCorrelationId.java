package com.hdfclife.capstoneone.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestCorrelationId {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String REQUEST_ATTRIBUTE = RequestCorrelationId.class.getName() + ".id";
    private static final Pattern UUID_PATTERN = Pattern.compile("(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    private RequestCorrelationId() {
    }

    public static String ensure(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ATTRIBUTE);

        if (existing instanceof String id) {
            return id;
        }

        String supplied = request.getHeader(HEADER_NAME);
        String id = supplied != null && UUID_PATTERN.matcher(supplied).matches()
                ? supplied.toLowerCase(java.util.Locale.ROOT)
                : UUID.randomUUID().toString();

        request.setAttribute(REQUEST_ATTRIBUTE, id);

        return id;
    }

    public static String attach(HttpServletRequest request, HttpServletResponse response) {
        String id = ensure(request);
        response.setHeader(HEADER_NAME, id);

        return id;
    }
}
