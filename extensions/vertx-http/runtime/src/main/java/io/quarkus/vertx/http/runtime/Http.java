package io.quarkus.vertx.http.runtime;

import org.eclipse.microprofile.config.ConfigProvider;

import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.http")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface Http {
    /**
     * The HTTP host
     * <p>
     * In dev/test mode this defaults to localhost, in prod mode this defaults to 0.0.0.0
     * <p>
     * Defaulting to 0.0.0.0 makes it easier to deploy Quarkus to container, however it
     * is not suitable for dev/test mode as other people on the network can connect to your
     * development machine.
     */
    @ConfigDocIgnore
    String host();

    /**
     * The HTTP port
     */
    @ConfigDocIgnore
    @WithDefault("8080")
    int port();

    /**
     * The HTTPS port
     */
    @ConfigDocIgnore
    @WithDefault("8443")
    int sslPort();

    static Http get() {
        return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getConfigMapping(Http.class);
    }
}
