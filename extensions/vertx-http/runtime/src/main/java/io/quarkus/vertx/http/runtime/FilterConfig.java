package io.quarkus.vertx.http.runtime;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.smallrye.config.WithDefault;

public interface FilterConfig {
    /**
     * A regular expression for the paths matching this configuration
     */
    String matches();

    /**
     * Additional HTTP Headers always sent in the response.
     * <p>
     * The headers are added to the response; a value that is already present is never replaced. When several filters
     * match a path, the values of all of them are sent.
     */
    @ConfigDocMapKey("header-name")
    Map<String, String> header();

    /**
     * The HTTP methods for this path configuration
     */
    Optional<List<String>> methods();

    /**
     * Order in which this path config is applied when several of them match a path. Filters with a lower order run
     * first, so their header values are added first; the order does not make one filter override the values of
     * another.
     */
    OptionalInt order();

    /**
     * If enabled, headers are applied only when the response status code is in the 2xx range.
     * This is useful to avoid caching error responses at CDNs when using aggressive {@code Cache-Control} headers.
     */
    @WithDefault("false")
    boolean applyOnSuccess();
}
