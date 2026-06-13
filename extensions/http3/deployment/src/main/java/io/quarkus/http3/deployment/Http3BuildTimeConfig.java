package io.quarkus.http3.deployment;

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
     * The header value follows the format {@code h3=":PORT"; ma=86400}, where PORT
     * is the HTTPS port the server is listening on.
     * <p>
     * Only effective when HTTP/3 is enabled.
     */
    @WithDefault("true")
    boolean altSvc();

}
