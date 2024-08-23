package io.quarkus.smallrye.jwt.runtime.auth;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.smallrye-jwt")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SmallRyeJwtConfig {

    /**
     * Enable this property if fetching the remote keys can be a time-consuming operation.
     * Do not enable it if you use the local keys.
     */
    @WithDefault("false")
    boolean blockingAuthentication();

    /**
     * Always create HTTP 401 challenge, even for requests containing no authentication credentials.
     *
     * JWT authentication mechanism will return HTTP 401 when an authentication challenge is required.
     * However if it is used alongside one of the interactive authentication mechanisms then returning HTTP 401
     * to the users accessing the application from a browser may not be desired.
     *
     * If you prefer you can request that JWT authentication mechanism does not create a challenge in such cases
     * by setting this property to 'true'.
     *
     */
    @WithDefault("false")
    boolean silent();

}
