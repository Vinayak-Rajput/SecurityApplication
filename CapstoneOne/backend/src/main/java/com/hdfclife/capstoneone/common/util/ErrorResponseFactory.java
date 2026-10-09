package com.hdfclife.capstoneone.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfclife.capstoneone.common.dto.ErrorResponse;
import com.hdfclife.capstoneone.logging.RequestCorrelationId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.function.Consumer;

@Component
public class ErrorResponseFactory {

    private final ObjectMapper objectMapper;

    public ErrorResponseFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ResponseEntity<ErrorResponse> create(
            HttpStatus status,
            String message,
            HttpServletRequest request
    ) {
        return create(status, message, request, response -> { });
    }

    public ResponseEntity<ErrorResponse> create(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Consumer<ResponseEntity.BodyBuilder> customize
    ) {
        String safeMessage = message == null || message.isBlank() ? "Invalid request" : message;
        String correlationId = RequestCorrelationId.ensure(request);
        MDC.put("correlationId", correlationId);

        ErrorResponse body = new ErrorResponse(
                status.value(), status.getReasonPhrase(), safeMessage, request.getRequestURI(), correlationId);
        ResponseEntity.BodyBuilder response = ResponseEntity.status(status)
                .header(RequestCorrelationId.HEADER_NAME, correlationId);
        customize.accept(response);
        return response.body(body);
    }

    public void write(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String message
    ) throws IOException {
        String safeMessage = message == null || message.isBlank() ? "Request failed" : message;
        String correlationId = RequestCorrelationId.attach(request, response);
        MDC.put("correlationId", correlationId);

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
                new ErrorResponse(status.value(), status.getReasonPhrase(), safeMessage,
                        request.getRequestURI(), correlationId));
    }
}
