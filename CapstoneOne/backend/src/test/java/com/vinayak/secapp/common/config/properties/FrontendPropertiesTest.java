package com.vinayak.secapp.common.config.properties;

import com.vinayak.secapp.common.config.properties.FrontendProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FrontendPropertiesTest {

    @Test
    void normalizesCommaSeparatedOriginsAndRemovesDuplicates() {
        FrontendProperties properties = new FrontendProperties();
        properties.setAllowedOrigins(List.of(" http://localhost:3000, https://portal.example ", "https://portal.example"));

        assertEquals(List.of("http://localhost:3000", "https://portal.example"), properties.getAllowedOrigins());
    }
}
