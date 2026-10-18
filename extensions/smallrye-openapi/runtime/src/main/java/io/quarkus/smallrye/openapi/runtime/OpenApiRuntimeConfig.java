package io.quarkus.smallrye.openapi.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.smallrye-openapi")
public interface OpenApiRuntimeConfig {

    /**
     * Enable the openapi endpoint. By default it's enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * OpenAPI documents
     */
    @WithParentName
    @WithUnnamedKey(OpenApiConstants.DEFAULT_DOCUMENT_NAME)
    @WithDefaults
    Map<String, OpenApiDocumentRuntimeConfig> documents();

}
