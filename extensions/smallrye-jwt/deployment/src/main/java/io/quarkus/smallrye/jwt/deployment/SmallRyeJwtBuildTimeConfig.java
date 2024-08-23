package io.quarkus.smallrye.jwt.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * deployment configuration
 */
@ConfigMapping(prefix = "quarkus.smallrye-jwt")
@ConfigRoot
public interface SmallRyeJwtBuildTimeConfig {

    /**
     * The MP-JWT configuration object
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The name of the {@linkplain java.security.Provider} that supports SHA256withRSA signatures
     */
    @WithDefault("SunRsaSign")
    String rsaSigProvider();
}
