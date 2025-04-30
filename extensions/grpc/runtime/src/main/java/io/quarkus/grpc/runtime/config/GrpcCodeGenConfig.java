package io.quarkus.grpc.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * not really used, here only to describe config options for code generation
 */
@ConfigMapping(prefix = "quarkus.generate-code.grpc")
@ConfigRoot(phase = ConfigPhase.BUILD_TIME)
public interface GrpcCodeGenConfig {

    /**
     * gRPC code generation can scan dependencies of the application for proto files to generate Java stubs from.
     * This property sets the scope of the dependencies to scan.
     * Applicable values:
     * <ul>
     * <li><i>none</i> - default - don't scan dependencies</li>
     * <li>a comma separated list of <i>groupId:artifactId</i> coordinates to scan</li>
     * <li><i>all</i> - scan all dependencies</li>
     * </ul>
     */
    @WithDefault("none")
    String scanForProto();

    /**
     * Specify the dependencies that are allowed to have proto files that can be imported by this application's protos
     * <p>
     * Applicable values:
     * <ul>
     * <li><i>none</i> - default - don't scan dependencies</li>
     * <li>a comma separated list of <i>groupId:artifactId</i> coordinates to scan</li>
     * <li><i>all</i> - scan all dependencies</li>
     * </ul>
     * <p>
     * By default, <i>com.google.protobuf:protobuf-java</i>.
     */
    @WithDefault("com.google.protobuf:protobuf-java")
    String scanForImports();

    /**
     * Controls whether Kotlin code is generated when the {@code quarkus-kotlin} extension is present (in which case the default
     * is {@code true}).
     */
    @WithName("kotlin.generate")
    Optional<Boolean> generateKotlin();
}
