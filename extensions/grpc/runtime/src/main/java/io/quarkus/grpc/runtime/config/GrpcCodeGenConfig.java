package io.quarkus.grpc.runtime.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * not really used, here only to describe config options for code generation
 */
@ConfigRoot(name = "generate-code.grpc", phase = ConfigPhase.BUILD_TIME)
public class GrpcCodeGenConfig {

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
    @ConfigItem(defaultValue = "none")
    public String scanForProto;

    /**
     * Specify the dependencies that are allowed to have proto files that can be imported by this application's protos
     *
     * Applicable values:
     * <ul>
     * <li><i>none</i> - default - don't scan dependencies</li>
     * <li>a comma separated list of <i>groupId:artifactId</i> coordinates to scan</li>
     * <li><i>all</i> - scan all dependencies</li>
     * </ul>
     *
     * By default, <i>com.google.protobuf:protobuf-java</i>.
     */
    @ConfigItem(defaultValue = "com.google.protobuf:protobuf-java")
    public String scanForImports;
}
