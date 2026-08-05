package io.quarkus.micrometer.runtime.config;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Build / static runtime config for HTTP metrics shared between server and client.
 */
@ConfigGroup
public interface HttpConfigGroup {

    /**
     * Comma-separated list of additional HTTP methods to allow in the
     * {@code method} metric tag, beyond the standard RFC 9110/5789/9512 methods
     * (GET, HEAD, POST, PUT, DELETE, CONNECT, OPTIONS, TRACE, PATCH, QUERY).
     *
     * Non-standard HTTP methods are replaced with {@code UNKNOWN} by default
     * to prevent metrics cardinality explosion. Use this property to allow
     * specific custom methods (e.g. WebDAV methods like {@code PROPFIND}).
     *
     * The total number of methods to track cannot exceed 32.
     *
     * @asciidoclet
     */
    Optional<List<String>> additionalMethods();
}
