package io.quarkus.micrometer.runtime.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "micrometer.binder.vertx", phase = ConfigPhase.RUN_TIME)
public class VertxConfig {
    /**
     * Comma-separated case-sensitive list of regular expressions defining Paths
     * that should be matched and used as tags. By default, the first path
     * segment will be used. This default behavior will also apply to any
     * URI path that is found (2xx or 5xx) but doesn't match elements
     * in this list.
     */
    @ConfigItem
    public Optional<List<String>> matchPatterns = Optional.empty();

    /**
     * Comma-separated case-sensitive list of regular expressions defining Paths
     * that should be ignored / not measured.
     */
    @ConfigItem
    public Optional<List<String>> ignorePatterns = Optional.empty();
}
