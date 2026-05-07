package io.quarkus.elasticsearch.javaclient.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface ElasticsearchJavaClientRuntimeConfig {

    /**
     * Defines whether the client should be active at runtime.
     */
    @ConfigDocDefault("`true` if the underlying low-level Elasticsearch REST client is also active; `false` otherwise")
    Optional<Boolean> active();

}
