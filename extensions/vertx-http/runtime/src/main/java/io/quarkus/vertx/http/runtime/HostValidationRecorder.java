package io.quarkus.vertx.http.runtime;

import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class HostValidationRecorder {
    private static final Logger LOG = Logger.getLogger(HostValidationRecorder.class);
    private static final Set<String> LOCAL_HOSTNAMES = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    private final RuntimeValue<VertxHttpConfig> httpConfig;

    public HostValidationRecorder(RuntimeValue<VertxHttpConfig> httpConfig) {
        this.httpConfig = httpConfig;
    }

    public Handler<RoutingContext> hostValidationHandler() {
        return hostValidationHandler(httpConfig.getValue().hostValidation(), httpConfig.getValue().host(), false);
    }

    static Handler<RoutingContext> hostValidationHandler(HostValidationConfig hostValidationConfig, String quarkusHttpHost,
            boolean managementRouter) {

        // Allowing a localhost only is not permitted when a list of hosts is explicitly configured and vice versa
        if (!hostValidationConfig.allowedHosts().isEmpty() && hostValidationConfig.requireLocalhost().orElse(false)) {
            String errorMessage = """
                    'quarkus.%1$s.host-validation.allowed-hosts' and 'quarkus.%1$s.host-validation.require-localhost' are mutually exclusive properties.
                    """
                    .formatted((managementRouter ? "management" : "http"));
            throw new ConfigurationException(errorMessage);
        }

        if (hostValidationConfig.allowedHosts().isPresent()
                && !hostValidationConfig.allowedHosts().get().isEmpty()) {
            LOG.debugf(
                    "HTTP Host header will be expected to contain one of the host names configured with 'quarkus.%s.host-validation.allowed-hosts'",
                    (managementRouter ? "management" : "http"));
            return new HostValidationFilter(hostValidationConfig.allowedHosts().get());
        }

        if (hostValidationConfig.requireLocalhost().isPresent()) {
            LOG.debug("HTTP Host header will be expected to contain one of the valid localhost names only");
            return hostValidationConfig.requireLocalhost().get() ? new HostValidationFilter(LOCAL_HOSTNAMES) : null;
        }

        // When in prod or dev mode, require localhost if the host has a valid localhost name
        if ((LaunchMode.current().isProduction() || LaunchMode.current().isDev())
                && LOCAL_HOSTNAMES.contains(quarkusHttpHost)) {
            LOG.debug("Enabling localhost name validation in production because the server is launched on localhost");
            return new HostValidationFilter(LOCAL_HOSTNAMES);
        }

        return null;
    }

}
