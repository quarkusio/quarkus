package io.quarkus.spiffe.client.runtime.internal;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.quarkus.spiffe.client.SpiffeClient;
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
     * Default audience values that a SPIFFE JSON Web Token (JWT-SVID) is required to contain in its audience {@code aud} claim.
     * These audiences are supplied to the Workload API when no audiences are directly provided to
     * {@link SpiffeClient} methods for retrieving workload tokens.
     */
    Optional<Set<@WithConverter(TrimmedStringConverter.class) String>> audiences();

    /**
     * The duration after which a request to the SPIFFE Workload API times out.
     */
    @WithDefault("10s")
    Duration requestTimeout();

    /**
     * Configuration of the X.509-SVID that backs the TLS configurations declared with {@code @X509Svid}.
     */
    X509SvidConfig x509Svid();

    interface X509SvidConfig {

        /**
         * How often the X.509-SVID and the trust bundle are fetched from the SPIFFE Workload API.
         * <p>
         * The SPIRE agent issues a new X.509-SVID once about half of the lifetime of the current one has passed, and
         * the workload has to fetch it before the current one expires. The period must therefore stay well below the
         * SVID lifetime, one hour by default in SPIRE. A quarter of the lifetime still guarantees a fetch after the
         * rotation, so a period above a quarter of the lifetime of the fetched SVID is reported with a warning.
         * <p>
         * A failed fetch is retried once after ten seconds when the period is a minute or longer, otherwise the next
         * period is the retry. The current SVID stays in use until it expires or until the Workload API denies the
         * workload.
         */
        @WithDefault("5m")
        Duration refreshPeriod();
    }
}
