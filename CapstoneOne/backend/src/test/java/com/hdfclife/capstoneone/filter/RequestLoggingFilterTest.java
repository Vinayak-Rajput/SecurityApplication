package com.hdfclife.capstoneone.filter;

import com.hdfclife.capstoneone.logging.RequestCorrelationId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void attachesCorrelationIdAndRestoresExistingMdcContext() throws Exception {
        String correlationId = "123e4567-e89b-12d3-a456-426614174000";
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(RequestCorrelationId.HEADER_NAME, correlationId);
        MDC.put("existingContext", "preserved");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertEquals(correlationId, MDC.get("correlationId"));
            assertEquals("GET", MDC.get("httpMethod"));
            response.setStatus(202);
        });

        assertEquals(correlationId, response.getHeader(RequestCorrelationId.HEADER_NAME));
        assertEquals(202, response.getStatus());
        assertEquals("preserved", MDC.get("existingContext"));
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("httpStatus"));
    }
}
