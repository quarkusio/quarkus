package io.quarkus.grpc.runtime.config;

import static io.quarkus.runtime.LaunchMode.TEST;
import static io.quarkus.runtime.annotations.ConfigPhase.RUN_TIME;

import java.util.Map;
import java.util.OptionalInt;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigDocIgnore;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.grpc")
@ConfigRoot(phase = RUN_TIME)
public interface GrpcConfig {
    @ConfigDocIgnore
    GrpcServer server();

    @ConfigDocIgnore
    Map<String, GrpcClient> clients();

    @ConfigDocIgnore
    @WithParentName
    Map<String, String> properties();

    interface GrpcServer {
        @WithDefault("true")
        boolean useSeparateServer();

        @ConfigDocIgnore
        @WithDefault("9000")
        int port();

        @ConfigDocIgnore
        @WithDefault("9001")
        int testPort();

        default int determinePort(LaunchMode mode) {
            return mode.equals(TEST) ? testPort() : port();
        }

        @ConfigDocIgnore
        @WithDefault("0.0.0.0")
        String host();

        @ConfigDocIgnore
        @WithDefault("true")
        boolean plainText();

        @ConfigDocIgnore
        Map<String, String> ssl();

        @ConfigDocIgnore
        @WithParentName
        Map<String, String> properties();
    }

    interface GrpcClient {
        @ConfigDocIgnore
        @WithDefault("9000")
        int port();

        /**
         * A duplicate mapping of {@link GrpcClient#port()}, to retrieve the full configuration key. We may need
         * the original property name to reassign the port, and because the client configuration contains a dynamic path
         * segment, we can avoid reconstructing the original property name from the
         * {@link GrpcConfig#clients()} <code>Map</code>.
         */
        @ConfigDocIgnore
        @WithName("port")
        ConfigValue portName();

        @ConfigDocIgnore
        OptionalInt testPort();

        /**
         * A duplicate mapping of {@link GrpcClient#testPort()}, to retrieve the full configuration key. We may need
         * the original property name to reassign the port, and because the client configuration contains a dynamic path
         * segment, we can avoid reconstructing the original property name from the
         * {@link GrpcConfig#clients()} <code>Map</code>.
         */
        @ConfigDocIgnore
        @WithName("test-port")
        ConfigValue testPortName();

        default int determinePort(LaunchMode mode, int defaultPort) {
            return mode.equals(TEST) ? testPort().orElse(defaultPort) : port();
        }

        @ConfigDocIgnore
        @WithDefault("localhost")
        String host();

        @ConfigDocIgnore
        @WithParentName
        Map<String, String> properties();
    }
}
