package io.quarkus.smallrye.openapi.deployment.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.openapi.common.deployment.OpenApiDocumentConfig;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;

class SecurityConfigFilterTest {

    @Test
    void testConfigureApiKeySecuritySchemeMissingName() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = new DummySmallRyeOpenApiConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey, null, null);
        assertThrows(ConfigurationException.class,
                () -> new SecurityConfigFilter(config.documents().get(SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME))
                        .configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeMissingParamIn() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = new DummySmallRyeOpenApiConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey,
                "KeyParamName", null);
        assertThrows(ConfigurationException.class,
                () -> new SecurityConfigFilter(config.documents().get(SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME))
                        .configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeInvalidParamIn() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = new DummySmallRyeOpenApiConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey,
                "KeyParamName", "path");
        assertThrows(ConfigurationException.class,
                () -> new SecurityConfigFilter(config.documents().get(SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME))
                        .configureApiKeySecurityScheme(securityScheme));
    }

    @Test
    void testConfigureApiKeySecuritySchemeSuccess() {
        SecurityScheme securityScheme = OASFactory.createSecurityScheme();
        SmallRyeOpenApiConfig config = new DummySmallRyeOpenApiConfig(SmallRyeOpenApiConfig.SecurityScheme.apiKey,
                "KeyParamName", "header");
        new SecurityConfigFilter(config.documents().get(SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME))
                .configureApiKeySecurityScheme(securityScheme);
        assertEquals("KeyParamName", securityScheme.getName());
        assertEquals(SecurityScheme.In.HEADER, securityScheme.getIn());
    }

    private class DummySmallRyeOpenApiConfig implements SmallRyeOpenApiConfig {

        private final DummySmallRyeOpenApiConfigConfig dummySmallRyeOpenApiConfigConfig;

        DummySmallRyeOpenApiConfig(SecurityScheme securityScheme,
                String apiKeyParameterName,
                String apiKeyParameterIn) {
            this.dummySmallRyeOpenApiConfigConfig = new DummySmallRyeOpenApiConfigConfig(securityScheme,
                    apiKeyParameterName,
                    apiKeyParameterIn);
        }

        @Override
        public boolean managementEnabled() {
            return false;
        }

        @Override
        public Map<String, OpenApiDocumentConfig> documents() {
            return Map.of(SmallRyeOpenApiConfig.DEFAULT_DOCUMENT_NAME, dummySmallRyeOpenApiConfigConfig);
        }
    }

    private static class DummySmallRyeOpenApiConfigConfig implements OpenApiDocumentConfig {

        private final SmallRyeOpenApiConfig.SecurityScheme securityScheme;
        private final String apiKeyParameterName;
        private final String apiKeyParameterIn;

        public DummySmallRyeOpenApiConfigConfig(SmallRyeOpenApiConfig.SecurityScheme securityScheme, String apiKeyParameterName,
                String apiKeyParameterIn) {
            this.securityScheme = securityScheme;
            this.apiKeyParameterName = apiKeyParameterName;
            this.apiKeyParameterIn = apiKeyParameterIn;
        }

        @Override
        public String path() {
            return null;
        }

        @Override
        public Optional<Path> storeSchemaDirectory() {
            return Optional.empty();
        }

        @Override
        public String storeSchemaFileName() {
            return null;
        }

        @Override
        public boolean alwaysRunFilter() {
            return false;
        }

        @Override
        public boolean ignoreStaticDocument() {
            return false;
        }

        @Override
        public Optional<List<Path>> additionalDocsDirectory() {
            return Optional.empty();
        }

        @Override
        public Optional<SmallRyeOpenApiConfig.SecurityScheme> securityScheme() {
            return Optional.ofNullable(securityScheme);
        }

        @Override
        public String securitySchemeName() {
            return null;
        }

        @Override
        public String securitySchemeDescription() {
            return null;
        }

        @Override
        public boolean autoAddSecurityRequirement() {
            return false;
        }

        @Override
        public boolean autoAddTags() {
            return false;
        }

        @Override
        public boolean autoAddBadRequestResponse() {
            return false;
        }

        @Override
        public boolean autoAddOperationSummary() {
            return false;
        }

        @Override
        public Optional<Boolean> autoAddServer() {
            return Optional.empty();
        }

        @Override
        public boolean autoAddSecurity() {
            return false;
        }

        @Override
        public boolean autoAddOpenApiEndpoint() {
            return false;
        }

        @Override
        public Optional<String> apiKeyParameterIn() {
            return Optional.ofNullable(apiKeyParameterIn);
        }

        @Override
        public Optional<String> apiKeyParameterName() {
            return Optional.ofNullable(apiKeyParameterName);
        }

        @Override
        public String basicSecuritySchemeValue() {
            return null;
        }

        @Override
        public String jwtSecuritySchemeValue() {
            return null;
        }

        @Override
        public String jwtBearerFormat() {
            return null;
        }

        @Override
        public String oauth2SecuritySchemeValue() {
            return null;
        }

        @Override
        public String oauth2BearerFormat() {
            return null;
        }

        @Override
        public Optional<String> oidcOpenIdConnectUrl() {
            return Optional.empty();
        }

        @Override
        public Optional<String> oauth2ImplicitRefreshUrl() {
            return Optional.empty();
        }

        @Override
        public Optional<String> oauth2ImplicitAuthorizationUrl() {
            return Optional.empty();
        }

        @Override
        public Optional<String> oauth2ImplicitTokenUrl() {
            return Optional.empty();
        }

        @Override
        public Optional<String> openApiVersion() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoTitle() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoVersion() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoDescription() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoTermsOfService() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoContactEmail() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoContactName() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoContactUrl() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoLicenseName() {
            return Optional.empty();
        }

        @Override
        public Optional<String> infoLicenseUrl() {
            return Optional.empty();
        }

        @Override
        public Optional<String> operationIdStrategy() {
            return Optional.empty();
        }

        @Override
        public Map<String, String> securitySchemeExtensions() {
            return Map.of();
        }

        @Override
        public boolean mergeSchemaExamples() {
            return true;
        }

        @Override
        public Optional<Set<String>> scanProfiles() {
            return Optional.empty();
        }

        @Override
        public Optional<Set<String>> scanExcludeProfiles() {
            return Optional.empty();
        }
    }
}
