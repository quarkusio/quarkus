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

    String DEFAULT_PATH = "openapi";

    String DEFAULT_STORE_SCHEMA_FILE_NAME = "openapi";

    /**
     * If management interface is turned on the openapi schema document will be published under the management interface.
     * This
     * allows you to exclude OpenAPI from management by setting the value to false
     */
    @WithName("management.enabled")
    @WithDefault("true")
    boolean managementEnabled();

    /**
     * OpenAPI documents
     */
    @ConfigDocMapKey("document-name")
    @WithParentName
    @WithUnnamedKey(DEFAULT_DOCUMENT_NAME)
    @WithDefaults
    Map<String, OpenApiDocumentConfig> documents();

    enum SecurityScheme {
        apiKey,
        basic,
        jwt,
        oauth2,
        oidc,
        oauth2Implicit
    }

    default String documentPath(String documentName) {
        OpenApiDocumentConfig openApiDocumentConfig = documents().get(documentName);
        if (openApiDocumentConfig == null) {
            return null;
        }

        if (DEFAULT_DOCUMENT_NAME.equals(documentName)) {
            return openApiDocumentConfig.path();
        }

        if (!DEFAULT_PATH.equals(openApiDocumentConfig.path())) {
            return openApiDocumentConfig.path();
        }

        return openApiDocumentConfig + "-" + documentName;
    }

    default String documentStoreFileName(String documentName) {
        OpenApiDocumentConfig openApiDocumentConfig = documents().get(documentName);
        if (openApiDocumentConfig == null) {
            return null;
        }

        if (DEFAULT_DOCUMENT_NAME.equals(documentName)) {
            return openApiDocumentConfig.storeSchemaFileName();
        }

        if (!DEFAULT_STORE_SCHEMA_FILE_NAME.equals(openApiDocumentConfig.storeSchemaFileName())) {
            return openApiDocumentConfig.storeSchemaFileName();
        }

        return openApiDocumentConfig + "-" + documentName;
    }
}
