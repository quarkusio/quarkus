package io.quarkus.elytron.security.oauth2.runtime;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

/**
 * See https://docs.wildfly.org/14/WildFly_Elytron_Security.html#validating-oauth2-bearer-tokens
 */
@ConfigMapping(prefix = "quarkus.oauth2")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OAuth2RuntimeConfig {

    /**
     * The OAuth2 client id used to validate the token.
     * Mandatory if the extension is enabled.
     */
    Optional<String> clientId();

    /**
     * The OAuth2 client secret used to validate the token.
     * Mandatory if the extension is enabled.
     */
    Optional<String> clientSecret();

    /**
     * The OAuth2 introspection endpoint URL used to validate the token and gather the authentication claims.
     * Mandatory if the extension is enabled.
     */
    Optional<String> introspectionUrl();

    /**
     * The OAuth2 server certificate file. <em>Warning</em>: this is not supported in native mode where the certificate
     * must be included in the truststore used during the native image generation, see
     * <a href="native-and-ssl.html">Using SSL With Native Executables</a>.
     */
    Optional<String> caCertFile();

    /**
     * Client connection timeout for token introspection.
     * Infinite if not set.
     */
    Optional<Duration> connectionTimeout();

    /**
     * Client read timeout for token introspection.
     * Infinite if not set.
     */
    Optional<Duration> readTimeout();
}
