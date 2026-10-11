package com.vinayak.secapp.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebPaginationConfigTest {

    @Test
    void configuresMaximumPageSize() {
        TestablePageableResolver resolver = new TestablePageableResolver();

        new WebPaginationConfig().pageableResolverCustomizer().customize(resolver);

        assertEquals(100, resolver.maxPageSize());
    }

    private static class TestablePageableResolver extends PageableHandlerMethodArgumentResolver {
        private int maxPageSize() {
            return getMaxPageSize();
        }
    }
}
