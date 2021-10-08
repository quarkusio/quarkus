package io.quarkus.smallrye.jwt.runtime.auth;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "smallrye-jwt", phase = ConfigPhase.RUN_TIME)
public class SmallRyeJwtConfig {

    /**
     * Enable this property if fetching the remote keys can be a time consuming operation.
     * Do not enable it if you use the local keys.
     */
    @ConfigItem(defaultValue = "false")
    public boolean blockingAuthentication;
}
