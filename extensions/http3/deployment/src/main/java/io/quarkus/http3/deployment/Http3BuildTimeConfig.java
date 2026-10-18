package io.quarkus.http3.deployment;

import java.time.Duration;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.http3")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface Http3BuildTimeConfig {

    /**
     * Whether HTTP/3 (QUIC) support is enabled.
     * <p>
     * When enabled and the native QUIC library is on the classpath,
     * HTTP/3 is added to the set of supported HTTP versions on the HTTPS server.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * Whether to add the {@code Alt-Svc} response header to HTTP/1.1 and HTTP/2 responses
     * on the HTTPS server, advertising HTTP/3 availability.
     * <p>
     * The header value follows the format {@code h3=":PORT"; ma=MAX_AGE}, where PORT
     * is the HTTPS port the server is listening on and MAX_AGE is controlled by
     * {@link #altSvcMaxAge()}.
     * <p>
     * Only effective when HTTP/3 is enabled.
     */
    @WithDefault("true")
    boolean altSvc();

    /**
     * The {@code max-age} value (in seconds) included in the {@code Alt-Svc} header.
     * <p>
     * This tells clients how long they should cache the HTTP/3 advertisement before
     * re-checking via HTTP/1.1 or HTTP/2.
     * <p>
     * Only effective when {@link #altSvc()} is {@code true}.
     */
    @WithDefault("24H")
    Duration altSvcMaxAge();

}
