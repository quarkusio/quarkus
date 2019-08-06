package io.quarkus.elytron.security.oauth2.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * See http://docs.wildfly.org/14/WildFly_Elytron_Security.html#validating-oauth2-bearer-tokens
 */
@ConfigRoot(name = "oauth2", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class OAuth2Config {
    /**
     * Enable the OAuth2 extension.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The identifier of the client on the OAuth2 Authorization Server
     */
    @ConfigItem
    public String clientId;

    /**
     * The secret of the client
     */
    @ConfigItem
    public String clientSecret;

    /**
     * The URL of token introspection endpoint
     */
    @ConfigItem
    public String introspectionUrl;

    /**
     * The path to a ca custom cert file
     */
    @ConfigItem
    public Optional<String> caCertFile;

    /**
     * The claim that provides the roles
     */
    @ConfigItem(defaultValue = "scope")
    public String roleClaim;
}
