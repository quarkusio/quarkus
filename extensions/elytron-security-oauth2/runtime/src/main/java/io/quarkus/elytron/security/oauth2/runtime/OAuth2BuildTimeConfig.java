package io.quarkus.elytron.security.oauth2.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * See https://docs.wildfly.org/14/WildFly_Elytron_Security.html#validating-oauth2-bearer-tokens
 */
@ConfigMapping(prefix = "quarkus.oauth2")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface OAuth2BuildTimeConfig {

    /**
     * Determine if the OAuth2 extension is enabled. Enabled by default if you include the
     * <code>elytron-security-oauth2</code> dependency, so this would be used to disable it.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * The claim that is used in the introspection endpoint response to load the roles.
     */
    @WithDefault("scope")
    String roleClaim();
}
