package io.quarkus.vertx.http.runtime.options;

import java.util.Optional;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.security.HttpSecurityConfiguration;
import io.vertx.core.http.ClientAuth;

/**
 * This class is a single source of truth for HTTP server TLS configuration name and TLS client authentication.
 * It must exist because HTTP Security fluent API allows users to configure programmatically
 * the 'quarkus.http.ssl.client-auth' and 'quarkus.http.tls-configuration-name' configuration properties.
 */
public final class HttpServerTlsConfig {

    private static final Logger LOG = Logger.getLogger(HttpServerTlsConfig.class.getName());
    private static volatile ClientAuth tlsClientAuth = null;
    private static volatile Optional<String> tlsConfigName = Optional.empty();

    public static ClientAuth getTlsClientAuth(VertxHttpConfig httpConfig, VertxHttpBuildTimeConfig httpBuildTimeConfig,
            LaunchMode launchMode) {
        if (HttpSecurityConfiguration.isNotReady(httpConfig, httpBuildTimeConfig, launchMode)) {
            if (launchMode == LaunchMode.DEVELOPMENT) {
                // right now, it is possible that when server starts after a failed start, Arc container is null
                // we can't really handle such a situation, because we have no way to get user input (fire CDI event)
                LOG.debug(
                        "CDI container is not available, Quarkus will ignore (possible) programmatic TLS client authentication"
                                + " configuration and default to the 'quarkus.http.ssl.client-auth' configuration property value");
                return httpBuildTimeConfig.tlsClientAuth();
            } else {
                throw new IllegalStateException(
                        "CDI container is not available, cannot retrieve programmatic TLS client authentication configuration");
            }
        }
        return tlsClientAuth == null ? httpBuildTimeConfig.tlsClientAuth() : tlsClientAuth;
    }

    public static Optional<String> getHttpServerTlsConfigName(VertxHttpConfig httpConfig,
            VertxHttpBuildTimeConfig httpBuildTimeConfig, LaunchMode launchMode) {

        if (HttpSecurityConfiguration.isNotReady(httpConfig, httpBuildTimeConfig, launchMode)) {
            if (launchMode == LaunchMode.DEVELOPMENT) {
                // right now, it is possible that when server starts after a failed start, Arc container is null
                // we can't really handle such a situation, because we have no way to get user input (fire CDI event)
                LOG.debug("CDI container is not available, Quarkus will ignore (possible) programmatic TLS configuration name"
                        + " configuration and default to the 'quarkus.http.tls-configuration-name' configuration property value");
                return httpConfig.tlsConfigurationName();
            } else {
                throw new IllegalStateException(
                        "CDI container is not available, cannot retrieve programmatic TLS configuration name");
            }
        }
        return tlsConfigName.isPresent() ? tlsConfigName : httpConfig.tlsConfigurationName();
    }

    public static void setConfiguration(HttpSecurityConfiguration.ProgrammaticTlsConfig programmaticConfig) {
        tlsConfigName = programmaticConfig.tlsConfigName;
        tlsClientAuth = programmaticConfig.tlsClientAuth;
    }
}
