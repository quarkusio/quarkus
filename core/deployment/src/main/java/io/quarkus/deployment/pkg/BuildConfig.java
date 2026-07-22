package io.quarkus.deployment.pkg;

import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithParentName;

/**
 * For Build properties to not be reported as unknowns.
 */
@ConfigMapping(prefix = "quarkus.build")
@ConfigRoot
public interface BuildConfig {
    /**
     * Build properties
     */
    @WithParentName
    Map<String, Optional<String>> properties();
}
