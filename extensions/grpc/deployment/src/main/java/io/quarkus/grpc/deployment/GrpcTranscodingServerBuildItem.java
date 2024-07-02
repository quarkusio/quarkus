package io.quarkus.grpc.deployment;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.grpc.transcoding.GrpcTranscodingServer;
import io.quarkus.runtime.RuntimeValue;

public final class GrpcTranscodingServerBuildItem extends SimpleBuildItem {
    private final RuntimeValue<GrpcTranscodingServer> transcodingServer;

    public GrpcTranscodingServerBuildItem(RuntimeValue<GrpcTranscodingServer> transcodingServer) {
        this.transcodingServer = transcodingServer;
    }

    public RuntimeValue<GrpcTranscodingServer> getTranscodingServer() {
        return transcodingServer;
    }
}
