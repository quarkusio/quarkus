package io.quarkus.grpc.runtime;

import io.vertx.grpc.VertxServer;

public class GrpcServerHolder {

    public static volatile VertxServer server;

}
