package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;

public interface FilterConfig {
    /**
     * A regular expression for the paths matching this configuration
     */
    String matches();

    /**
     * Additional HTTP Headers always sent in the response
     */
    @ConfigDocMapKey("header-name")
    Map<String, String> header();

    /**
     * The HTTP methods for this path configuration
     */
    Optional<List<String>> methods();

    /**
     * Order in which this path config is applied. Higher priority takes precedence
     */
    OptionalInt order();
}
