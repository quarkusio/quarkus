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

}
