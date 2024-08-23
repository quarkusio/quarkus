package io.quarkus.grpc.server.devmode;

import jakarta.enterprise.context.RequestScoped;

import devmodetest.v1.Devmodetest;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.GrpcService;

@GrpcService
public class DevModeTestService extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        String greeting = "Hello, ";
        String response;
        if (request.getName().equals("HACK_TO_GET_STATUS_NUMBER")) {
            response = Integer.toString(Devmodetest.DevModeResponse.Status.TEST_ONE.getNumber());
        } else {
            response = greeting + request.getName();
        }
        if (Arc.container().getActiveContext(RequestScoped.class) != null) {
            responseObserver.onNext(HelloReply.newBuilder().setMessage(response).build());
        } else {
            throw new IllegalStateException("request context not active, failing");
        }
        responseObserver.onCompleted();
    }
}
