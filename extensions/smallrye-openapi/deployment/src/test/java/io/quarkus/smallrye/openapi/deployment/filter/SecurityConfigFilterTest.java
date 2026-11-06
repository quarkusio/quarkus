package io.quarkus.smallrye.openapi.deployment.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.openapi.common.deployment.OpenApiDocumentConfig;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.smallrye.config.ConfigInstanceBuilder;

class SecurityConfigFilterTest {

    @Test
    void testConfigureApiKeySecuritySchemeMissingName() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = createConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey, null, null);
        assertThrows(ConfigurationException.class,
                () -> new SecurityConfigFilter(config.defaultDocument())
                        .configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeMissingParamIn() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = createConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey,
                "KeyParamName", null);
        assertThrows(ConfigurationException.class,
                () -> new SecurityConfigFilter(config.defaultDocument())
                        .configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeInvalidParamIn() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = createConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey,
                "KeyParamName", "path");
        assertThrows(ConfigurationException.class,
                () -> new SecurityConfigFilter(config.defaultDocument())
                        .configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeSuccess() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = createConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey,
                "KeyParamName", "header");
        new SecurityConfigFilter(config.defaultDocument())
                .configureApiKeySecurityScheme(securityScheme);
        assertEquals("KeyParamName", securityScheme.getName());
        assertEquals(SecurityScheme.In.HEADER, securityScheme.getIn());
    }

    private static SmallRyeOpenApiConfig createConfig(SmallRyeOpenApiConfig.SecurityScheme securityScheme,
            String apiKeyParameterName, String apiKeyParameterIn) {
        ConfigInstanceBuilder<OpenApiDocumentConfig> document = ConfigInstanceBuilder.forInterface(OpenApiDocumentConfig.class);
        if (securityScheme != null) {
            document.withOptional(OpenApiDocumentConfig::securityScheme, securityScheme);
        }
        if (apiKeyParameterName != null) {
            document.withOptional(OpenApiDocumentConfig::apiKeyParameterName, apiKeyParameterName);
        }
        if (apiKeyParameterIn != null) {
            document.withOptional(OpenApiDocumentConfig::apiKeyParameterIn, apiKeyParameterIn);
        }

        return ConfigInstanceBuilder.forInterface(SmallRyeOpenApiConfig.class)
                .with(SmallRyeOpenApiConfig::documents,
                        Map.of(SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME, document.build()))
                .build();
    }
}
