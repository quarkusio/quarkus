package io.quarkus.micrometer.runtime.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "quarkus.micrometer.binder.vertx")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface VertxConfig {

    /**
     * Comma-separated list of regular expressions used to specify uri
     * labels in http metrics.
     *
     * Vertx instrumentation will attempt to transform parameterized
     * resource paths, `/item/123`, into a generic form, `/item/{id}`,
     * to reduce the cardinality of uri label values.
     *
     * Patterns specified here will take precedence over those computed
     * values.
     *
     * For example, if `/item/\\\\d+=/item/custom` or
     * `/item/[0-9]+=/item/custom` is specified in this list,
     * a request to a matching path (`/item/123`) will use the specified
     * replacement value (`/item/custom`) as the value for the uri label.
     * Note that backslashes must be double escaped as `\\\\`.
     *
     * @asciidoclet
     * @deprecated use {@code quarkus.micrometer.binder.http-server.match-patterns}
     */
    @Deprecated
    Optional<List<String>> matchPatterns();

    /**
     * Comma-separated list of regular expressions defining uri paths
     * that should be ignored (not measured).
     *
     * @deprecated use {@code quarkus.micrometer.binder.http-server.ignore-patterns}
     */
    @Deprecated
    Optional<List<String>> ignorePatterns();
}
