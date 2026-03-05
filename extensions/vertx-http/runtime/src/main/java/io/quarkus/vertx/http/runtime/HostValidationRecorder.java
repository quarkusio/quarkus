package io.quarkus.vertx.http.runtime;

import java.util.Set;

import org.jboss.logging.Logger;

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
        final HostValidationConfig hostValidationConfig = httpConfig.getValue().hostValidation();

        // Allowing a locahost only is not permitted when a list of hosts is explicitly configured and vice versa
        if (!hostValidationConfig.allowedHosts().isEmpty() && hostValidationConfig.allowLocalhost().orElse(false)) {
            throw new ConfigurationException(
                    "'quarkus.http.host-validation.allowed-hosts' and 'quarkus.http.host-validation.allowed-localhost'"
                            + " are mutually exclusive properties.");

        }

        // Allowed hosts are configured
        if (!hostValidationConfig.allowedHosts().isEmpty()) {
            return new HostValidationFilter(hostValidationConfig.allowedHosts().get());
        }

        // Allow localhost is configured
        if (hostValidationConfig.allowLocalhost().isPresent()) {
            return hostValidationConfig.allowLocalhost().get() ? new HostValidationFilter(LOCAL_HOSTNAMES) : null;
        }

        // Allow localhost is not configured, enable it if quarkus.http.host contains a valid localhost name
        if (LOCAL_HOSTNAMES.contains(httpConfig.getValue().host())) {
            return new HostValidationFilter(LOCAL_HOSTNAMES);
        }

        return null;
    }

}
