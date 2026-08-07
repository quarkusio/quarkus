package io.quarkus.spiffe.client.deployment;

import java.util.Optional;
import java.util.OptionalInt;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.spiffe-client")
@ConfigRoot
interface SpiffeClientBuildTimeConfig {

    /**
     * If SPIFFE Workload API client is enabled.
     */
    @WithDefault("true")
    boolean enabled();

    /**
     * SPIFFE Dev Services config.
     */
    DevServices devservices();

    /**
     * SPIFFE Dev Services config.
     */
    interface DevServices {

        /**
         * Flag to enable (default) or disable Dev Services.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Transport protocol for the SPIFFE Workload API server.
         */
        @ConfigDocDefault("Defaults to `tcp` on Windows and `unix` on all other platforms.")
        Optional<Transport> transport();

        /**
         * The specific port for the bundle HTTP server to listen on.
         * <p>
         * If not specified, a random port is selected.
         */
        OptionalInt httpPort();

        enum Transport {
            TCP,
            UNIX
        }

    }
}
