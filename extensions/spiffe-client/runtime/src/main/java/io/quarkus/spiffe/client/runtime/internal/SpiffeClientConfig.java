package io.quarkus.spiffe.client.runtime.internal;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;

/**
 * Configuration for the SPIFFE Workload API client.
 */
@ConfigMapping(prefix = "quarkus.spiffe-client")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface SpiffeClientConfig {

    /**
     * SPIFFE Workload Endpoint socket URI. Supports {@code unix://} for Unix Domain Socket
     * and {@code tcp://} for TCP transport as defined by the
     * <a href="https://github.com/spiffe/spiffe/blob/main/standards/SPIFFE_Workload_Endpoint.md">SPIFFE Workload
     * Endpoint</a> specification.
     * <p>
     * If not set, the standard {@code SPIFFE_ENDPOINT_SOCKET} environment variable is used as a fallback,
     * as defined by the
     * <a href="https://spiffe.io/docs/latest/spiffe-specs/spiffe_workload_endpoint/#4-locating-the-endpoint">
     * SPIFFE Workload Endpoint &sect;4 &ndash; Locating the Endpoint</a> specification.
     * <p>
     * Example values: {@code unix:///run/spire/sockets/agent.sock}, {@code tcp://127.0.0.1:8080}
     */
    @WithDefault("${SPIFFE_ENDPOINT_SOCKET:}")
    Optional<@WithConverter(SpiffeEndpointSocketConverter.class) URI> endpointSocket();

    /**
     * Default audience values embedded into the JWT {@code aud} claim when fetching JWT-SVIDs.
     * The first value is the primary audience; additional values identify further intended recipients.
     * <p>
     * These audiences are used when {@link io.quarkus.spiffe.client.api.SpiffeClient#fetchJwtSvid()} is called
     * without an explicit request, or when a {@link io.quarkus.spiffe.client.api.JwtSvidRequest} specifies no
     * audiences. Audiences specified in the request take precedence over these configured defaults.
     */
    Optional<List<@WithConverter(TrimmedStringConverter.class) String>> audiences();
}
