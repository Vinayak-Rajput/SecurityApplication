package com.hdfclife.capstoneone.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration(proxyBeanMethods = false)
public class WebPaginationConfig {

    @Bean
    public PageableHandlerMethodArgumentResolverCustomizer pageableResolverCustomizer() {
        return resolver -> resolver.setMaxPageSize(100);
    }
}
