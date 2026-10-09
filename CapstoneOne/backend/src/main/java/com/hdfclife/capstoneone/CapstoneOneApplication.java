package com.hdfclife.capstoneone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@OpenAPIDefinition(info = @Info(
        title = "CapstoneOne Auth API",
        description = "Authentication and Identity endpoints for CapstoneOne.",
        version = "v1.0.0"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT access token")
public class CapstoneOneApplication {

    public static void main(String[] args) {
        SpringApplication.run(CapstoneOneApplication.class, args);
    }

}
