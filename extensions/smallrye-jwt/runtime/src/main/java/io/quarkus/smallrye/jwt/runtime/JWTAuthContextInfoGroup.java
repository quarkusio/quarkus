package io.quarkus.smallrye.jwt.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * deployment configuration
 */
@ConfigRoot(name = "smallrye-jwt")
public class JWTAuthContextInfoGroup {

    /**
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "MP-JWT")
    public String authMechanism;

    /**
     * The authentication mechanism
     */
    @ConfigItem(defaultValue = "Quarkus-JWT")
    public String realmName;

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
