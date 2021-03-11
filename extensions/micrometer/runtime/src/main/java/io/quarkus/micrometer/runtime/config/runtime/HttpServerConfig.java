package io.quarkus.micrometer.runtime.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "micrometer.binder.http-server", phase = ConfigPhase.RUN_TIME)
public class HttpServerConfig {
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
     */
    @ConfigItem
    public Optional<List<String>> matchPatterns = Optional.empty();

    /**
     * Comma-separated list of regular expressions defining uri paths
     * that should be ignored (not measured).
     */
    @ConfigItem
    public Optional<List<String>> ignorePatterns = Optional.empty();

    /**
     * Maximum number of unique URI tag values allowed. After the max number of
     * tag values is reached, metrics with additional tag values are denied by
     * filter.
     */
    @ConfigItem(defaultValue = "100")
    public int maxUriTags;

    public void mergeDeprecatedConfig(VertxConfig config) {
        if (!ignorePatterns.isPresent()) {
            ignorePatterns = config.ignorePatterns;
        }
        if (!matchPatterns.isPresent()) {
            matchPatterns = config.matchPatterns;
        }
    }
}
