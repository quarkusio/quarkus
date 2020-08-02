package io.quarkus.elytron.security.oauth2.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * See http://docs.wildfly.org/14/WildFly_Elytron_Security.html#validating-oauth2-bearer-tokens
 */
@ConfigRoot(name = "oauth2", phase = ConfigPhase.RUN_TIME)
public class OAuth2RuntimeConfig {

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
}
