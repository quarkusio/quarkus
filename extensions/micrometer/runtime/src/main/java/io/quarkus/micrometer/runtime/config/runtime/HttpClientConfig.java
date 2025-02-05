package io.quarkus.micrometer.runtime.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.micrometer.binder.http-client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface HttpClientConfig {
    /**
     * Comma-separated list of regular expressions used to specify uri
     * labels in http metrics.
     *
     * Outbount HTTP client instrumentation will attempt to transform parameterized
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
     */
    Optional<List<String>> matchPatterns();

    /**
     * Comma-separated list of regular expressions defining uri paths
     * that should be ignored (not measured).
     */
    Optional<List<String>> ignorePatterns();

    /**
     * Suppress 4xx errors from metrics collection for unmatched templates.
     * This configuration exists to limit cardinality explosion from caller side errors. Does not apply to 404 errors.
     *
     * Suppressing 4xx errors is disabled by default.
     *
     * @asciidoclet
     */
    @WithDefault("false")
    boolean suppress4xxErrors();

    /**
     * Maximum number of unique URI tag values allowed. After the max number of
     * tag values is reached, metrics with additional tag values are denied by
     * filter.
     */
    @WithDefault("100")
    int maxUriTags();
}
