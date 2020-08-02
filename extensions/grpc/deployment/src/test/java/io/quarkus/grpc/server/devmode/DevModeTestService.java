package io.quarkus.grpc.server.devmode;

import javax.inject.Singleton;

import devmodetest.v1.Devmodetest;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.stub.StreamObserver;

@Singleton
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
        responseObserver.onNext(HelloReply.newBuilder().setMessage(response).build());
        responseObserver.onCompleted();
    }
}