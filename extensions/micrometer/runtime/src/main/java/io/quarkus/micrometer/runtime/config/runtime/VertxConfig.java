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
