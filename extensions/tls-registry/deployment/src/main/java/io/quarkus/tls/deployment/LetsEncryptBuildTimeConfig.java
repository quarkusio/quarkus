package io.quarkus.tls.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.tls.lets-encrypt")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface LetsEncryptBuildTimeConfig {

    /**
     * Set to {@code true} to enable let's encrypt support.
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * The Let's Encrypt client of the application.
     */
    Client client();

    /**
     * The build-time configuration of the Let's Encrypt client of the application.
     */
    interface Client {

        /**
         * Set to {@code true} to let the application create the Let's Encrypt account, order the certificate and renew
         * it.
         * <p>
         * The application then serves the HTTP-01 challenge itself, and the Quarkus CLI is not needed.
         * The runtime properties {@code quarkus.tls.lets-encrypt.client.*} configure the domains and the account.
         */
        @WithDefault("false")
        boolean enabled();

        /**
         * The name of the TLS configuration that uses the Let's Encrypt certificate.
         * <p>
         * If not set, the default TLS configuration uses it.
         * The TLS configuration must not configure a key store, because the client provides it.
         */
        Optional<String> tlsConfigurationName();
    }
}
