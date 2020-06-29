package io.quarkus.elytron.security.oauth2.runtime;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * See http://docs.wildfly.org/14/WildFly_Elytron_Security.html#validating-oauth2-bearer-tokens
 */
@ConfigRoot(name = "oauth2", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OAuth2BuildTimeConfig {

    /**
     * Determine if the OAuth2 extension is enabled. Enabled by default if you include the
     * <code>elytron-security-oauth2</code> dependency, so this would be used to disable it.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The claim that is used in the introspection endpoint response to load the roles.
     */
    @ConfigItem(defaultValue = "scope")
    public String roleClaim;
}
