package io.quarkus.smallrye.openapi.runtime;

import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface OpenApiDocumentRuntimeConfig {

    /**
     * Specify the list of servers that provide connectivity information
     */
    Optional<Set<String>> servers();
}
