package io.quarkus.smallrye.jwt.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * deployment configuration
 */
@ConfigRoot(name = "smallrye-jwt")
public class SmallRyeJwtBuildTimeConfig {

    /**
     * The MP-JWT configuration object
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled = true;

    /**
     * The name of the {@linkplain java.security.Provider} that supports SHA256withRSA signatures
     */
    @ConfigItem(defaultValue = "SunRsaSign")
    public String rsaSigProvider;
}
