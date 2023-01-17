package com.example.grpc.exc;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class LegacyHelloGrpcService extends LegacyHelloGrpcGrpc.LegacyHelloGrpcImplBase {
    @Override
    public void legacySayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        // it NEEDS to be plain StatusException, and NOT StatusRuntimeException ?!
        final StatusException t = new StatusException(Status.INVALID_ARGUMENT);
        responseObserver.onError(t);
    }
}
