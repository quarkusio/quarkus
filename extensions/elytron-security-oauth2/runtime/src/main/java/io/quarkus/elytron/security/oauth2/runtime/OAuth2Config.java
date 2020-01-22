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
     * Determine if the OAuth2 extension is enabled. Enabled by default if you include the
     * <code>elytron-security-oauth2</code> dependency, so this would be used to disable it.
     */
    @ConfigItem(defaultValue = "true")
    public boolean enabled;

    /**
     * The OAuth2 client id used to validate the token.
     * Mandatory if the extension is enabled.
     */
    @ConfigItem
    public Optional<String> clientId;

    /**
     * The OAuth2 client secret used to validate the token.
     * Mandatory if the extension is enabled.
     */
    @ConfigItem
    public Optional<String> clientSecret;

    /**
     * The OAuth2 introspection endpoint URL used to validate the token and gather the authentication claims.
     * Mandatory if the extension is enabled.
     */
    @ConfigItem
    public Optional<String> introspectionUrl;

    /**
     * The OAuth2 server certificate file. <em>Warning</em>: this is not supported in native mode where the certificate
     * must be included in the truststore used during the native image generation, see
     * <a href="native-and-ssl.html">Using SSL With Native Executables</a>.
     */
    @ConfigItem
    public Optional<String> caCertFile;

    /**
     * The claim that is used in the introspection endpoint response to load the roles.
     */
    @ConfigItem(defaultValue = "scope")
    public String roleClaim;
}
