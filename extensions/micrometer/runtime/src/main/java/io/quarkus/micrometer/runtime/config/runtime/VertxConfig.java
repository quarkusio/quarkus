package io.quarkus.micrometer.runtime.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "micrometer.binder.vertx", phase = ConfigPhase.RUN_TIME)
public class VertxConfig {
    /**
     * @deprecated use {@code quarkus.micrometer.binder.http-server.match-patterns}
     */
    @Deprecated
    @ConfigItem
    public Optional<List<String>> matchPatterns = Optional.empty();

    /**
     * Comma-separated list of regular expressions defining uri paths
     * that should be ignored (not measured).
     * 
     * @deprecated use {@code quarkus.micrometer.binder.http-server.ignore-patterns}
     */
    @Deprecated
    @ConfigItem
    public Optional<List<String>> ignorePatterns = Optional.empty();
}
