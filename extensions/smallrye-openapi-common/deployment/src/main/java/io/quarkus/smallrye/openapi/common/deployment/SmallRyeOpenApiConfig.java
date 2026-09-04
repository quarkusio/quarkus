package io.quarkus.smallrye.openapi.common.deployment;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

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
     * If set, the generated OpenAPI schema documents of every configured OpenAPI document will be stored in this
     * directory on build, without having to configure a {@code store-schema-directory} for each document
     * individually. Both the json and yaml variants of each document will be stored here.
     * <p>
     * A {@code store-schema-directory} configured for an individual document takes precedence over this directory
     * for that document.
     */
    @WithName("store-schemas-directory")
    Optional<Path> storeSchemasDirectory();

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
