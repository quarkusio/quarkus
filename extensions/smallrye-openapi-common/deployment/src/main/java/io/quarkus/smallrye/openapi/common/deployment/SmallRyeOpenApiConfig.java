package io.quarkus.smallrye.openapi.common.deployment;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.smallrye-openapi")
public interface SmallRyeOpenApiConfig {

    String DEFAULT_DOCUMENT_NAME = "<default>";

    String DEFAULT_PATH = "openapi-<document-name>";

    String DEFAULT_STORE_SCHEMA_FILE_NAME = "openapi-<document-name>";

    enum SecurityScheme {
        apiKey,
        basic,
        jwt,
        oauth2,
        oidc,
        oauth2Implicit
    }

    /**
     * If management interface is turned on the openapi schema document will be published under the management interface. This
     * allows you to exclude OpenAPI from management by setting the value to false
     */
    @WithName("management.enabled")
    @WithDefault("true")
    boolean managementEnabled();

    /**
     * Configuration properties for the JavaScript client proxy generation
     */
    @WithName("js-client")
    SmallRyeOpenApiJsClientConfig jsClient();

    /**
     * OpenAPI documents
     */
    @ConfigDocMapKey("document-name")
    @WithParentName
    @WithUnnamedKey(DEFAULT_DOCUMENT_NAME)
    @WithDefaults
    Map<String, OpenApiDocumentConfig> documents();

    default OpenApiDocumentConfig defaultDocument() {
        return documents().get(DEFAULT_DOCUMENT_NAME);
    }
}
