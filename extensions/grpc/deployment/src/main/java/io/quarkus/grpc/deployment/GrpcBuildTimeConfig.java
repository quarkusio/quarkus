package io.quarkus.grpc.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.grpc")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface GrpcBuildTimeConfig {

    /**
     * Configuration gRPC dev mode.
     */
    @ConfigDocSection(generated = true)
    GrpcDevModeConfig devMode();

    /**
     * gRPC code generation configuration.
     */
    @ConfigDocSection(generated = true)
    GrpcCodegenConfig codegen();

    @ConfigGroup
    interface GrpcDevModeConfig {

        /**
         * Start gRPC server in dev mode even if no gRPC services are implemented.
         * By default set to `true` to ease incremental development of new services using dev mode.
         */
        @WithDefault("true")
        boolean forceServerStart();
    }

    @ConfigGroup
    interface GrpcCodegenConfig {

        /**
         * Directory containing the proto files to compile.
         * <p>
         * By default, proto files are expected in {@code src/main/proto}.
         * <p>
         * This property is typically set in the build descriptor (Maven plugin properties or Gradle
         * {@code quarkusBuildProperties}).
         */
        Optional<String> protoDirectory();

        /**
         * Skip gRPC code generation.
         */
        @WithDefault("false")
        boolean skip();
    }
}
