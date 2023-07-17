package io.quarkus.smallrye.openapi.deployment.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;

class SecurityConfigFilterTest {

    SmallRyeOpenApiConfig config;
    SecurityConfigFilter target;

    @BeforeEach
    void setup() {
        config = new SmallRyeOpenApiConfig();
        target = new SecurityConfigFilter(config);
    }

    @Test
    void testConfigureApiKeySecuritySchemeMissingName() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        config.securityScheme = Optional.of(SmallRyeOpenApiConfig.SecurityScheme.apiKey);
        config.apiKeyParameterName = Optional.empty();
        assertThrows(ConfigurationException.class, () -> target.configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeMissingParamIn() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        config.securityScheme = Optional.of(SmallRyeOpenApiConfig.SecurityScheme.apiKey);
        config.apiKeyParameterName = Optional.of("KeyParamName");
        config.apiKeyParameterIn = Optional.empty();
        assertThrows(ConfigurationException.class, () -> target.configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeInvalidParamIn() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        config.securityScheme = Optional.of(SmallRyeOpenApiConfig.SecurityScheme.apiKey);
        config.apiKeyParameterName = Optional.of("KeyParamName");
        config.apiKeyParameterIn = Optional.of("path");
        assertThrows(ConfigurationException.class, () -> target.configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeSuccess() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        config.securityScheme = Optional.of(SmallRyeOpenApiConfig.SecurityScheme.apiKey);
        config.apiKeyParameterName = Optional.of("KeyParamName");
        config.apiKeyParameterIn = Optional.of("header");
        target.configureApiKeySecurityScheme(securityScheme);
        assertEquals("KeyParamName", securityScheme.getName());
        assertEquals(SecurityScheme.In.HEADER, securityScheme.getIn());
    }

}
